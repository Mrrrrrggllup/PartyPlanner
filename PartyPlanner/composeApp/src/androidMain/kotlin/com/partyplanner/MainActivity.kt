package com.partyplanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.arkivanov.decompose.retainedComponent
import com.partyplanner.domain.repository.AuthRepository
import com.partyplanner.domain.usecase.auth.LoginUseCase
import com.partyplanner.domain.usecase.auth.RegisterUseCase
import com.partyplanner.presentation.root.DefaultRootComponent
import org.koin.android.ext.android.get

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Extract invite token from deep link partyplanner://invite/{token}
        val inviteToken = intent?.data?.let { uri ->
            if (uri.scheme == "partyplanner" && uri.host == "invite") uri.lastPathSegment
            else null
        }

        val root = retainedComponent {
            DefaultRootComponent(
                componentContext = it,
                authRepository   = get<AuthRepository>(),
                loginUseCase     = get<LoginUseCase>(),
                registerUseCase  = get<RegisterUseCase>(),
                initialInviteToken = inviteToken,
            )
        }

        setContent {
            App(root)
        }
    }
}
