package com.partyplanner

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.arkivanov.decompose.retainedComponent
import com.google.firebase.messaging.FirebaseMessaging
import com.partyplanner.data.remote.UserApi
import com.partyplanner.domain.repository.AuthRepository
import com.partyplanner.domain.usecase.auth.LoginUseCase
import com.partyplanner.domain.usecase.auth.RegisterUseCase
import com.partyplanner.presentation.root.DefaultRootComponent
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get

class MainActivity : ComponentActivity() {

    private val userApi: UserApi by lazy { get() }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val inviteToken = intent?.data?.let { uri ->
            if (uri.scheme == "partyplanner" && uri.host == "invite") uri.lastPathSegment
            else null
        }
        val resetToken = intent?.data?.let { uri ->
            if (uri.scheme == "partyplanner" && uri.host == "reset-password") uri.getQueryParameter("token")
            else null
        }

        val root = retainedComponent {
            DefaultRootComponent(
                componentContext   = it,
                authRepository     = get<AuthRepository>(),
                loginUseCase       = get<LoginUseCase>(),
                registerUseCase    = get<RegisterUseCase>(),
                initialInviteToken = inviteToken,
                initialResetToken  = resetToken,
            )
        }

        setContent {
            App(root)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
        }

        // Register FCM token — fire-and-forget, silently skipped if not logged in
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            lifecycleScope.launch { runCatching { userApi.registerDeviceToken(token) } }
        }
    }

    override fun onResume() {
        super.onResume()
        // Reset pending notification flag so the backend doesn't send another push
        // while the app is in the foreground
        lifecycleScope.launch { runCatching { userApi.markActive() } }
    }
}
