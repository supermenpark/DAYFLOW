
package com.dayflow.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.util.Calendar

class DayActivity : Activity() {

    private var y = 0
    private var m = 0
    private var d = 0

    private lateinit var eventBox: LinearLayout
    private lateinit var titleInput: EditText
    private lateinit var hourInput: EditText
    private lateinit var minInput: EditText
    private lateinit var memoInput: EditText
    private lateinit var chkSame: CheckBox
    private lateinit var chkDay1: CheckBox
    private lateinit var chkDay2: CheckBox
    private lateinit var musicInfo: TextView

    private fun dp(v: Int): Int {
        return (v * resources.displayMetrics.density).toInt()
    }

    private fun label(
        s: String,
        size: Float,
        bold: Boolean,
        color: String
    ): TextView {
        val t = TextView(this)
        t.text = s
        t.textSize = size
        t.setTextColor(Color.parseColor(color))
        if (bold) t.setTypeface(null, Typeface.BOLD)
        return t
    }

    private fun round(color: String, r: Int): GradientDrawable {
        val g = GradientDrawable()
        g.setColor(Color.parseColor(color))
        g.cornerRadius = dp(r).toFloat()
        return g
    }

    private fun card(): LinearLayout {
        val c = LinearLayout(this)
        c.orientation = LinearLayout.VERTICAL
        c.background = round("#FFFFFF", 18)
        c.elevation = dp(3).toFloat()
        c.setPadding(dp(16), dp(14), dp(16), dp(14))
        return c
    }

    private fun addCard(root: LinearLayout, c: View) {
        val lp = LinearLayout.LayoutParams(-1, -2)
        lp.setMargins(dp(14), 0, dp(14), dp(12))
        root.addView(c, lp)
    }

    private fun button(text: String, color: String): TextView {
        val b = label(text, 15f, true, "#FFFFFF")
        b.gravity = Gravity.CENTER
        b.background = round(color, 14)
        b.setPadding(0, dp(13), 0, dp(13))
        return b
    }

    // 날짜별 음악 저장 위치
    private fun musicPrefs() =
        getSharedPreferences("dayflow_memo_music", Context.MODE_PRIVATE)

    private fun musicKey(): String {
        return "${y}_${m}_${d}"
    }

    // 저장된 음악 제목과 주소 불러오기
    private fun loadMemoMusic(): Pair<String, String>? {
        val raw = musicPrefs().getString(musicKey(), null) ?: return null
        val parts = raw.split("\n", limit = 2)
        if (parts.size < 2) return null
        if (parts[1].isBlank()) return null
        return Pair(parts[0], parts[1])
    }

    // 선택한 음악을 해당 날짜에 저장
    private fun saveMemoMusic(title: String, uri: String) {
        musicPrefs().edit()
            .putString(musicKey(), title + "\n" + uri)
            .apply()
        showMemoMusic()
    }

    // 메모 음악 표시 갱신
    private fun showMemoMusic() {
        val selected = loadMemoMusic()
        if (selected == null) {
            musicInfo.text = "아직 연결된 음악이 없어요."
        } else {
            musicInfo.text = "🎵 ${selected.first}"
        }
    }

