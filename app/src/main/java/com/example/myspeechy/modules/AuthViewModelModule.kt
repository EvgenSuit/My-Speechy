package com.example.myspeechy.modules

import android.content.Context
import com.example.myspeechy.domain.auth.AccountDeletionService
import com.example.myspeechy.domain.auth.AuthService
import com.example.myspeechy.domain.auth.GoogleAuthService
import com.example.myspeechy.domain.useCases.CheckIfIsAdminUseCase
import com.example.myspeechy.domain.useCases.DecrementMemberCountUseCase
import com.example.myspeechy.domain.useCases.DeletePublicChatUseCase
import com.example.myspeechy.domain.useCases.LeavePrivateChatUseCase
import com.example.myspeechy.domain.useCases.LeavePublicChatUseCase
import com.google.android.gms.auth.api.identity.Identity
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext

@Module
@InstallIn(ActivityRetainedComponent::class)
object AuthViewModelModule {
    @Provides
    fun provideAuthService(): AuthService {
        return AuthService(Firebase.auth, Firebase.database.reference,
            Firebase.firestore,
            Firebase.storage.reference,
            LeavePublicChatUseCase(Firebase.auth.currentUser?.uid, Firebase.database.reference, DecrementMemberCountUseCase(Firebase.database.reference)),
            LeavePrivateChatUseCase(Firebase.auth.currentUser?.uid, Firebase.database.reference),
            CheckIfIsAdminUseCase(Firebase.auth.currentUser?.uid, Firebase.database.reference),
            DeletePublicChatUseCase(Firebase.database.reference, DecrementMemberCountUseCase(Firebase.database.reference)),
        )
    }
    @Provides
    fun provideAccountDeletionService(): AccountDeletionService {
        return AccountDeletionService(provideAuthService(), Firebase.auth)
    }

    @Provides
    fun provideGoogleAuthService(@ApplicationContext context: Context): GoogleAuthService {
        return lazy {
            GoogleAuthService(context.applicationInfo, Identity.getSignInClient(context), provideAuthService())
        }.value
    }
}
