package com.example.myspeechy.utils.chat

import android.util.Log
import android.widget.Toast
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myspeechy.components.AlertDialogDataClass
import com.example.myspeechy.data.chat.User
import com.example.myspeechy.services.chat.PictureStorageError
import com.example.myspeechy.services.chat.UserProfileServiceImpl
import com.google.firebase.database.getValue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val userProfileServiceImpl: UserProfileServiceImpl,
    filesDirPath: String,
    @Named("ProfilePictureSizeError") private val storageSizeError: Toast,
    savedStateHandle: SavedStateHandle
): ViewModel() {
    private val _uiState = MutableStateFlow(UserProfileUiState())
    val uiState = _uiState.asStateFlow()
    val userId: String = checkNotNull(savedStateHandle["userId"])
    val currUserId = userProfileServiceImpl.userId
    private val normalQualityPicDir = "${filesDirPath}/profilePics/${userId}/normalQuality/"
    private val lowQualityPicDir = "${filesDirPath}/profilePics/${userId}/lowQuality/"
    val normalQualityPicRef = File(normalQualityPicDir, "$userId.jpg")
    private val lowQualityPicRef = File(lowQualityPicDir, "$userId.jpg")

    fun startOrStopListening(removeListeners: Boolean, onLogout: () -> Unit) {
        listenForUser(removeListeners)
        listenForUserPicture(removeListeners)
        listenForAuthState(onLogout)
    }
    private fun listenForAuthState(onLogout: () -> Unit) {
        userProfileServiceImpl.listenForAuthState { if (it) onLogout() }
    }
    private fun listenForUser(remove: Boolean) {
        userProfileServiceImpl.userListener(userId, {updateErrorCode(it)}, {user ->
             _uiState.update { it.copy(user = user.getValue<User>()) }
        }, remove)
    }

    //Listen for only normal quality image
    private fun listenForUserPicture(remove: Boolean) {
        _uiState.update { it.copy(pictureState = PictureState.DOWNLOADING) }
        userProfileServiceImpl.userPictureListener(userId,
            normalQualityPicRef,
            dir = normalQualityPicDir,
            onCancelled = {updateErrorCode(it)},
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

    fun writePicture(imgBytes: ByteArray, lowQuality: Boolean, quality: Int) {
        userProfileServiceImpl.compressPicture(imgBytes, quality, {compressedBytes ->
            userProfileServiceImpl.createPicDir(if (lowQuality) lowQualityPicDir else normalQualityPicDir)
            if (lowQuality) {
                lowQualityPicRef.writeBytes(compressedBytes)
            } else {
                normalQualityPicRef.writeBytes(compressedBytes)
            }
            uploadUserPicture(lowQuality)
        }, {
            if (!_uiState.value.logginOut) {
                storageSizeError.show()
            }
        })
    }

    init {
        userProfileServiceImpl.createPicDir(lowQualityPicDir)
        userProfileServiceImpl.createPicDir(normalQualityPicDir)
    }
    suspend fun changeUserInfo(newName: String, newInfo: String) {
        val nameIsSame = _uiState.value.user?.name == newName
        val infoIsSame = _uiState.value.user?.info == newInfo
        if (!nameIsSame) {
            userProfileServiceImpl.changeUsername(newName)
        }
        if (!infoIsSame) {
            userProfileServiceImpl.changeUserInfo(newInfo)
        }
    }
    private fun uploadUserPicture(lowQuality: Boolean) {
            _uiState.update { it.copy(pictureState = PictureState.UPLOADING) }
            userProfileServiceImpl.uploadUserPicture(if (lowQuality) lowQualityPicRef else normalQualityPicRef,
                lowQuality, {
                    updateStorageMessage(it)
                    _uiState.update { it.copy(pictureState = PictureState.ERROR) }
                }) {
                _uiState.update { it.copy(pictureState = PictureState.SUCCESS, storageMessage = "" ) }
            }
    }
    fun removeUserPicture() {
        listOf(true, false).forEach {lowQuality ->
            userProfileServiceImpl.removeUserPicture(lowQuality, {updateStorageMessage(it)}, { m ->
                _uiState.update { it.copy(storageMessage = m) }
                if (lowQuality) File(lowQualityPicDir).deleteRecursively()
                else File(normalQualityPicDir).deleteRecursively()
            })
        }
    }
    fun logout() {
        _uiState.update { it.copy(logginOut = true, deletingAccount = false) }
        userProfileServiceImpl.logout()
    }
    fun deleteAccount() {
        _uiState.update { it.copy(chatAlertDialogDataClass = AlertDialogDataClass(
            title = "Are you sure?",
            text = "Account will be deleted along with all your progress and conversations",
            onConfirm = {
                viewModelScope.launch {
                    try {
                        _uiState.update { it.copy(chatAlertDialogDataClass = AlertDialogDataClass(), deletingAccount = true, userManagementError = "") }
                        userProfileServiceImpl.deleteUser()
                        logout()
                    } catch (e: Exception) {
                        _uiState.update { it.copy(userManagementError = "Couldn't delete account", deletingAccount = false) }
                    }
                }
            },
            onDismiss = {_uiState.update { it.copy(chatAlertDialogDataClass = AlertDialogDataClass()) }}))
        }
    }
    private fun updateErrorCode(e: Int) {
        _uiState.update { it.copy(errorCode = e) }
    }
    private fun updateStorageMessage(e: String) {
        _uiState.update { it.copy(storageMessage = e.formatStorageErrorMessage(),
            pictureState = PictureState.ERROR) }
    }

    data class UserProfileUiState(
        val user: User? = User(),
        val logginOut: Boolean = false,
        val deletingAccount: Boolean = false,
        val recomposePic: String = "",
        val errorCode: Int = 0,
        val chatAlertDialogDataClass: AlertDialogDataClass = AlertDialogDataClass(),
        val userManagementError: String = "",
        val storageMessage: String = "",
        val pictureState: PictureState = PictureState.IDLE
    )
}
enum class PictureState {
    IDLE,
    DOWNLOADING,
    UPLOADING,
    SUCCESS,
    ERROR,
}