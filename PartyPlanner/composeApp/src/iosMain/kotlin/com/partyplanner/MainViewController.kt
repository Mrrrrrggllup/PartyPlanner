package com.partyplanner

import androidx.compose.ui.window.ComposeUIViewController
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.ApplicationLifecycle
import com.partyplanner.domain.repository.AuthRepository
import com.partyplanner.domain.usecase.auth.LoginUseCase
import com.partyplanner.domain.usecase.auth.RegisterUseCase
import com.partyplanner.presentation.root.DefaultRootComponent
import org.koin.mp.KoinPlatform

fun MainViewController() = ComposeUIViewController {
    val koin = KoinPlatform.getKoin()
    val root = DefaultRootComponent(
        componentContext = DefaultComponentContext(lifecycle = ApplicationLifecycle()),
        authRepository = koin.get<AuthRepository>(),
        loginUseCase = koin.get<LoginUseCase>(),
        registerUseCase = koin.get<RegisterUseCase>()
    )
    App(root)
}