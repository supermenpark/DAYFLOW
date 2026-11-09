package com.dayflow.app

import java.util.Random

object Fortune {

    private val msgs = arrayOf(
        "작은 기회가 큰 행운으로 이어지는 날",
        "서두르지 말고 차분히 움직이면 좋은 날",
        "주변 사람의 말에 귀 기울이면 득이 되는 날",
        "새로운 시도가 좋은 결과를 부르는 날",
        "약속과 일정을 꼼꼼히 챙기면 빛나는 날",
        "마음의 여유가 행운을 데려오는 날"
    )

    private fun rnd(y: Int, m: Int, d: Int, k: Int): Random {
        val seed = (y * 10000L + m * 100L + d) * 31L + k * 7919L
        return Random(seed)
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
}
