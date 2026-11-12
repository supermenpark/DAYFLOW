package com.dayflow.app

import android.content.Context
import android.content.Intent
import android.net.Uri

object LocalMusic {

    fun play(c: Context, uri: String): Boolean {
        return try {
            val i = Intent(Intent.ACTION_VIEW)
            i.setDataAndType(Uri.parse(uri), "audio/*")
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            c.startActivity(i)
            true
        } catch (e: Exception) {
            false
        }
    }
}
