package com.example.myspeechy.modules

import android.content.Context
import androidx.room.Room
import com.example.myspeechy.data.lesson.LessonDb
import com.example.myspeechy.data.lesson.LessonRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named


@Module
@InstallIn(SingletonComponent::class)
object TestingModule {

    @Provides
    @Named("FakeLessonDb")
    fun provideInRoomFakeLessonDb(@ApplicationContext context: Context): LessonDb {
        return Room.inMemoryDatabaseBuilder(context, LessonDb::class.java)
            .build()
    }
    @Provides
    @Named("FakeLessonRepository")
    fun provideInRoomFakeLessonRepository(@Named("FakeLessonDb") db: LessonDb): LessonRepository {
        return LessonRepository(db.lessonDao())
    }
}

/*
@Module
@TestInstallIn(components = [ActivityRetainedComponent::class],
    replaces = [ThoughtTrackerModule::class])
object FakeThoughtTrackerModule {
    @Provides
    fun provideIsDateDifferentFromCurrentUseCase(): IsDateEqualToCurrentUseCase =
        IsDateEqualToCurrentUseCase(LocalDateTime.now().plusDays(1))
    @Provides
    fun provideGetCurrentDateInTimestampUseCase(): GetCurrentDateInTimestampUseCase =
        GetCurrentDateInTimestampUseCase()
    @Provides
    fun provideThoughtTrackerService(): ThoughtTrackerService =
        ThoughtTrackerService(Firebase.firestore.collection("users"), Firebase.auth)
    @Provides
    fun provideThoughtTrackerItemService(): ThoughtTrackerItemService =
        ThoughtTrackerItemService(Firebase.firestore.collection("users"), Firebase.auth)
}*/
