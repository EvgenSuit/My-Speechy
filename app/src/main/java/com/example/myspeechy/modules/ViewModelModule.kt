package com.example.myspeechy.modules

import android.content.Context
import com.example.myspeechy.data.LessonDb
import com.example.myspeechy.data.LessonFlagsDb
import com.example.myspeechy.data.LessonFlagsRepository
import com.example.myspeechy.data.LessonRepository
import com.example.myspeechy.services.LessonServiceImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(ViewModelComponent::class)
object ViewModelModule {

    @Provides
    fun provideLessonDb(@ApplicationContext context: Context): LessonDb {
        return LessonDb.getDb(context)
    }

    @Provides
    fun provideLessonRepository(db: LessonDb): LessonRepository {
        return LessonRepository(db.lessonDao())
    }

    @Provides
    fun provideLessonFlagsDb(@ApplicationContext context: Context): LessonFlagsDb {
        return LessonFlagsDb.getDb(context)
    }
    @Provides
    fun provideLessonFlagsRepository(db: LessonFlagsDb): LessonFlagsRepository {
        return LessonFlagsRepository(db.lessonFlagsDao())
    }

    @Provides
    fun provideLessonServiceImpl(): LessonServiceImpl {
        return LessonServiceImpl()
    }


}
