
package com.dayflow.app

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.provider.MediaStore
import android.net.Uri
import android.os.Build
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

object MusicView {

    private var current = "전체"
    private var queue = mutableListOf<Pair<String, String>>()
    private var queueIndex = -1
    private var nowPlaying = ""

    private fun dp(c: Context, v: Int) =
        (v * c.resources.displayMetrics.density).toInt()

    private fun tv(
        c: Context, s: String, size: Float,
        bold: Boolean, color: String
    ): TextView {
        return TextView(c).apply {
            text = s
            textSize = size
            setTextColor(Color.parseColor(color))
            if (bold) setTypeface(null, Typeface.BOLD)
        }
    }

    private fun shape(c: Context, color: String, r: Int) =
        GradientDrawable().apply {
            setColor(Color.parseColor(color))
            cornerRadius = dp(c, r).toFloat()
        }

    private fun button(
        c: Context, text: String, color: String
    ): TextView {
        return tv(c, text, 14f, true, "#FFFFFF").apply {
            gravity = Gravity.CENTER
            background = shape(c, color, 18)
            setPadding(dp(c, 12), dp(c, 13), dp(c, 12), dp(c, 13))
        }
    }

    private fun prefs(c: Context) =
        c.getSharedPreferences("dayflow_music", Context.MODE_PRIVATE)

    private fun load(c: Context): MutableList<String> =
        (prefs(c).getString("list", "") ?: "")
            .split("\n").filter { it.isNotEmpty() }.toMutableList()

    private fun save(c: Context, list: List<String>) {
        prefs(c).edit().putString("list", list.joinToString("\n")).apply()
    }

    private fun lists(c: Context): MutableList<String> =
        (prefs(c).getString("names", "") ?: "")
            .split("\n").filter { it.isNotEmpty() }.toMutableList()

    private fun saveLists(c: Context, names: List<String>) {
        prefs(c).edit().putString("names", names.joinToString("\n")).apply()
    }

    private fun clean(s: String) =
        s.trim().replace("|", " ").replace("\n", " ")

