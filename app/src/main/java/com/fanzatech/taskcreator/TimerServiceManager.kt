package com.fanzatech.taskcreator

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder

object TimerServiceManager {
    private var timerService: TimerService? = null
    private var isServiceBound = false
    private var context: Context? = null
    private var currentRunningTaskId: Int = -1
    private var isRunning: Boolean = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as TimerService.TimerBinder
            timerService = binder.getService()
            isServiceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isServiceBound = false
            timerService = null
        }
    }

    fun initialize(context: Context) {
        this.context = context.applicationContext
        val intent = Intent(this.context, TimerService::class.java)
        this.context?.startService(intent)
    }

    fun bindService() {
        context?.let {
            val intent = Intent(it, TimerService::class.java)
            it.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    fun unbindService() {
        if (isServiceBound) {
            context?.unbindService(serviceConnection)
            isServiceBound = false
        }
    }

    fun startTimer(taskId: Int, callback: (Long) -> Unit) {
        if (!isServiceBound) {
            bindService()
        }
        currentRunningTaskId = taskId
        isRunning = true
        timerService?.startTimer(taskId, callback)
    }

    fun stopTimer(): Long {
        currentRunningTaskId = -1
        isRunning = false
        return timerService?.stopTimer() ?: 0L
    }

    fun pauseTimer(): Long {
        isRunning = false
        return timerService?.pauseTimer() ?: 0L
    }

    fun resumeTimer(taskId: Int, callback: (Long) -> Unit) {
        if (!isServiceBound) {
            bindService()
        }
        currentRunningTaskId = taskId
        isRunning = true
        timerService?.resumeTimer(taskId, callback)
    }

    fun getCurrentTaskId(): Int = currentRunningTaskId

    fun isTimerRunning(): Boolean = isRunning
}

