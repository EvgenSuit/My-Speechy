package com.myspeechy.myspeechy.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.myspeechy.myspeechy.data.lesson.LessonCategories
import kotlinx.coroutines.flow.collectLatest

val Context.authDataStore: DataStore<Preferences> by preferencesDataStore("Auth")
val Context.navBarDataStore: DataStore<Preferences> by preferencesDataStore("NavBar")
val showNavBarDataStore = booleanPreferencesKey("showNavBar")
val loggedOutDataStore = booleanPreferencesKey("loggedOut")
val errorKey = stringPreferencesKey("error")
val Context.loadData: DataStore<Preferences> by preferencesDataStore("loadingData")
val isDataLoaded = booleanPreferencesKey("isDataLoaded")
val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore("Theme")
val isDarkTheme = booleanPreferencesKey("isDarkTheme")
val Context.notificationsDataStore: DataStore<Preferences> by preferencesDataStore("Notifications")
val isMeditationNotificationCancelled = booleanPreferencesKey("isMeditationNotificationCancelled")
val Context.lessonItemsDataStore: DataStore<Preferences> by preferencesDataStore("LessonItems")
val welcomeDialogBoxShown = stringSetPreferencesKey("welcomeDialogBoxShown")

class DataStoreManager(private val authDataStore: DataStore<Preferences>,
    private val navBarDataStore: DataStore<Preferences>,
    private val loadingDataStore: DataStore<Preferences>,
    private val themeDataStore: DataStore<Preferences>,
    private val notificationsDataStore: DataStore<Preferences>,
    private val lessonItemDataStore: DataStore<Preferences>) {

    suspend fun editWelcomeDialogBoxShown(category: LessonCategories) {
        lessonItemDataStore.edit {
            val currSet = it[welcomeDialogBoxShown] ?: emptySet()
            it[welcomeDialogBoxShown] = currSet + category.name
        }
    }
    suspend fun collectWelcomeDialogBoxShow(onData: (Set<String>) -> Unit) {
        lessonItemDataStore.data.collect {
            onData(it[welcomeDialogBoxShown] ?: emptySet())
        }
    }
    suspend fun editError(error: String) {
        authDataStore.edit {
            it[errorKey] = error
        }
    }
    suspend fun showNavBar(show: Boolean) {
        navBarDataStore.edit {
            it[showNavBarDataStore] = show
        }
    }
    suspend fun onDataLoad(loaded: Boolean) {
        loadingDataStore.edit {
            it[isDataLoaded] = loaded
        }
    }
    suspend fun collectOnDataLoad(onLoad: (Boolean) -> Unit) {
        loadingDataStore.data.collectLatest {
            onLoad(it[isDataLoaded] ?: false)
        }
    }

    suspend fun editTheme(darkTheme: Boolean) {
        themeDataStore.edit {
            it[isDarkTheme] = darkTheme
        }
    }
    suspend fun collectThemeMode(onData: (Boolean) -> Unit) {
        themeDataStore.data.collect {
            onData(it[isDarkTheme] ?: false)
        }
    }

    suspend fun collectMeditationNotificationStatus(onData: (Boolean) -> Unit) {
        notificationsDataStore.data.collect {
            onData(it[isMeditationNotificationCancelled] ?: false)
        }
    }
    suspend fun editMeditationNotificationStatus(isCancelled: Boolean) {
        notificationsDataStore.edit {
            it[isMeditationNotificationCancelled] = isCancelled
        }
    }
}
