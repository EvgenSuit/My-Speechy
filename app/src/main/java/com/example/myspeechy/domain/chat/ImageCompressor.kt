package com.example.myspeechy.domain.chat

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.io.ByteArrayOutputStream

sealed class ImageCompressor {
    companion object {
        fun compressPicture(imgBytes: ByteArray,
                            lowQuality: Boolean): ByteArray? {
            var quality = if (lowQuality) 5 else 15
            val baos = ByteArrayOutputStream()
            var bmp = BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.size)
            val ratio: Float = bmp.width.toFloat() / bmp.height.toFloat()
            val width = 600
            val height = Math.round(width / ratio)

            bmp = Bitmap.createScaledBitmap(bmp, width, height, true)
            var currSize: Int
            var compressionNotComplete: Boolean
            do {
                baos.reset()
                bmp.compress(Bitmap.CompressFormat.JPEG, quality, baos)
                currSize = baos.toByteArray().size
                quality -= 1
                //calculate condition for each of image types(low and normal quality) separately
                //low quality image can't be more than 50kb in size
                compressionNotComplete = !lowQuality && currSize >= 100 * 1024 || lowQuality && currSize >= 50 * 1024
        } while (quality > 0 && compressionNotComplete)
            return if (compressionNotComplete) null else baos.toByteArray()
        }
    }
}