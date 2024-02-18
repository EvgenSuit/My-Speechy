package com.example.myspeechy.modules

import android.content.Context
import android.content.res.AssetManager
import android.widget.Toast
import com.example.myspeechy.data.lesson.LessonDb
import com.example.myspeechy.data.lesson.LessonRepository
import com.example.myspeechy.data.meditation.MeditationStatsDb
import com.example.myspeechy.data.meditation.MeditationStatsRepository
import com.example.myspeechy.services.chat.ChatsServiceImpl
import com.example.myspeechy.services.lesson.MainLessonServiceImpl
import com.example.myspeechy.services.lesson.MeditationLessonServiceImpl
import com.example.myspeechy.services.MeditationNotificationServiceImpl
import com.example.myspeechy.services.meditation.MeditationStatsServiceImpl
import com.example.myspeechy.services.chat.PrivateChatServiceImpl
import com.example.myspeechy.services.chat.PublicChatServiceImpl
import com.example.myspeechy.services.chat.UserProfileServiceImpl
import com.example.myspeechy.services.lesson.ReadingLessonServiceImpl
import com.example.myspeechy.services.lesson.RegularLessonServiceImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File

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
    fun provideChatsServiceImpl(): ChatsServiceImpl {
        return ChatsServiceImpl()
    }
    @Provides
    fun providePublicChatServiceImpl(): PublicChatServiceImpl {
        return PublicChatServiceImpl()
    }
    @Provides
    fun providePrivateChatServiceImpl(): PrivateChatServiceImpl {
        return PrivateChatServiceImpl()
    }
    @Provides
    fun provideUserProfileServiceImpl(): UserProfileServiceImpl {
        return UserProfileServiceImpl()
    }
    @Provides
    fun provideFilesDir(@ApplicationContext context: Context): File {
        return context.filesDir
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
