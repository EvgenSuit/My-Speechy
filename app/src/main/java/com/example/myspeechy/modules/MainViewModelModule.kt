package com.example.myspeechy.modules

import android.content.Context
import com.example.myspeechy.data.LessonDb
import com.example.myspeechy.data.LessonRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped



@Module
@InstallIn(ViewModelComponent::class)
object MainViewModelModule {
    @Provides
    fun provideLessonDb(@ApplicationContext context: Context): LessonDb {
        return LessonDb.getDb(context)
    }

    @Provides
    fun provideLessonRepository(db: LessonDb): LessonRepository {
        return LessonRepository(db.lessonDao())
    }

}
