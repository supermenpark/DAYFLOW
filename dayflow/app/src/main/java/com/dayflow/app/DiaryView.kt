
package com.dayflow.app

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object DiaryView {

    private var selectedDate = Calendar.getInstance()

    private fun dp(c: Context, v: Int) =
        (v * c.resources.displayMetrics.density).toInt()

    private fun background(color: String, radius: Int, c: Context) =
        GradientDrawable().apply {
            setColor(Color.parseColor(color))
            cornerRadius = dp(c, radius).toFloat()
        }

    private fun prefs(c: Context) =
        c.getSharedPreferences("dayflow_diary", Context.MODE_PRIVATE)

    private fun key(): String =
        SimpleDateFormat("yyyyMMdd", Locale.KOREA)
            .format(selectedDate.time)

    private fun text(
        c: Context,
        value: String,
        size: Float,
        bold: Boolean,
        color: String
    ): TextView {
        return TextView(c).apply {
            text = value
            textSize = size
            setTextColor(Color.parseColor(color))
            if (bold) setTypeface(null, Typeface.BOLD)
        }
    }

    fun build(c: Context): ScrollView {
        val scroll = ScrollView(c)
        scroll.setBackgroundColor(Color.parseColor("#F4EFE5"))

        val root = LinearLayout(c)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(dp(c, 20), dp(c, 28), dp(c, 20), dp(c, 30))
        scroll.addView(root)

        root.addView(text(c, "MY DIARY", 13f, true, "#A28B70"))
        root.addView(text(c, "나의 하루를 기록하다", 25f, true, "#514438"))

        val date = text(
            c,
            SimpleDateFormat("yyyy년 M월 d일", Locale.KOREA)
                .format(selectedDate.time),
            17f, true, "#806B55"
        )
        date.gravity = Gravity.CENTER
        date.setPadding(0, dp(c, 22), 0, dp(c, 12))
        root.addView(date)

        val navigation = LinearLayout(c)
        navigation.gravity = Gravity.CENTER

        val prev = Button(c)
        prev.text = "〈 이전 날"
        val next = Button(c)
        next.text = "다음 날 〉"

        navigation.addView(prev)
        navigation.addView(next)
        root.addView(navigation)

        val paper = LinearLayout(c)
        paper.orientation = LinearLayout.VERTICAL
        paper.background = background("#FFFCF5", 18, c)
        paper.elevation = dp(c, 3).toFloat()
        paper.setPadding(dp(c, 18), dp(c, 20), dp(c, 18), dp(c, 20))

        paper.addView(text(c, "오늘의 기록", 19f, true, "#5A4938"))

        val memo = EditText(c)
        memo.hint = "오늘 있었던 일, 기억하고 싶은 순간을 적어보세요."
        memo.gravity = Gravity.TOP
        memo.minLines = 12
        memo.textSize = 16f
        memo.setTextColor(Color.parseColor("#514438"))
        memo.setHintTextColor(Color.parseColor("#B6AA9A"))
        memo.setPadding(dp(c, 8), dp(c, 16), dp(c, 8), dp(c, 16))
        memo.background = null

        paper.addView(memo)
        root.addView(paper)

        val save = Button(c)
        save.text = "기록 저장하기"
        save.background = background("#9B8063", 14, c)
        save.setTextColor(Color.WHITE)

        val lp = LinearLayout.LayoutParams(-1, dp(c, 54))
        lp.setMargins(0, dp(c, 16), 0, dp(c, 20))
        root.addView(save, lp)

        val quote = text(
            c,
            "오늘도 충분히 잘해냈어요.\n내일은 또 다른 좋은 하루가 될 거예요.",
            15f, false, "#947D65"
        )
        quote.gravity = Gravity.CENTER
        quote.setPadding(0, dp(c, 12), 0, dp(c, 24))
        root.addView(quote)

        val info = arrayOf(
            "☀ 오늘의 날씨",
            "📰 오늘의 뉴스",
            "📈 나의 관심 주식",
            "📷 사진과 동영상"
        )

        for (item in info) {
            val card = text(c, item + "\n준비 중인 다이어리 공간", 15f, true, "#786653")
            card.background = background("#FFFCF5", 16, c)
            card.setPadding(dp(c, 16), dp(c, 18), dp(c, 16), dp(c, 18))
            val p = LinearLayout.LayoutParams(-1, -2)
            p.setMargins(0, dp(c, 5), 0, dp(c, 7))
            root.addView(card, p)
        }

        fun load() {
            date.text = SimpleDateFormat(
                "yyyy년 M월 d일", Locale.KOREA
            ).format(selectedDate.time)

            memo.setText(prefs(c).getString(key(), ""))
        }

        prev.setOnClickListener {
            selectedDate.add(Calendar.DAY_OF_MONTH, -1)
            load()
        }

        next.setOnClickListener {
            selectedDate.add(Calendar.DAY_OF_MONTH, 1)
            load()
        }

        save.setOnClickListener {
            prefs(c).edit()
                .putString(key(), memo.text.toString())
                .apply()

            Toast.makeText(c, "오늘의 기록을 저장했어요", Toast.LENGTH_SHORT).show()
        }

        load()
        return scroll
    }
}
