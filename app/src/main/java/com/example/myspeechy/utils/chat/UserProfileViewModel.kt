package com.example.myspeechy.utils.chat

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.example.myspeechy.services.chat.UserProfileServiceImpl
import com.google.firebase.database.getValue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import javax.inject.Inject

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val userProfileServiceImpl: UserProfileServiceImpl,
    filesDir: File,
    savedStateHandle: SavedStateHandle
): ViewModel() {
    private val _uiState = MutableStateFlow(UserProfileUiState())
    val uiState = _uiState.asStateFlow()
    val userId: String = checkNotNull(savedStateHandle["userId"])
    private val picDir = "${filesDir}/profilePics/${userId}"
    val picRef = File(picDir, "$userId.jpg")

    private fun listenForUsername() {
        userProfileServiceImpl.usernameListener(userId, {updateErrorCode(it)}) {newName ->
            _uiState.update {
                it.copy(name = newName.getValue<String>() ?: "")
            }
        }
    }
    private fun listenForUserInfo() {
        userProfileServiceImpl.userInfoListener(userId, {updateErrorCode(it)}) {newInfo ->
            _uiState.update {
                it.copy(info = newInfo.getValue<String>() ?: "")
            }
        }
    }

    fun createPicDir() {
        if (!Files.isDirectory(Paths.get(picDir))) {
            Files.createDirectories(Paths.get(picDir))
        }
    }

    init {
        createPicDir()
        _uiState.update { it.copy(pic = picRef) }
        listenForUsername()
        listenForUserInfo()
        listenForUserPicture()
    }
    fun changeUserDescription(newName: String, newInfo: String) {
        if (_uiState.value.info != newInfo || _uiState.value.name != newName) {
            userProfileServiceImpl.changeUsername(userId, newName)
            userProfileServiceImpl.changeUserInfo(userId, newInfo)
        }
    }
    fun uploadUserPicture(pic: File) {
        _uiState.update { it.copy(uploadingPicture = true) }
        userProfileServiceImpl.uploadUserPicture(userId, pic){
            _uiState.update { it.copy(uploadingPicture = false) }
        }
    }
    fun removeUserPicture() {
        userProfileServiceImpl.removeUserPicture(userId) {m ->
            _uiState.value.pic?.delete()
            Files.deleteIfExists(Paths.get(picDir))
            _uiState.update { it.copy(storageErrorMessage = m, pic = null) }
        }
    }
    private fun listenForUserPicture() {
        userProfileServiceImpl.userPictureListener(userId, picRef, {updateErrorCode(it)},
            {updateStorageErrorMessage(it)}) {
            _uiState.update { it.copy(pic = picRef, uploadingPicture = false) }
            updateStorageErrorMessage("")
        }
    }

    private fun updateErrorCode(e: Int) {
        _uiState.update { it.copy(errorCode = e) }
    }
    private fun updateStorageErrorMessage(e: String) {
        _uiState.update { it.copy(storageErrorMessage = e) }
    }

    data class UserProfileUiState(
        val name: String = "",
        val info: String = "",
        val pic: File? = null,
        val uploadingPicture: Boolean = false,
        val errorCode: Int = 0,
        val storageErrorMessage: String = ""
    )
}