package com.myspeechy.myspeechy.presentation.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.myspeechy.myspeechy.components.AlertDialogDataClass
import com.myspeechy.myspeechy.data.chat.User
import com.myspeechy.myspeechy.domain.Result
import com.myspeechy.myspeechy.domain.chat.DirectoryManager
import com.myspeechy.myspeechy.domain.chat.ImageCompressor
import com.myspeechy.myspeechy.domain.chat.UserProfileService
import com.myspeechy.myspeechy.domain.error.PictureStorageError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val userProfileService: UserProfileService,
    filesDirPath: String,
    savedStateHandle: SavedStateHandle
): ViewModel() {
    private val _uiState = MutableStateFlow(UserProfileUiState())
    val uiState = _uiState.asStateFlow()
    val userId: String = checkNotNull(savedStateHandle["userId"])
    val currUserId = userProfileService.userId
    private val normalQualityPicDir = "${filesDirPath}/profilePics/${userId}/normalQuality/"
    private val lowQualityPicDir = "${filesDirPath}/profilePics/${userId}/lowQuality/"
    val normalQualityPicRef = File(normalQualityPicDir, "$userId.jpg")
    private val lowQualityPicRef = File(lowQualityPicDir, "$userId.jpg")
    val authResultFlow = _uiState.map { it.authResult }
    val errorMessageFlow = _uiState.map { it.errorMessage }
    val accountDeletionResultFlow = _uiState.map { it.accountDeletionResult }

    fun startOrStopListening(removeListeners: Boolean) {
        listenForUser(removeListeners)
        listenForUserPicture(removeListeners)
        if (removeListeners) updateErrorMessage("")
    }
    private fun listenForUser(remove: Boolean) {
        userProfileService.userListener(userId, {updateErrorMessage(it)}, { user ->
             _uiState.update { it.copy(user = user.getValue(User::class.java)) }
        }, remove)
    }

    //Listen for only normal quality image
    private fun listenForUserPicture(remove: Boolean) {
        updatePictureState(PictureState.DOWNLOADING)
        userProfileService.userPictureListener(userId,
            normalQualityPicRef,
            dir = normalQualityPicDir,
            onCancelled = {updateErrorMessage(it)},
            onStorageFailure = {m ->
                updateStorageMessage(m)
                val errorMessage = _uiState.value.storageMessage
                //Using contains since firebase may return error message of different structure
                //for which updateStorageErrorMessage may not account for
                if (PictureStorageError.OBJECT_DOES_NOT_EXIST_AT_LOCATION.name.contains(errorMessage) ||
                    PictureStorageError.USING_DEFAULT_PROFILE_PICTURE.name.contains(errorMessage)) {
                    File(normalQualityPicDir).deleteRecursively()
                }
            }, {
                _uiState.update { it.copy(
                    storageMessage = "",
                    recomposePic = UUID.randomUUID().toString(),
                    pictureState = PictureState.SUCCESS
                ) }
            }, remove)
    }

    fun writePicture(imgBytes: ByteArray) {
        try {
            updateErrorMessage("")
            viewModelScope.launch {
                for (lowQuality in listOf(true, false)) {
                    if (_uiState.value.errorMessage != "Couldn't compress picture. Perhaps it's too big") {
                        val compressedBytes = ImageCompressor.compressPicture(imgBytes, lowQuality)
                        if (compressedBytes != null && compressedBytes.isNotEmpty()) {
                            DirectoryManager.createPicDir(if (lowQuality) lowQualityPicDir else normalQualityPicDir)
                            if (lowQuality) {
                                lowQualityPicRef.writeBytes(compressedBytes)
                            } else {
                                normalQualityPicRef.writeBytes(compressedBytes)
                            }
                            uploadUserPicture(lowQuality)
                        } else {
                            if (_uiState.value.authResult !is Result.InProgress) {
                                delay(1) //without it the launched effect in the screen doesn't work
                                updateErrorMessage("Couldn't compress picture. Perhaps it's too big")
                            }
                        }
                    }
            }
            }
        } catch (e: Exception){
            updateErrorMessage(e.message!!)
        }
    }

    private fun uploadUserPicture(lowQuality: Boolean) {
        viewModelScope.launch {
            try {
                updatePictureState(PictureState.UPLOADING)
                userProfileService.uploadUserPicture(if (lowQuality) lowQualityPicRef else normalQualityPicRef, lowQuality)
            } catch (e: Exception) {
                updatePictureState(PictureState.ERROR)
                updateErrorMessage(e.message!!)
            }
        }
    }
    fun removeUserPicture() {
        viewModelScope.launch {
            try {
                listOf(true, false).forEach {lowQuality ->
                    userProfileService.removeUserPicture(lowQuality)
                    if (lowQuality) File(lowQualityPicDir).deleteRecursively()
                    else File(normalQualityPicDir).deleteRecursively()
                    _uiState.update { it.copy(storageMessage = PictureStorageError.USING_DEFAULT_PROFILE_PICTURE.name) }
                }
            } catch (e: Exception) {
                updateErrorMessage(e.message!!)
            }
        }
    }
    suspend fun changeUserInfo(newName: String, newInfo: String) {
        try {
            val nameIsSame = _uiState.value.user?.name == newName
            val infoIsSame = _uiState.value.user?.info == newInfo
            if (!nameIsSame) {
                userProfileService.changeUsername(newName)
            }
            if (!infoIsSame) {
                userProfileService.changeUserInfo(newInfo)
            }
        } catch (e: Exception) {
            updateErrorMessage(e.message!!)
        }
    }
    fun logout() {
        try {
            viewModelScope.launch {
                updateResult(Result.InProgress)
                userProfileService.logout()
            }
        } catch (e: Exception) {
            updateResult(Result.Error(e.message!!))
        }
    }
    fun deleteAccount() {
        _uiState.update { it.copy(
            accountDeletionResult = Result.Idle,
            chatAlertDialogDataClass = AlertDialogDataClass(
            title = "Are you sure?",
            text = "Account will be deleted along with all your progress, conversations and chats of which you're an admin",
            onConfirm = {
                _uiState.update { it.copy(chatAlertDialogDataClass = AlertDialogDataClass(), accountDeletionResult = Result.InProgress, userManagementError = "") }
            },
            onDismiss = {_uiState.update { it.copy(chatAlertDialogDataClass = AlertDialogDataClass(), accountDeletionResult = Result.Idle) }}))
        }
    }
    fun setDeletionResultToIdle() {
        _uiState.update { it.copy(accountDeletionResult = Result.Idle) }
    }
    private fun updateResult(result: Result) {
        if (Firebase.auth.currentUser != null) {
            _uiState.update { it.copy(authResult = result) }
        }
    }
    private fun updateErrorMessage(m: String) {
        if (Firebase.auth.currentUser != null) {
            _uiState.update { it.copy(errorMessage = m) }
            }
    }
    private fun updateStorageMessage(e: String) {
        _uiState.update { it.copy(storageMessage = e.formatStorageErrorMessage(),
            pictureState = PictureState.ERROR) }
    }
    private fun updatePictureState(state: PictureState) {
        _uiState.update { it.copy(pictureState = state) }
    }

    data class UserProfileUiState(
        val user: User? = User(),
        val accountDeletionResult: Result = Result.Idle,
        val recomposePic: String = "",
        val chatAlertDialogDataClass: AlertDialogDataClass = AlertDialogDataClass(),
        val userManagementError: String = "",
        val storageMessage: String = "",
        val pictureState: PictureState = PictureState.DOWNLOADING,
        val authResult: Result = Result.Idle,
        val errorMessage: String = ""
    )
}
enum class PictureState {
    IDLE,
    DOWNLOADING,
    UPLOADING,
    SUCCESS,
    ERROR,
}