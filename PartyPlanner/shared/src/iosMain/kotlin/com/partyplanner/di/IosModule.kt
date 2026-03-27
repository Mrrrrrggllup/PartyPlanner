package com.partyplanner.di

import com.partyplanner.data.local.DriverFactory
import org.koin.dsl.module

val iosModule = module {
    single { DriverFactory() }
}