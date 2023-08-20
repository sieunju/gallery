package com.gallery.example

import android.app.Application
import timber.log.Timber

/**
 * Description :
 *
 * Created by juhongmin on 2022/09/13
 */
class ExampleApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        initTimber()
    }

    /**
     * init Timber
     */
    private fun initTimber() {
        Timber.plant(object : Timber.DebugTree() {
            override fun createStackElementTag(element: StackTraceElement): String {
                val str = StringBuilder()
                str.append("JLOGGER")
                str.append(element.className.substringAfterLast("."))
                str.append(":")
                str.append(element.methodName.substringAfterLast("."))
                return str.toString()
            }
        })
    }
}
