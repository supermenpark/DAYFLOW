package com.dayflow.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.util.Calendar

object DiaryView {

    private var key = ""
    private val dows = arrayOf("일", "월", "화", "수", "목", "금", "토")

    private fun dp(c: Context, v: Int): Int {
        return (v * c.resources.displayMetrics.density).toInt()
    }

    private fun prefs(c: Context) = c.getSharedPreferences("dayflow_diary", Context.MODE_PRIVATE)

    private fun fmt(cal: Calendar): String {
        return Extra.key(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
    }

    private fun parse(k: String): Calendar {
        val cal = Calendar.getInstance()
        val p = k.split("-")
        if (p.size == 3) {
            val y = p[0].toIntOrNull()
            val m = p[1].toIntOrNull()
            val d = p[2].toIntOrNull()
            if (y != null && m != null && d != null) cal.set(y, m - 1, d, 12, 0, 0)
        }
        return cal
    }

    private fun shift(k: String, delta: Int): String {
        val cal = parse(k)
        cal.add(Calendar.DAY_OF_MONTH, delta)
        return fmt(cal)
    }

    private fun nice(k: String): String {
        val cal = parse(k)
        return cal.get(Calendar.YEAR).toString() + "년 " + (cal.get(Calendar.MONTH) + 1) + "월 " +
            cal.get(Calendar.DAY_OF_MONTH) + "일 (" + dows[cal.get(Calendar.DAY_OF_WEEK) - 1] + ")"
    }

    private fun short(k: String): String {
        val cal = parse(k)
        return (cal.get(Calendar.MONTH) + 1).toString() + "월 " + cal.get(Calendar.DAY_OF_MONTH) +
            "일 (" + dows[cal.get(Calendar.DAY_OF_WEEK) - 1] + ")"
    }

    private fun tv(c: Context, s: String, size: Float, bold: Boolean, color: String): TextView {
        val t = TextView(c)
        t.text = s
        t.textSize = size
        t.setTextColor(Color.parseColor(color))
        if (bold) t.setTypeface(null, Typeface.BOLD)
        return t
    }

    private fun shape(c: Context, color: String, r: Int): GradientDrawable {
        val g = GradientDrawable()
        g.setColor(Color.parseColor(color))
        g.cornerRadius = dp(c, r).toFloat()
        return g
    }

    private fun save(c: Context, k: String, text: String) {
        val e = prefs(c).edit()
        if (text.isBlank()) e.remove(k) else e.putString(k, text)
        e.apply()
    }

    private fun refresh(c: Context, sv: ScrollView) {
        val p = sv.parent
        if (p is ViewGroup) {
            val k = p.indexOfChild(sv)
            p.removeViewAt(k)
            p.addView(build(c), k, FrameLayout.LayoutParams(-1, -1))
        }
    }

    private fun graph(c: Context, vals: IntArray): View {
        return object : View(c) {
            override fun onDraw(cv: Canvas) {
                val w = width.toFloat()
                val h = height.toFloat()
                val pad = dp(c, 16).toFloat()
                val grid = Paint(Paint.ANTI_ALIAS_FLAG)
                grid.color = Color.parseColor("#E4EAF2")
                grid.strokeWidth = 2f
                val line = Paint(Paint.ANTI_ALIAS_FLAG)
                line.color = Color.parseColor("#2F7BE8")
                line.strokeWidth = dp(c, 2).toFloat()
                val dot = Paint(Paint.ANTI_ALIAS_FLAG)
                dot.color = Color.parseColor("#6A3DE8")
                for (i in 0..4) {
                    val gy = pad + (h - 2 * pad) * i / 4f
                    cv.drawLine(pad, gy, w - pad, gy, grid)
                }
                val span = (vals.size - 1).coerceAtLeast(1)
                var px = -1f
                var py = -1f
                for (i in vals.indices) {
                    if (vals[i] < 0) {
                        px = -1f
                        continue
                    }
                    val x = pad + (w - 2 * pad) * i / span
                    val y = pad + (h - 2 * pad) * vals[i] / 4f
                    if (px >= 0) cv.drawLine(px, py, x, y, line)
                    cv.drawCircle(x, y, dp(c, 4).toFloat(), dot)
                    px = x
                    py = y
                }
            }
        }
    }

    fun build(c: Context): ScrollView {
        val todayKey = fmt(Calendar.getInstance())
        if (key.isEmpty()) key = todayKey

        val sv = ScrollView(c)
        sv.setBackgroundColor(Color.parseColor("#F4F7FB"))
        val root = LinearLayout(c)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(dp(c, 16), dp(c, 32), dp(c, 16), dp(c, 30))
        sv.addView(root)

        root.addView(tv(c, "📖 나의 다이어리", 25f, true, "#26364A"))
        val st = Extra.streak(c)
        if (st >= 2) root.addView(tv(c, "🔥 " + st + "일 연속 기록 중", 13f, true, "#E8772E"))

        val paper = LinearLayout(c)
        paper.orientation = LinearLayout.VERTICAL
        paper.background = shape(c, "#FFFEF5", 18)
        paper.elevation = dp(c, 4).toFloat()
        paper.setPadding(dp(c, 18), dp(c, 14), dp(c, 18), dp(c, 18))

        val input = EditText(c)

        val nav = LinearLayout(c)
        nav.orientation = LinearLayout.HORIZONTAL
        nav.gravity = Gravity.CENTER_VERTICAL
        val prev = tv(c, "‹", 28f, true, "#4A6A91")
        prev.setPadding(dp(c, 12), 0, dp(c, 12), 0)
        val next = tv(c, "›", 28f, true, "#4A6A91")
        next.setPadding(dp(c, 12), 0, dp(c, 12), 0)
        val dateLabel = tv(c, nice(key), 17f, true, "#4A6A91")
        dateLabel.gravity = Gravity.CENTER
        nav.addView(prev)
        nav.addView(dateLabel, LinearLayout.LayoutParams(0, -2, 1f))
        nav.addView(next)
        paper.addView(nav)

        prev.setOnClickListener {
            save(c, key, input.text.toString())
            key = shift(key, -1)
            refresh(c, sv)
        }
        next.setOnClickListener {
            save(c, key, input.text.toString())
            key = shift(key, 1)
            refresh(c, sv)
        }

        if (key != todayKey) {
            val back = tv(c, "오늘로 돌아가기", 12f, true, "#2F7BE8")
            back.gravity = Gravity.CENTER
            back.setPadding(0, dp(c, 6), 0, 0)
            back.setOnClickListener {
                save(c, key, input.text.toString())
                key = todayKey
                refresh(c, sv)
            }
            paper.addView(back)
        }

        val moodRow = LinearLayout(c)
        moodRow.orientation = LinearLayout.HORIZONTAL
        val cur = Extra.mood(c, key)
        for (i in 0..4) {
            val t = tv(c, Extra.moods[i], 26f, false, "#000000")
            t.gravity = Gravity.CENTER
            t.setPadding(0, dp(c, 8), 0, dp(c, 8))
            if (cur == i) t.background = shape(c, "#E4EEFC", 14)
            t.setOnClickListener {
                save(c, key, input.text.toString())
                Extra.setMood(c, key, if (Extra.mood(c, key) == i) -1 else i)
                refresh(c, sv)
            }
            moodRow.addView(t, LinearLayout.LayoutParams(0, -2, 1f))
        }
        val mlp = LinearLayout.LayoutParams(-1, -2)
        mlp.setMargins(0, dp(c, 10), 0, 0)
        paper.addView(moodRow, mlp)

        val line = View(c)
        line.setBackgroundColor(Color.parseColor("#E8DFC8"))
        val lineLp = LinearLayout.LayoutParams(-1, dp(c, 1))
        lineLp.setMargins(0, dp(c, 12), 0, dp(c, 12))
        paper.addView(line, lineLp)

        input.textSize = 16f
        input.setTextColor(Color.parseColor("#333333"))
        input.setHintTextColor(Color.parseColor("#AAAAAA"))
        input.hint = "이 날은 어땠나요?\n\n기억하고 싶은 일, 생각, 감정을 자유롭게 적어보세요."
        input.gravity = Gravity.TOP or Gravity.START
        input.minLines = 10
        input.setPadding(dp(c, 4), dp(c, 8), dp(c, 4), dp(c, 8))
        input.background = null
        input.inputType = InputType.TYPE_CLASS_TEXT or
            InputType.TYPE_TEXT_FLAG_MULTI_LINE or
            InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        input.setText(prefs(c).getString(key, "") ?: "")
        paper.addView(input, LinearLayout.LayoutParams(-1, -2))

        val plp = LinearLayout.LayoutParams(-1, -2)
        plp.setMargins(0, dp(c, 14), 0, dp(c, 14))
        root.addView(paper, plp)

        val saveBtn = Button(c)
        saveBtn.text = "일기 저장하기"
        Ui.style(c, saveBtn, 0)
        saveBtn.setOnClickListener {
            save(c, key, input.text.toString())
            val imm = c.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(input.windowToken, 0)
            Toast.makeText(c, nice(key) + " 일기를 저장했어요.", Toast.LENGTH_SHORT).show()
            refresh(c, sv)
        }
        root.addView(saveBtn, LinearLayout.LayoutParams(-1, dp(c, 52)))

        // 월간 기분 그래프
        val kc = parse(key)
        val ky = kc.get(Calendar.YEAR)
        val km = kc.get(Calendar.MONTH) + 1
        val dim = kc.getActualMaximum(Calendar.DAY_OF_MONTH)
        val vals = IntArray(dim)
        var cnt = 0
        for (d in 1..dim) {
            vals[d - 1] = Extra.mood(c, Extra.key(ky, km, d))
            if (vals[d - 1] >= 0) cnt++
        }
        val gh = tv(c, km.toString() + "월 기분 흐름 (" + cnt + "일 기록)  😀위 ~ 😢아래", 14f, true, "#26364A")
        val ghl = LinearLayout.LayoutParams(-1, -2)
        ghl.setMargins(0, dp(c, 22), 0, dp(c, 8))
        root.addView(gh, ghl)
        val gv = graph(c, vals)
        gv.background = shape(c, "#FFFFFF", 14)
        root.addView(gv, LinearLayout.LayoutParams(-1, dp(c, 130)))

        val days = ArrayList<String>()
        for ((k, v) in prefs(c).all) {
            if (v is String && v.isNotBlank()) days.add(k)
        }
        days.sortDescending()

        val head = tv(c, "지난 일기 (" + days.size + ")", 16f, true, "#26364A")
        val hlp = LinearLayout.LayoutParams(-1, -2)
        hlp.setMargins(0, dp(c, 22), 0, dp(c, 8))
        root.addView(head, hlp)

        if (days.isEmpty()) {
            root.addView(tv(c, "아직 쓴 일기가 없어요.", 13f, false, "#8895A7"))
        }
        for (k in days.take(60)) {
            val raw = prefs(c).getString(k, "") ?: ""
            val first = raw.trim().lines().firstOrNull() ?: ""
            val preview = if (first.length > 32) first.substring(0, 32) + "…" else first
            val mi = Extra.mood(c, k)
            val row = LinearLayout(c)
            row.orientation = LinearLayout.VERTICAL
            row.background = shape(c, if (k == key) "#E4EEFC" else "#FFFFFF", 14)
            row.elevation = dp(c, 2).toFloat()
            row.setPadding(dp(c, 14), dp(c, 10), dp(c, 14), dp(c, 10))
            row.addView(tv(c, (if (mi >= 0) Extra.moods[mi] + " " else "") + short(k), 13f, true, "#2F7BE8"))
            row.addView(tv(c, preview, 14f, false, "#333333"))
            row.setOnClickListener {
                save(c, key, input.text.toString())
                key = k
                refresh(c, sv)
            }
            val rlp = LinearLayout.LayoutParams(-1, -2)
            rlp.setMargins(0, dp(c, 4), 0, dp(c, 4))
            root.addView(row, rlp)
        }
        return sv
    }
}
