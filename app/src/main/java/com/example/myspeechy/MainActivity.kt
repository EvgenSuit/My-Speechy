package com.example.myspeechy

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.lifecycleScope
import com.example.myspeechy.data.DataStoreManager
import com.example.myspeechy.domain.auth.AuthService
import com.example.myspeechy.domain.chat.DirectoryManager
import com.example.myspeechy.ui.theme.MySpeechyTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var authService: AuthService
    @Inject
    lateinit var dataStoreManager: DataStoreManager
    private fun createNotificationChannel() {
        val name = "Meditation"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel("1", name, importance)
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
    private fun listenForAuthState() {
        authService.listenForAuthState {isLoggedOut ->
            lifecycleScope.launch {
                if (isLoggedOut) {
                    DirectoryManager.clearCache(applicationContext.cacheDir.toString())
                    clearDataStore()
                }
                applicationContext.authDataStore.edit { loggedOutPref ->
                    loggedOutPref[loggedOutDataStore] = isLoggedOut
                }
            }
        }
    }

    private fun clearDataStore() {
        runBlocking {
            dataStoreManager.editError("")
            dataStoreManager.showNavBar(false)
            dataStoreManager.onDataLoad(false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        listenForAuthState()
        createNotificationChannel()
        clearDataStore()
        setContent {
            MySpeechyTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MySpeechyApp()
                }
            }
        }
    }
}
