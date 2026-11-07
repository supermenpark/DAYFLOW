package com.dayflow.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object Scheduler {

    private fun prefs(c: Context) =
        c.getSharedPreferences("dayflow_cal", Context.MODE_PRIVATE)

    fun memoKey(y: Int, m: Int, d: Int): String {
        return "m_" + y + "_" + m + "_" + d
    }

    private fun evKey(y: Int, m: Int, d: Int): String {
        return "e_" + y + "_" + m + "_" + d
    }

    fun getMemo(c: Context, y: Int, m: Int, d: Int): String {
        return prefs(c).getString(memoKey(y, m, d), "") ?: ""
    }

    fun setMemo(c: Context, y: Int, m: Int, d: Int, text: String) {
        val e = prefs(c).edit()
        if (text.isEmpty()) {
            e.remove(memoKey(y, m, d))
        } else {
            e.putString(memoKey(y, m, d), text)
        }
        e.apply()
    }

    fun loadEvents(c: Context, y: Int, m: Int, d: Int): MutableList<String> {
        val raw = prefs(c).getString(evKey(y, m, d), "") ?: ""
        val list = ArrayList<String>()
        if (raw.isNotEmpty()) {
            for (line in raw.split("\n")) {
                if (line.isNotEmpty()) list.add(line)
            }
        }
        return list
    }

    fun saveEvents(c: Context, y: Int, m: Int, d: Int, list: List<String>) {
        val e = prefs(c).edit()
        if (list.isEmpty()) {
            e.remove(evKey(y, m, d))
        } else {
            e.putString(evKey(y, m, d), list.joinToString("\n"))
        }
        e.apply()
    }

    private fun alarmId(y: Int, m: Int, d: Int, h: Int, mi: Int, off: Int): Int {
        val a = ((y % 100) * 10000 + m * 100 + d).toLong()
        val v = (a * 10000L + h * 100 + mi) * 3L + off
        return (v % 2000000000L).toInt()
    }

    private fun pending(c: Context, id: Int, intent: Intent): PendingIntent {
        return PendingIntent.getBroadcast(
            c, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun schedule(c: Context, y: Int, m: Int, d: Int, h: Int, mi: Int, off: Int, title: String) {
        val cal = Calendar.getInstance()
        cal.set(y, m - 1, d, h, mi, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.add(Calendar.DAY_OF_MONTH, -off)
        if (cal.timeInMillis <= System.currentTimeMillis()) return

        val label = if (off == 0) "오늘" else if (off == 1) "내일" else "모레"
        val id = alarmId(y, m, d, h, mi, off)

        val intent = Intent(c, AlarmReceiver::class.java)
        intent.putExtra("title", "DAYFLOW 일정")
        intent.putExtra("text", label + " " + h + "시 " + mi + "분 - " + title)
        intent.putExtra("id", id)

        val am = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        var exact = true
        if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) exact = false
        if (exact) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pending(c, id, intent))
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pending(c, id, intent))
        }
    }

    fun cancel(c: Context, y: Int, m: Int, d: Int, h: Int, mi: Int, off: Int) {
        val id = alarmId(y, m, d, h, mi, off)
        val intent = Intent(c, AlarmReceiver::class.java)
        val am = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pending(c, id, intent))
    }
}
