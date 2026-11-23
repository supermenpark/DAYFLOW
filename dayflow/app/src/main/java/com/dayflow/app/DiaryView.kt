package com.dayflow.app

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.view.Gravity
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
        return String.format(
            "%04d-%02d-%02d",
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH)
        )
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

        val paper = LinearLayout(c)
        paper.orientation = LinearLayout.VERTICAL
        paper.background = shape(c, "#FFFEF5", 18)
        paper.elevation = dp(c, 4).toFloat()
        paper.setPadding(dp(c, 18), dp(c, 14), dp(c, 18), dp(c, 18))

        val input = EditText(c)

        // 날짜 이동 줄
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

        val line = android.view.View(c)
        line.setBackgroundColor(Color.parseColor("#E8DFC8"))
        val lineLp = LinearLayout.LayoutParams(-1, dp(c, 1))
        lineLp.setMargins(0, dp(c, 12), 0, dp(c, 12))
        paper.addView(line, lineLp)

        input.textSize = 16f
        input.setTextColor(Color.parseColor("#333333"))
        input.setHintTextColor(Color.parseColor("#AAAAAA"))
        input.hint = "이 날은 어땠나요?\n\n기억하고 싶은 일, 생각, 감정을 자유롭게 적어보세요."
        input.gravity = Gravity.TOP or Gravity.START
        input.minLines = 12
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

        // 쓴 날 목록
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
            val row = LinearLayout(c)
            row.orientation = LinearLayout.VERTICAL
            row.background = shape(c, if (k == key) "#E4EEFC" else "#FFFFFF", 14)
            row.elevation = dp(c, 2).toFloat()
            row.setPadding(dp(c, 14), dp(c, 10), dp(c, 14), dp(c, 10))
            row.addView(tv(c, short(k), 13f, true, "#2F7BE8"))
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
