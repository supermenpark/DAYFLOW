package com.dayflow.app

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.Calendar

object MemoView {

    private val colors = arrayOf("#FFF1B8", "#C9E6FA", "#C4EBD6", "#F9CFDC", "#DACCF3")
    private var grid = false

    private fun dp(c: Context, v: Int): Int {
        return (v * c.resources.displayMetrics.density).toInt()
    }

    private fun card(c: Context, y: Int, m: Int, d: Int, text: String, idx: Int): LinearLayout {
        val box = LinearLayout(c)
        box.orientation = LinearLayout.VERTICAL
        val g = GradientDrawable()
        g.setColor(Color.parseColor(colors[idx % colors.size]))
        g.cornerRadius = dp(c, 6).toFloat()
        box.background = g
        box.elevation = dp(c, 3).toFloat()
        box.setPadding(dp(c, 12), dp(c, 12), dp(c, 12), dp(c, 12))

        val head = TextView(c)
        head.text = m.toString() + "월 " + d + "일"
        head.textSize = 13f
        head.setTypeface(null, Typeface.BOLD)
        head.setTextColor(Color.parseColor("#555555"))
        box.addView(head)

        val body = TextView(c)
        body.text = text
        body.textSize = 15f
        body.setTextColor(Color.parseColor("#222222"))
        body.maxLines = if (grid) 4 else 8
        body.setPadding(0, dp(c, 6), 0, 0)
        box.addView(body)

        if (!grid) box.rotation = if (idx % 2 == 0) -1.2f else 1.2f

        box.setOnClickListener {
            val i = Intent(c, DayActivity::class.java)
            i.putExtra("y", y)
            i.putExtra("m", m)
            i.putExtra("d", d)
            c.startActivity(i)
        }
        return box
    }

    fun build(c: Context): ScrollView {
        val sv = ScrollView(c)
        val root = LinearLayout(c)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(dp(c, 14), dp(c, 32), dp(c, 14), dp(c, 24))
        sv.addView(root)

        val title = TextView(c)
        title.text = "메모"
        title.textSize = 26f
        title.setTypeface(null, Typeface.BOLD)
        title.setTextColor(Color.parseColor("#222222"))
        root.addView(title)

        val toggle = Button(c)
        toggle.text = if (grid) "포스트잇 모양으로 보기" else "바둑판 모양으로 보기"
        Ui.style(c, toggle, 1)
        toggle.setOnClickListener {
            grid = !grid
            val p = sv.parent
            if (p is android.view.ViewGroup) {
                val k = p.indexOfChild(sv)
                p.removeViewAt(k)
                p.addView(build(c), k, android.widget.FrameLayout.LayoutParams(-1, -1))
            }
        }
        root.addView(toggle)

        val now = Calendar.getInstance()
        val start = Calendar.getInstance()
        start.set(now.get(Calendar.YEAR) - 1, 0, 1, 12, 0, 0)
        val items = ArrayList<IntArray>()
        val texts = ArrayList<String>()
        val cur = Calendar.getInstance()
        cur.time = start.time
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
            val none = TextView(c)
            none.text = "저장된 메모가 없어요.\n달력에서 날짜를 눌러 메모를 남겨 보세요."
            none.setTextColor(Color.parseColor("#888888"))
            none.setPadding(0, dp(c, 24), 0, 0)
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
                    lp.setMargins(dp(c, 6), dp(c, 8), dp(c, 6), dp(c, 8))
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
