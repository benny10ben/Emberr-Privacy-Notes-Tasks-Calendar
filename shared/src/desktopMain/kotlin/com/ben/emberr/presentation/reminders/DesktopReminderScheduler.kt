package com.ben.emberr.presentation.reminders

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class DesktopReminderScheduler : ReminderScheduler {

    private val scheduler = Executors.newSingleThreadScheduledExecutor()
    private val activeTasks = ConcurrentHashMap<String, ScheduledFuture<*>>()

    private val os = System.getProperty("os.name").lowercase()

    override fun schedule(blockId: String, noteTitle: String, text: String, timestamp: Long) {
        cancel(blockId)
        val delay = timestamp - System.currentTimeMillis()
        if (delay <= 0) return

        val task = scheduler.schedule({
            triggerNativeNotification(noteTitle, text)
            activeTasks.remove(blockId)
        }, delay, TimeUnit.MILLISECONDS)

        activeTasks[blockId] = task
    }

    override fun cancel(blockId: String) {
        activeTasks[blockId]?.cancel(false)
        activeTasks.remove(blockId)
    }

    private fun triggerNativeNotification(title: String, message: String) {
        try {
            when {
                os.contains("win") -> {
                    val psScript = """
                        [Windows.UI.Notifications.ToastNotificationManager, Windows.UI.Notifications, ContentType = WindowsRuntime] | Out-Null
                        [Windows.UI.Notifications.ToastNotification, Windows.UI.Notifications, ContentType = WindowsRuntime] | Out-Null
                        
                        ${'$'}template = [Windows.UI.Notifications.ToastNotificationManager]::GetTemplateContent([Windows.UI.Notifications.ToastTemplateType]::ToastText02)
                        ${'$'}textNodes = ${'$'}template.GetElementsByTagName('text')
                        ${'$'}textNodes.Item(0).AppendChild(${'$'}template.CreateTextNode('$title')) | Out-Null
                        ${'$'}textNodes.Item(1).AppendChild(${'$'}template.CreateTextNode('$message')) | Out-Null
                        
                        ${'$'}notifier = [Windows.UI.Notifications.ToastNotificationManager]::CreateToastNotifier('Emberr')
                        ${'$'}notifier.Show([Windows.UI.Notifications.ToastNotification]::new(${'$'}template))
                    """.trimIndent()

                    ProcessBuilder("powershell", "-NoProfile", "-Command", psScript).start()
                }
                os.contains("mac") -> {
                    val script = "display notification \"$message\" with title \"$title\""
                    ProcessBuilder("osascript", "-e", script).start()
                }
                else -> {
                    ProcessBuilder(
                        "notify-send",
                        "-a", "Emberr",
                        "-u", "normal",
                        title,
                        message
                    ).start()
                }
            }
        } catch (e: Exception) {
            println("Notification failed: ${e.message}")
        }
    }
}