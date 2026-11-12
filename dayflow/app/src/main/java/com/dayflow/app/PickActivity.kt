
package com.dayflow.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast

class PickActivity : Activity() {

    private var group = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        group = intent.getStringExtra("group") ?: "memo"

        // 삼성뮤직 등 다른 앱에서 공유한 음악 받기
        if (intent.action == Intent.ACTION_SEND ||
            intent.action == Intent.ACTION_SEND_MULTIPLE
        ) {
            receiveSharedMusic()
            return
        }

        // 기존 휴대폰 음악 파일 선택 기능 유지
        val i = Intent(Intent.ACTION_OPEN_DOCUMENT)
        i.type = "audio/*"
        i.addCategory(Intent.CATEGORY_OPENABLE)
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        i.addFlags(
            Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )

        try {
            startActivityForResult(i, 5)
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "파일 선택 화면을 열 수 없어요",
                Toast.LENGTH_SHORT
            ).show()
            finish()
        }
    }

    private fun receiveSharedMusic() {
        val uris = ArrayList<Uri>()

        try {
            if (intent.action == Intent.ACTION_SEND) {
                val uri = intent.getParcelableExtra<Uri>(
                    Intent.EXTRA_STREAM
                ) ?: intent.data

                if (uri != null) uris.add(uri)

            } else if (intent.action == Intent.ACTION_SEND_MULTIPLE) {
                val list = intent.getParcelableArrayListExtra<Uri>(
                    Intent.EXTRA_STREAM
                )

                if (list != null) uris.addAll(list)
            }

            // 공유 앱이 ClipData로 전달하는 경우도 처리
            val clip = intent.clipData

            if (clip != null) {
                for (i in 0 until clip.itemCount) {
                    val uri = clip.getItemAt(i).uri
                    if (!uris.contains(uri)) uris.add(uri)
                }
            }

            if (uris.isEmpty()) {
                Toast.makeText(
                    this,
                    "전달받은 음악이 없습니다",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                saveMusic(uris)
            }

        } catch (e: Exception) {
            Toast.makeText(
                this,
                "음악을 가져오지 못했습니다",
                Toast.LENGTH_SHORT
            ).show()
        }

        finish()
    }

    private fun nameOf(u: Uri): String {
        var n = ""

        try {
            val cur = contentResolver.query(
                u, null, null, null, null
            )

            if (cur != null) {
                if (cur.moveToFirst()) {
                    val k = cur.getColumnIndex(
                        OpenableColumns.DISPLAY_NAME
                    )
                    if (k >= 0) {
                        n = cur.getString(k) ?: ""
                    }
                }
                cur.close()
            }
        } catch (e: Exception) {
        }

        if (n.isEmpty()) n = "음악"

        val dot = n.lastIndexOf('.')
        if (dot > 0) n = n.substring(0, dot)

        return n.replace("|", " ")
            .replace("\n", " ")
    }

    private fun saveMusic(uris: ArrayList<Uri>) {
        val sp = getSharedPreferences(
            "dayflow_music",
            Context.MODE_PRIVATE
        )

        val old = sp.getString("list", "") ?: ""
        val sb = StringBuilder()

        for (u in uris) {
            try {
                contentResolver.takePersistableUriPermission(
                    u,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                // 외부 앱이 영구 권한을 주지 않는 경우
            }

            sb.append(nameOf(u))
                .append("|")
                .append(u.toString().replace("|", "%7C"))
                .append("|")
                .append(group)
                .append("\n")
        }

        sp.edit()
            .putString("list", sb.toString() + old)
            .apply()

        Toast.makeText(
            this,
            uris.size.toString() + "곡 저장됨",
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(
            requestCode,
            resultCode,
            data
        )

        if (requestCode == 5 &&
            resultCode == RESULT_OK &&
            data != null
        ) {
            val uris = ArrayList<Uri>()

            val clip = data.clipData

            if (clip != null) {
                for (k in 0 until clip.itemCount) {
                    uris.add(clip.getItemAt(k).uri)
                }
            } else if (data.data != null) {
                uris.add(data.data!!)
            }

            if (uris.isNotEmpty()) {
                saveMusic(uris)
            }
        }

        finish()
    }
}