    private fun permission(): String {
        return if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    private fun importPhoneMusic(c: Context, sv: ScrollView) {
        if (Build.VERSION.SDK_INT >= 23 &&
            c.checkSelfPermission(permission()) != PackageManager.PERMISSION_GRANTED
        ) {
            if (c is Activity) {
                c.requestPermissions(arrayOf(permission()), 2026)
                Toast.makeText(
                    c,
                    "음악 접근을 허용한 뒤 불러오기 버튼을 다시 눌러 주세요.",
                    Toast.LENGTH_LONG
                ).show()
            }
            return
        }

        val all = load(c)
        val existing = all.mapNotNull {
            val p = it.split("|", limit = 3)
            p.getOrNull(1)
        }.toHashSet()

        var added = 0

        try {
            val collection =
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST
            )

            val selection =
                "${MediaStore.Audio.Media.IS_MUSIC} != 0"

            val sortOrder =
                "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

            c.contentResolver.query(
                collection,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->

                val idColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)

                val titleColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)

                val artistColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val title = cursor.getString(titleColumn) ?: "이름 없는 음악"
                    val artist = cursor.getString(artistColumn) ?: ""

                    val uri = ContentUris.withAppendedId(collection, id)
                    val link = uri.toString()

                    if (!existing.contains(link)) {
                        val displayTitle =
                            if (artist.isNotBlank() && artist != "<unknown>") {
                                "$title - $artist"
                            } else {
                                title
                            }

                        all.add(
                            0,
                            clean(displayTitle) + "|" + link + "|" +
                                (if (current == "전체") "" else current)
                        )

                        existing.add(link)
                        added++
                    }
                }
            }

            save(c, all)

            Toast.makeText(
                c,
                if (added > 0) {
                    "${added}곡을 DAYFLOW에 추가했어요."
                } else {
                    "새로 가져올 음악이 없어요."
                },
                Toast.LENGTH_LONG
            ).show()

            refresh(c, sv)

        } catch (e: Exception) {
            Toast.makeText(
                c,
                "음악 목록을 읽지 못했어요: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun refresh(c: Context, sv: ScrollView) {
        val parent = sv.parent
        if (parent is ViewGroup) {
            val index = parent.indexOfChild(sv)
            parent.removeViewAt(index)
            parent.addView(
                build(c),
                index,
                FrameLayout.LayoutParams(-1, -1)
            )
        }
    }

    private fun play(
        c: Context,
        songs: List<Pair<String, String>>,
        index: Int,
        label: TextView
    ) {
        if (index !in songs.indices) return

        val song = songs[index]
        val title = song.first
        val link = song.second

        if (!link.startsWith("content://")) {
            val url = if (link.startsWith("http")) link
                else "https://www.youtube.com/results?search_query=" +
                    Uri.encode(title)

            try {
                c.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } catch (_: Exception) {
                Toast.makeText(c, "열 수 있는 앱이 없어요.", Toast.LENGTH_SHORT).show()
            }
            return
        }

        queue = songs.toMutableList()
        queueIndex = index

        if (LocalMusic.play(c, link)) {
            nowPlaying = title
            label.text = "🎵 재생 준비 중: $title"
        } else {
            label.text = "재생할 수 없습니다."
            Toast.makeText(c, "음악 파일을 열 수 없어요.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun input(c: Context, hint: String): EditText =
        EditText(c).apply {
            this.hint = hint
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_TEXT
            background = shape(c, "#F1F5FB", 12)
            setPadding(dp(c, 12), dp(c, 8), dp(c, 12), dp(c, 8))
        }

    fun build(c: Context): ScrollView {
        val sv = ScrollView(c)
        sv.setBackgroundColor(Color.parseColor("#F6F9FD"))

        val root = LinearLayout(c).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(c, 16), dp(c, 35), dp(c, 16), dp(c, 24))
        }
        sv.addView(root)

        root.addView(tv(c, "🎵 음악", 28f, true, "#0F2A55"))

        val importButton = button(
            c, "📂 휴대폰 음악 전체 불러오기", "#2FA866"
        )
        val bp = LinearLayout.LayoutParams(-1, -2)
        bp.setMargins(0, dp(c, 15), 0, dp(c, 10))
        root.addView(importButton, bp)

        importButton.setOnClickListener {
            importPhoneMusic(c, sv)
        }

        val nowBox = LinearLayout(c).apply {
            orientation = LinearLayout.VERTICAL
            background = shape(c, "#FFFFFF", 16)
            setPadding(dp(c, 14), dp(c, 12), dp(c, 14), dp(c, 12))
        }

        nowBox.addView(tv(c, "DAYFLOW 플레이어", 16f, true, "#0F2A55"))

        val nowLabel = tv(
            c,
            if (nowPlaying.isEmpty()) "재생할 곡을 선택하세요."
            else "🎵 $nowPlaying",
            14f, true, "#333333"
        )
        nowLabel.setPadding(0, dp(c, 10), 0, dp(c, 10))
        nowBox.addView(nowLabel)

        val controls = LinearLayout(c).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        val prev = button(c, "⏮ 이전", "#8A55D8")
        val toggle = button(c, "▶ / ⏸", "#2F86EA")
        val next = button(c, "다음 ⏭", "#8A55D8")

        controls.addView(prev, LinearLayout.LayoutParams(0, -2, 1f))
        controls.addView(toggle, LinearLayout.LayoutParams(0, -2, 1.2f))
        controls.addView(next, LinearLayout.LayoutParams(0, -2, 1f))
        nowBox.addView(controls)

        prev.setOnClickListener {
            if (queue.isNotEmpty() && queueIndex >= 0) {
                val i = if (queueIndex == 0) queue.size - 1 else queueIndex - 1
                play(c, queue, i, nowLabel)
            }
        }

        next.setOnClickListener {
            if (queue.isNotEmpty() && queueIndex >= 0) {
                play(c, queue, (queueIndex + 1) % queue.size, nowLabel)
            }
        }

        toggle.setOnClickListener {
            if (LocalMusic.toggle()) {
                nowLabel.text = "🎵 $nowPlaying" +
                    if (LocalMusic.isPlaying()) " (재생 중)" else " (일시정지)"
            } else {
                Toast.makeText(c, "먼저 곡을 선택하세요.", Toast.LENGTH_SHORT).show()
            }
        }

        val nowParams = LinearLayout.LayoutParams(-1, -2)
        nowParams.setMargins(0, 0, 0, dp(c, 15))
        root.addView(nowBox, nowParams)

        val names = lists(c)
        val all = load(c)

        val tabs = LinearLayout(c).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val allTab = ArrayList<String>()
        allTab.add("전체")
        allTab.addAll(names)

        for (name in allTab) {
            val chip = button(
                c, name,
                if (current == name) "#2F7BE8" else "#8DA9D0"
            )
            val cp = LinearLayout.LayoutParams(-2, -2)
            cp.setMargins(0, 0, dp(c, 6), dp(c, 8))
            tabs.addView(chip, cp)
            chip.setOnClickListener {
                current = name
                refresh(c, sv)
            }
        }

        val newList = button(c, "＋ 리스트", "#8A55D8")
        tabs.addView(newList)
        newList.setOnClickListener {
            val edit = input(c, "리스트 이름")
            AlertDialog.Builder(c)
                .setTitle("새 플레이리스트")
                .setView(edit)
                .setPositiveButton("만들기") { _, _ ->
                    val name = clean(edit.text.toString())
                    val currentNames = lists(c)
                    if (name.isNotEmpty() && name != "전체" &&
                        !currentNames.contains(name)
                    ) {
                        currentNames.add(name)
                        saveLists(c, currentNames)
                        current = name
                    }
                    refresh(c, sv)
                }
                .setNegativeButton("취소", null)
                .show()
        }

        root.addView(tabs)

        val addForm = LinearLayout(c).apply {
            orientation = LinearLayout.VERTICAL
            background = shape(c, "#FFFFFF", 16)
            setPadding(dp(c, 12), dp(c, 12), dp(c, 12), dp(c, 12))
        }

        addForm.addView(tv(c, "직접 추가", 15f, true, "#222222"))
        val titleInput = input(c, "곡 제목")
        val linkInput = input(c, "링크 (선택)")

        addForm.addView(titleInput)
        addForm.addView(linkInput)

        val addButton = button(c, "＋ 저장", "#2F86EA")
        addForm.addView(addButton)

        addButton.setOnClickListener {
            val title = clean(titleInput.text.toString())
            val link = clean(linkInput.text.toString())

            if (title.isEmpty()) {
                Toast.makeText(c, "곡 제목을 입력하세요.", Toast.LENGTH_SHORT).show()
            } else {
                val updated = load(c)
                updated.add(
                    0,
                    title + "|" + link + "|" +
                        (if (current == "전체") "" else current)
                )
                save(c, updated)
                refresh(c, sv)
            }
        }

        val formParams = LinearLayout.LayoutParams(-1, -2)
        formParams.setMargins(0, dp(c, 10), 0, dp(c, 12))
        root.addView(addForm, formParams)

        if (current != "전체") {
            val deleteList = tv(
                c, "이 리스트 삭제", 12f, false, "#E0526F"
            )
            root.addView(deleteList)
            deleteList.setOnClickListener {
                val updatedNames = lists(c)
                updatedNames.remove(current)
                saveLists(c, updatedNames)

                val updated = load(c).map { raw ->
                    val p = raw.split("|", limit = 3)
                    if (p.size > 2 && p[2] == current) {
                        p[0] + "|" + p[1] + "|"
                    } else raw
                }
                save(c, updated)
                current = "전체"
                refresh(c, sv)
            }
        }

        val visible = all.map { raw ->
            val p = raw.split("|", limit = 3)
            Triple(
                p.getOrElse(0) { "" },
                p.getOrElse(1) { "" }.replace("%7C", "|"),
                p.getOrElse(2) { "" }
            )
        }.filter {
            current == "전체" || it.third == current
        }

        if (visible.isEmpty()) {
            val empty = tv(
                c,
                "아직 음악이 없어요.\n위의 휴대폰 음악 전체 불러오기를 눌러 주세요.",
                14f, false, "#8895A7"
            )
            empty.gravity = Gravity.CENTER
            empty.setPadding(0, dp(c, 24), 0, 0)
            root.addView(empty)
        }

        val songs = visible.map { it.first to it.second }

        visible.forEachIndexed { index, song ->
            val card = LinearLayout(c).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = shape(c, "#FFFFFF", 14)
                setPadding(dp(c, 10), dp(c, 12), dp(c, 8), dp(c, 12))
            }

            val title = tv(c, "🎵 ${song.first}", 14f, true, "#222222")
            card.addView(title, LinearLayout.LayoutParams(0, -2, 1f))

            val playButton = button(c, "▶", "#2F86EA")
            card.addView(playButton)

            val deleteButton = button(c, "✕", "#8895A7")
            card.addView(deleteButton)

            val action = View.OnClickListener {
                play(c, songs, index, nowLabel)
            }
            card.setOnClickListener(action)
            playButton.setOnClickListener(action)

            deleteButton.setOnClickListener {
                val updated = load(c)
                val originalIndex = all.indexOfFirst {
                    val p = it.split("|", limit = 3)
                    p.getOrElse(0) { "" } == song.first &&
                        p.getOrElse(1) { "" }.replace("%7C", "|") == song.second
                }
                if (originalIndex >= 0) {
                    updated.removeAt(originalIndex)
                    save(c, updated)
                    refresh(c, sv)
                }
            }

            val itemParams = LinearLayout.LayoutParams(-1, -2)
            itemParams.setMargins(0, dp(c, 5), 0, dp(c, 5))
            root.addView(card, itemParams)
        }

        return sv
    }
}
