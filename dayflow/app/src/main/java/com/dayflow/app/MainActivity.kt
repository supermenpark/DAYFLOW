package com.dayflow.app

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import java.util.Calendar

class MainActivity : Activity() {

    private var year = 0
    private var month = 0

    private lateinit var monthTitle: TextView
    private lateinit var gridBox: LinearLayout
    private var ready = false

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

    override fun onResume() {
        super.onResume()
              if (ready) {
            drawMonth()
            Weather.refresh(this) { runOnUiThread { drawMonth() } }
              }  
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val crashPrefs = getSharedPreferences("crash", MODE_PRIVATE)
        val crash = crashPrefs.getString("msg", null)
        if (crash != null) {
            crashPrefs.edit().clear().commit()
            val tv = TextView(this)
            tv.text = crash
            tv.setPadding(40, 100, 40, 40)
            setContentView(tv)
            return
        }
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            val st = e.stackTrace.take(8).joinToString("\n")
            getSharedPreferences("crash", MODE_PRIVATE).edit()
                .putString("msg", e.toString() + "\n" + st).commit()
            android.os.Process.killProcess(android.os.Process.myPid())
        }

        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf("android.permission.POST_NOTIFICATIONS"), 1)
        }

        val now = Calendar.getInstance()
        year = now.get(Calendar.YEAR)
        month = now.get(Calendar.MONTH) + 1

        val scroll = ScrollView(this)
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(dp(12), dp(32), dp(12), dp(24))
        scroll.addView(root)

        val themeSpin = Spinner(this)
        themeSpin.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, Theme.names
        )
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

        root.addView(label("날짜를 누르면 운세, 일정, 메모를 볼 수 있어요", 13f, false))

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

        Ui.style(this, prev, 0)
        Ui.style(this, next, 0)
        Ui.style(this, bgPick, 1)
        Ui.style(this, bgClear, 4)

        val frame = FrameLayout(this)
        val bgd = BgImage.drawable(this)
        if (bgd != null) {
            val bgView = ImageView(this)
            bgView.scaleType = ImageView.ScaleType.CENTER_CROP
            bgd.alpha = 140
            bgView.setImageDrawable(bgd)
            frame.addView(bgView, FrameLayout.LayoutParams(-1, -1))
        } else {
            frame.setBackgroundColor(Color.parseColor(Theme.bg(this)))
        }
        frame.addView(scroll, FrameLayout.LayoutParams(-1, -1))
                val pages = FrameLayout(this)
        val homeV = HomeView.build(this)
        val memoV = HomeView.placeholder(this, "메모")
        val musicV = HomeView.placeholder(this, "음악")
        val myV = HomeView.placeholder(this, "MY")
        pages.addView(homeV, FrameLayout.LayoutParams(-1, -1))
        pages.addView(frame, FrameLayout.LayoutParams(-1, -1))
        pages.addView(memoV, FrameLayout.LayoutParams(-1, -1))
        pages.addView(musicV, FrameLayout.LayoutParams(-1, -1))
        pages.addView(myV, FrameLayout.LayoutParams(-1, -1))
        val all = arrayOf<View>(homeV, frame, memoV, musicV, myV)

        val bar = LinearLayout(this)
        bar.orientation = LinearLayout.HORIZONTAL
        bar.setBackgroundColor(Color.parseColor("#FFFFFF"))
        val tabNames = arrayOf("홈", "달력", "메모", "음악", "MY")
        val tabs = ArrayList<TextView>()
        for (i in 0..4) {
            val t = label(tabNames[i], 14f, true)
            t.gravity = Gravity.CENTER
            t.setPadding(0, dp(14), 0, dp(14))
            bar.addView(t, wrap())
            tabs.add(t)
            t.setOnClickListener {
                for (j in 0..4) {
                    all[j].visibility = if (j == i) View.VISIBLE else View.GONE
                    tabs[j].setTextColor(Color.parseColor(if (j == i) "#1976D2" else "#888888"))
                }
            }
        }
        for (j in 0..4) {
            all[j].visibility = if (j == 1) View.VISIBLE else View.GONE
            tabs[j].setTextColor(Color.parseColor(if (j == 1) "#1976D2" else "#888888"))
        }

        val outer = LinearLayout(this)
        outer.orientation = LinearLayout.VERTICAL
        outer.addView(pages, LinearLayout.LayoutParams(-1, 0, 1f))
        outer.addView(bar, LinearLayout.LayoutParams(-1, -2))
        setContentView(outer)

        ready = true
        drawMonth()
                Weather.refresh(this) { runOnUiThread { drawMonth() } }
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

                    val hasEvent = Scheduler.loadEvents(this, year, month, d).isNotEmpty()
                    val hasMemo = Scheduler.getMemo(this, year, month, d).isNotEmpty()

                    cell.addView(cellText(d.toString(), 16f, color, true))
                    cell.addView(cellText(CalData.lunarLabel(year, month, d), 10f, "#888888", false))
                    cell.addView(cellText(Fortune.score(year, month, d, 0).toString(), 11f, "#B8860B", true))
                    cell.addView(cellText(if (hasEvent) "●" else " ", 10f, "#E53935", false))
                                        cell.addView(cellText(Weather.label(this, year, month, d), 10f, "#1976D2", false))
                    
                    cell.addView(cellText(Weather.label(this, year, month, d), 10f, "#1976D2", false))
                    if (hasMemo) {
                        cell.setBackgroundColor(Color.parseColor(Theme.card(this)))
                    }
                    if (year == ty && month == tm && d == td) {
                        cell.setBackgroundColor(Color.parseColor("#E3F2FD"))
                    }
                    cell.setOnClickListener {
                        val i = Intent(this, DayActivity::class.java)
                        i.putExtra("y", year)
                        i.putExtra("m", month)
                        i.putExtra("d", d)
                        startActivity(i)
                    }
                }
                row.addView(cell, wrap())
                day += 1
            }
            gridBox.addView(row)
        }
    }
}
