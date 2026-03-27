package com.partyplanner.di

import com.partyplanner.data.local.DriverFactory
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidModule = module {
    single { DriverFactory(androidContext()) }
}