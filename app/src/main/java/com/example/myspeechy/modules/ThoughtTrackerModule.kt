package com.example.myspeechy.modules

import com.example.myspeechy.domain.thoughtTracker.ThoughtTrackerItemService
import com.example.myspeechy.domain.thoughtTracker.ThoughtTrackerService
import com.example.myspeechy.domain.useCases.GetCurrentDateUseCase
import com.example.myspeechy.domain.useCases.IsDateEqualToCurrentUseCase
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent


@Module
@InstallIn(ActivityRetainedComponent::class)
object ThoughtTrackerModule {
    @Provides
    fun provideIsDateDifferentFromCurrentUseCase(): IsDateEqualToCurrentUseCase =
        IsDateEqualToCurrentUseCase()
    @Provides
    fun provideGetCurrentDateInTimestampUseCase(): GetCurrentDateUseCase =
        GetCurrentDateUseCase()
    @Provides
    fun provideThoughtTrackerService(): ThoughtTrackerService =
        ThoughtTrackerService(Firebase.firestore.collection("users"), Firebase.auth)
    @Provides
    fun provideThoughtTrackerItemService(): ThoughtTrackerItemService =
        ThoughtTrackerItemService(Firebase.firestore.collection("users"), Firebase.auth)
}