package com.dayflow.app

import android.content.Context
import java.util.Calendar

object Extra {

    val moods = arrayOf("😀", "🙂", "😐", "😟", "😢")

    private val tips = mapOf(
        "소한" to "1년 중 가장 추운 때로 꼽혀요. 따뜻한 국물이 좋아요",
        "대한" to "겨울 끝자락의 추위예요. 곧 봄이 와요",
        "입춘" to "봄이 시작되는 날이에요. 입춘첩을 붙이기도 해요",
        "우수" to "눈이 녹아 비가 되는 때예요",
        "경칩" to "겨울잠 자던 개구리가 깨어나는 때예요",
        "춘분" to "낮과 밤의 길이가 같아요",
        "청명" to "하늘이 맑아 농사 준비를 시작해요",
        "곡우" to "봄비가 곡식을 키워요",
        "입하" to "여름이 시작돼요",
        "소만" to "만물이 점차 자라 가득 차는 때예요",
        "망종" to "보리 수확과 모내기가 한창이에요",
        "하지" to "1년 중 낮이 가장 길어요",
        "소서" to "본격 더위가 시작돼요",
        "대서" to "가장 무더운 때예요. 건강 챙기세요",
        "입추" to "가을이 시작돼요. 아침저녁 바람이 달라져요",
        "처서" to "더위가 한풀 꺾이는 때예요",
        "백로" to "이슬이 맺히기 시작해요",
        "추분" to "낮과 밤의 길이가 같아요",
        "한로" to "찬 이슬이 맺혀요. 단풍이 짙어져요",
        "상강" to "서리가 내리기 시작해요",
        "입동" to "겨울이 시작돼요. 김장을 준비하는 때예요",
        "소설" to "첫눈이 내릴 무렵이에요",
        "대설" to "눈이 많이 내리는 때예요",
        "동지" to "1년 중 밤이 가장 길어요. 팥죽을 먹어요"
    )

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

        val tm = Calendar.getInstance()
        tm.add(Calendar.DAY_OF_MONTH, 1)
        val ty2 = tm.get(Calendar.YEAR)
        val tm2 = tm.get(Calendar.MONTH) + 1
        val td2 = tm.get(Calendar.DAY_OF_MONTH)
        val ev2 = Scheduler.loadEvents(c, ty2, tm2, td2)
        val w2 = Weather.detail(c, ty2, tm2, td2)
        if (ev2.isNotEmpty() && (w2.startsWith("비") || w2.startsWith("소나기") || w2.startsWith("눈") || w2.startsWith("뇌우"))) {
            parts.add("⚠ 내일 일정 " + ev2.size + "개인데 " + w2.substringBefore("  ") + " 예보예요")
        }

        val h = CalData.holidayOf(y, m, d)
        if (h != null) parts.add("오늘은 " + h)
        val t = CalData.termOf(m, d)
        if (t != null) parts.add("절기 " + t + ": " + (tips[t] ?: ""))

        val dd = ddayNext(c)
        if (dd != null) parts.add("📌 " + dd)

        val s = streak(c)
        if (s >= 2) parts.add("일기 " + s + "일 연속 기록 중")

        return parts.joinToString("\n")
    }
}
