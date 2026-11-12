
package com.dayflow.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast

class MusicShareActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        receiveMusic(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        receiveMusic(intent)
    }

    private fun receiveMusic(data: Intent?) {
        if (data == null) {
            finish()
            return
        }

        val uris = ArrayList<Uri>()

        try {
            data.clipData?.let { clip ->
                for (i in 0 until clip.itemCount) {
                    uris.add(clip.getItemAt(i).uri)
                }
            }

            if (uris.isEmpty()) {
                if (android.os.Build.VERSION.SDK_INT >= 33) {
                    data.getParcelableExtra(
                        Intent.EXTRA_STREAM,
                        Uri::class.java
                    )?.let { uris.add(it) }
                } else {
                    @Suppress("DEPRECATION")
                    (data.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))
                        ?.let { uris.add(it) }
                }
            }

            if (uris.isEmpty() && data.action == Intent.ACTION_SEND_MULTIPLE) {
                if (android.os.Build.VERSION.SDK_INT >= 33) {
                    val list = data.getParcelableArrayListExtra(
                        Intent.EXTRA_STREAM,
                        Uri::class.java
                    )
                    list?.let { uris.addAll(it) }
                } else {
                    @Suppress("DEPRECATION")
                    val list = data.getParcelableArrayListExtra<Uri>(
                        Intent.EXTRA_STREAM
                    )
                    list?.let { uris.addAll(it) }
                }
            }
        } catch (e: Exception) {
        }

        val uniqueUris = uris.distinct()

        if (uniqueUris.isEmpty()) {
            Toast.makeText(
                this,
                "삼성 뮤직에서 음악 파일을 전달하지 않았어요.",
                Toast.LENGTH_LONG
            ).show()
            finish()
            return
        }

        val prefs = getSharedPreferences(
            "dayflow_music",
            Context.MODE_PRIVATE
        )

        val old = prefs.getString("list", "") ?: ""
        val added = StringBuilder()

        var count = 0

        for (uri in uniqueUris) {
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                // 공유 제공자가 영구 권한을 지원하지 않을 수도 있음
            }

            val title = getMusicName(uri)
            val uriText = uri.toString().replace("|", "%7C")

            val entry = "$title|$uriText|삼성뮤직"

            if (!old.contains(entry) && !added.toString().contains(entry)) {
                added.append(entry).append("\n")
                count++
            }
        }

        if (count > 0) {
            prefs.edit()
                .putString("list", added.toString() + old)
                .apply()
        }

        Toast.makeText(
            this,
            if (count > 0) "${count}곡이 DAYFLOW 음악 저장소에 추가됐어요."
            else "이미 저장된 음악이에요.",
            Toast.LENGTH_LONG
        ).show()

        finish()
    }

    private fun getMusicName(uri: Uri): String {
        var name = ""

        try {
            contentResolver.query(
                uri,
                null,
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(
                        OpenableColumns.DISPLAY_NAME
                    )
                    if (index >= 0) {
                        name = cursor.getString(index) ?: ""
                    }
                }
            }
        } catch (e: Exception) {
        }

        if (name.isBlank()) name = "삼성뮤직 음악"

        val dot = name.lastIndexOf(".")
        if (dot > 0) name = name.substring(0, dot)

        return name.replace("|", " ").replace("\n", " ")
    }
}
