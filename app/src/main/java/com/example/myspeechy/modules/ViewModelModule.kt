package com.example.myspeechy.modules

import android.content.Context
import android.content.res.AssetManager
import com.example.myspeechy.data.LessonDb
import com.example.myspeechy.data.LessonRepository
import com.example.myspeechy.services.LessonServiceImpl
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext

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
    fun provideLessonServiceImpl(): LessonServiceImpl {
        return LessonServiceImpl(Firebase.auth.currentUser!!.uid)
    }

    @Provides
    fun provideAssetManager(@ApplicationContext context: Context): AssetManager {
        return context.assets
    }

}
