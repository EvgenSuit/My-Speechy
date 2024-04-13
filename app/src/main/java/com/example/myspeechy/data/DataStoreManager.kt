package com.example.myspeechy.data

import androidx.datastore.core.DataStore
import com.example.myspeechy.errorKey
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.example.myspeechy.showNavBarDataStore

class DataStoreManager(private val authDataStore: DataStore<Preferences>,
    private val navBarDataStore: DataStore<Preferences>) {
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
}