package com.dayflow.app

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.util.Calendar

class MainActivity : Activity() {

    private var year = 0
    private var month = 0
    private var selY = 0
    private var selM = 0
    private var selD = 0

    private lateinit var monthTitle: TextView
    private lateinit var gridBox: LinearLayout
    private lateinit var infoText: TextView
    private lateinit var memoInput: EditText
    private lateinit var eventBox: LinearLayout
    private lateinit var titleInput: EditText
    private lateinit var hourInput: EditText
    private lateinit var minInput: EditText
    private lateinit var chkSame: CheckBox
    private lateinit var chkDay1: CheckBox
    private lateinit var chkDay2: CheckBox

    private val solarHolidays = mapOf(
        "1-1" to "신정", "3-1" to "삼일절", "5-5" to "어린이날",
        "6-6" to "현충일", "8-15" to "광복절", "10-3" to "개천절",
        "10-9" to "한글날", "12-25" to "성탄절"
    )

    private val solarTerms = mapOf(
        "1-5" to "소한", "1-20" to "대한", "2-4" to "입춘", "2-19" to "우수",
        "3-5" to "경칩", "3-20" to "춘분", "4-5" to "청명", "4-20" to "곡우",
        "5-5" to "입하", "5-21" to "소만", "6-5" to "망종", "6-21" to "하지",
        "7-7" to "소서", "7-22" to "대서", "8-7" to "입추", "8-23" to "처서",
        "9-7" to "백로", "9-23" to "추분", "10-8" to "한로", "10-23" to "상강",
        "11-7" to "입동", "11-22" to "소설", "12-7" to "대설", "12-22" to "동지"
    )

    private fun key(y: Int, m: Int, d: Int): String {
        return "m_" + y + "_" + m + "_" + d
    }

    private fun evKey(y: Int, m: Int, d: Int): String {
        return "e_" + y + "_" + m + "_" + d
    }

    private fun prefs() = getSharedPreferences("dayflow_cal", Context.MODE_PRIVATE)

    private fun lunarOf(y: Int, m: Int, d: Int): IntArray {
        val g = Calendar.getInstance()
        g.set(y, m - 1, d, 12, 0, 0)
        val c = android.icu.util.ChineseCalendar()
        c.timeInMillis = g.timeInMillis
        val lm = c.get(android.icu.util.Calendar.MONTH) + 1
        val ld = c.get(android.icu.util.Calendar.DAY_OF_MONTH)
        val leap = c.get(android.icu.util.Calendar.IS_LEAP_MONTH)
        return intArrayOf(lm, ld, leap)
    }

    private fun holidayOf(y: Int, m: Int, d: Int): String? {
        val s = solarHolidays[m.toString() + "-" + d.toString()]
        if (s != null) return s
        val l = lunarOf(y, m, d)
        if (l[2] == 0) {
            if (l[0] == 1 && l[1] == 1) return "설날"
            if (l[0] == 1 && l[1] == 2) return "설연휴"
            if (l[0] == 4 && l[1] == 8) return "석가탄신일"
            if (l[0] == 8 && l[1] == 14) return "추석연휴"
            if (l[0] == 8 && l[1] == 15) return "추석"
            if (l[0] == 8 && l[1] == 16) return "추석연휴"
        }
        val t = Calendar.getInstance()
        t.set(y, m - 1, d, 12, 0, 0)
        t.add(Calendar.DAY_OF_MONTH, 1)
        val n = lunarOf(t.get(Calendar.YEAR), t.get(Calendar.MONTH) + 1, t.get(Calendar.DAY_OF_MONTH))
        if (n[0] == 1 && n[1] == 1 && n[2] == 0) return "설연휴"
        return null
    }

    private fun dp(v: Int): Int {
        return (v * resources.displayMetrics.density).toInt()
    }

    // 일정 저장 형식: 줄마다 "시|분|제목|알림코드" (알림코드: 0=당일 1=하루전 2=이틀전, 쉼표 구분)
    private fun loadEvents(y: Int, m: Int, d: Int): MutableList<String> {
        val raw = prefs().getString(evKey(y, m, d), "") ?: ""
        val list = ArrayList<String>()
        if (raw.isNotEmpty()) {
            for (line in raw.split("\n")) {
                if (line.isNotEmpty()) list.add(line)
            }
        }
        return list
    }

    private fun saveEvents(y: Int, m: Int, d: Int, list: List<String>) {
        val e = prefs().edit()
        if (list.isEmpty()) {
            e.remove(evKey(y, m, d))
        } else {
            e.putString(evKey(y, m, d), list.joinToString("\n"))
        }
        e.apply()
    }

    private fun alarmId(y: Int, m: Int, d: Int, h: Int, mi: Int, offset: Int): Int {
        return (((y % 100) * 10000 + m * 100 + d) * 10000 + h * 100 + mi) * 3 + offset
    }

