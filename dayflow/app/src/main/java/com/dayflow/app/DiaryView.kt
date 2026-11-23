
package com.dayflow.app

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.util.Calendar

object DiaryView {

    private var key = ""

    private val dows =
        arrayOf("일", "월", "화", "수", "목", "금", "토")

    private fun dp(c: Context, v: Int): Int {
        return (v * c.resources.displayMetrics.density).toInt()
    }

    private fun prefs(c: Context) =
        c.getSharedPreferences("dayflow_diary", Context.MODE_PRIVATE)

    private fun fmt(cal: Calendar): String {
        return String.format(
            "%04d-%02d-%02d",
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    private fun parse(k: String): Calendar {
        val cal = Calendar.getInstance()
        val p = k.split("-")

        if (p.size == 3) {
            val y = p[0].toIntOrNull()
            val m = p[1].toIntOrNull()
            val d = p[2].toIntOrNull()

            if (y != null && m != null && d != null) {
                cal.set(y, m - 1, d, 12, 0, 0)
            }
        }
        return cal
    }

    private fun shift(k: String, delta: Int): String {
        val cal = parse(k)
        cal.add(Calendar.DAY_OF_MONTH, delta)
        return fmt(cal)
    }

    private fun nice(k: String): String {
        val cal = parse(k)

        return cal.get(Calendar.YEAR).toString() + "년 " +
            (cal.get(Calendar.MONTH) + 1) + "월 " +
            cal.get(Calendar.DAY_OF_MONTH) + "일 (" +
            dows[cal.get(Calendar.DAY_OF_WEEK) - 1] + ")"
    }

    private fun short(k: String): String {
        val cal = parse(k)

        return (cal.get(Calendar.MONTH) + 1).toString() +
            "월 " + cal.get(Calendar.DAY_OF_MONTH) +
            "일 (" +
            dows[cal.get(Calendar.DAY_OF_WEEK) - 1] + ")"
    }

    private fun tv(
        c: Context,
        s: String,
        size: Float,
        bold: Boolean,
        color: String
    ): TextView {
        val t = TextView(c)
        t.text = s
        t.textSize = size
        t.setTextColor(Color.parseColor(color))

        if (bold) {
            t.setTypeface(null, Typeface.BOLD)
        }

        return t
    }

    private fun shape(
        c: Context,
        color: String,
        r: Int
    ): GradientDrawable {
        val g = GradientDrawable()
        g.setColor(Color.parseColor(color))
        g.cornerRadius = dp(c, r).toFloat()
        return g
    }

    private fun save(
        c: Context,
        k: String,
        text: String
    ) {
        val e = prefs(c).edit()

        if (text.isBlank()) {
            e.remove(k)
        } else {
            e.putString(k, text)
        }

        e.apply()
    }

    // 기존 홈 화면과 동일한 배너 배경 적용
    private fun buildBanner(c: Context): FrameLayout {

        val box = FrameLayout(c)

        val bgd = BgImage.drawable(c)

        if (bgd != null) {

            val iv = ImageView(c)
            iv.scaleType = ImageView.ScaleType.CENTER_CROP
            iv.setImageDrawable(bgd)

            box.addView(
                iv,
                FrameLayout.LayoutParams(-1, -1)
            )

        } else {

            // 자동 오늘의 사진 기능
            DailyBanner.attach(box)
        }

        // 배너 제목
        val titleBox = LinearLayout(c)
        titleBox.orientation = LinearLayout.VERTICAL
        titleBox.setPadding(
            dp(c, 20),
            dp(c, 40),
            dp(c, 5),
            0
        )

        val title = tv(
            c,
            "DAYFLOW",
            28f,
            true,
            "#FFFFFF"
        )
        title.setShadowLayer(
            6f, 0f, 2f,
            Color.parseColor("#66000000")
        )

        val subtitle = tv(
            c,
            "오늘도, 더 나은 하루를 위해",
            14f,
            false,
            "#FFFFFF"
        )
        subtitle.setShadowLayer(
            6f, 0f, 2f,
            Color.parseColor("#66000000")
        )

        titleBox.addView(title)
        titleBox.addView(subtitle)

        box.addView(
            titleBox,
            FrameLayout.LayoutParams(-1, -2)
        )

        // 오른쪽 원형 날씨 표시
        val weatherCircle = LinearLayout(c)
        weatherCircle.orientation = LinearLayout.VERTICAL
        weatherCircle.gravity = Gravity.CENTER

        weatherCircle.background =
            GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#EFFFFFFF"))
                setStroke(
                    dp(c, 1),
                    Color.WHITE
                )
            }

        val city = tv(
            c,
            Weather.names[Weather.city(c)],
            12f,
            true,
            "#33485F"
        )
        city.gravity = Gravity.CENTER

        val weather = tv(
            c,
            "날씨 확인 중",
            12f,
            true,
            "#33485F"
        )
        weather.gravity = Gravity.CENTER

        val weatherSub = tv(
            c,
            "오늘의 날씨",
            9f,
            false,
            "#65778B"
        )
        weatherSub.gravity = Gravity.CENTER

        weatherCircle.addView(city)
        weatherCircle.addView(weather)
        weatherCircle.addView(weatherSub)

        val circleSize = dp(c, 88)

        val circleLp = FrameLayout.LayoutParams(
            circleSize,
            circleSize,
            Gravity.END or Gravity.CENTER_VERTICAL
        )

        circleLp.setMargins(
            0,
            0,
            dp(c, 18),
            0
        )

        box.addView(weatherCircle, circleLp)

        // 날씨 정보 표시 및 갱신
        fun updateWeather() {

            val now = Calendar.getInstance()

            city.text = Weather.names[Weather.city(c)]

            val result = Weather.label(
                c,
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH) + 1,
                now.get(Calendar.DAY_OF_MONTH)
            )

            weather.text =
                if (result.isBlank()) {
                    "예보 준비 중"
                } else {
                    result
                }
        }

        updateWeather()

        Weather.refresh(c) {
            weatherCircle.post {
                updateWeather()
            }
        }

        return box
    }

