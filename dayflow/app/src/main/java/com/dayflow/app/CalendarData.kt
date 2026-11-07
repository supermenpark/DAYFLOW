package com.dayflow.app

import java.util.Calendar

object CalData {

    private val solarHolidays = mapOf(
        "1-1" to "신정", "3-1" to "삼일절", "5-5" to "어린이날",
        "6-6" to "현충일", "8-15" to "광복절", "10-3" to "개천절",
        "10-9" to "한글날", "12-25" to "성탄절"
    )

    private val solarTerms = mapOf(
        "1-5" to "소한", "1-20" to "대한", "2-4" to "입춘", "2-19" to "우수",
        "3-5" to "경칩", "3-20" to "춘분", "4-5" to "청명", "4-20" to "곡우",
        "5-5" to "입하", "5-21" to "소만", "6-5" to "망종", "6-21" to "하지",
        "7-7" to "소서", "7-22" to "대서", "8-7" to "입추", "8-23" to "처서",
        "9-7" to "백로", "9-23" to "추분", "10-8" to "한로", "10-23" to "상강",
        "11-7" to "입동", "11-22" to "소설", "12-7" to "대설", "12-22" to "동지"
    )

    fun lunarOf(y: Int, m: Int, d: Int): IntArray {
        val g = Calendar.getInstance()
        g.set(y, m - 1, d, 12, 0, 0)
        val c = android.icu.util.ChineseCalendar()
        c.timeInMillis = g.timeInMillis
        val lm = c.get(android.icu.util.Calendar.MONTH) + 1
        val ld = c.get(android.icu.util.Calendar.DAY_OF_MONTH)
        val leap = c.get(android.icu.util.Calendar.IS_LEAP_MONTH)
        return intArrayOf(lm, ld, leap)
    }

    fun lunarLabel(y: Int, m: Int, d: Int): String {
        val l = lunarOf(y, m, d)
        if (l[1] == 1) {
            return (if (l[2] == 1) "윤" else "") + l[0] + "." + l[1]
        }
        return l[1].toString()
    }

    fun termOf(m: Int, d: Int): String? {
        return solarTerms[m.toString() + "-" + d.toString()]
    }

    fun holidayOf(y: Int, m: Int, d: Int): String? {
        val s = solarHolidays[m.toString() + "-" + d.toString()]
        if (s != null) return s
        val l = lunarOf(y, m, d)
        if (l[2] == 0) {
            if (l[0] == 1 && l[1] == 1) return "설날"
            if (l[0] == 1 && l[1] == 2) return "설연휴"
            if (l[0] == 4 && l[1] == 8) return "석가탄신일"
            if (l[0] == 8 && l[1] == 14) return "추석연휴"
            if (l[0] == 8 && l[1] == 15) return "추석"
            if (l[0] == 8 && l[1] == 16) return "추석연휴"
        }
        val t = Calendar.getInstance()
        t.set(y, m - 1, d, 12, 0, 0)
        t.add(Calendar.DAY_OF_MONTH, 1)
        val n = lunarOf(
            t.get(Calendar.YEAR),
            t.get(Calendar.MONTH) + 1,
            t.get(Calendar.DAY_OF_MONTH)
        )
        if (n[0] == 1 && n[1] == 1 && n[2] == 0) return "설연휴"
        return null
    }
}
