package com.dayflow.app

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

object MusicView {

    private val accents = arrayOf("#8A55D8", "#2F7BE8", "#E0526F", "#2FA866", "#E8772E")

    private fun dp(c: Context, v: Int): Int {
        return (v * c.resources.displayMetrics.density).toInt()
    }

    private fun tv(c: Context, s: String, size: Float, bold: Boolean, color: String): TextView {
        val t = TextView(c)
        t.text = s
        t.textSize = size
        t.setTextColor(Color.parseColor(color))
        if (bold) t.setTypeface(null, Typeface.BOLD)
        return t
    }

    private fun shape(c: Context, color: String, r: Int): GradientDrawable {
        val g = GradientDrawable()
        g.setColor(Color.parseColor(color))
        g.cornerRadius = dp(c, r).toFloat()
        return g
    }

    private fun prefs(c: Context) =
        c.getSharedPreferences("dayflow_music", Context.MODE_PRIVATE)

    private fun load(c: Context): MutableList<String> {
        val raw = prefs(c).getString("list", "") ?: ""
        val l = ArrayList<String>()
        for (line in raw.split("\n")) {
            if (line.isNotEmpty()) l.add(line)
        }
        return l
    }

    private fun save(c: Context, l: List<String>) {
        prefs(c).edit().putString("list", l.joinToString("\n")).apply()
    }

    private fun clean(s: String): String {
        return s.trim().replace("|", " ").replace("\n", " ")
    }

