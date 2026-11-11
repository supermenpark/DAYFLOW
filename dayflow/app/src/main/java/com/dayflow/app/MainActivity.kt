package com.dayflow.app

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.util.Calendar

class MainActivity : Activity() {

    private var year = 0
    private var month = 0
    private lateinit var monthTitle: TextView
    private lateinit var gridBox: LinearLayout
    private lateinit var pages: FrameLayout
    private lateinit var tabs: ArrayList<TextView>
    private var ready = false
    private var cur = 0

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

    private fun round(color: String, r: Int): GradientDrawable {
        val g = GradientDrawable()
        g.setColor(Color.parseColor(color))
        g.cornerRadius = dp(r).toFloat()
        return g
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
            show(cur)
            Weather.refresh(this) { runOnUiThread { if (cur == 0) show(0) } }
        }
    }

    private fun banner(): View {
        val box = FrameLayout(this)
        val bgd = BgImage.drawable(this)
        if (bgd != null) {
            val iv = ImageView(this)
            iv.scaleType = ImageView.ScaleType.CENTER_CROP
            iv.setImageDrawable(bgd)
            box.addView(iv, FrameLayout.LayoutParams(-1, -1))
        } else {
            val g = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.parseColor("#7EC8F5"), Color.parseColor("#CDE9FA"))
            )
            box.background = g
        }
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(dp(20), dp(40), dp(20), 0)
        val t1 = label("DAYFLOW", 28f, true)
        t1.setTextColor(Color.WHITE)
        t1.setShadowLayer(6f, 0f, 2f, Color.parseColor("#66000000"))
        val t2 = label("오늘도, 더 나은 하루를 위해", 14f, false)
        t2.setTextColor(Color.WHITE)
        t2.setShadowLayer(6f, 0f, 2f, Color.parseColor("#66000000"))
        col.addView(t1)
        col.addView(t2)
        box.addView(col, FrameLayout.LayoutParams(-1, -2))
        return box
    }

    private fun summaryCard(): View {
        val now = Calendar.getInstance()
        val y = now.get(Calendar.YEAR)
        val m = now.get(Calendar.MONTH) + 1
        val d = now.get(Calendar.DAY_OF_MONTH)
        val card = LinearLayout(this)
        card.orientation = LinearLayout.HORIZONTAL
        card.gravity = Gravity.CENTER_VERTICAL
        card.background = round("#FFFFFF", 18)
        card.elevation = dp(4).toFloat()
        card.setPadding(dp(16), dp(14), dp(16), dp(14))

        val w = label("☀ 오늘 날씨\n" + Weather.detail(this, y, m, d), 14f, false)
        w.setTextColor(Color.parseColor("#333333"))
        card.addView(w, wrap())

        val f = LinearLayout(this)
        f.orientation = LinearLayout.VERTICAL
        f.gravity = Gravity.END
        val a = label("오늘의 운세", 13f, false)
        a.setTextColor(Color.parseColor("#666666"))
        val b = label(Fortune.score(y, m, d, 0).toString() + "점", 24f, true)
        b.setTextColor(Color.parseColor("#6A3DE8"))
        f.addView(a)
        f.addView(b)
        card.addView(f, LinearLayout.LayoutParams(-2, -2))
        return card
    }

    private fun buildHome(): View {
        val sv = ScrollView(this)
        sv.setBackgroundColor(Color.parseColor("#F4F7FB"))
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        sv.addView(root)

        root.addView(banner(), LinearLayout.LayoutParams(-1, dp(190)))

        val cal = LinearLayout(this)
        cal.orientation = LinearLayout.VERTICAL
        cal.background = round("#FFFFFF", 22)
        cal.elevation = dp(4).toFloat()
        cal.setPadding(dp(8), dp(10), dp(8), dp(10))

        val head = LinearLayout(this)
        head.orientation = LinearLayout.HORIZONTAL
        head.gravity = Gravity.CENTER_VERTICAL
        val prev = label("‹", 28f, true)
        prev.gravity = Gravity.CENTER
        prev.setPadding(dp(16), 0, dp(16), 0)
        head.addView(prev)
        monthTitle = label("", 20f, true)
        monthTitle.gravity = Gravity.CENTER
        monthTitle.setTextColor(Color.parseColor("#222222"))
        head.addView(monthTitle, wrap())
        val next = label("›", 28f, true)
        next.gravity = Gravity.CENTER
        next.setPadding(dp(16), 0, dp(16), 0)
        head.addView(next)
        cal.addView(head)

        val week = LinearLayout(this)
        week.orientation = LinearLayout.HORIZONTAL
        val names = arrayOf("일", "월", "화", "수", "목", "금", "토")
        for (i in 0..6) {
            var color = "#444444"
            if (i == 0) color = "#E53935"
            if (i == 6) color = "#1976D2"
            val w = cellText(names[i], 13f, color, true)
            w.setPadding(0, dp(10), 0, dp(10))
            week.addView(w, wrap())
        }
        cal.addView(week)

        gridBox = LinearLayout(this)
        gridBox.orientation = LinearLayout.VERTICAL
        cal.addView(gridBox)

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

        val lp = LinearLayout.LayoutParams(-1, -2)
        lp.setMargins(dp(12), dp(-24), dp(12), dp(12))
        root.addView(cal, lp)

        val lp2 = LinearLayout.LayoutParams(-1, -2)
        lp2.setMargins(dp(12), 0, dp(12), dp(20))
        root.addView(summaryCard(), lp2)

        drawMonth()
        return sv
    }

    private fun buildMy(): View {
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.parseColor("#F4F7FB"))
        root.setPadding(dp(20), dp(48), dp(20), dp(20))
        root.addView(label("MY", 26f, true))
        val pick = Button(this)
        pick.text = "배너 사진 선택"
        pick.setOnClickListener {
            val i = Intent(Intent.ACTION_GET_CONTENT)
            i.type = "image/*"
            startActivityForResult(i, 77)
        }
        Ui.style(this, pick, 0)
        root.addView(pick)
        val clear = Button(this)
        clear.text = "배너 사진 지우기"
        clear.setOnClickListener {
            BgImage.clear(this)
            show(0)
        }
        Ui.style(this, clear, 4)
        root.addView(clear)
        return root
    }

    private fun show(i: Int) {
        cur = i
        pages.removeAllViews()
        val v: View = when (i) {
            0 -> buildHome()
            1 -> MemoView.build(this)
            2 -> HomeView.placeholder(this, "음악")
            else -> buildMy()
        }
        pages.addView(v, FrameLayout.LayoutParams(-1, -1))
        for (j in 0..3) {
            tabs[j].setTextColor(Color.parseColor(if (j == i) "#1976D2" else "#888888"))
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

        pages = FrameLayout(this)

        val bar = LinearLayout(this)
        bar.orientation = LinearLayout.HORIZONTAL
        bar.setBackgroundColor(Color.WHITE)
        bar.elevation = dp(8).toFloat()
        val tabNames = arrayOf("🏠\n홈", "📝\n메모", "🎵\n음악", "👤\nMY")
        tabs = ArrayList()
        for (i in 0..3) {
            val t = label(tabNames[i], 12f, true)
            t.gravity = Gravity.CENTER
            t.setPadding(0, dp(8), 0, dp(8))
            bar.addView(t, wrap())
            tabs.add(t)
            t.setOnClickListener { show(i) }
        }

        val outer = LinearLayout(this)
        outer.orientation = LinearLayout.VERTICAL
        outer.addView(pages, LinearLayout.LayoutParams(-1, 0, 1f))
        outer.addView(bar, LinearLayout.LayoutParams(-1, -2))
        setContentView(outer)

        ready = true
        show(0)
        Weather.refresh(this) { runOnUiThread { if (cur == 0) show(0) } }
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
                    val isToday = year == ty && month == tm && d == td
                    var color = "#222222"
                    if (col == 0 || holiday != null) color = "#E53935"
                    else if (col == 6) color = "#1976D2"
                    if (isToday) color = "#FFFFFF"

                    val hasEvent = Scheduler.loadEvents(this, year, month, d).isNotEmpty()
                    val dot = if (hasEvent) "●" else " "
                    val wcolor = if (isToday) "#FFFFFF" else "#1976D2"
                    val scolor = if (isToday) "#FFFFFF" else "#7A5AE0"

                    cell.addView(cellText(d.toString() + " " + dot, 15f, color, true))
                    cell.addView(cellText(Weather.label(this, year, month, d), 10f, wcolor, false))
                    cell.addView(cellText(Fortune.score(year, month, d, 0).toString() + "점", 10f, scolor, false))

                    if (isToday) cell.background = round("#2F7BE8", 12)
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
