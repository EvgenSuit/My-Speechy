package com.example.myspeechy.modules

import android.content.Context
import androidx.room.Room
import com.example.myspeechy.data.LessonDb
import com.example.myspeechy.data.LessonFlagsDb
import com.example.myspeechy.data.LessonFlagsRepository
import com.example.myspeechy.data.LessonRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
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

    @Provides
    @Named("FakeLessonFlagsDb")
    fun provideFakeLessonFlagsDb(@ApplicationContext context: Context): LessonFlagsDb {
        return LessonFlagsDb.getDb(context)
    }
    @Provides
    @Named("FakeLessonFlagsRepository")
    fun provideFakeLessonFlagsRepository(@Named("FakeLessonFlagsDb") db: LessonFlagsDb): LessonFlagsRepository {
        return LessonFlagsRepository(db.lessonFlagsDao())
    }
}
