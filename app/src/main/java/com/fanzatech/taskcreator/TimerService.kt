package com.fanzatech.taskcreator

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import kotlinx.coroutines.*

class TimerService : Service() {
    private val binder = TimerBinder()
    private var timerJob: Job? = null
    private var currentTaskId: Int = -1
    private var taskStartTime: Long = 0L
    private var onTimerTick: ((Long) -> Unit)? = null

    inner class TimerBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    fun startTimer(taskId: Int, callback: (Long) -> Unit) {
        onTimerTick = callback
        currentTaskId = taskId
        taskStartTime = System.currentTimeMillis()

        timerJob?.cancel()
        timerJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                val elapsed = System.currentTimeMillis() - taskStartTime
                onTimerTick?.invoke(elapsed)
                delay(200)
            }
        }
    }

    fun stopTimer(): Long {
        timerJob?.cancel()
        timerJob = null
        val elapsed = if (taskStartTime > 0) System.currentTimeMillis() - taskStartTime else 0L
        taskStartTime = 0L
        currentTaskId = -1
        return elapsed
    }

    fun pauseTimer(): Long {
        timerJob?.cancel()
        timerJob = null
        val elapsed = if (taskStartTime > 0) System.currentTimeMillis() - taskStartTime else 0L
        return elapsed
    }

    fun resumeTimer(taskId: Int, callback: (Long) -> Unit) {
        onTimerTick = callback
        currentTaskId = taskId
        taskStartTime = System.currentTimeMillis()

        timerJob?.cancel()
        timerJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                val elapsed = System.currentTimeMillis() - taskStartTime
                onTimerTick?.invoke(elapsed)
                delay(200)
            }
        }
    }

    fun getCurrentTaskId(): Int = currentTaskId

    override fun onDestroy() {
        super.onDestroy()
        timerJob?.cancel()
    }
}

