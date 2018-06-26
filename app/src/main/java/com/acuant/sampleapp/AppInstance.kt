package com.acuant.sampleapp

import android.app.Application
import com.acuant.acuantmobilesdk.Controller

/**
 * Created by tapasbehera on 4/18/18.
 */
class AppInstance : Application() {
    private var controller : Controller? = null
    private var mDefaultUncaughtExceptionHandler: Thread.UncaughtExceptionHandler? = null

    private val mCaughtExceptionHandler = Thread.UncaughtExceptionHandler { thread, ex ->
        if(controller != null){
            controller!!.cleanup()
            controller = null;
        }
        mDefaultUncaughtExceptionHandler!!.uncaughtException(thread, ex)
    }
    companion object {
        lateinit var instance: AppInstance
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        mDefaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(mCaughtExceptionHandler);
    }

    override fun onTerminate() {
        super.onTerminate()
    }

    public fun setController(controller : Controller?){
        this.controller = controller
    }

    public fun getController(): Controller?{
        return controller
    }



}