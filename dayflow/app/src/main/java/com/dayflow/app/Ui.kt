package com.dayflow.app

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.widget.Button

object Ui {

    val tops = arrayOf("#7DB8FF", "#86E3A6", "#FFC27A", "#CDA6FF", "#FF9DB3")
    val bottoms = arrayOf("#3A7BD5", "#2FA866", "#E8772E", "#8A55D8", "#E0526F")

    fun style(c: Context, b: Button, idx: Int) {
        val i = idx % tops.size
        val d = c.resources.displayMetrics.density
        val g = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(Color.parseColor(tops[i]), Color.parseColor(bottoms[i]))
        )
        g.cornerRadius = 14f * d
        g.setStroke((1 * d).toInt(), Color.parseColor(bottoms[i]))
        b.background = g
        b.setTextColor(Color.WHITE)
        b.isAllCaps = false
        b.elevation = 6f * d
    }
}
