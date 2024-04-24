package com.example.myspeechy.modules

import android.content.Context
import com.example.myspeechy.data.DataStoreManager
import com.example.myspeechy.data.authDataStore
import com.example.myspeechy.data.loadData
import com.example.myspeechy.data.navBarDataStore
import com.example.myspeechy.data.notificationsDataStore
import com.example.myspeechy.data.themeDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
@Module
@InstallIn(SingletonComponent::class)
object NotificationModule {
    @Provides
    @Named("NotificationModuleDataStore")
    fun provideDataStoreManager(@ApplicationContext context: Context): DataStoreManager {
        return DataStoreManager(
            context.authDataStore, context.navBarDataStore, context.loadData,
            context.themeDataStore,
            context.notificationsDataStore
        )
    }
}