package com.myspeechy.myspeechy.domain.chat

import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

sealed class DirectoryManager {
    companion object {
        fun clearCache(cacheDir: String) {
            File(cacheDir).deleteRecursively()
        }
        fun createPicDir(dir: String) {
            if (!Files.isDirectory(Paths.get(dir))) {
                Files.createDirectories(Paths.get(dir))
            }
        }
    }
}