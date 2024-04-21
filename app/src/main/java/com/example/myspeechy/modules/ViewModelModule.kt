package com.example.myspeechy.modules

import android.content.Context
import android.content.res.AssetManager
import android.widget.Toast
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.example.myspeechy.authDataStore
import com.example.myspeechy.data.DataStoreManager
import com.example.myspeechy.data.lesson.LessonDb
import com.example.myspeechy.data.lesson.LessonRepository
import com.example.myspeechy.data.meditation.MeditationStatsDb
import com.example.myspeechy.data.meditation.MeditationStatsRepository
import com.example.myspeechy.domain.MeditationNotificationServiceImpl
import com.example.myspeechy.domain.auth.AuthService
import com.example.myspeechy.domain.chat.ChatsServiceImpl
import com.example.myspeechy.domain.chat.PrivateChatServiceImpl
import com.example.myspeechy.domain.chat.PublicChatServiceImpl
import com.example.myspeechy.domain.chat.UserProfileService
import com.example.myspeechy.domain.lesson.MainLessonServiceImpl
import com.example.myspeechy.domain.lesson.MeditationLessonServiceImpl
import com.example.myspeechy.domain.lesson.ReadingLessonServiceImpl
import com.example.myspeechy.domain.lesson.RegularLessonServiceImpl
import com.example.myspeechy.domain.meditation.MeditationStatsServiceImpl
import com.example.myspeechy.domain.useCases.CheckIfIsAdminUseCase
import com.example.myspeechy.domain.useCases.DecrementMemberCountUseCase
import com.example.myspeechy.domain.useCases.DeletePublicChatUseCase
import com.example.myspeechy.domain.useCases.JoinPublicChatUseCase
import com.example.myspeechy.domain.useCases.LeavePrivateChatUseCase
import com.example.myspeechy.domain.useCases.LeavePublicChatUseCase
import com.example.myspeechy.loadData
import com.example.myspeechy.navBarDataStore
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import es.dmoral.toasty.Toasty
import javax.inject.Named

@Module
@InstallIn(ActivityRetainedComponent::class)
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
    @Named("AuthDataStore")
    fun provideAuthDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.authDataStore

    @Provides
    fun provideDataStoreManager(@ApplicationContext context: Context): DataStoreManager {
        return DataStoreManager(context.authDataStore, context.navBarDataStore, context.loadData)
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
        return MainLessonServiceImpl(Firebase.firestore)
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
            provideLeavePrivateChatUseCase(),
            provideLeavePublicChatUseCase(),
            provideJoinPublicChatUseCase(),
            provideCheckIfIsAdminUseCase(),
            provideDeletePublicChatUseCase()
        )
    }
    @Provides
    fun providePublicChatServiceImpl(): PublicChatServiceImpl {
        return PublicChatServiceImpl(
            Firebase.auth,
            Firebase.storage.reference,
            Firebase.database.reference,
            provideLeavePublicChatUseCase(),
            provideDeletePublicChatUseCase(),
            provideJoinPublicChatUseCase()
        )
    }
    @Provides
    fun providePrivateChatServiceImpl(): PrivateChatServiceImpl = PrivateChatServiceImpl(
        Firebase.auth,
        Firebase.storage.reference,
        Firebase.database.reference,
        provideLeavePrivateChatUseCase(),)

    @Provides
    fun provideCheckIfIsAdminUseCase(): CheckIfIsAdminUseCase = CheckIfIsAdminUseCase(Firebase.auth.currentUser?.uid, Firebase.database.reference)
    @Provides
    fun provideDeletePublicChatUseCase(): DeletePublicChatUseCase =
        DeletePublicChatUseCase(Firebase.database.reference, DecrementMemberCountUseCase(Firebase.database.reference))
    @Provides
    fun provideJoinPublicChatUseCase(): JoinPublicChatUseCase = JoinPublicChatUseCase(Firebase.auth.currentUser?.uid, Firebase.database.reference)
    @Provides
    fun provideLeavePublicChatUseCase(): LeavePublicChatUseCase =
        LeavePublicChatUseCase(Firebase.auth.currentUser?.uid, Firebase.database.reference, DecrementMemberCountUseCase(Firebase.database.reference))
    @Provides
    fun provideLeavePrivateChatUseCase(): LeavePrivateChatUseCase = LeavePrivateChatUseCase(Firebase.auth.currentUser?.uid, Firebase.database.reference)
    @Provides
    fun provideUserProfileServiceImpl(): UserProfileService =
        UserProfileService(AuthService(Firebase.auth,
            Firebase.database.reference,
            Firebase.firestore, Firebase.storage.reference,
            provideLeavePublicChatUseCase(),
            provideLeavePrivateChatUseCase(),
            provideCheckIfIsAdminUseCase(),
            provideDeletePublicChatUseCase(),
        ))
    @Provides
    fun provideFilesDirPath(@ApplicationContext context: Context): String = context.cacheDir.path

    @Provides
    fun provideListenErrorToast(@ApplicationContext context: Context): Toast =
        Toasty.error(context, "Error listening to remote data", Toast.LENGTH_LONG, true)
    @Provides
    fun provideMeditationNotificationServiceImpl(@ApplicationContext context: Context):
            MeditationNotificationServiceImpl = MeditationNotificationServiceImpl(context)
}
