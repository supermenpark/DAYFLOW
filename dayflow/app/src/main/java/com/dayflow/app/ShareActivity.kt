package com.dayflow.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast

class ShareActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var title = ""
        var link = ""
        val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
        val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT) ?: ""
        val stream: Uri? = intent.getParcelableExtra(Intent.EXTRA_STREAM)

        if (stream != null) {
            link = stream.toString()
            title = if (subject.isNotEmpty()) subject else "공유된 음악"
        } else if (text.isNotEmpty()) {
            val idx = text.indexOf("http")
            if (idx >= 0) {
                link = text.substring(idx).trim().split(" ", "\n")[0]
                title = text.substring(0, idx).trim()
            } else {
                title = text.trim()
            }
            if (title.isEmpty()) title = if (subject.isNotEmpty()) subject else "공유된 음악"
        }
        title = title.replace("|", " ").replace("\n", " ")
        link = link.replace("|", "%7C")

        if (title.isEmpty() && link.isEmpty()) {
            Toast.makeText(this, "저장할 내용이 없어요", Toast.LENGTH_SHORT).show()
        } else {
            val sp = getSharedPreferences("dayflow_music", Context.MODE_PRIVATE)
            val old = sp.getString("list", "") ?: ""
            sp.edit().putString("list", title + "|" + link + "|\n" + old).apply()
            Toast.makeText(this, "음악 칸에 저장됨: " + title, Toast.LENGTH_LONG).show()
        }
        finish()
    }
}
