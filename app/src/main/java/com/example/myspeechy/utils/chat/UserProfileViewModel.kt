package com.example.myspeechy.utils.chat

import android.util.Log
import android.widget.Toast
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.example.myspeechy.services.chat.PictureStorageError
import com.example.myspeechy.services.chat.UserProfileServiceImpl
import com.google.firebase.database.getValue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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

    fun startOrStopListening(removeListeners: Boolean) {
        listenForUsername(removeListeners)
        listenForUserInfo(removeListeners)
        listenForUserPicture(removeListeners)
    }

    private fun listenForUsername(remove: Boolean) {
        userProfileServiceImpl.usernameListener(userId, {updateErrorCode(it)}, {newName ->
            _uiState.update {
                it.copy(name = newName.getValue<String>())
            }
        }, remove)
    }
    private fun listenForUserInfo(remove: Boolean) {
        userProfileServiceImpl.userInfoListener(userId, {updateErrorCode(it)}, {newInfo ->
            _uiState.update {
                it.copy(info = newInfo.getValue<String>())
            }
        }, remove)
    }
    //Listen for only normal quality image
    private fun listenForUserPicture(remove: Boolean) {
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
                _uiState.update { it.copy(uploadingPicture = false, recomposePic = UUID.randomUUID().toString()
                ) }
                updateStorageMessage("")
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
            storageSizeError.show()
        })
    }

    init {
        userProfileServiceImpl.createPicDir(lowQualityPicDir)
        userProfileServiceImpl.createPicDir(normalQualityPicDir)
    }
    fun changeUserInfo(newName: String, newInfo: String, onSuccess: () -> Unit) {
        val nameIsSame = _uiState.value.name == newName
        val infoIsSame = _uiState.value.info == newInfo
        if (!nameIsSame) {
            userProfileServiceImpl.changeUsername(newName) {
                if (!infoIsSame) {
                    userProfileServiceImpl.changeUserInfo(newInfo, onSuccess)
                } else {onSuccess()}
            }
        }
        else if (!infoIsSame) {
            userProfileServiceImpl.changeUserInfo(newInfo, onSuccess)
        }
        else onSuccess()
    }
    private fun uploadUserPicture(lowQuality: Boolean) {
        _uiState.update { it.copy(uploadingPicture = true) }
        userProfileServiceImpl.uploadUserPicture(if (lowQuality) lowQualityPicRef else normalQualityPicRef,
            lowQuality, {updateStorageMessage(it)
            _uiState.update { it.copy(uploadingPicture = false) }}){
            _uiState.update { it.copy(uploadingPicture = false) }
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
    private fun updateErrorCode(e: Int) {
        _uiState.update { it.copy(errorCode = e) }
    }
    private fun updateStorageMessage(e: String) {
        _uiState.update { it.copy(storageMessage = e.formatStorageErrorMessage()) }
    }

    data class UserProfileUiState(
        val name: String? = null,
        val info: String? = null,
        val recomposePic: String = "",
        val uploadingPicture: Boolean = false,
        val errorCode: Int = 0,
        val storageMessage: String = ""
    )
}