package com.myspeechy.myspeechy

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.lifecycleScope
import com.myspeechy.myspeechy.data.DataStoreManager
import com.myspeechy.myspeechy.data.authDataStore
import com.myspeechy.myspeechy.data.loggedOutDataStore
import com.myspeechy.myspeechy.domain.auth.AuthService
import com.myspeechy.myspeechy.domain.chat.DirectoryManager
import com.myspeechy.myspeechy.ui.theme.MySpeechyTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var darkTheme = mutableStateOf(false)

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
        Log.d("LOADED", "clearing")
        runBlocking {
            dataStoreManager.editError("")
            dataStoreManager.showNavBar(false)
            dataStoreManager.onDataLoad(false)
        }
    }
    private fun collectThemeMode() {
        lifecycleScope.launch {
            dataStoreManager.collectThemeMode {
                darkTheme.value = it
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        listenForAuthState()
        createNotificationChannel()
        collectThemeMode()
        if (savedInstanceState == null) {
            clearDataStore()
        }
        setContent {
            MySpeechyTheme(darkTheme = darkTheme.value) {
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
