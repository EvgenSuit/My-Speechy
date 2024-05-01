package com.myspeechy.myspeechy.modules

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.myspeechy.myspeechy.data.DataStoreManager
import com.myspeechy.myspeechy.data.authDataStore
import com.myspeechy.myspeechy.data.lesson.LessonDb
import com.myspeechy.myspeechy.data.lesson.LessonRepository
import com.myspeechy.myspeechy.data.lessonItemsDataStore
import com.myspeechy.myspeechy.data.loadData
import com.myspeechy.myspeechy.data.navBarDataStore
import com.myspeechy.myspeechy.data.notificationsDataStore
import com.myspeechy.myspeechy.data.themeDataStore
import com.myspeechy.myspeechy.domain.MeditationNotificationServiceImpl
import com.myspeechy.myspeechy.domain.auth.AuthService
import com.myspeechy.myspeechy.domain.chat.ChatsServiceImpl
import com.myspeechy.myspeechy.domain.chat.PrivateChatServiceImpl
import com.myspeechy.myspeechy.domain.chat.PublicChatServiceImpl
import com.myspeechy.myspeechy.domain.chat.UserProfileService
import com.myspeechy.myspeechy.domain.lesson.MainLessonServiceImpl
import com.myspeechy.myspeechy.domain.lesson.MeditationLessonServiceImpl
import com.myspeechy.myspeechy.domain.lesson.ReadingLessonServiceImpl
import com.myspeechy.myspeechy.domain.lesson.RegularLessonServiceImpl
import com.myspeechy.myspeechy.domain.meditation.MeditationStatsServiceImpl
import com.myspeechy.myspeechy.domain.useCases.CheckIfIsAdminUseCase
import com.myspeechy.myspeechy.domain.useCases.DecrementMemberCountUseCase
import com.myspeechy.myspeechy.domain.useCases.DeletePublicChatUseCase
import com.myspeechy.myspeechy.domain.useCases.JoinPublicChatUseCase
import com.myspeechy.myspeechy.domain.useCases.LeavePrivateChatUseCase
import com.myspeechy.myspeechy.domain.useCases.LeavePublicChatUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.qualifiers.ApplicationContext
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
    @Named("AuthDataStore")
    fun provideAuthDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.authDataStore

    @Provides
    fun provideDataStoreManager(@ApplicationContext context: Context): DataStoreManager {
        return DataStoreManager(context.authDataStore, context.navBarDataStore, context.loadData,
            context.themeDataStore,
            context.notificationsDataStore,
            context.lessonItemsDataStore)
    }

    @Provides
    fun provideMainLessonServiceImpl(@ApplicationContext context: Context): MainLessonServiceImpl {
        return MainLessonServiceImpl(Firebase.firestore.collection("users"),
            provideDataStoreManager(context),
            Firebase.auth)
    }
    @Provides
    fun provideReadingLessonServiceImpl(@ApplicationContext context: Context): ReadingLessonServiceImpl {
        return ReadingLessonServiceImpl(Firebase.firestore.collection("users"),
            provideDataStoreManager(context),
            Firebase.auth)
    }
    @Provides
    fun provideRegularLessonServiceImpl(@ApplicationContext context: Context): RegularLessonServiceImpl {
        return RegularLessonServiceImpl(Firebase.firestore.collection("users"),
            provideDataStoreManager(context),
            Firebase.auth)
    }

    @Provides
    fun provideMeditationLessonServiceImpl(@ApplicationContext context: Context): MeditationLessonServiceImpl {
        return MeditationLessonServiceImpl(Firebase.firestore.collection("users"),
            provideDataStoreManager(context),
            Firebase.auth)
    }
    @Provides
    fun provideMeditationStatsServiceImpl(): MeditationStatsServiceImpl {
        return MeditationStatsServiceImpl(
            Firebase.firestore,
            Firebase.auth)
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
    fun provideMeditationNotificationServiceImpl(@ApplicationContext context: Context):
            MeditationNotificationServiceImpl = MeditationNotificationServiceImpl(context)
}
