package com.dayflow.app

import android.content.Context
import java.util.Calendar

object Extra {

    val moods = arrayOf("😀", "🙂", "😐", "😟", "😢")

    fun key(y: Int, m: Int, d: Int): String {
        return String.format("%04d-%02d-%02d", y, m, d)
    }

    private fun mp(c: Context) = c.getSharedPreferences("dayflow_mood", Context.MODE_PRIVATE)
    private fun dp(c: Context) = c.getSharedPreferences("dayflow_dday", Context.MODE_PRIVATE)

    fun mood(c: Context, k: String): Int {
        return mp(c).getInt(k, -1)
    }

    fun setMood(c: Context, k: String, v: Int) {
        val e = mp(c).edit()
        if (v < 0) e.remove(k) else e.putInt(k, v)
        e.apply()
    }

    fun ddayGet(c: Context, y: Int, m: Int, d: Int): String? {
        return dp(c).getString(key(y, m, d), null)
    }

    fun ddaySet(c: Context, y: Int, m: Int, d: Int, title: String) {
        dp(c).edit().putString(key(y, m, d), title.replace("\n", " ")).apply()
    }

    fun ddayClear(c: Context, y: Int, m: Int, d: Int) {
        dp(c).edit().remove(key(y, m, d)).apply()
    }

    fun ddayNext(c: Context): String? {
        val cal = Calendar.getInstance()
        cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 12, 0, 0)
        for (n in 0..400) {
            val t = ddayGet(c, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
            if (t != null) return (if (n == 0) "D-DAY" else "D-" + n) + " " + t
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        return null
    }

    fun streak(c: Context): Int {
        val sp = c.getSharedPreferences("dayflow_diary", Context.MODE_PRIVATE)
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 12)
        fun has(): Boolean {
            val v = sp.getString(key(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH)), "")
            return !v.isNullOrBlank()
        }
        var n = 0
        if (!has()) cal.add(Calendar.DAY_OF_MONTH, -1)
        while (has() && n < 3650) {
            n++
            cal.add(Calendar.DAY_OF_MONTH, -1)
        }
        return n
    }

    fun brief(c: Context): String {
        val now = Calendar.getInstance()
        val y = now.get(Calendar.YEAR)
        val m = now.get(Calendar.MONTH) + 1
        val d = now.get(Calendar.DAY_OF_MONTH)
        val parts = ArrayList<String>()

        val ev = Scheduler.loadEvents(c, y, m, d)
        if (ev.isEmpty()) {
            parts.add("오늘은 등록된 일정이 없어요")
        } else {
            var best = ev[0]
            var bestT = 99999
            for (e in ev) {
                val p = e.split("|")
                if (p.size < 3) continue
                val t = (p[0].toIntOrNull() ?: 0) * 60 + (p[1].toIntOrNull() ?: 0)
                if (t < bestT) {
                    bestT = t
                    best = e
                }
            }
            val p = best.split("|")
            if (p.size >= 3) {
                parts.add("오늘 일정 " + ev.size + "개, 첫 일정은 " +
                    String.format("%02d:%02d", p[0].toIntOrNull() ?: 0, p[1].toIntOrNull() ?: 0) + " " + p[2])
            } else {
                parts.add("오늘 일정 " + ev.size + "개")
            }
        }

        val w = Weather.detail(c, y, m, d)
        if (!w.contains("예보 없음")) {
            parts.add(w.substringBefore("  (").trim())
            if (w.startsWith("비") || w.startsWith("소나기") || w.startsWith("눈") || w.startsWith("뇌우")) {
                parts.add("우산을 챙기세요")
            }
        }

        val h = CalData.holidayOf(y, m, d)
        if (h != null) parts.add("오늘은 " + h)
        val t = CalData.termOf(m, d)
        if (t != null) parts.add("절기 " + t)

        val dd = ddayNext(c)
        if (dd != null) parts.add("📌 " + dd)

        val s = streak(c)
        if (s >= 2) parts.add("일기 " + s + "일 연속 기록 중")

        return parts.joinToString("\n")
    }
}
