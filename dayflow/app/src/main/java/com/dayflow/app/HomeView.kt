package com.dayflow.app

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.Calendar

object HomeView {

    private fun tv(c: Context, s: String, size: Float, bold: Boolean, color: String): TextView {
        val t = TextView(c)
        t.text = s
        t.textSize = size
        t.setTextColor(Color.parseColor(color))
        if (bold) t.setTypeface(null, Typeface.BOLD)
        return t
    }

    private fun card(c: Context): LinearLayout {
        val dp = c.resources.displayMetrics.density
        val l = LinearLayout(c)
        l.orientation = LinearLayout.VERTICAL
        l.setBackgroundColor(Color.parseColor("#F2FFFFFF"))
        val p = (16 * dp).toInt()
        l.setPadding(p, p, p, p)
        return l
    }

    private fun add(parent: LinearLayout, v: LinearLayout) {
        val dp = parent.resources.displayMetrics.density
        val lp = LinearLayout.LayoutParams(-1, -2)
        lp.setMargins(0, 0, 0, (12 * dp).toInt())
        parent.addView(v, lp)
    }

    fun build(c: Context): ScrollView {
        val dp = c.resources.displayMetrics.density
        val now = Calendar.getInstance()
        val y = now.get(Calendar.YEAR)
        val m = now.get(Calendar.MONTH) + 1
        val d = now.get(Calendar.DAY_OF_MONTH)

        val sv = ScrollView(c)
        val root = LinearLayout(c)
        root.orientation = LinearLayout.VERTICAL
        val pad = (16 * dp).toInt()
        root.setPadding(pad, (32 * dp).toInt(), pad, pad)
        sv.addView(root)

        root.addView(tv(c, "Good Day", 28f, true, "#222222"))
        root.addView(tv(c, y.toString() + "년 " + m + "월 " + d + "일", 15f, false, "#666666"))

        val w = card(c)
        w.addView(tv(c, "오늘의 날씨 (부산)", 16f, true, "#222222"))
        w.addView(tv(c, Weather.detail(c, y, m, d), 15f, false, "#333333"))
        add(root, w)

        val f = card(c)
        f.addView(tv(c, "오늘의 운세 (샘플 점수)", 16f, true, "#222222"))
        f.addView(tv(c, Fortune.score(y, m, d, 0).toString() + "점", 34f, true, "#B8860B"))
        f.addView(tv(c, Fortune.message(y, m, d), 15f, false, "#333333"))
        f.addView(
            tv(
                c,
                "금전 " + Fortune.score(y, m, d, 1) + "  사업 " + Fortune.score(y, m, d, 2) +
                    "  애정 " + Fortune.score(y, m, d, 3) + "  건강 " + Fortune.score(y, m, d, 4) +
                    "\n행운의 숫자 " + Fortune.lucky(y, m, d),
                14f, false, "#555555"
            )
        )
        add(root, f)

        val e = card(c)
        e.addView(tv(c, "오늘 일정", 16f, true, "#222222"))
        val events = Scheduler.loadEvents(c, y, m, d)
        if (events.isEmpty()) {
            e.addView(tv(c, "등록된 일정이 없어요", 14f, false, "#888888"))
        }
        for (line in events) {
            val p = line.split("|")
            if (p.size < 3) continue
            val hh = p[0].toIntOrNull() ?: 0
            val mm = p[1].toIntOrNull() ?: 0
            e.addView(tv(c, "● " + String.format("%02d:%02d", hh, mm) + "  " + p[2], 15f, false, "#333333"))
        }
        add(root, e)

        return sv
    }

    fun placeholder(c: Context, title: String): LinearLayout {
        val l = LinearLayout(c)
        l.orientation = LinearLayout.VERTICAL
        l.gravity = android.view.Gravity.CENTER
        l.addView(tv(c, title, 24f, true, "#222222"))
        l.addView(tv(c, "곧 만들어져요", 14f, false, "#888888"))
        return l
    }
}
