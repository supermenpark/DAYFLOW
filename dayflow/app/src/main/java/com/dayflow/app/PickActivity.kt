
package com.dayflow.app

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast

class PickActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val group = intent.getStringExtra("group") ?: "memo"

        try {
            val sp = getSharedPreferences(
                "dayflow_music",
                Context.MODE_PRIVATE
            )

            val old = sp.getString("list", "") ?: ""
            val saved = old.split("\n")
                .filter { it.isNotBlank() }
                .toMutableList()

            val existing = saved.mapNotNull {
                it.split("|", limit = 3).getOrNull(1)
            }.toHashSet()

            val collection =
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST
            )

            val selection =
                "${MediaStore.Audio.Media.IS_MUSIC} != 0"

            var added = 0

            contentResolver.query(
                collection,
                projection,
                selection,
                null,
                "${MediaStore.Audio.Media.TITLE} ASC"
            )?.use { cursor ->

                val idCol = cursor.getColumnIndexOrThrow(
                    MediaStore.Audio.Media._ID
                )

                val titleCol = cursor.getColumnIndexOrThrow(
                    MediaStore.Audio.Media.TITLE
                )

                val artistCol = cursor.getColumnIndexOrThrow(
                    MediaStore.Audio.Media.ARTIST
                )

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)

                    val title = cursor.getString(titleCol)
                        ?: "이름 없는 음악"

                    val artist = cursor.getString(artistCol) ?: ""

                    val uri = android.content.ContentUris
                        .withAppendedId(collection, id)

                    val link = uri.toString()

                    if (!existing.contains(link)) {
                        val displayName =
                            if (artist.isNotBlank() &&
                                artist != "<unknown>"
                            ) "$title - $artist" else title

                        saved.add(
                            0,
                            displayName.replace("|", " ")
                                .replace("\n", " ") +
                                "|" + link + "|" + group
                        )

                        existing.add(link)
                        added++
                    }
                }
            }

            sp.edit()
                .putString("list", saved.joinToString("\n"))
                .apply()

            Toast.makeText(
                this,
                if (added > 0) {
                    "${added}곡을 DAYFLOW에 담았습니다."
                } else {
                    "새로 가져올 음악이 없습니다."
                },
                Toast.LENGTH_LONG
            ).show()

        } catch (e: Exception) {
            Toast.makeText(
                this,
                "음악 검색 오류: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }

        finish()
    }
}
