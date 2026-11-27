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

    private fun graph(c: Context, y: Int, m: Int, vals: IntArray): View {
        val days = vals.size
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
                var px = -1f
                var py = -1f
                val span = (days - 1).coerceAtLeast(1)
                for (i in 0 until days) {
                    if (vals[i] < 0) {
