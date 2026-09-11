package com.autoanswer.app

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.util.Log

class KeepAliveJob : JobService() {

    companion object {
        private const val JOB_ID = 1002

        fun schedule(c: Context) {
            val scheduler = c.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            val info = JobInfo.Builder(JOB_ID, ComponentName(c, KeepAliveJob::class.java))
                .setPeriodic(15 * 60 * 1000L)
                .setPersisted(true)
                .build()
            scheduler.schedule(info)
        }
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        Log.i("KeepAliveJob", "wake up, service running=${CallMonitorService.running}")
        if (!CallMonitorService.running) {
            runCatching { CallMonitorService.start(this) }
        }
        return false
    }

    override fun onStopJob(params: JobParameters?): Boolean = false
}
