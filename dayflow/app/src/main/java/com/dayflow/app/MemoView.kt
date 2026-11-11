package com.dayflow.app

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.Calendar

object MemoView {

    private val accents = arrayOf("#2F7BE8", "#2FA866", "#E8772E", "#8A55D8", "#E0526F")
    private var grid = false

    private fun dp(c: Context, v: Int): Int {
        return (v * c.resources.displayMetrics.density).toInt()
    }

    private fun tv(c: Context, s: String, size: Float, bold: Boolean, color: String): TextView {
        val t = TextView(c)
        t.text = s
        t.textSize = size
        t.setTextColor(Color.parseColor(color))
        if (bold) t.setTypeface(null, Typeface.BOLD)
        return t
    }

    private fun shape(color: String, r: Int, c: Context): GradientDrawable {
        val g = GradientDrawable()
        g.setColor(Color.parseColor(color))
        g.cornerRadius = dp(c, r).toFloat()
        return g
    }

    // 왼쪽에 색 띠가 있는 흰 카드
    private fun card(c: Context, y: Int, m: Int, d: Int, text: String, idx: Int): View2 {
        val accent = accents[idx % accents.size]
        val outer = LinearLayout(c)
        outer.orientation = LinearLayout.HORIZONTAL
        outer.background = shape("#FFFFFF", 16, c)
        outer.elevation = dp(c, 3).toFloat()

        val bar = android.view.View(c)
        bar.background = shape(accent, 4, c)
        outer.addView(bar, LinearLayout.LayoutParams(dp(c, 5), -1))

        val col = LinearLayout(c)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(dp(c, 14), dp(c, 12), dp(c, 14), dp(c, 14))
        col.addView(tv(c, "📅 " + m + "월 " + d + "일", 14f, true, accent))
        val body = tv(c, text, 15f, false, "#222222")
        body.maxLines = if (grid) 4 else 8
        body.setPadding(0, dp(c, 6), 0, 0)
        col.addView(body)
        outer.addView(col, LinearLayout.LayoutParams(0, -2, 1f))

        val pen = tv(c, "✎", 16f, true, "#5B7FA8")
        pen.gravity = Gravity.CENTER
        pen.background = shape("#E8F0FA", 18, c)
        val plp = LinearLayout.LayoutParams(dp(c, 36), dp(c, 36))
        plp.setMargins(0, dp(c, 10), dp(c, 10), 0)
        outer.addView(pen, plp)

        outer.setOnClickListener {
            val i = Intent(c, DayActivity::class.java)
            i.putExtra("y", y)
            i.putExtra("m", m)
            i.putExtra("d", d)
            c.startActivity(i)
        }
        return outer
    }

    fun build(c: Context): ScrollView {
        val sv = ScrollView(c)
        sv.setBackgroundColor(Color.parseColor("#F6F9FD"))
        val root = LinearLayout(c)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(dp(c, 16), dp(c, 40), dp(c, 16), dp(c, 24))
        sv.addView(root)

        root.addView(tv(c, "📝 메모", 30f, true, "#0F2A55"))

        val toggle = tv(c, if (grid) "＋  포스트잇 모양으로 보기" else "＋  바둑판 모양으로 보기", 16f, true, "#FFFFFF")
        toggle.gravity = Gravity.CENTER
        toggle.background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(Color.parseColor("#5CB8FF"), Color.parseColor("#2F86EA"))
        ).also { it.cornerRadius = dp(c, 28).toFloat() }
        toggle.elevation = dp(c, 4).toFloat()
        toggle.setPadding(0, dp(c, 16), 0, dp(c, 16))
        toggle.setOnClickListener {
            grid = !grid
            val p = sv.parent
            if (p is ViewGroup) {
                val k = p.indexOfChild(sv)
                p.removeViewAt(k)
                p.addView(build(c), k, FrameLayout.LayoutParams(-1, -1))
            }
        }
        val tlp = LinearLayout.LayoutParams(-1, -2)
        tlp.setMargins(0, dp(c, 16), 0, dp(c, 12))
        root.addView(toggle, tlp)

        val now = Calendar.getInstance()
        val cur = Calendar.getInstance()
        cur.set(now.get(Calendar.YEAR) - 1, 0, 1, 12, 0, 0)
        val items = ArrayList<IntArray>()
        val texts = ArrayList<String>()
        for (n in 0..1100) {
            val y = cur.get(Calendar.YEAR)
            val m = cur.get(Calendar.MONTH) + 1
            val d = cur.get(Calendar.DAY_OF_MONTH)
            val t = Scheduler.getMemo(c, y, m, d)
            if (t.isNotEmpty()) {
                items.add(intArrayOf(y, m, d))
                texts.add(t)
            }
            cur.add(Calendar.DAY_OF_MONTH, 1)
        }

        if (items.isEmpty()) {
            val none = tv(c, "저장된 메모가 없어요.\n홈 달력에서 날짜를 눌러 메모를 남겨 보세요.", 14f, false, "#8895A7")
            none.gravity = Gravity.CENTER
            none.setPadding(0, dp(c, 40), 0, 0)
            root.addView(none)
            return sv
        }

        val cols = if (grid) 2 else 1
        var i = 0
        while (i < items.size) {
            val row = LinearLayout(c)
            row.orientation = LinearLayout.HORIZONTAL
            for (k in 0 until cols) {
                val idx = i + k
                if (idx < items.size) {
                    val it2 = items[idx]
                    val cv = card(c, it2[0], it2[1], it2[2], texts[idx], idx)
                    val lp = LinearLayout.LayoutParams(0, -2, 1f)
                    lp.setMargins(dp(c, 4), dp(c, 6), dp(c, 4), dp(c, 6))
                    row.addView(cv, lp)
                } else {
                    row.addView(TextView(c), LinearLayout.LayoutParams(0, -2, 1f))
                }
            }
            root.addView(row)
            i += cols
        }
        return sv
    }
}

typealias View2 = LinearLayout
