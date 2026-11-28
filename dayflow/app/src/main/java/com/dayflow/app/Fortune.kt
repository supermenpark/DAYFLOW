package com.dayflow.app

import android.content.Context
import java.util.Random

object Fortune {

    private var seed = 0L
    var label = ""

    private val msgs = arrayOf(
        "작은 기회가 큰 행운으로 이어지는 날",
        "서두르지 말고 차분히 움직이면 좋은 날",
        "주변 사람의 말에 귀 기울이면 득이 되는 날",
        "새로운 시도가 좋은 결과를 부르는 날",
        "약속과 일정을 꼼꼼히 챙기면 빛나는 날",
        "마음의 여유가 행운을 데려오는 날"
    )
    private val animals = arrayOf("원숭이", "닭", "개", "돼지", "쥐", "소", "호랑이", "토끼", "용", "뱀", "말", "양")
    private val colors = arrayOf("파랑", "초록", "노랑", "보라", "분홍", "주황", "하양")
    private val actions = arrayOf("가벼운 산책", "일찍 귀가하기", "오랜 친구에게 연락", "따뜻한 차 한 잔", "책상 정리", "감사 인사 전하기", "새로운 길로 가보기")

    private fun star(m: Int, d: Int): String {
        return when {
            (m == 1 && d >= 20) || (m == 2 && d <= 18) -> "물병자리"
            m == 2 || (m == 3 && d <= 20) -> "물고기자리"
            m == 3 || (m == 4 && d <= 19) -> "양자리"
            m == 4 || (m == 5 && d <= 20) -> "황소자리"
            m == 5 || (m == 6 && d <= 21) -> "쌍둥이자리"
            m == 6 || (m == 7 && d <= 22) -> "게자리"
            m == 7 || (m == 8 && d <= 22) -> "사자자리"
            m == 8 || (m == 9 && d <= 22) -> "처녀자리"
            m == 9 || (m == 10 && d <= 23) -> "천칭자리"
            m == 10 || (m == 11 && d <= 22) -> "전갈자리"
            m == 11 || (m == 12 && d <= 21) -> "사수자리"
            else -> "염소자리"
        }
    }

    private fun prefs(c: Context) = c.getSharedPreferences("dayflow_profile", Context.MODE_PRIVATE)

    fun load(c: Context) {
        val p = (prefs(c).getString("birth", "") ?: "").split("-")
        if (p.size == 3) {
            val y = p[0].toIntOrNull()
            val m = p[1].toIntOrNull()
            val d = p[2].toIntOrNull()
            if (y != null && m != null && d != null) {
                seed = y * 400L + m * 31L + d
                label = animals[y % 12] + "띠 · " + star(m, d)
                return
            }
        }
        seed = 0L
        label = ""
    }

    fun setBirth(c: Context, y: Int, m: Int, d: Int) {
        prefs(c).edit().putString("birth", String.format("%04d-%02d-%02d", y, m, d)).apply()
        load(c)
    }

    fun clearBirth(c: Context) {
        prefs(c).edit().remove("birth").apply()
        load(c)
    }

    private fun rnd(y: Int, m: Int, d: Int, k: Int): Random {
        return Random((y * 10000L + m * 100L + d) * 31L + k * 7919L + seed * 13L)
    }

    fun score(y: Int, m: Int, d: Int, k: Int): Int {
        return 60 + rnd(y, m, d, k).nextInt(40)
    }

    fun lucky(y: Int, m: Int, d: Int): Int {
        return 1 + rnd(y, m, d, 99).nextInt(45)
    }

    fun message(y: Int, m: Int, d: Int): String {
        return msgs[rnd(y, m, d, 55).nextInt(msgs.size)]
    }

    private fun money(s: Int): String {
        return if (s >= 85) "수입의 기운이 좋아요" else if (s >= 72) "계획한 만큼만 쓰면 안정적이에요" else "충동구매는 한 번 더 생각해요"
    }

    private fun health(s: Int): String {
        return if (s >= 85) "컨디션이 가벼운 날이에요" else if (s >= 72) "무리하지 않으면 괜찮아요" else "충분히 쉬고 물을 자주 마셔요"
    }

    // 오늘의 운세 한마디 (재미용)
    fun today(y: Int, m: Int, d: Int): String {
        val col = colors[rnd(y, m, d, 7).nextInt(colors.size)]
        val act = actions[rnd(y, m, d, 8).nextInt(actions.size)]
        return message(y, m, d) + "\n" +
            "💰 " + money(score(y, m, d, 1)) + "\n" +
            "💪 " + health(score(y, m, d, 4)) + "\n" +
            "🎨 행운의 색 " + col + " · 🔢 행운의 숫자 " + lucky(y, m, d) + "\n" +
            "👉 추천 행동: " + act
    }
}
