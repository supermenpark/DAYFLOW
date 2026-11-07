package com.dayflow.app

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
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
            if (l[0] == 1 && l[1] == 2) return "설날 연휴"
            if (l[0] == 4 && l[1] == 8) return "부처님오신날"
            if (l[0] == 8 && l[1] == 14) return "추석 연휴"
            if (l[0] == 8 && l[1] == 15) return "추석"
            if (l[0] == 8 && l[1] == 16) return "추석 연휴"
        }
        val t = Calendar.getInstance()
        t.set(y, m - 1, d, 12, 0, 0)
        t.add(Calendar.DAY_OF_MONTH, 1)
        val n = lunarOf(t.get(Calendar.YEAR), t.get(Calendar.MONTH) + 1, t.get(Calendar.DAY_OF_MONTH))
        if (n[0] == 1 && n[1] == 1 && n[2] == 0) return "설날 연휴"
        return null
    }

    private fun dp(v: Int): Int {
        return (v * resources.displayMetrics.density).toInt()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val now = Calendar.getInstance()
        year = now.get(Calendar.YEAR)
        month = now.get(Calendar.MONTH) + 1
        selY = year
        selM = month
        selD = now.get(Calendar.DAY_OF_MONTH)

        val scroll = ScrollView(this)
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(dp(12), dp(32), dp(12), dp(16))
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
        }

        setContentView(scroll)
        drawMonth()
        showDay()
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
                val cell = TextView(this)
                cell.gravity = Gravity.CENTER
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
                    val mark = if (hasMemo) " ●" else ""
                    cell.text = d.toString() + mark + "\n" + lunarLabel
                    cell.textSize = 14f

                    val holiday = holidayOf(year, month, d)
                    var color = Color.parseColor("#222222")
                    if (col == 0 || holiday != null) color = Color.parseColor("#D32F2F")
                    else if (col == 6) color = Color.parseColor("#1976D2")
                    cell.setTextColor(color)

                    if (year == selY && month == selM && d == selD) {
                        cell.setBackgroundColor(Color.parseColor("#FFF3C4"))
                    } else if (year == ty && month == tm && d == td) {
                        cell.setBackgroundColor(Color.parseColor("#E3F2FD"))
                    }

                    cell.setOnClickListener {
                        selY = year
                        selM = month
                        selD = d
                        drawMonth()
                        showDay()
                    }
                } else {
                    cell.text = " \n "
                }
                row.addView(cell, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                day += 1
            }
            gridBox.addView(row)
        }
    }

    private fun showDay() {
        val l = lunarOf(selY, selM, selD)
        val c = Calendar.getInstance()
        c.set(selY, selM - 1, selD, 12, 0, 0)
        val names = arrayOf("일", "월", "화", "수", "목", "금", "토")
        val dow = names[c.get(Calendar.DAY_OF_WEEK) - 1]

        val sb = StringBuilder()
        sb.append(selY).append("년 ").append(selM).append("월 ").append(selD).append("일 (").append(dow).append(")\n")
        sb.append("음력 ")
        if (l[2] == 1) sb.append("윤")
        sb.append(l[0]).append("월 ").append(l[1]).append("일")

        val h = holidayOf(selY, selM, selD)
        if (h != null) sb.append("\n공휴일: ").append(h)
        val t = solarTerms[selM.toString() + "-" + selD.toString()]
        if (t != null) sb.append("\n절기: ").append(t).append(" (대략적인 날짜)")

        infoText.text = sb.toString()
        memoInput.setText(prefs().getString(key(selY, selM, selD), "") ?: "")
    }
}
