package com.emberr.presentation.desktop.tray

import com.emberr.presentation.desktop.TrayMenuAction
import java.awt.Image
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import org.freedesktop.dbus.DBusPath
import org.freedesktop.dbus.connections.impl.DBusConnection
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder
import org.freedesktop.dbus.interfaces.Properties
import org.freedesktop.dbus.types.UInt32
import org.freedesktop.dbus.types.Variant

private const val TRAY_ICON_SIZE = 48
private const val MENU_REVISION = 1

internal class TrayItemObject(
    private val iconPixmap: TrayIconPixmap?,
    private val tooltip: String,
    private val onActivate: () -> Unit
) : StatusNotifierItem, Properties {

    private val itemProperties: Map<String, Variant<*>> = buildMap {
        put("Category", Variant("ApplicationStatus"))
        put("Id", Variant("emberr"))
        put("Title", Variant(tooltip))
        put("Status", Variant("Active"))
        put("IconName", Variant(""))
        put("ItemIsMenu", Variant(true))
        put("Menu", Variant(DBusPath(TRAY_MENU_PATH)))
        if (iconPixmap != null) put("IconPixmap", Variant(listOf(iconPixmap), "a(iiay)"))
    }

    override fun getObjectPath(): String = STATUS_NOTIFIER_ITEM_PATH

    override fun isRemote(): Boolean = false

    override fun Activate(x: Int, y: Int) = onActivate()

    override fun SecondaryActivate(x: Int, y: Int) = onActivate()

    override fun ContextMenu(x: Int, y: Int) = Unit

    override fun Scroll(delta: Int, orientation: String) = Unit

    @Suppress("UNCHECKED_CAST")
    override fun <A : Any?> Get(interfaceName: String, propertyName: String): A =
        itemProperties[propertyName] as A

    override fun <A : Any?> Set(interfaceName: String, propertyName: String, value: A) = Unit

    override fun GetAll(interfaceName: String): Map<String, Variant<*>> = itemProperties
}

internal class TrayMenuObject(
    private val currentActions: () -> List<TrayMenuAction>
) : DBusMenu, Properties {

    private val menuProperties: Map<String, Variant<*>> = mapOf(
        "Version" to Variant(UInt32(3)),
        "Status" to Variant("normal"),
        "TextDirection" to Variant("ltr"),
        "IconThemePath" to Variant(emptyList<String>(), "as")
    )

    override fun getObjectPath(): String = TRAY_MENU_PATH

    override fun isRemote(): Boolean = false

    @Suppress("UNCHECKED_CAST")
    override fun <A : Any?> Get(interfaceName: String, propertyName: String): A =
        menuProperties[propertyName] as A

    override fun <A : Any?> Set(interfaceName: String, propertyName: String, value: A) = Unit

    override fun GetAll(interfaceName: String): Map<String, Variant<*>> = menuProperties

    private fun propertiesFor(action: TrayMenuAction): Map<String, Variant<*>> = mapOf(
        "label" to Variant(action.label),
        "enabled" to Variant(true),
        "visible" to Variant(true),
        "type" to Variant("standard")
    )

    override fun GetLayout(
        parentId: Int,
        recursionDepth: Int,
        propertyNames: List<String>
    ): DBusPair<UInt32, MenuItemLayout> {
        val children = currentActions().mapIndexed { index, action ->
            Variant(
                MenuItemLayout(index + 1, propertiesFor(action), emptyList()),
                "(ia{sv}av)"
            )
        }
        val root = MenuItemLayout(
            0,
            mapOf("children-display" to Variant("submenu")),
            children
        )
        return DBusPair(UInt32(MENU_REVISION.toLong()), root)
    }

    override fun GetGroupProperties(
        ids: List<Int>,
        propertyNames: List<String>
    ): List<MenuItemProperties> = currentActions()
        .mapIndexed { index, action -> MenuItemProperties(index + 1, propertiesFor(action)) }
        .filter { ids.isEmpty() || ids.contains(it.id) }

    override fun GetProperty(id: Int, name: String): Variant<*> {
        val action = currentActions().getOrNull(id - 1) ?: return Variant("")
        return propertiesFor(action)[name] ?: Variant("")
    }

    override fun Event(id: Int, eventId: String, data: Variant<*>, timestamp: UInt32) {
        if (eventId != "clicked") return
        currentActions().getOrNull(id - 1)?.onSelected?.invoke()
    }

    override fun EventGroup(events: List<MenuEvent>): List<Int> {
        events.forEach { menuEvent ->
            Event(menuEvent.id, menuEvent.eventId, menuEvent.data, menuEvent.timestamp)
        }
        return emptyList()
    }

    override fun AboutToShow(id: Int): Boolean = false
}

private fun loadTrayIcon(iconResourcePath: String): TrayIconPixmap? = runCatching {
    val iconStream = TrayMenuAction::class.java.classLoader.getResourceAsStream(iconResourcePath)
        ?: return null
    val originalIcon = iconStream.use { ImageIO.read(it) } ?: return null

    val scaledIcon = BufferedImage(TRAY_ICON_SIZE, TRAY_ICON_SIZE, BufferedImage.TYPE_INT_ARGB)
    val canvas = scaledIcon.createGraphics()
    canvas.drawImage(
        originalIcon.getScaledInstance(TRAY_ICON_SIZE, TRAY_ICON_SIZE, Image.SCALE_SMOOTH),
        0,
        0,
        null
    )
    canvas.dispose()

    val argbBytes = ByteArray(TRAY_ICON_SIZE * TRAY_ICON_SIZE * 4)
    var writeAt = 0
    for (y in 0 until TRAY_ICON_SIZE) {
        for (x in 0 until TRAY_ICON_SIZE) {
            val pixel = scaledIcon.getRGB(x, y)
            argbBytes[writeAt++] = (pixel ushr 24).toByte()
            argbBytes[writeAt++] = (pixel ushr 16).toByte()
            argbBytes[writeAt++] = (pixel ushr 8).toByte()
            argbBytes[writeAt++] = pixel.toByte()
        }
    }

    TrayIconPixmap(TRAY_ICON_SIZE, TRAY_ICON_SIZE, argbBytes)
}.getOrNull()

class StatusNotifierTray private constructor(
    private val connection: DBusConnection,
    private val busName: String
) {
    fun stop() {
        runCatching { connection.releaseBusName(busName) }
        runCatching { connection.disconnect() }
    }

    companion object {
        fun start(
            iconResourcePath: String,
            tooltip: String,
            onActivate: () -> Unit,
            currentActions: () -> List<TrayMenuAction>
        ): StatusNotifierTray? = runCatching {
            val connection = DBusConnectionBuilder.forSessionBus().build()
            val busName = "org.kde.StatusNotifierItem-${ProcessHandle.current().pid()}-1"

            connection.requestBusName(busName)
            connection.exportObject(
                STATUS_NOTIFIER_ITEM_PATH,
                TrayItemObject(loadTrayIcon(iconResourcePath), tooltip, onActivate)
            )
            connection.exportObject(TRAY_MENU_PATH, TrayMenuObject(currentActions))

            val watcher = connection.getRemoteObject(
                STATUS_NOTIFIER_WATCHER_NAME,
                STATUS_NOTIFIER_WATCHER_PATH,
                StatusNotifierWatcher::class.java,
                true
            )
            watcher.RegisterStatusNotifierItem(busName)

            StatusNotifierTray(connection, busName)
        }.onFailure { it.printStackTrace() }.getOrNull()
    }
}
