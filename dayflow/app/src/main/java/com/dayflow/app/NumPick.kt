package com.dayflow.app

import android.app.Activity
import android.app.AlertDialog
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.TextView
import java.util.Calendar

object NumPick {

    private fun picker(a: Activity, min: Int, max: Int, v: Int, wrap: Boolean): NumberPicker {
        val n = NumberPicker(a)
        n.minValue = min
        n.maxValue = max
        n.value = v
        n.wrapSelectorWheel = wrap
        n.descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
        return n
    }

    private fun unit(a: Activity, s: String): TextView {
        val t = TextView(a)
        t.text = s
        t.textSize = 16f
        t.setPadding(6, 0, 14, 0)
        return t
    }

    fun date(a: Activity, title: String, y0: Int, m0: Int, d0: Int, done: (Int, Int, Int) -> Unit) {
        val row = LinearLayout(a)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER
        row.setPadding(20, 30, 20, 10)
        val ny = picker(a, 1930, 2100, y0, false)
        val nm = picker(a, 1, 12, m0, true)
        val nd = picker(a, 1, 31, d0, true)
        row.addView(ny)
        row.addView(unit(a, "년"))
        row.addView(nm)
        row.addView(unit(a, "월"))
        row.addView(nd)
        row.addView(unit(a, "일"))
        AlertDialog.Builder(a)
            .setTitle(title)
            .setView(row)
            .setPositiveButton("확인") { _, _ ->
                val cal = Calendar.getInstance()
                cal.set(ny.value, nm.value - 1, 1)
                val last = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                done(ny.value, nm.value, if (nd.value > last) last else nd.value)
            }
            .setNegativeButton("취소", null)
            .show()
    }
}
