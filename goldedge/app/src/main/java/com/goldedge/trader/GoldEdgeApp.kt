package com.goldedge.trader

import android.app.Application

class GoldEdgeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NewsAlertScheduler.ensureScheduled(this)
    }
}
