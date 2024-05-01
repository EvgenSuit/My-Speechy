package com.myspeechy.myspeechy.modules

import android.content.Context
import androidx.room.Room
import com.myspeechy.myspeechy.data.lesson.LessonDb
import com.myspeechy.myspeechy.data.lesson.LessonRepository
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



