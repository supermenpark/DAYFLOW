package com.dayflow.app

import android.app.Activity
import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView

class MainActivity : Activity() {

    private val items = ArrayList<String>()
    private lateinit var adapter: ArrayAdapter<String>

    private fun save() {
        getSharedPreferences("dayflow", Context.MODE_PRIVATE)
            .edit()
            .putString("tasks", items.joinToString("\n"))
            .apply()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val saved = getSharedPreferences("dayflow", Context.MODE_PRIVATE)
            .getString("tasks", "") ?: ""
        if (saved.isNotEmpty()) items.addAll(saved.split("\n"))

        val p = (16 * resources.displayMetrics.density).toInt()

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(p, p * 2, p, p)

        val title = TextView(this)
        title.text = "DAYFLOW"
        title.textSize = 28f
        title.setTypeface(null, Typeface.BOLD)
        root.addView(title)

        val hint = TextView(this)
        hint.text = "탭: 완료 체크 / 길게 누르기: 삭제"
        hint.textSize = 13f
        root.addView(hint)

        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL

        val input = EditText(this)
        input.hint = "오늘 할 일"
        input.setSingleLine(true)
        row.addView(
            input,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        )

        val addButton = Button(this)
        addButton.text = "추가"
        row.addView(addButton)
        root.addView(row)

        val list = ListView(this)
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, items)
        list.adapter = adapter
        root.addView(
            list,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        )

        addButton.setOnClickListener {
            val t = input.text.toString().trim()
            if (t.isNotEmpty()) {
                items.add("[ ] " + t)
                input.setText("")
                adapter.notifyDataSetChanged()
                save()
            }
        }

        list.setOnItemClickListener { _, _, pos, _ ->
            val s = items[pos]
            items[pos] = if (s.startsWith("[x] ")) {
                "[ ] " + s.substring(4)
            } else {
                "[x] " + s.substring(4)
            }
            adapter.notifyDataSetChanged()
            save()
        }

        list.setOnItemLongClickListener { _, _, pos, _ ->
            items.removeAt(pos)
            adapter.notifyDataSetChanged()
            save()
            true
        }

        setContentView(root)
    }
}
