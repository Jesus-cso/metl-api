package com.example.metl

import android.app.Application

class MetlApp : Application() {
    override fun onCreate() {
        super.onCreate()
        MetlGraph.init(this)
    }
}