    // 기존 플레이리스트에서 휴대폰 음악 선택
    private fun selectMemoMusic() {
        val raw = getSharedPreferences(
            "dayflow_music",
            Context.MODE_PRIVATE
        ).getString("list", "") ?: ""

        val titles = ArrayList<String>()
        val uris = ArrayList<String>()

        for (line in raw.split("\n")) {
            if (line.isBlank()) continue

            val p = line.split("|")
            if (p.size < 2) continue

            val title = p[0].trim()
            val uri = p[1].trim()

            // 휴대폰에 저장된 음악만 선택
            if (uri.startsWith("content://")) {
                titles.add(title)
                uris.add(uri.replace("%7C", "|"))
            }
        }

        if (titles.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("내 음악")
                .setMessage(
                    "저장된 휴대폰 음악이 없어요.\n\n" +
                    "음악 메뉴에서 '폰 음악 파일 담기'를 눌러 " +
                    "먼저 음악을 추가해 주세요."
                )
                .setPositiveButton("확인", null)
                .show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("메모에 연결할 음악 선택")
            .setItems(titles.toTypedArray()) { _, which ->
                saveMemoMusic(titles[which], uris[which])
                Toast.makeText(
                    this,
                    "메모에 음악이 연결됐어요.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    // 날짜별 음악 카드
    private fun buildMusicCard(root: LinearLayout) {
        val mc = card()

        mc.addView(
            label("🎵 메모 음악", 17f, true, "#222222")
        )

        musicInfo = label(
            "아직 연결된 음악이 없어요.",
            14f,
            false,
            "#667085"
        )
        musicInfo.setPadding(0, dp(10), 0, dp(12))
        mc.addView(musicInfo)

        val pick = button("내 음악 목록에서 선택", "#2F86EA")
        pick.setOnClickListener {
            selectMemoMusic()
        }
        mc.addView(pick)

        val play = button("▶  연결된 음악 재생", "#2FA866")
        val plp = LinearLayout.LayoutParams(-1, -2)
        plp.setMargins(0, dp(8), 0, 0)
        mc.addView(play, plp)

        play.setOnClickListener {
            val selected = loadMemoMusic()
            if (selected == null) {
                Toast.makeText(
                    this,
                    "먼저 음악을 연결해 주세요.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                val ok = LocalMusic.play(this, selected.second)
                if (!ok) {
                    Toast.makeText(
                        this,
                        "음악을 재생할 앱이 없어요.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        val unlink = button("음악 연결 해제", "#8995A7")
        val ulp = LinearLayout.LayoutParams(-1, -2)
        ulp.setMargins(0, dp(8), 0, 0)
        mc.addView(unlink, ulp)

        unlink.setOnClickListener {
            musicPrefs().edit().remove(musicKey()).apply()
            showMemoMusic()
            Toast.makeText(
                this,
                "음악 연결을 해제했어요.",
                Toast.LENGTH_SHORT
            ).show()
        }

        showMemoMusic()
        addCard(root, mc)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        y = intent.getIntExtra("y", 2026)
        m = intent.getIntExtra("m", 1)
        d = intent.getIntExtra("d", 1)

        val scroll = ScrollView(this)
        scroll.setBackgroundColor(Color.parseColor("#F4F7FB"))

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        scroll.addView(root)

        val c = Calendar.getInstance()
        c.set(y, m - 1, d, 12, 0, 0)

        val names = arrayOf("일", "월", "화", "수", "목", "금", "토")
        val dow = names[c.get(Calendar.DAY_OF_WEEK) - 1]
        val l = CalData.lunarOf(y, m, d)

        val header = LinearLayout(this)
        header.orientation = LinearLayout.VERTICAL
        header.background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(
                Color.parseColor("#6FB7F2"),
                Color.parseColor("#A9D8F8")
            )
        )
        header.setPadding(dp(18), dp(36), dp(18), dp(40))

        val back = label("‹ 달력으로", 15f, true, "#FFFFFF")
        back.setPadding(0, 0, 0, dp(10))
        back.setOnClickListener { finish() }
        header.addView(back)

        header.addView(
            label(
                m.toString() + "월 " + d + "일 " + dow + "요일",
                26f,
                true,
                "#FFFFFF"
            )
        )

        val sb = StringBuilder(y.toString() + "년  ·  음력 ")
        if (l[2] == 1) sb.append("윤")
        sb.append(l[0]).append("월 ").append(l[1]).append("일")

        val h = CalData.holidayOf(y, m, d)
        if (h != null) sb.append("  ·  ").append(h)

        val t = CalData.termOf(m, d)
        if (t != null) sb.append("  ·  ").append(t)

        header.addView(
            label(sb.toString(), 13f, false, "#EAF5FF")
        )

        root.addView(header, LinearLayout.LayoutParams(-1, -2))

        val spacer = View(this)
        root.addView(spacer, LinearLayout.LayoutParams(-1, dp(-20)))

        val fc = card()
        val fr = LinearLayout(this)
        fr.orientation = LinearLayout.HORIZONTAL
        fr.gravity = Gravity.CENTER_VERTICAL

        val fl = LinearLayout(this)
        fl.orientation = LinearLayout.VERTICAL
        fl.addView(label("오늘의 운세 (샘플)", 13f, false, "#777777"))
        fl.addView(
            label(Fortune.message(y, m, d), 14f, false, "#333333")
        )

        fr.addView(fl, LinearLayout.LayoutParams(0, -2, 1f))
        fr.addView(
            label(
                Fortune.score(y, m, d, 0).toString() + "점",
                28f,
                true,
                "#6A3DE8"
            )
        )
        fc.addView(fr)

        val dt = label(
            "금전 " + Fortune.score(y, m, d, 1) +
                "   사업 " + Fortune.score(y, m, d, 2) +
                "   애정 " + Fortune.score(y, m, d, 3) +
                "   건강 " + Fortune.score(y, m, d, 4) +
                "\n행운의 숫자 " + Fortune.lucky(y, m, d),
            13f,
            false,
            "#555555"
        )
        dt.setPadding(0, dp(8), 0, 0)
        fc.addView(dt)

        val wl = label(
            "☀ " + Weather.detail(this, y, m, d),
            14f,
            false,
            "#1976D2"
        )
        wl.setPadding(0, dp(8), 0, 0)
        fc.addView(wl)
        addCard(root, fc)

        // 일정 카드
        val ec = card()
        ec.addView(label("일정", 16f, true, "#222222"))

        eventBox = LinearLayout(this)
        eventBox.orientation = LinearLayout.VERTICAL
        ec.addView(eventBox)

        val timeRow = LinearLayout(this)
        timeRow.orientation = LinearLayout.HORIZONTAL
        timeRow.gravity = Gravity.CENTER_VERTICAL

        hourInput = EditText(this)
        hourInput.hint = "시"
        hourInput.inputType = InputType.TYPE_CLASS_NUMBER
        timeRow.addView(hourInput, LinearLayout.LayoutParams(dp(56), -2))

        timeRow.addView(label(" : ", 16f, false, "#333333"))

        minInput = EditText(this)
        minInput.hint = "분"
        minInput.inputType = InputType.TYPE_CLASS_NUMBER
        timeRow.addView(minInput, LinearLayout.LayoutParams(dp(56), -2))

        titleInput = EditText(this)
        titleInput.hint = "일정 제목"
        titleInput.setSingleLine(true)
        timeRow.addView(titleInput, LinearLayout.LayoutParams(0, -2, 1f))
        ec.addView(timeRow)

        val chkRow = LinearLayout(this)
        chkRow.orientation = LinearLayout.HORIZONTAL

        chkSame = CheckBox(this)
        chkSame.text = "당일"
        chkSame.isChecked = true
        chkRow.addView(chkSame)

        chkDay1 = CheckBox(this)
        chkDay1.text = "하루 전"
        chkRow.addView(chkDay1)

        chkDay2 = CheckBox(this)
        chkDay2.text = "이틀 전"
        chkRow.addView(chkDay2)

        ec.addView(chkRow)

        val addEv = button("일정 추가 + 알림 예약", "#2F7BE8")
        addEv.setOnClickListener { addEvent() }
        ec.addView(addEv)

        addCard(root, ec)

        // 메모 카드
        val mc = card()
        mc.addView(label("메모", 16f, true, "#222222"))

        memoInput = EditText(this)
        memoInput.hint = "이 날의 메모"
        memoInput.minLines = 4
        memoInput.gravity = Gravity.TOP
        memoInput.background = round("#FFF6CC", 12)
        memoInput.setPadding(
            dp(12), dp(12), dp(12), dp(12)
        )
        memoInput.setText(Scheduler.getMemo(this, y, m, d))

        val mlp = LinearLayout.LayoutParams(-1, -2)
        mlp.setMargins(0, dp(8), 0, dp(10))
        mc.addView(memoInput, mlp)

        val save = button("메모 저장", "#6A3DE8")
        save.setOnClickListener {
            Scheduler.setMemo(
                this,
                y,
                m,
                d,
                memoInput.text.toString().trim()
            )
            Toast.makeText(
                this,
                "메모 저장됨",
                Toast.LENGTH_SHORT
            ).show()
        }
        mc.addView(save)
        addCard(root, mc)

        // 새로 추가한 날짜별 음악 연결 카드
        buildMusicCard(root)

        val pad = View(this)
        root.addView(pad, LinearLayout.LayoutParams(-1, dp(20)))

        setContentView(scroll)
        showEvents()
    }

    private fun addEvent() {
        val h = hourInput.text.toString().trim().toIntOrNull()
        val mi = minInput.text.toString().trim().toIntOrNull()

        val title = titleInput.text.toString()
            .trim()
            .replace("|", " ")
            .replace("\n", " ")

        if (
            h == null || mi == null ||
            h < 0 || h > 23 ||
            mi < 0 || mi > 59
        ) {
            Toast.makeText(
                this,
                "시(0-23)와 분(0-59)을 입력하세요",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (title.isEmpty()) {
            Toast.makeText(
                this,
                "일정 제목을 입력하세요",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val codes = ArrayList<String>()
        if (chkSame.isChecked) codes.add("0")
        if (chkDay1.isChecked) codes.add("1")
        if (chkDay2.isChecked) codes.add("2")

        for (code in codes) {
            Scheduler.schedule(
                this, y, m, d, h, mi,
                code.toInt(), title
            )
        }

        val list = Scheduler.loadEvents(this, y, m, d)
        list.add(
            h.toString() + "|" + mi + "|" +
                title + "|" + codes.joinToString(",")
        )
        Scheduler.saveEvents(this, y, m, d, list)

        hourInput.setText("")
        minInput.setText("")
        titleInput.setText("")

        showEvents()

        Toast.makeText(
            this,
            "일정과 알림이 저장됨",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun showEvents() {
        eventBox.removeAllViews()

        val events = Scheduler.loadEvents(this, y, m, d)

        if (events.isEmpty()) {
            val none = label(
                "등록된 일정이 없어요",
                14f,
                false,
                "#999999"
            )
            none.setPadding(0, dp(6), 0, dp(6))
            eventBox.addView(none)
        }

        for (i in events.indices) {
            val parts = events[i].split("|")
            if (parts.size < 3) continue

            val hh = parts[0].toIntOrNull() ?: 0
            val mm = parts[1].toIntOrNull() ?: 0
            val codes = if (parts.size > 3) parts[3] else ""

            val an = ArrayList<String>()
            for (cd in codes.split(",")) {
                if (cd == "0") an.add("당일")
                if (cd == "1") an.add("하루 전")
                if (cd == "2") an.add("이틀 전")
            }

            var line = "● " +
                String.format("%02d:%02d", hh, mm) +
                "  " + parts[2]

            if (an.isNotEmpty()) {
                line += "\n    알림 " +
                    an.joinToString(", ") +
                    "  (길게 누르면 삭제)"
            }

            val tv = label(line, 15f, false, "#222222")
            tv.background = round("#EAF3FF", 10)
            tv.setPadding(
                dp(12), dp(10), dp(12), dp(10)
            )

            val idx = i
            tv.setOnLongClickListener {
                removeEvent(idx)
                true
            }

            val lp = LinearLayout.LayoutParams(-1, -2)
            lp.setMargins(0, dp(4), 0, dp(4))
            eventBox.addView(tv, lp)
        }
    }

    private fun removeEvent(idx: Int) {
        val cur = Scheduler.loadEvents(this, y, m, d)

        if (idx < cur.size) {
            val p = cur[idx].split("|")

            if (p.size > 3) {
                val h = p[0].toIntOrNull() ?: 0
                val mi = p[1].toIntOrNull() ?: 0

                for (cd in p[3].split(",")) {
                    val off = cd.toIntOrNull()
                    if (off != null) {
                        Scheduler.cancel(this, y, m, d, h, mi, off)
                    }
                }
            }

            cur.removeAt(idx)
            Scheduler.saveEvents(this, y, m, d, cur)
        }

        showEvents()
    }
}
