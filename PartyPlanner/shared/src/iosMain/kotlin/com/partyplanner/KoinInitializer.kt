package com.partyplanner

import com.partyplanner.di.iosModule
import com.partyplanner.di.sharedModule
import org.koin.core.context.startKoin

// Called from Swift in the iOS app entry point
fun initKoin() {
    startKoin {
        modules(sharedModule, iosModule)
    }
}