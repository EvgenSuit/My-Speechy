package com.example.myspeechy.utils.chat

import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val userProfileServiceImpl: UserProfileServiceImpl,
    filesDir: File,
    @Named("ProfilePictureSizeError") private val storageSizeError: Toast,
    savedStateHandle: SavedStateHandle
): ViewModel() {
    private val _uiState = MutableStateFlow(UserProfileUiState())
    val uiState = _uiState.asStateFlow()
    val userId: String = checkNotNull(savedStateHandle["userId"])
    val currUserId = userProfileServiceImpl.userId
    private val normalQualityPicDir = "${filesDir}/profilePics/${userId}/normalQuality/"
    private val lowQualityPicDir = "${filesDir}/profilePics/${userId}/lowQuality/"
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
            onDirCreate = {createPicDir(normalQualityPicDir)
                normalQualityPicRef.createNewFile()},
            onCancelled = {updateErrorCode(it)},
            onStorageFailure = {m ->
                updateStorageErrorMessage(m)
                val errorMessage = _uiState.value.storageErrorMessage
                if (PictureStorageError.OBJECT_DOES_NOT_EXIST_AT_LOCATION.name.contains(errorMessage) ||
                    PictureStorageError.USING_DEFAULT_PROFILE_PICTURE.name.contains(errorMessage)) {
                    normalQualityPicRef.delete()
                    File(normalQualityPicDir).deleteRecursively()
                }
            }, {
                _uiState.update { it.copy(uploadingPicture = false, storageErrorMessage = "",
                    picId = UUID.randomUUID().toString()
                ) }
                updateStorageErrorMessage("")
            }, remove)
    }

    private fun createPicDir(dir: String) {
        if (!Files.isDirectory(Paths.get(dir))) {
            Files.createDirectories(Paths.get(dir))
        }
    }
    fun writePicture(imgBytes: ByteArray, lowQuality: Boolean, quality: Int) {
        val baos = ByteArrayOutputStream()
        val bmp = BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.size)
        bmp.compress(Bitmap.CompressFormat.JPEG, quality, baos)
        val compressedBytes = baos.toByteArray()
        if (compressedBytes.size < 2 * 1024 * 1024) {
            createPicDir(if (lowQuality) lowQualityPicDir else normalQualityPicDir)
            if (lowQuality) {
                lowQualityPicRef.writeBytes(compressedBytes)
            } else {
                normalQualityPicRef.writeBytes(compressedBytes)
            }
            uploadUserPicture(if (lowQuality) lowQualityPicRef else normalQualityPicRef, lowQuality)
        } else {
            storageSizeError.show()
        }
    }

    init {
        createPicDir(lowQualityPicDir)
        createPicDir(normalQualityPicDir)
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
    private fun uploadUserPicture(pic: File, lowQuality: Boolean) {
        _uiState.update { it.copy(uploadingPicture = true) }
        userProfileServiceImpl.uploadUserPicture(pic, lowQuality, {updateStorageErrorMessage(it)
            _uiState.update { it.copy(uploadingPicture = false) }}){
            _uiState.update { it.copy(uploadingPicture = false) }
        }
    }
    fun removeUserPicture() {
        listOf(true, false).forEach {lowQuality ->
            userProfileServiceImpl.removeUserPicture(lowQuality, {updateStorageErrorMessage(it)}, {m ->
                _uiState.update { it.copy(storageErrorMessage = m) }
                if (lowQuality) File(lowQualityPicDir).deleteRecursively()
                else File(normalQualityPicDir).deleteRecursively()
            })
        }
    }
    private fun updateErrorCode(e: Int) {
        _uiState.update { it.copy(errorCode = e) }
    }
    private fun updateStorageErrorMessage(e: String) {
        _uiState.update { it.copy(storageErrorMessage = e.split(" ").joinToString("_").uppercase(Locale.ROOT).dropLast(1)) }
    }

    data class UserProfileUiState(
        val name: String? = null,
        val info: String? = null,
        val picId: String = "",
        val uploadingPicture: Boolean = false,
        val errorCode: Int = 0,
        val storageErrorMessage: String = ""
    )
}