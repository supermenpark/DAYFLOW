package com.dayflow.app

import android.content.Context

object Theme {

    val names = arrayOf("크림", "하늘", "민트", "핑크", "라벤더")
    val bgColors = arrayOf("#FFFBEA", "#E8F4FD", "#E6F7EE", "#FDEBF0", "#F0EAFB")
    val cardColors = arrayOf("#FFF1B8", "#C9E6FA", "#C4EBD6", "#F9CFDC", "#DACCF3")

    private fun prefs(c: Context) =
        c.getSharedPreferences("dayflow_theme", Context.MODE_PRIVATE)

    fun get(c: Context): Int {
        val v = prefs(c).getInt("idx", 0)
        if (v < 0 || v >= names.size) return 0
        return v
    }

    fun set(c: Context, idx: Int) {
        prefs(c).edit().putInt("idx", idx).apply()
    }

    fun bg(c: Context): String {
        return bgColors[get(c)]
    }

    fun card(c: Context): String {
        return cardColors[get(c)]
    }
}
