package com.dayflow.app

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.Button
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

    private fun dp(v: Int): Int {
        return (v * resources.displayMetrics.density).toInt()
    }

    private fun label(s: String, size: Float, bold: Boolean): TextView {
        val t = TextView(this)
        t.text = s
        t.textSize = size
        if (bold) t.setTypeface(null, Typeface.BOLD)
        return t
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        y = intent.getIntExtra("y", 2026)
        m = intent.getIntExtra("m", 1)
        d = intent.getIntExtra("d", 1)

        val scroll = ScrollView(this)
        scroll.setBackgroundColor(Color.parseColor(Theme.bg(this)))
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(dp(16), dp(32), dp(16), dp(24))
        scroll.addView(root)

        val back = Button(this)
        back.text = "← 달력으로"
        back.setOnClickListener { finish() }
        Ui.style(this, back, 0)
        root.addView(back)

        val c = Calendar.getInstance()
        c.set(y, m - 1, d, 12, 0, 0)
        val names = arrayOf("일", "월", "화", "수", "목", "금", "토")
        val dow = names[c.get(Calendar.DAY_OF_WEEK) - 1]
        val l = CalData.lunarOf(y, m, d)

        val head = label(y.toString() + "년 " + m + "월 " + d + "일 (" + dow + ")", 22f, true)
        head.setPadding(0, dp(12), 0, dp(4))
        root.addView(head)

        val sb = StringBuilder("음력 ")
        if (l[2] == 1) sb.append("윤")
        sb.append(l[0]).append("월 ").append(l[1]).append("일")
        val h = CalData.holidayOf(y, m, d)
        if (h != null) sb.append("  ·  ").append(h)
        val t = CalData.termOf(m, d)
        if (t != null) sb.append("  ·  절기 ").append(t)
        root.addView(label(sb.toString(), 15f, false))

        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.setBackgroundColor(Color.parseColor(Theme.card(this)))
        card.setPadding(dp(16), dp(14), dp(16), dp(14))
        card.addView(label("오늘의 운세 (샘플 점수)", 15f, true))
        val total = label(Fortune.score(y, m, d, 0).toString() + "점", 34f, true)
        total.setTextColor(Color.parseColor("#B8860B"))
        card.addView(total)
        card.addView(label(Fortune.message(y, m, d), 15f, false))
        val detail = "금전운 " + Fortune.score(y, m, d, 1) + "점   사업운 " + Fortune.score(y, m, d, 2) +
            "점\n애정운 " + Fortune.score(y, m, d, 3) + "점   건강운 " + Fortune.score(y, m, d, 4) +
            "점\n행운의 숫자 " + Fortune.lucky(y, m, d)
        val dt = label(detail, 15f, false)
        dt.setPadding(0, dp(8), 0, 0)
        card.addView(dt)
        val lp = LinearLayout.LayoutParams(-1, -2)
        lp.setMargins(0, dp(12), 0, dp(12))
        root.addView(card, lp)

        root.addView(label("일정 (길게 누르면 삭제)", 16f, true))
        eventBox = LinearLayout(this)
        eventBox.orientation = LinearLayout.VERTICAL
        root.addView(eventBox)

        val timeRow = LinearLayout(this)
        timeRow.orientation = LinearLayout.HORIZONTAL
        timeRow.gravity = Gravity.CENTER_VERTICAL
        hourInput = EditText(this)
        hourInput.hint = "시"
        hourInput.inputType = InputType.TYPE_CLASS_NUMBER
        timeRow.addView(hourInput, LinearLayout.LayoutParams(dp(60), -2))
        timeRow.addView(label(" : ", 16f, false))
        minInput = EditText(this)
        minInput.hint = "분"
        minInput.inputType = InputType.TYPE_CLASS_NUMBER
        timeRow.addView(minInput, LinearLayout.LayoutParams(dp(60), -2))
        titleInput = EditText(this)
        titleInput.hint = "일정 제목"
        titleInput.setSingleLine(true)
        timeRow.addView(titleInput, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(timeRow)

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
        root.addView(chkRow)

        val addEv = Button(this)
        addEv.text = "일정 추가 + 알림 예약"
        addEv.setOnClickListener { addEvent() }
        Ui.style(this, addEv, 2)
        root.addView(addEv)

        val mh = label("메모", 16f, true)
        mh.setPadding(0, dp(16), 0, 0)
        root.addView(mh)
        memoInput = EditText(this)
        memoInput.hint = "이 날의 메모"
        memoInput.minLines = 3
        memoInput.gravity = Gravity.TOP
        memoInput.setBackgroundColor(Color.parseColor(Theme.card(this)))
        memoInput.setPadding(dp(12), dp(12), dp(12), dp(12))
        memoInput.setText(Scheduler.getMemo(this, y, m, d))
        root.addView(memoInput)

        val save = Button(this)
        save.text = "메모 저장"
        save.setOnClickListener {
            Scheduler.setMemo(this, y, m, d, memoInput.text.toString().trim())
            Toast.makeText(this, "메모 저장됨", Toast.LENGTH_SHORT).show()
        }
        Ui.style(this, save, 3)
        root.addView(save)

        setContentView(scroll)
        showEvents()
    }

    private fun addEvent() {
        val h = hourInput.text.toString().trim().toIntOrNull()
        val mi = minInput.text.toString().trim().toIntOrNull()
        val title = titleInput.text.toString().trim().replace("|", " ").replace("\n", " ")
        if (h == null || mi == null || h < 0 || h > 23 || mi < 0 || mi > 59) {
            Toast.makeText(this, "시(0-23)와 분(0-59)을 입력하세요", Toast.LENGTH_SHORT).show()
            return
        }
        if (title.isEmpty()) {
            Toast.makeText(this, "일정 제목을 입력하세요", Toast.LENGTH_SHORT).show()
            return
        }
        val codes = ArrayList<String>()
        if (chkSame.isChecked) codes.add("0")
        if (chkDay1.isChecked) codes.add("1")
        if (chkDay2.isChecked) codes.add("2")
        for (code in codes) {
            Scheduler.schedule(this, y, m, d, h, mi, code.toInt(), title)
        }
        val list = Scheduler.loadEvents(this, y, m, d)
        list.add(h.toString() + "|" + mi + "|" + title + "|" + codes.joinToString(","))
        Scheduler.saveEvents(this, y, m, d, list)
        hourInput.setText("")
        minInput.setText("")
        titleInput.setText("")
        showEvents()
        Toast.makeText(this, "일정과 알림이 저장됨", Toast.LENGTH_SHORT).show()
    }

    private fun showEvents() {
        eventBox.removeAllViews()
        val events = Scheduler.loadEvents(this, y, m, d)
        if (events.isEmpty()) {
            val none = label("등록된 일정이 없어요", 14f, false)
            none.setTextColor(Color.parseColor("#888888"))
            none.setPadding(0, dp(4), 0, dp(4))
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
            var line = "● " + String.format("%02d:%02d", hh, mm) + "  " + parts[2]
            if (an.isNotEmpty()) line += "\n    알림: " + an.joinToString(", ")
            val tv = label(line, 15f, false)
            tv.setPadding(dp(8), dp(8), dp(8), dp(8))
            tv.setBackgroundColor(Color.parseColor("#FFF8E1"))
            val idx = i
            tv.setOnLongClickListener {
                removeEvent(idx)
                true
            }
            val lp = LinearLayout.LayoutParams(-1, -2)
            lp.setMargins(0, dp(3), 0, dp(3))
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
                    if (off != null) Scheduler.cancel(this, y, m, d, h, mi, off)
                }
            }
            cur.removeAt(idx)
            Scheduler.saveEvents(this, y, m, d, cur)
        }
        showEvents()
    }
}
