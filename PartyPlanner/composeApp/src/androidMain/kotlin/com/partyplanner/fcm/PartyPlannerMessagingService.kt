package com.partyplanner.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.partyplanner.MainActivity
import com.partyplanner.R
import com.partyplanner.data.remote.UserApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class PartyPlannerMessagingService : FirebaseMessagingService() {

    private val userApi: UserApi by inject()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onNewToken(token: String) {
        scope.launch { runCatching { userApi.registerDeviceToken(token) } }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        // App is in foreground — show a heads-up notification manually
        val notification = message.notification ?: return
        showNotification(notification.title ?: "PartyPlanner", notification.body ?: "")
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private fun showNotification(title: String, body: String) {
        val channelId = CHANNEL_ID
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(channelId, "PartyPlanner", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notif = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(NOTIFICATION_ID, notif)
    }

    companion object {
        private const val CHANNEL_ID = "partyplanner_events"
        private const val NOTIFICATION_ID = 1001
    }
}