    private fun scheduleAlarm(y: Int, m: Int, d: Int, h: Int, mi: Int, offset: Int, title: String) {
        val c = Calendar.getInstance()
        c.set(y, m - 1, d, h, mi, 0)
        c.set(Calendar.MILLISECOND, 0)
        c.add(Calendar.DAY_OF_MONTH, -offset)
        if (c.timeInMillis <= System.currentTimeMillis()) return

        val label = if (offset == 0) "오늘" else if (offset == 1) "내일" else "모레"
        val text = label + " " + h + "시 " + mi + "분 - " + title

        val intent = Intent(this, AlarmReceiver::class.java)
        intent.putExtra("title", "DAYFLOW 일정")
        intent.putExtra("text", text)
        val id = alarmId(y, m, d, h, mi, offset) and 0x7FFFFFFF
        intent.putExtra("id", id)
        val pi = PendingIntent.getBroadcast(
            this, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        var exact = true
        if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) exact = false
        if (exact) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, c.timeInMillis, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, c.timeInMillis, pi)
        }
    }

    private fun cancelAlarm(y: Int, m: Int, d: Int, h: Int, mi: Int, offset: Int) {
        val intent = Intent(this, AlarmReceiver::class.java)
        val id = alarmId(y, m, d, h, mi, offset) and 0x7FFFFFFF
        val pi = PendingIntent.getBroadcast(
            this, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pi)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf("android.permission.POST_NOTIFICATIONS"), 1)
        }

        val now = Calendar.getInstance()
        year = now.get(Calendar.YEAR)
        month = now.get(Calendar.MONTH) + 1
        selY = year
        selM = month
        selD = now.get(Calendar.DAY_OF_MONTH)

        val scroll = ScrollView(this)
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(dp(12), dp(32), dp(12), dp(24))
        scroll.addView(root)

        val head = LinearLayout(this)
        head.orientation = LinearLayout.HORIZONTAL
        head.gravity = Gravity.CENTER_VERTICAL

        val prev = Button(this)
        prev.text = "<"
        head.addView(prev)

        monthTitle = TextView(this)
        monthTitle.textSize = 22f
        monthTitle.gravity = Gravity.CENTER
        monthTitle.setTypeface(null, Typeface.BOLD)
        head.addView(
            monthTitle,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        )

        val next = Button(this)
        next.text = ">"
        head.addView(next)
        root.addView(head)

        val week = LinearLayout(this)
        week.orientation = LinearLayout.HORIZONTAL
        val names = arrayOf("일", "월", "화", "수", "목", "금", "토")
        for (i in 0..6) {
            val w = TextView(this)
            w.text = names[i]
            w.gravity = Gravity.CENTER
            w.setPadding(0, dp(8), 0, dp(8))
            if (i == 0) w.setTextColor(Color.parseColor("#D32F2F"))
            if (i == 6) w.setTextColor(Color.parseColor("#1976D2"))
            week.addView(w, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }
        root.addView(week)

        gridBox = LinearLayout(this)
        gridBox.orientation = LinearLayout.VERTICAL
        root.addView(gridBox)

        infoText = TextView(this)
        infoText.textSize = 16f
        infoText.setPadding(0, dp(16), 0, dp(8))
        root.addView(infoText)

        val evHeader = TextView(this)
        evHeader.text = "일정 (길게 누르면 삭제)"
        evHeader.textSize = 15f
        evHeader.setTypeface(null, Typeface.BOLD)
        root.addView(evHeader)

        eventBox = LinearLayout(this)
        eventBox.orientation = LinearLayout.VERTICAL
        root.addView(eventBox)

        val timeRow = LinearLayout(this)
        timeRow.orientation = LinearLayout.HORIZONTAL
        timeRow.gravity = Gravity.CENTER_VERTICAL

        hourInput = EditText(this)
        hourInput.hint = "시"
        hourInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        timeRow.addView(hourInput, LinearLayout.LayoutParams(dp(60), LinearLayout.LayoutParams.WRAP_CONTENT))

        val colon = TextView(this)
        colon.text = " : "
        timeRow.addView(colon)

        minInput = EditText(this)
        minInput.hint = "분"
        minInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        timeRow.addView(minInput, LinearLayout.LayoutParams(dp(60), LinearLayout.LayoutParams.WRAP_CONTENT))

        titleInput = EditText(this)
        titleInput.hint = "일정 제목 (예: 영화, 면접)"
        titleInput.setSingleLine(true)
        timeRow.addView(
            titleInput,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        )
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
        root.addView(addEv)

        val memoHeader = TextView(this)
        memoHeader.text = "메모"
        memoHeader.textSize = 15f
        memoHeader.setTypeface(null, Typeface.BOLD)
        memoHeader.setPadding(0, dp(16), 0, 0)
        root.addView(memoHeader)

        memoInput = EditText(this)
        memoInput.hint = "이 날의 메모"
        memoInput.minLines = 3
        memoInput.gravity = Gravity.TOP
        root.addView(memoInput)

        val save = Button(this)
        save.text = "메모 저장"
        root.addView(save)

        prev.setOnClickListener {
            month -= 1
            if (month < 1) {
                month = 12
                year -= 1
            }
            drawMonth()
        }
        next.setOnClickListener {
            month += 1
            if (month > 12) {
                month = 1
                year += 1
            }
            drawMonth()
        }
        save.setOnClickListener {
            val text = memoInput.text.toString().trim()
            val e = prefs().edit()
            if (text.isEmpty()) {
                e.remove(key(selY, selM, selD))
            } else {
                e.putString(key(selY, selM, selD), text)
            }
            e.apply()
            drawMonth()
            Toast.makeText(this, "메모 저장됨", Toast.LENGTH_SHORT).show()
        }
        addEv.setOnClickListener {
            val h = hourInput.text.toString().trim().toIntOrNull()
            val mi = minInput.text.toString().trim().toIntOrNull()
            val title = titleInput.text.toString().trim().replace("|", " ").replace("\n", " ")
            if (h == null || mi == null || h < 0 || h > 23 || mi < 0 || mi > 59) {
                Toast.makeText(this, "시(0-23)와 분(0-59)을 입력하세요", Toast.LENGTH_SHORT).show()
            } else if (title.isEmpty()) {
                Toast.makeText(this, "일정 제목을 입력하세요", Toast.LENGTH_SHORT).show()
            } else {
                val codes = ArrayList<String>()
                if (chkSame.isChecked) codes.add("0")
                if (chkDay1.isChecked) codes.add("1")
                if (chkDay2.isChecked) codes.add("2")
                for (code in codes) {
                    scheduleAlarm(selY, selM, selD, h, mi, code.toInt(), title)
                }
                val list = loadEvents(selY, selM, selD)
                list.add(h.toString() + "|" + mi + "|" + title + "|" + codes.joinToString(","))
                saveEvents(selY, selM, selD, list)
                hourInput.setText("")
                minInput.setText("")
                titleInput.setText("")
                drawMonth()
                showDay()
                Toast.makeText(this, "일정과 알림이 저장됨", Toast.LENGTH_SHORT).show()
            }
        }

        setContentView(scroll)
        drawMonth()
        showDay()
    }

    private fun makeText(s: String, size: Float, color: String, bold: Boolean): TextView {
        val t = TextView(this)
        t.text = s
        t.textSize = size
        t.gravity = Gravity.CENTER
        t.setTextColor(Color.parseColor(color))
        if (bold) t.setTypeface(null, Typeface.BOLD)
        return t
    }

    private fun drawMonth() {
        monthTitle.text = year.toString() + "년 " + month + "월"
        gridBox.removeAllViews()

        val first = Calendar.getInstance()
        first.set(year, month - 1, 1, 12, 0, 0)
        val startOffset = first.get(Calendar.DAY_OF_WEEK) - 1
        val daysInMonth = first.getActualMaximum(Calendar.DAY_OF_MONTH)

        val today = Calendar.getInstance()
        val ty = today.get(Calendar.YEAR)
        val tm = today.get(Calendar.MONTH) + 1
        val td = today.get(Calendar.DAY_OF_MONTH)

        var day = 1 - startOffset
        while (day <= daysInMonth) {
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            for (col in 0..6) {
                val cell = LinearLayout(this)
                cell.orientation = LinearLayout.VERTICAL
                cell.gravity = Gravity.CENTER_HORIZONTAL
                cell.setPadding(0, dp(6), 0, dp(6))
                if (day in 1..daysInMonth) {
                    val d = day
                    val l = lunarOf(year, month, d)
                    val lunarLabel = if (l[1] == 1) {
                        (if (l[2] == 1) "윤" else "") + l[0] + "." + l[1]
                    } else {
                        l[1].toString()
                    }
                    val hasMemo = prefs().getString(key(year, month, d), null) != null
                    val hasEvent = loadEvents(year, month, d).isNotEmpty()
                    val holiday = holidayOf(year, month, d)

                    var color = "#222222"
                    if (col == 0 || holiday != null) color = "#D32F2F"
                    else if (col == 6) color = "#1976D2"

                    var mark = ""
                    if (hasMemo) mark += "•"
                    if (hasEvent) mark += "★"
                    cell.addView(makeText(d.toString() + mark, 16f, color, true))
                    cell.addView(makeText(lunarL
