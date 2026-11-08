package com.dayflow.app

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
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
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.AdapterView
import android.view.View
import android.content.Intent
import android.net.Uri

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
        private fun applyBg() {
        val d = BgImage.drawable(this)
        if (d != null) {
            d.alpha = 110
            window.setBackgroundDrawable(d)
        }
        }

    

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 77 && resultCode == RESULT_OK && data != null) {
            val u: Uri? = data.data
            if (u != null && BgImage.save(this, u)) {
                recreate()
            } else {
                Toast.makeText(this, "사진을 불러오지 못했어요", Toast.LENGTH_SHORT).show()
            }
        }
    }

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

    private fun cellText(s: String, size: Float, color: String, bold: Boolean): TextView {
        val t = label(s, size, bold)
        t.gravity = Gravity.CENTER
        t.setTextColor(Color.parseColor(color))
        return t
    }

    private fun wrap(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
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
   
        root.setBackgroundColor(Color.parseColor(Theme.bg(this)))
        scroll.setBackgroundColor(Color.parseColor(Theme.bg(this)))
        scroll.addView(root)

        val themeSpin = Spinner(this)
        themeSpin.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, Theme.names)
        themeSpin.setSelection(Theme.get(this))
        themeSpin.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                if (pos != Theme.get(this@MainActivity)) {
                    Theme.set(this@MainActivity, pos)
                    recreate()
                }
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
        root.addView(themeSpin)
                val bgRow = LinearLayout(this)
        bgRow.orientation = LinearLayout.HORIZONTAL
        val bgPick = Button(this)
        bgPick.text = "배경 사진 선택"
        bgPick.setOnClickListener {
            val i = Intent(Intent.ACTION_GET_CONTENT)
            i.type = "image/*"
            startActivityForResult(i, 77)
        }
        bgRow.addView(bgPick, wrap())
        val bgClear = Button(this)
        bgClear.text = "배경 지우기"
        bgClear.setOnClickListener {
            BgImage.clear(this)
            recreate()
        }
        bgRow.addView(bgClear, wrap())
        root.addView(bgRow)

        val head = LinearLayout(this)
        head.orientation = LinearLayout.HORIZONTAL
        head.gravity = Gravity.CENTER_VERTICAL
        val prev = Button(this)
        prev.text = "<"
        head.addView(prev)
        monthTitle = label("", 22f, true)
        monthTitle.gravity = Gravity.CENTER
        head.addView(monthTitle, wrap())
        val next = Button(this)
        next.text = ">"
        head.addView(next)
        root.addView(head)

        val week = LinearLayout(this)
        week.orientation = LinearLayout.HORIZONTAL
        val names = arrayOf("일", "월", "화", "수", "목", "금", "토")
        for (i in 0..6) {
            var color = "#222222"
            if (i == 0) color = "#D32F2F"
            if (i == 6) color = "#1976D2"
            val w = cellText(names[i], 14f, color, false)
            w.setPadding(0, dp(8), 0, dp(8))
            week.addView(w, wrap())
        }
        root.addView(week)

        gridBox = LinearLayout(this)
        gridBox.orientation = LinearLayout.VERTICAL
        root.addView(gridBox)

        infoText = label("", 16f, false)
        infoText.setPadding(0, dp(16), 0, dp(8))
        root.addView(infoText)

        root.addView(label("일정 (길게 누르면 삭제)", 15f, true))

        eventBox = LinearLayout(this)
        eventBox.orientation = LinearLayout.VERTICAL
        root.addView(eventBox)

        val timeRow = LinearLayout(this)
        timeRow.orientation = LinearLayout.HORIZONTAL
        timeRow.gravity = Gravity.CENTER_VERTICAL
        hourInput = EditText(this)
        hourInput.hint = "시"
        hourInput.inputType = InputType.TYPE_CLASS_NUMBER
        timeRow.addView(hourInput, LinearLayout.LayoutParams(dp(60), LinearLayout.LayoutParams.WRAP_CONTENT))
        timeRow.addView(label(" : ", 16f, false))
        minInput = EditText(this)
        minInput.hint = "분"
        minInput.inputType = InputType.TYPE_CLASS_NUMBER
        timeRow.addView(minInput, LinearLayout.LayoutParams(dp(60), LinearLayout.LayoutParams.WRAP_CONTENT))
        titleInput = EditText(this)
        titleInput.hint = "일정 제목"
        titleInput.setSingleLine(true)
        timeRow.addView(titleInput, wrap())
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

        val memoHeader = label("메모", 15f, true)
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
            Scheduler.setMemo(this, selY, selM, selD, memoInput.text.toString().trim())
            drawMonth()
            Toast.makeText(this, "메모 저장됨", Toast.LENGTH_SHORT).show()
        }
        addEv.setOnClickListener { addEvent() }

        setContentView(scroll)
                Ui.style(this, prev, 0)
        Ui.style(this, next, 0)
        Ui.style(this, bgPick, 1)
        Ui.style(this, bgClear, 4)
        Ui.style(this, addEv, 2)
        Ui.style(this, save, 3)

        val frame = android.widget.FrameLayout(this)
        val bgd = BgImage.drawable(this)
        if (bgd != null) {
            val bgView = android.widget.ImageView(this)
            bgView.scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
            bgd.alpha = 140
            bgView.setImageDrawable(bgd)
            frame.addView(bgView, android.widget.FrameLayout.LayoutParams(-1, -1))
            scroll.setBackgroundColor(Color.TRANSPARENT)
            root.setBackgroundColor(Color.TRANSPARENT)
        } else {
            frame.setBackgroundColor(Color.parseColor(Theme.bg(this)))
        }
        frame.addView(scroll, android.widget.FrameLayout.LayoutParams(-1, -1))
        setContentView(frame)
    
        
            
            
            root.setBackgroundColor(Color.TRANSPARENT)
        }
        drawMonth()
        showDay()
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
            Scheduler.schedule(this, selY, selM, selD, h, mi, code.toInt(), title)
        }
        val list = Scheduler.loadEvents(this, selY, selM, selD)
        list.add(h.toString() + "|" + mi + "|" + title + "|" + codes.joinToString(","))
        Scheduler.saveEvents(this, selY, selM, selD, list)
        hourInput.setText("")
        minInput.setText("")
        titleInput.setText("")
        drawMonth()
        showDay()
        Toast.makeText(this, "일정과 알림이 저장됨", Toast.LENGTH_SHORT).show()
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
                    val holiday = CalData.holidayOf(year, month, d)
                    var color = "#222222"
                    if (col == 0 || holiday != null) color = "#D32F2F"
                    else if (col == 6) color = "#1976D2"

                    var mark = ""
                    if (Scheduler.getMemo(this, year, month, d).isNotEmpty()) mark += "•"
                    if (Scheduler.loadEvents(this, year, month, d).isNotEmpty()) mark += "★"

                    cell.addView(cellText(d.toString() + mark, 16f, color, true))
                    cell.addView(cellText(CalData.lunarLabel(year, month, d), 10f, "#888888", false))
                    cell.addView(cellText(holiday ?: " ", 9f, "#D32F2F", false))

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
                }
                row.addView(cell, wrap())
                day += 1
            }
            gridBox.addView(row)
        }
    }

    private fun showDay() {
        val l = CalData.lunarOf(selY, selM, selD)
        val c = Calendar.getInstance()
        c.set(selY, selM - 1, selD, 12, 0, 0)
        val names = arrayOf("일", "월", "화", "수", "목", "금", "토")
        val dow = names[c.get(Calendar.DAY_OF_WEEK) - 1]

        val sb = StringBuilder()
        sb.append(selY).append("년 ").append(selM).append("월 ").append(selD)
        sb.append("일 (").append(dow).append(")\n음력 ")
        if (l[2] == 1) sb.append("윤")
        sb.append(l[0]).append("월 ").append(l[1]).append("일")
        val h = CalData.holidayOf(selY, selM, selD)
        if (h != null) sb.append("\n공휴일: ").append(h)
        val t = CalData.termOf(selM, selD)
        if (t != null) sb.append("\n절기: ").append(t).append(" (대략적인 날짜)")
        infoText.text = sb.toString()

        memoInput.setText(Scheduler.getMemo(this, selY, selM, selD))
        showEvents()
    }

    private fun showEvents() {
        eventBox.removeAllViews()
        val events = Scheduler.loadEvents(this, selY, selM, selD)
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
            var line = "★ " + String.format("%02d:%02d", hh, mm) + "  " + parts[2]
            if (an.isNotEmpty()) line += "\n    알림: " + an.joinToString(", ")
            val tv = label(line, 15f, false)
            tv.setPadding(dp(4), dp(6), dp(4), dp(6))
            tv.setBackgroundColor(Color.parseColor("#FFF8E1"))
            val idx = i
            tv.setOnLongClickListener {
                removeEvent(idx)
                true
            }
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.setMargins(0, dp(3), 0, dp(3))
            eventBox.addView(tv, lp)
        }
    }

    private fun removeEvent(idx: Int) {
        val cur = Scheduler.loadEvents(this, selY, selM, selD)
        if (idx < cur.size) {
            val p = cur[idx].split("|")
            if (p.size > 3) {
                val h = p[0].toIntOrNull() ?: 0
                val mi = p[1].toIntOrNull() ?: 0
                for (cd in p[3].split(",")) {
                    val off = cd.toIntOrNull()
                    if (off != null) Scheduler.cancel(this, selY, selM, selD, h, mi, off)
                }
            }
            cur.removeAt(idx)
            Scheduler.saveEvents(this, selY, selM, selD, cur)
        }
        drawMonth()
        showDay()
    }
}