    private fun open(c: Context, title: String, link: String) {
        val u = if (link.isNotEmpty()) {
            if (link.startsWith("http")) link else "https://" + link
        } else {
            "https://www.youtube.com/results?search_query=" + Uri.encode(title)
        }
        try {
            val i = Intent(Intent.ACTION_VIEW, Uri.parse(u))
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            c.startActivity(i)
        } catch (e: Exception) {
            Toast.makeText(c, "열 수 있는 앱이 없어요", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openMusicApp(c: Context) {
        try {
            val i = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MUSIC)
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            c.startActivity(i)
        } catch (e: Exception) {
            Toast.makeText(c, "기본 음악 앱을 찾지 못했어요", Toast.LENGTH_SHORT).show()
        }
    }

    private fun refresh(c: Context, sv: ScrollView) {
        val p = sv.parent
        if (p is ViewGroup) {
            val k = p.indexOfChild(sv)
            p.removeViewAt(k)
            p.addView(build(c), k, FrameLayout.LayoutParams(-1, -1))
        }
    }

    private fun pill(c: Context, text: String, top: String, bottom: String): TextView {
        val b = tv(c, text, 15f, true, "#FFFFFF")
        b.gravity = Gravity.CENTER
        val g = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(Color.parseColor(top), Color.parseColor(bottom))
        )
        g.cornerRadius = dp(c, 26).toFloat()
        b.background = g
        b.elevation = dp(c, 4).toFloat()
        b.setPadding(0, dp(c, 14), 0, dp(c, 14))
        return b
    }

    private fun input(c: Context, hint: String): EditText {
        val e = EditText(c)
        e.hint = hint
        e.setSingleLine(true)
        e.inputType = InputType.TYPE_CLASS_TEXT
        e.background = shape(c, "#F1F5FB", 12)
        e.setPadding(dp(c, 14), dp(c, 12), dp(c, 14), dp(c, 12))
        return e
    }

    private fun item(c: Context, sv: ScrollView, idx: Int, line: String, list: MutableList<String>): View {
        val p = line.split("|")
        val title = p[0]
        val link = if (p.size > 1) p[1] else ""
        val accent = accents[idx % accents.size]

        val outer = LinearLayout(c)
        outer.orientation = LinearLayout.HORIZONTAL
        outer.gravity = Gravity.CENTER_VERTICAL
        outer.background = shape(c, "#FFFFFF", 16)
        outer.elevation = dp(c, 3).toFloat()

        val bar = View(c)
        bar.background = shape(c, accent, 4)
        outer.addView(bar, LinearLayout.LayoutParams(dp(c, 5), dp(c, 64)))

        val col = LinearLayout(c)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(dp(c, 14), dp(c, 12), dp(c, 8), dp(c, 12))
        col.addView(tv(c, "🎵 " + title, 16f, true, "#222222"))
        val sub = if (link.isEmpty()) "유튜브에서 검색해서 재생" else "저장된 링크로 열기"
        col.addView(tv(c, sub, 12f, false, "#8895A7"))
        outer.addView(col, LinearLayout.LayoutParams(0, -2, 1f))

        val play = tv(c, "▶", 16f, true, "#FFFFFF")
        play.gravity = Gravity.CENTER
        play.background = shape(c, accent, 20)
        val plp = LinearLayout.LayoutParams(dp(c, 40), dp(c, 40))
        plp.setMargins(0, 0, dp(c, 8), 0)
        outer.addView(play, plp)

        val del = tv(c, "✕", 15f, true, "#8895A7")
        del.gravity = Gravity.CENTER
        del.background = shape(c, "#EEF2F7", 20)
        val dlp = LinearLayout.LayoutParams(dp(c, 34), dp(c, 34))
        dlp.setMargins(0, 0, dp(c, 10), 0)
        outer.addView(del, dlp)

        val go = View.OnClickListener { open(c, title, link) }
        outer.setOnClickListener(go)
        play.setOnClickListener(go)
        del.setOnClickListener {
            list.removeAt(idx)
            save(c, list)
            refresh(c, sv)
        }
        return outer
    }

    fun build(c: Context): ScrollView {
        val sv = ScrollView(c)
        sv.setBackgroundColor(Color.parseColor("#F6F9FD"))
        val root = LinearLayout(c)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(dp(c, 16), dp(c, 40), dp(c, 16), dp(c, 24))
        sv.addView(root)

        root.addView(tv(c, "🎵 음악", 30f, true, "#0F2A55"))

        val row = LinearLayout(c)
        row.orientation = LinearLayout.HORIZONTAL
        val b1 = pill(c, "내 뮤직 열기", "#B58CF0", "#7C4DDB")
        b1.setOnClickListener { openMusicApp(c) }
        val l1 = LinearLayout.LayoutParams(0, -2, 1f)
        l1.setMargins(0, 0, dp(c, 6), 0)
        row.addView(b1, l1)
        val b2 = pill(c, "유튜브 뮤직", "#FF8A9B", "#E0526F")
        b2.setOnClickListener {
            open(c, "", "https://music.youtube.com")
        }
        val l2 = LinearLayout.LayoutParams(0, -2, 1f)
        l2.setMargins(dp(c, 6), 0, 0, 0)
        row.addView(b2, l2)
        val rlp = LinearLayout.LayoutParams(-1, -2)
        rlp.setMargins(0, dp(c, 16), 0, dp(c, 14))
        root.addView(row, rlp)

        val form = LinearLayout(c)
        form.orientation = LinearLayout.VERTICAL
        form.background = shape(c, "#FFFFFF", 18)
        form.elevation = dp(c, 3).toFloat()
        form.setPadding(dp(c, 14), dp(c, 14), dp(c, 14), dp(c, 14))
        form.addView(tv(c, "듣고 싶은 음악 저장", 15f, true, "#222222"))
        val tIn = input(c, "곡 제목 / 가수")
        val lIn = input(c, "링크 (선택, 없으면 유튜브 검색)")
        val ilp = LinearLayout.LayoutParams(-1, -2)
        ilp.setMargins(0, dp(c, 8), 0, 0)
        form.addView(tIn, ilp)
        form.addView(lIn, ilp)
        val add = pill(c, "＋  저장", "#5CB8FF", "#2F86EA")
        val alp = LinearLayout.LayoutParams(-1, -2)
        alp.setMargins(0, dp(c, 12), 0, 0)
        form.addView(add, alp)
        val flp = LinearLayout.LayoutParams(-1, -2)
        flp.setMargins(0, 0, 0, dp(c, 14))
        root.addView(form, flp)

        val list = load(c)
        add.setOnClickListener {
            val t = clean(tIn.text.toString())
            val l = clean(lIn.text.toString())
            if (t.isEmpty()) {
                Toast.makeText(c, "곡 제목을 입력하세요", Toast.LENGTH_SHORT).show()
            } else {
                list.add(0, t + "|" + l)
                save(c, list)
                refresh(c, sv)
            }
        }

        if (list.isEmpty()) {
            val none = tv(c, "저장된 음악이 없어요.\n듣고 싶은 곡을 적어 보세요.", 14f, false, "#8895A7")
            none.gravity = Gravity.CENTER
            none.setPadding(0, dp(c, 24), 0, 0)
            root.addView(none)
        }
        for (i in list.indices) {
            val lp = LinearLayout.LayoutParams(-1, -2)
            lp.setMargins(0, dp(c, 6), 0, dp(c, 6))
            root.addView(item(c, sv, i, list[i], list), lp)
        }
        return sv
    }
}