    private fun refresh(
        c: Context,
        sv: ScrollView
    ) {
        val p = sv.parent

        if (p is ViewGroup) {
            val k = p.indexOfChild(sv)
            p.removeViewAt(k)

            p.addView(
                build(c),
                k,
                FrameLayout.LayoutParams(-1, -1)
            )
        }
    }

    fun build(c: Context): ScrollView {

        val todayKey = fmt(Calendar.getInstance())

        if (key.isEmpty()) {
            key = todayKey
        }

        val sv = ScrollView(c)
        sv.setBackgroundColor(
            Color.parseColor("#F4F7FB")
        )

        val root = LinearLayout(c)
        root.orientation = LinearLayout.VERTICAL

        sv.addView(root)

        // =================================
        // 1. 상단 배너
        // =================================

        root.addView(
            buildBanner(c),
            LinearLayout.LayoutParams(
                -1,
                dp(c, 190)
            )
        )

        // =================================
        // 2. 다이어리 본문
        // =================================

        val content = LinearLayout(c)
        content.orientation = LinearLayout.VERTICAL

        content.setPadding(
            dp(c, 16),
            dp(c, 20),
            dp(c, 16),
            dp(c, 30)
        )

        root.addView(
            content,
            LinearLayout.LayoutParams(-1, -2)
        )

        content.addView(
            tv(
                c,
                "📖 나의 다이어리",
                25f,
                true,
                "#26364A"
            )
        )

        // =================================
        // 3. 일기 작성 영역
        // =================================

        val paper = LinearLayout(c)
        paper.orientation = LinearLayout.VERTICAL
        paper.background = shape(c, "#FFFEF5", 18)
        paper.elevation = dp(c, 4).toFloat()

        paper.setPadding(
            dp(c, 18),
            dp(c, 14),
            dp(c, 18),
            dp(c, 18)
        )

        val input = EditText(c)

        // 날짜 이동 줄
        val nav = LinearLayout(c)
        nav.orientation = LinearLayout.HORIZONTAL
        nav.gravity = Gravity.CENTER_VERTICAL

        val prev = tv(
            c,
            "‹",
            28f,
            true,
            "#4A6A91"
        )
        prev.setPadding(
            dp(c, 12), 0,
            dp(c, 12), 0
        )

        val next = tv(
            c,
            "›",
            28f,
            true,
            "#4A6A91"
        )
        next.setPadding(
            dp(c, 12), 0,
            dp(c, 12), 0
        )

        val dateLabel = tv(
            c,
            nice(key),
            17f,
            true,
            "#4A6A91"
        )
        dateLabel.gravity = Gravity.CENTER

        nav.addView(prev)

        nav.addView(
            dateLabel,
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        nav.addView(next)
        paper.addView(nav)

        prev.setOnClickListener {
            save(c, key, input.text.toString())
            key = shift(key, -1)
            refresh(c, sv)
        }

        next.setOnClickListener {
            save(c, key, input.text.toString())
            key = shift(key, 1)
            refresh(c, sv)
        }

        if (key != todayKey) {

            val back = tv(
                c,
                "오늘로 돌아가기",
                12f,
                true,
                "#2F7BE8"
            )

            back.gravity = Gravity.CENTER
            back.setPadding(0, dp(c, 6), 0, 0)

            back.setOnClickListener {
                save(c, key, input.text.toString())
                key = todayKey
                refresh(c, sv)
            }

            paper.addView(back)
        }

        val line = View(c)
        line.setBackgroundColor(
            Color.parseColor("#E8DFC8")
        )

        val lineLp = LinearLayout.LayoutParams(
            -1,
            dp(c, 1)
        )

        lineLp.setMargins(
            0,
            dp(c, 12),
            0,
            dp(c, 12)
        )

        paper.addView(line, lineLp)

        input.textSize = 16f
        input.setTextColor(Color.parseColor("#333333"))
        input.setHintTextColor(Color.parseColor("#AAAAAA"))

        input.hint =
            "이 날은 어땠나요?\n\n기억하고 싶은 일, 생각, 감정을 자유롭게 적어보세요."

        input.gravity =
            Gravity.TOP or Gravity.START

        input.minLines = 12

        input.setPadding(
            dp(c, 4),
            dp(c, 8),
            dp(c, 4),
            dp(c, 8)
        )

        input.background = null

        input.inputType =
            InputType.TYPE_CLASS_TEXT or
            InputType.TYPE_TEXT_FLAG_MULTI_LINE or
            InputType.TYPE_TEXT_FLAG_CAP_SENTENCES

        input.setText(
            prefs(c).getString(key, "") ?: ""
        )

        paper.addView(
            input,
            LinearLayout.LayoutParams(-1, -2)
        )

        val plp = LinearLayout.LayoutParams(
            -1,
            -2
        )

        plp.setMargins(
            0,
            dp(c, 14),
            0,
            dp(c, 14)
        )

        content.addView(paper, plp)

        // =================================
        // 4. 저장 버튼
        // =================================

        val saveBtn = Button(c)
        saveBtn.text = "일기 저장하기"

        Ui.style(c, saveBtn, 0)

        saveBtn.setOnClickListener {

            save(c, key, input.text.toString())

            val imm =
                c.getSystemService(
                    Context.INPUT_METHOD_SERVICE
                ) as InputMethodManager

            imm.hideSoftInputFromWindow(
                input.windowToken,
                0
            )

            Toast.makeText(
                c,
                nice(key) + " 일기를 저장했어요.",
                Toast.LENGTH_SHORT
            ).show()

            refresh(c, sv)
        }

        content.addView(
            saveBtn,
            LinearLayout.LayoutParams(
                -1,
                dp(c, 52)
            )
        )

        // =================================
        // 5. 지난 일기 목록
        // =================================

        val days = ArrayList<String>()

        for ((k, v) in prefs(c).all) {
            if (v is String && v.isNotBlank()) {
                days.add(k)
            }
        }

        days.sortDescending()

        val head = tv(
            c,
            "지난 일기 (" + days.size + ")",
            16f,
            true,
            "#26364A"
        )

        val hlp = LinearLayout.LayoutParams(
            -1,
            -2
        )

        hlp.setMargins(
            0,
            dp(c, 22),
            0,
            dp(c, 8)
        )

        content.addView(head, hlp)

        if (days.isEmpty()) {
            content.addView(
                tv(
                    c,
                    "아직 쓴 일기가 없어요.",
                    13f,
                    false,
                    "#8895A7"
                )
            )
        }

        for (k in days.take(60)) {

            val raw =
                prefs(c).getString(k, "") ?: ""

            val first =
                raw.trim().lines().firstOrNull() ?: ""

            val preview =
                if (first.length > 32) {
                    first.substring(0, 32) + "…"
                } else {
                    first
                }

            val row = LinearLayout(c)
            row.orientation = LinearLayout.VERTICAL

            row.background = shape(
                c,
                if (k == key) "#E4EEFC" else "#FFFFFF",
                14
            )

            row.elevation = dp(c, 2).toFloat()

            row.setPadding(
                dp(c, 14),
                dp(c, 10),
                dp(c, 14),
                dp(c, 10)
            )

            row.addView(
                tv(
                    c,
                    short(k),
                    13f,
                    true,
                    "#2F7BE8"
                )
            )

            row.addView(
                tv(
                    c,
                    preview,
                    14f,
                    false,
                    "#333333"
                )
            )

            row.setOnClickListener {
                save(c, key, input.text.toString())
                key = k
                refresh(c, sv)
            }

            val rlp = LinearLayout.LayoutParams(
                -1,
                -2
            )

            rlp.setMargins(
                0,
                dp(c, 4),
                0,
                dp(c, 4)
            )

            content.addView(row, rlp)
        }

        return sv
    }
}
