// Watches the stored task list and pushes its changes to the home screen while the app runs.
package com.ben.emberr.presentation.widget.tasks

import android.content.Context
import com.ben.emberr.presentation.widget.WidgetLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

private const val tasksSettleDelayMillis = 150L

class TasksWidgetCoordinator(
    private val context: Context,
    private val contentReader: TasksWidgetContentReader
) {
    private val coordinatorScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @OptIn(FlowPreview::class)
    fun start() {
        coordinatorScope.launch {
            try {
                contentReader.observeTasks()
                    .distinctUntilChanged()
                    .debounce(tasksSettleDelayMillis)
                    .collect { tasks -> pushTasksToWidgets(context, tasks) }
            } catch (cause: Exception) {
                WidgetLog.e("Stopped watching the task list", cause)
            }
        }
    }
}
