package com.example.myspeechy.modules

import android.content.Context
import android.content.res.AssetManager
import android.widget.Toast
import com.example.myspeechy.MySpeechyApplication
import com.example.myspeechy.data.LessonDb
import com.example.myspeechy.data.LessonRepository
import com.example.myspeechy.data.MeditationStatsDb
import com.example.myspeechy.data.MeditationStatsRepository
import com.example.myspeechy.services.ChatServiceImpl
import com.example.myspeechy.services.ChatsServiceImpl
import com.example.myspeechy.services.MainLessonServiceImpl
import com.example.myspeechy.services.MeditationLessonServiceImpl
import com.example.myspeechy.services.MeditationNotificationServiceImpl
import com.example.myspeechy.services.MeditationStatsServiceImpl
import com.example.myspeechy.services.ReadingLessonServiceImpl
import com.example.myspeechy.services.RegularLessonServiceImpl
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
    fun provideMeditationStatsDb(@ApplicationContext context: Context): MeditationStatsDb {
        return MeditationStatsDb.getDb(context)
    }
    @Provides
    fun provideMeditationStatsRepository(db: MeditationStatsDb): MeditationStatsRepository {
        return MeditationStatsRepository(db.meditationStatsDao())
    }

    @Provides
    fun provideAssetManager(@ApplicationContext context: Context): AssetManager {
        return context.assets
    }

    @Provides
    fun provideMainLessonServiceImpl(): MainLessonServiceImpl {
        return MainLessonServiceImpl()
    }
    @Provides
    fun provideReadingLessonServiceImpl(): ReadingLessonServiceImpl {
        return ReadingLessonServiceImpl()
    }
    @Provides
    fun provideRegularLessonServiceImpl(): RegularLessonServiceImpl {
        return RegularLessonServiceImpl()
    }

    @Provides
    fun provideMeditationLessonServiceImpl(): MeditationLessonServiceImpl {
        return MeditationLessonServiceImpl()
    }
    @Provides
    fun provideMeditationStatsServiceImpl(): MeditationStatsServiceImpl {
        return MeditationStatsServiceImpl()
    }
    @Provides
    fun provideChatsService(): ChatsServiceImpl {
        return ChatsServiceImpl()
    }
    @Provides
    fun provideChatService(): ChatServiceImpl {
        return ChatServiceImpl()
    }
    @Provides
    fun provideListenErrorToast(@ApplicationContext context: Context): Toast {
        return Toast.makeText(context, "Error listening to remote data", Toast.LENGTH_SHORT)
    }
    @Provides
    fun provideMeditationNotificationServiceImpl(@ApplicationContext context: Context):
            MeditationNotificationServiceImpl {
        return MeditationNotificationServiceImpl(context)
    }
}
