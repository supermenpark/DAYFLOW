package com.dayflow.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object BgImage {

    private fun file(c: Context): File {
        return File(c.filesDir, "bg.jpg")
    }

    fun has(c: Context): Boolean {
        return file(c).exists()
    }

    fun clear(c: Context) {
        val f = file(c)
        if (f.exists()) f.delete()
    }

    fun save(c: Context, uri: Uri): Boolean {
        try {
            val opt = BitmapFactory.Options()
            opt.inSampleSize = 2
            val input = c.contentResolver.openInputStream(uri) ?: return false
            val bmp = BitmapFactory.decodeStream(input, null, opt)
            input.close()
            if (bmp == null) return false
            val out = FileOutputStream(file(c))
            bmp.compress(Bitmap.CompressFormat.JPEG, 85, out)
            out.close()
            return true
        } catch (e: Exception) {
            return false
        }
    }

    fun drawable(c: Context): BitmapDrawable? {
        val f = file(c)
        if (!f.exists()) return null
        val bmp = BitmapFactory.decodeFile(f.absolutePath) ?: return null
        return BitmapDrawable(c.resources, bmp)
    }
}
