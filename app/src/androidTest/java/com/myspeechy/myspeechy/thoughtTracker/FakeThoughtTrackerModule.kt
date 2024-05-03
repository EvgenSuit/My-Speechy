package com.myspeechy.myspeechy.thoughtTracker

import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.myspeechy.myspeechy.domain.thoughtTracker.ThoughtTrackerItemService
import com.myspeechy.myspeechy.domain.thoughtTracker.ThoughtTrackerService
import com.myspeechy.myspeechy.domain.useCases.GetCurrentDateUseCase
import com.myspeechy.myspeechy.domain.useCases.IsDateEqualToCurrentUseCase
import com.myspeechy.myspeechy.modules.ThoughtTrackerModule
import dagger.Module
import dagger.Provides
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.testing.TestInstallIn
import java.time.LocalDateTime

@Module
@TestInstallIn(components = [ActivityRetainedComponent::class],
    replaces = [ThoughtTrackerModule::class])
object FakeThoughtTrackerModule {
    @Provides
    fun provideIsDateDifferentFromCurrentUseCase(): IsDateEqualToCurrentUseCase {
        val fakeDate = LocalDateTime.now().plusDays(1)
        return IsDateEqualToCurrentUseCase(fakeDate)
    }
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