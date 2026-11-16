package com.isep.swapcampus

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.*
import com.isep.composeapp.App
import com.isep.shared.data.AppContainer

class MainActivity : AppCompatActivity() {
    private val channelId = "listing_alerts"
    private val shownIds = mutableSetOf<Int>()
    private var lastUserId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                channelId,
                "Listing alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )

        setContent {
            val user by AppContainer.authRepository.currentUser.collectAsState()
            val alerts by AppContainer.listingRepository.notifications.collectAsState()

            var permissionGranted by remember {
                mutableStateOf(
                    Build.VERSION.SDK_INT < 33 ||
                            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                            PackageManager.PERMISSION_GRANTED
                )
            }
            val permissionRequest = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { granted ->
                permissionGranted = granted
            }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= 33 && !permissionGranted) {
                    permissionRequest.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            LaunchedEffect(user?.id, alerts, permissionGranted) {
                if (lastUserId != user?.id) {
                    manager.cancelAll()
                    lastUserId = user?.id
                }

                val currentUser = user
                if (permissionGranted && currentUser != null) {
                    alerts.filter {
                        it.recipientId == currentUser.id && it.id !in shownIds
                    }.forEach { alert ->
                        val notification =
                            Notification.Builder(this@MainActivity, channelId)
                                .setSmallIcon(android.R.drawable.ic_dialog_info)
                                .setContentTitle("SwapCampus")
                                .setContentText(alert.message)
                                .setAutoCancel(true)
                                .build()

                        manager.notify(alert.id, notification)
                        shownIds.add(alert.id)
                    }
                }
            }

            App()
        }
    }
}