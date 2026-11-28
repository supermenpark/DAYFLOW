package com.dayflow.app

import java.util.Calendar

object Terms {

    private val mon = intArrayOf(1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7, 8, 8, 9, 9, 10, 10, 11, 11, 12, 12)
    private val day = intArrayOf(5, 20, 4, 19, 5, 20, 5, 20, 5, 21, 5, 21, 7, 22, 7, 23, 7, 23, 8, 23, 7, 22, 7, 22)
    private val name = arrayOf(
        "소한", "대한", "입춘", "우수", "경칩", "춘분", "청명", "곡우", "입하", "소만", "망종", "하지",
        "소서", "대서", "입추", "처서", "백로", "추분", "한로", "상강", "입동", "소설", "대설", "동지"
    )
    private val tip = arrayOf(
        "가장 추운 때, 따뜻한 국물이 좋아요", "겨울 끝자락, 곧 봄이에요", "봄의 시작, 입춘첩을 붙여요", "눈이 녹아 비가 돼요",
        "개구리가 깨어나요", "낮과 밤의 길이가 같아요", "하늘이 맑고 농사를 준비해요", "봄비가 곡식을 키워요",
        "여름의 시작이에요", "만물이 자라 가득 차요", "보리 수확과 모내기철이에요", "낮이 가장 긴 날이에요",
        "본격 더위가 시작돼요", "가장 무더운 때, 건강 챙기세요", "가을의 시작이에요", "더위가 한풀 꺾여요",
        "이슬이 맺히기 시작해요", "낮과 밤의 길이가 같아요", "찬 이슬, 단풍이 짙어져요", "서리가 내리기 시작해요",
        "겨울의 시작, 김장철이에요", "첫눈이 올 무렵이에요", "눈이 많이 내려요", "밤이 가장 긴 날, 팥죽을 먹어요"
    )

    // 오늘이 절기 당일이면 null(브리핑이 따로 표시), 아니면 다음 절기까지 남은 날
    fun line(): String? {
        val cal = Calendar.getInstance()
        for (n in 0..370) {
            val m = cal.get(Calendar.MONTH) + 1
            val d = cal.get(Calendar.DAY_OF_MONTH)
            for (i in mon.indices) {
                if (mon[i] == m && day[i] == d) {
                    if (n == 0) return null
                    return "🌿 다음 절기 " + name[i] + " D-" + n + " · " + tip[i]
                }
            }
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        return null
    }
}
