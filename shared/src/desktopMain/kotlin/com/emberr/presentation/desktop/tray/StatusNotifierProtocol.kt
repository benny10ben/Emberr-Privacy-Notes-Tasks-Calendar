package com.emberr.presentation.desktop.tray

import org.freedesktop.dbus.Struct
import org.freedesktop.dbus.Tuple
import org.freedesktop.dbus.annotations.DBusInterfaceName
import org.freedesktop.dbus.annotations.Position
import org.freedesktop.dbus.interfaces.DBusInterface
import org.freedesktop.dbus.types.UInt32
import org.freedesktop.dbus.types.Variant

const val STATUS_NOTIFIER_WATCHER_NAME = "org.kde.StatusNotifierWatcher"
const val STATUS_NOTIFIER_WATCHER_PATH = "/StatusNotifierWatcher"
const val STATUS_NOTIFIER_ITEM_PATH = "/StatusNotifierItem"
const val TRAY_MENU_PATH = "/MenuBar"

@DBusInterfaceName("org.kde.StatusNotifierWatcher")
interface StatusNotifierWatcher : DBusInterface {
    fun RegisterStatusNotifierItem(service: String)
}

@DBusInterfaceName("org.kde.StatusNotifierItem")
interface StatusNotifierItem : DBusInterface {
    fun Activate(x: Int, y: Int)
    fun SecondaryActivate(x: Int, y: Int)
    fun ContextMenu(x: Int, y: Int)
    fun Scroll(delta: Int, orientation: String)
}

@DBusInterfaceName("com.canonical.dbusmenu")
interface DBusMenu : DBusInterface {
    fun GetLayout(
        parentId: Int,
        recursionDepth: Int,
        propertyNames: List<String>
    ): DBusPair<UInt32, MenuItemLayout>
    fun GetGroupProperties(ids: List<Int>, propertyNames: List<String>): List<MenuItemProperties>
    fun GetProperty(id: Int, name: String): Variant<*>
    fun Event(id: Int, eventId: String, data: Variant<*>, timestamp: UInt32)
    fun EventGroup(events: List<MenuEvent>): List<Int>
    fun AboutToShow(id: Int): Boolean
}

class TrayIconPixmap(
    @Position(0) val width: Int,
    @Position(1) val height: Int,
    @Position(2) val argbBytes: ByteArray
) : Struct()

class MenuItemLayout(
    @Position(0) val id: Int,
    @Position(1) val properties: Map<String, Variant<*>>,
    @Position(2) val children: List<Variant<*>>
) : Struct()

class MenuItemProperties(
    @Position(0) val id: Int,
    @Position(1) val properties: Map<String, Variant<*>>
) : Struct()

class MenuEvent(
    @Position(0) val id: Int,
    @Position(1) val eventId: String,
    @Position(2) val data: Variant<*>,
    @Position(3) val timestamp: UInt32
) : Struct()

class DBusPair<A, B>(
    @Position(0) val first: A,
    @Position(1) val second: B
) : Tuple()

