package com.dayflow.app

import android.app.Activity
import android.app.AlertDialog
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast

object LocalMusic {

    const val REQ = 88

    fun permission(): String {
        return if (Build.VERSION.SDK_INT >= 33) {
            "android.permission.READ_MEDIA_AUDIO"
        } else {
            "android.permission.READ_EXTERNAL_STORAGE"
        }
    }

    fun granted(a: Activity): Boolean {
        return a.checkSelfPermission(permission()) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    fun ask(a: Activity) {
        a.requestPermissions(arrayOf(permission()), REQ)
    }

    // 폰의 음악 목록을 읽어 "제목 - 가수|content://uri" 형태로 돌려줌
    fun list(c: Context): List<String> {
        val out = ArrayList<String>()
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val cols = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST
        )
        val sel = MediaStore.Audio.Media.IS_MUSIC + " != 0"
        val cur = c.contentResolver.query(
            uri, cols, sel, null, MediaStore.Audio.Media.TITLE + " ASC"
        ) ?: return out
        while (cur.moveToNext() && out.size < 500) {
            val id = cur.getLong(0)
            val title = (cur.getString(1) ?: "").replace("|", " ")
            val artist = (cur.getString(2) ?: "").replace("|", " ")
            val u = ContentUris.withAppendedId(uri, id)
            val name = if (artist.isEmpty() || artist == "<unknown>") title else title + " - " + artist
            out.add(name + "|" + u.toString())
        }
        cur.close()
        return out
    }

    // 선택 창을 띄우고, 고른 곡을 콜백으로 전달
    
        val items = list(a)
        if (items.isEmpty()) {
            Toast.makeText(a, "폰에서 음악 파일을 찾지 못했어요", Toast.LENGTH_SHORT).show()
            return
        }
        val names = Array(items.size) { items[it].substringBefore("|") }
        AlertDialog.Builder(a)
            .setTitle("내 폰 음악 (" + items.size + "곡)")
               // 여러 곡을 체크해서 고르면 한꺼번에 콜백으로 전달
    fun pick(a: Activity, done: (List<String>) -> Unit) {
        val items = list(a)
        if (items.isEmpty()) {
            Toast.makeText(a, "폰에서 음악 파일을 찾지 못했어요", Toast.LENGTH_SHORT).show()
            return
        }
        val names = Array(items.size) { items[it].substringBefore("|") }
        val checked = BooleanArray(items.size)
        AlertDialog.Builder(a)
            .setTitle("담을 곡을 고르세요 (" + items.size + "곡)")
            .setMultiChoiceItems(names, checked) { _, which, isChecked ->
                checked[which] = isChecked
            }
            .setPositiveButton("저장") { _, _ ->
                val picked = ArrayList<String>()
                for (i in items.indices) {
                    if (checked[i]) picked.add(items[i])
                }
                done(picked)
            }
            .setNegativeButton("닫기", null)
            .show()
    } .setItems(names) { _, which -> done(items[which]) }
            .setNegativeButton("닫기", null)
            .show()
    }

    fun play(c: Context, uri: String): Boolean {
        return try {
            val i = Intent(Intent.ACTION_VIEW)
            i.setDataAndType(Uri.parse(uri), "audio/*")
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            c.startActivity(i)
            true
        } catch (e: Exception) {
            false
        }
    }
}
