package com.example.myspeechy.modules

import android.content.Context
import android.content.res.AssetManager
import android.widget.Toast
import com.example.myspeechy.data.lesson.LessonDb
import com.example.myspeechy.data.lesson.LessonRepository
import com.example.myspeechy.data.meditation.MeditationStatsDb
import com.example.myspeechy.data.meditation.MeditationStatsRepository
import com.example.myspeechy.services.MeditationNotificationServiceImpl
import com.example.myspeechy.services.auth.AuthService
import com.example.myspeechy.services.chat.ChatsServiceImpl
import com.example.myspeechy.services.chat.PrivateChatServiceImpl
import com.example.myspeechy.services.chat.PublicChatServiceImpl
import com.example.myspeechy.services.chat.UserProfileServiceImpl
import com.example.myspeechy.services.lesson.MainLessonServiceImpl
import com.example.myspeechy.services.lesson.MeditationLessonServiceImpl
import com.example.myspeechy.services.lesson.ReadingLessonServiceImpl
import com.example.myspeechy.services.lesson.RegularLessonServiceImpl
import com.example.myspeechy.services.meditation.MeditationStatsServiceImpl
import com.example.myspeechy.useCases.CheckIfIsAdminUseCase
import com.example.myspeechy.useCases.DeletePublicChatUseCase
import com.example.myspeechy.useCases.FormatDateUseCase
import com.example.myspeechy.useCases.JoinPublicChatUseCase
import com.example.myspeechy.useCases.LeavePrivateChatUseCase
import com.example.myspeechy.useCases.LeavePublicChatUseCase
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import es.dmoral.toasty.Toasty
import javax.inject.Named

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
        return ChatsServiceImpl(
            Firebase.database.reference,
            Firebase.auth,
            LeavePrivateChatUseCase(), LeavePublicChatUseCase(),
            JoinPublicChatUseCase(), CheckIfIsAdminUseCase(),
            DeletePublicChatUseCase(),
            FormatDateUseCase()
        )
    }
    @Provides
    fun providePublicChatServiceImpl(): PublicChatServiceImpl {
        return PublicChatServiceImpl(
            Firebase.auth,
            Firebase.storage.reference,
            Firebase.database.reference,
            LeavePublicChatUseCase(),
            JoinPublicChatUseCase(),
            FormatDateUseCase()
        )
    }
    @Provides
    fun providePrivateChatServiceImpl(): PrivateChatServiceImpl = PrivateChatServiceImpl(
        Firebase.auth,
        Firebase.storage.reference,
        Firebase.database.reference,
        LeavePrivateChatUseCase(),
        FormatDateUseCase())
    @Provides
    fun provideUserProfileServiceImpl(): UserProfileServiceImpl = UserProfileServiceImpl(AuthService(Firebase.auth, Firebase.database.reference.child("users")))
    @Provides
    fun provideFilesDirPath(@ApplicationContext context: Context): String = context.cacheDir.path

    @Provides
    @Named("ProfilePictureSizeError")
    fun provideProfilePictureSizeError(@ApplicationContext context: Context): Toast =
        Toasty.error(context, "Picture must be less than 2 mb in size", Toast.LENGTH_LONG, true)

    @Provides
    fun provideListenErrorToast(@ApplicationContext context: Context): Toast =
        Toasty.error(context, "Error listening to remote data", Toast.LENGTH_LONG, true)
    @Provides
    fun provideMeditationNotificationServiceImpl(@ApplicationContext context: Context):
            MeditationNotificationServiceImpl = MeditationNotificationServiceImpl(context)
}
