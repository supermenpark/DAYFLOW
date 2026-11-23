package com.dayflow.app

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.text.InputType
import android.widget.EditText
import android.widget.Toast
import org.json.JSONObject
import java.util.Calendar

object Backup {

    private val files = arrayOf("dayflow_cal", "dayflow_diary", "dayflow_music", "dayflow_theme")

    private fun make(c: Context): String {
        val root = JSONObject()
        root.put("app", "DAYFLOW")
        root.put("version", 1)
        for (f in files) {
            val o = JSONObject()
            for ((k, v) in c.getSharedPreferences(f, Context.MODE_PRIVATE).all) {
                if (v is String) o.put(k, v)
                else if (v is Int) o.put(k, v)
            }
            root.put(f, o)
        }
        return root.toString()
    }

    private fun restoreAlarms(c: Context) {
        val cal = Calendar.getInstance()
        cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 12, 0, 0)
        for (n in 0..400) {
            val y = cal.get(Calendar.YEAR)
            val m = cal.get(Calendar.MONTH) + 1
            val d = cal.get(Calendar.DAY_OF_MONTH)
            for (e in Scheduler.loadEvents(c, y, m, d)) {
                val p = e.split("|")
                if (p.size < 4) continue
                val h = p[0].toIntOrNull() ?: continue
                val mi = p[1].toIntOrNull() ?: continue
                for (cd in p[3].split(",")) {
                    val off = cd.toIntOrNull() ?: continue
                    Scheduler.schedule(c, y, m, d, h, mi, off, p[2])
                }
            }
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
    }

    // 내보내기: 클립보드에 복사하고 공유 창을 열어요
    fun export(a: Activity) {
        val text = make(a)
        val cm = a.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("DAYFLOW", text))
        Toast.makeText(a, "백업이 복사됐어요. 메모장이나 메신저에 붙여 넣어 보관하세요.", Toast.LENGTH_LONG).show()
        try {
            val i = Intent(Intent.ACTION_SEND)
            i.type = "text/plain"
            i.putExtra(Intent.EXTRA_TEXT, text)
            a.startActivity(Intent.createChooser(i, "DAYFLOW 백업 보내기"))
        } catch (e: Exception) {
        }
    }

    // 불러오기: 붙여 넣은 백업 텍스트로 복원해요 (같은 날짜 데이터는 덮어쓰고, 나머지는 유지)
    fun import(a: Activity, done: () -> Unit) {
        val e = EditText(a)
        e.hint = "백업 텍스트를 여기에 붙여 넣으세요"
        e.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        e.minLines = 4
        try {
            val cm = a.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = cm.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val t = clip.getItemAt(0).coerceToText(a).toString()
                if (t.contains("\"app\":\"DAYFLOW\"")) e.setText(t)
            }
        } catch (ex: Exception) {
        }
        AlertDialog.Builder(a)
            .setTitle("백업 불러오기")
            .setView(e)
            .setPositiveButton("복원") { _, _ ->
                if (restore(a, e.text.toString())) {
                    Toast.makeText(a, "복원했어요", Toast.LENGTH_SHORT).show()
                    done()
                } else {
                    Toast.makeText(a, "백업 형식이 맞지 않아요", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun restore(c: Context, text: String): Boolean {
        return try {
            val root = JSONObject(text.trim())
            if (root.optString("app") != "DAYFLOW") return false
            for (f in files) {
                if (!root.has(f)) continue
                val o = root.getJSONObject(f)
                val ed = c.getSharedPreferences(f, Context.MODE_PRIVATE).edit()
                val it = o.keys()
                while (it.hasNext()) {
                    val k = it.next()
                    val v = o.get(k)
                    if (v is String) ed.putString(k, v)
                    else if (v is Int) ed.putInt(k, v)
                }
                ed.apply()
            }
            restoreAlarms(c)
            true
        } catch (ex: Exception) {
            false
        }
    }
}
