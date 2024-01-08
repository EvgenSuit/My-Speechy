package com.example.myspeechy.modules

import android.content.Context
import com.example.myspeechy.services.AuthService
import com.example.myspeechy.services.GoogleAuthService
import com.example.myspeechy.utils.AuthViewModel
import com.google.android.gms.auth.api.identity.Identity
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
object AuthViewModelModule {

    @ViewModelScoped
    @Provides
    fun provideAuthViewModel(authService: AuthService, googleAuthService: GoogleAuthService): AuthViewModel {
        return AuthViewModel(authService, googleAuthService)
    }

    @ViewModelScoped
    @Provides
    fun provideAuthService(): AuthService {
        return AuthService(Firebase.auth)
    }

    @ViewModelScoped
    @Provides
    fun provideGoogleAuthService(@ApplicationContext context: Context): GoogleAuthService {
        return lazy {
            GoogleAuthService(context, Identity.getSignInClient(context))
        }.value
    }
}
