package com.myspeechy.myspeechy.modules

import android.content.Context
import android.content.pm.PackageManager
import com.myspeechy.myspeechy.domain.auth.AccountDeletionService
import com.myspeechy.myspeechy.domain.auth.AuthService
import com.myspeechy.myspeechy.domain.auth.GoogleAuthService
import com.myspeechy.myspeechy.domain.useCases.CheckIfIsAdminUseCase
import com.myspeechy.myspeechy.domain.useCases.DecrementMemberCountUseCase
import com.myspeechy.myspeechy.domain.useCases.DeletePublicChatUseCase
import com.myspeechy.myspeechy.domain.useCases.LeavePrivateChatUseCase
import com.myspeechy.myspeechy.domain.useCases.LeavePublicChatUseCase
import com.myspeechy.myspeechy.domain.useCases.ValidateEmailUseCase
import com.myspeechy.myspeechy.domain.useCases.ValidatePasswordUseCase
import com.google.android.gms.auth.api.identity.Identity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.myspeechy.myspeechy.R
import com.myspeechy.myspeechy.domain.useCases.ValidateUsernameUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.qualifiers.ApplicationContext


//ActivityRetainedComponent allows for testing
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
    fun provideFirebaseAuth(): FirebaseAuth = Firebase.auth

    @Provides
    fun provideValidateUsernameUseCase(): ValidateUsernameUseCase =
        ValidateUsernameUseCase(provideAuthService())
    @Provides
    fun provideValidateEmailUseCase(): ValidateEmailUseCase = ValidateEmailUseCase(
        provideAuthService()
    )
    @Provides
    fun provideValidatePasswordUseCase(): ValidatePasswordUseCase = ValidatePasswordUseCase(
        provideAuthService()
    )

    @Provides
    fun provideGoogleAuthService(@ApplicationContext context: Context): GoogleAuthService {
        return lazy {
            GoogleAuthService(
                context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA),
                context.resources.getInteger(R.integer.max_username_or_title_length),
                Identity.getSignInClient(context),
                provideAuthService())
        }.value
    }
}
