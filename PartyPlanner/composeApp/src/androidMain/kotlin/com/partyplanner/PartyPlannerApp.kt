package com.partyplanner

import android.app.Application
import com.partyplanner.di.androidModule
import com.partyplanner.di.sharedModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class PartyPlannerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@PartyPlannerApp)
            modules(sharedModule, androidModule)
        }
    }
}