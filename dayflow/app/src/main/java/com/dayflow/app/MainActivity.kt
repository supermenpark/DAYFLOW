package com.dayflow.app

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
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
import android.widget.EditText
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        LocalMusic.onResult(requestCode, grantResults)
    }

    override fun onResume() {
        super.onResume()
        if (ready) {
            show(cur)
            WidgetProvider.refresh(this)
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
            DailyBanner.attach(box)
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
        val title = if (Fortune.label.isEmpty()) "오늘의 운세(재미용)" else Fortune.label
        val a = label(title, 13f, false)
        a.setTextColor(Color.parseColor("#666666"))
        val b = label(Fortune.score(y, m, d, 0).toString() + "점", 24f, true)
        b.setTextColor(Color.parseColor("#6A3DE8"))
        f.addView(a)
        f.addView(b)
        card.addView(f, LinearLayout.LayoutParams(-2, -2))
        return card
    }

    private fun briefCard(): View {
        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.background = round("#FFFFFF", 18)
        card.elevation = dp(4).toFloat()
        card.setPadding(dp(16), dp(14), dp(16), dp(14))
        val h = label("📋 오늘의 브리핑", 15f, true)
        h.setTextColor(Color.parseColor("#26364A"))
        card.addView(h)
        val b = label(Extra.brief(this), 14f, false)
        b.setTextColor(Color.parseColor("#333333"))
        b.setLineSpacing(0f, 1.2f)
        b.setPadding(0, dp(6), 0, 0)
        card.addView(b)
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
        lp2.setMargins(dp(12), 0, dp(12), dp(12))
        root.addView(summaryCard(), lp2)

        val lp3 = LinearLayout.LayoutParams(-1, -2)
        lp3.setMargins(dp(12), 0, dp(12), dp(20))
        root.addView(briefCard(), lp3)

        drawMonth()
        return sv
    }

    private fun sectionTitle(root: LinearLayout, text: String) {
        val t = label(text, 15f, true)
        t.setTextColor(Color.parseColor("#26364A"))
        val lp = LinearLayout.LayoutParams(-1, -2)
        lp.setMargins(0, dp(22), 0, dp(6))
        root.addView(t, lp)
    }

    private fun menuButton(root: LinearLayout, text: String, color: Int, action: () -> Unit) {
        val b = Button(this)
        b.text = text
        Ui.style(this, b, color)
        b.setOnClickListener { action() }
        val lp = LinearLayout.LayoutParams(-1, -2)
        lp.setMargins(0, dp(4), 0, dp(4))
        root.addView(b, lp)
    }

    private fun addDday() {
        val n = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, yy, mm, dd ->
                val e = EditText(this)
                e.hint = "이름 (예: 여행, 시험)"
                AlertDialog.Builder(this)
                    .setTitle("D-day 이름")
                    .setView(e)
                    .setPositiveButton("저장") { _, _ ->
                        val t = e.text.toString().trim()
                        if (t.isNotEmpty()) {
                            Extra.ddaySet(this, yy, mm + 1, dd, t)
                            show(4)
                        }
                    }
                    .setNegativeButton("취소", null)
                    .show()
            },
            n.get(Calendar.YEAR), n.get(Calendar.MONTH), n.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun pickBirth() {
        DatePickerDialog(
            this,
            { _, yy, mm, dd ->
                Fortune.setBirth(this, yy, mm + 1, dd)
                WidgetProvider.refresh(this)
                show(4)
            },
            1990, 0, 1
        ).show()
    }

    private fun buildMy(): View {
        val sv = ScrollView(this)
        sv.setBackgroundColor(Color.parseColor("#F4F7FB"))
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(dp(20), dp(48), dp(20), dp(30))
        sv.addView(root)
        root.addView(label("MY", 26f, true))

        sectionTitle(root, "내 운세 (생일을 넣으면 띠·별자리 운세로 바뀌어요)")
        val prof = if (Fortune.label.isEmpty()) "생일 미등록" else Fortune.label
        val pt = label(prof, 13f, false)
        pt.setTextColor(Color.parseColor("#6A3DE8"))
        root.addView(pt)
        menuButton(root, "🎂 생일 입력", 3) { pickBirth() }
        menuButton(root, "생일 지우기", 4) {
            Fortune.clearBirth(this)
            WidgetProvider.refresh(this)
            show(4)
        }

        sectionTitle(root, "날씨 지역")
        menuButton(root, "📍 " + Weather.names[Weather.city(this)] + "  (눌러서 변경)", 1) {
            AlertDialog.Builder(this)
                .setTitle("날씨 지역 선택")
                .setItems(Weather.names) { _, which ->
                    Weather.setCity(this, which)
                    Weather.refresh(this) { runOnUiThread { if (cur == 0) show(0) } }
                    show(4)
                }
                .show()
        }

        sectionTitle(root, "D-day (달력에 📌 표시, 홈 브리핑에 남은 날)")
        val nx = Extra.ddayNext(this)
        if (nx != null) {
            val t = label("다음: " + nx, 13f, false)
            t.setTextColor(Color.parseColor("#2F7BE8"))
            root.addView(t)
        }
        menuButton(root, "📌 D-day 등록", 2) { addDday() }
        menuButton(root, "D-day 모두 지우기", 4) {
            getSharedPreferences("dayflow_dday", Context.MODE_PRIVATE).edit().clear().apply()
            show(4)
        }

        sectionTitle(root, "백업 (폰을 바꾸거나 앱을 지우기 전에)")
        menuButton(root, "💾 백업 내보내기 (복사 + 공유)", 3) { Backup.export(this) }
        menuButton(root, "📥 백업 불러오기", 2) { Backup.import(this) { show(4) } }

        sectionTitle(root, "홈 배너")
        menuButton(root, "배너 사진 직접 선택", 0) {
            val i = Intent(Intent.ACTION_GET_CONTENT)
            i.type = "image/*"
            startActivityForResult(i, 77)
        }
        menuButton(root, "직접 고른 사진 지우기 (매일 랜덤 사진으로)", 4) {
            BgImage.clear(this)
            show(4)
        }

        val note = label("운세 점수는 재미로 보는 샘플이에요.\n바탕화면 길게 누르기 → 위젯 → DAYFLOW에서 홈 위젯을 추가할 수 있어요.", 12f, false)
        note.setTextColor(Color.parseColor("#8895A7"))
        note.setPadding(0, dp(24), 0, 0)
        root.addView(note)
        return sv
    }

    private fun show(i: Int) {
        cur = i
        pages.removeAllViews()
        val v: View = when (i) {
            0 -> buildHome()
            1 -> MemoView.build(this)
            2 -> MusicView.build(this)
            3 -> DiaryView.build(this)
            else -> buildMy()
        }
        pages.addView(v, FrameLayout.LayoutParams(-1, -1))
        for (j in 0..4) {
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

        Fortune.load(this)

        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf("android.permission.POST_NOTIFICATIONS"), 1)
        }

        val now = Calendar.getInstance()
        year = now.get(Calendar.YEAR)
        month = now.get(Calendar.MONTH) + 1

        pages = FrameLayout(this)

        val bar = LinearLayout(this)
        bar.orientation = LinearLayout.HORIZONTAL
        bar.background = round("#FFFFFF", 28)
        bar.elevation = dp(10).toFloat()
        bar.setPadding(dp(8), dp(6), dp(8), dp(6))
        val tabNames = arrayOf("🏠\n홈", "📝\n메모", "🎵\n음악", "📖\n다이어리", "👤\nMY")
        tabs = ArrayList()
        for (i in 0..4) {
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
        val blp = LinearLayout.LayoutParams(-1, -2)
        blp.setMargins(dp(10), 0, dp(10), dp(8))
        outer.addView(bar, blp)
        setContentView(outer)

        ready = true
        show(0)
        WidgetProvider.refresh(this)
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
                    val pin = if (Extra.ddayGet(this, year, month, d) != null) "📌" else ""
                    val wcolor = if (isToday) "#FFFFFF" else "#1976D2"
                    val scolor = if (isToday) "#FFFFFF" else "#7A5AE0"

                    cell.addView(cellText(d.toString() + " " + dot + pin, 15f, color, true))
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
