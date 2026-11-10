package com.dayflow.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.util.Calendar

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val now = Calendar.getInstance()
        val c = Calendar.getInstance()
        c.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH), 12, 0, 0)

        // 오늘부터 400일 앞까지 저장된 일정을 찾아 알림을 다시 예약
        for (n in 0..400) {
            val y = c.get(Calendar.YEAR)
            val m = c.get(Calendar.MONTH) + 1
            val d = c.get(Calendar.DAY_OF_MONTH)
            val events = Scheduler.loadEvents(context, y, m, d)
            for (e in events) {
                val p = e.split("|")
                if (p.size < 4) continue
                val h = p[0].toIntOrNull() ?: continue
                val mi = p[1].toIntOrNull() ?: continue
                for (cd in p[3].split(",")) {
                    val off = cd.toIntOrNull() ?: continue
                    Scheduler.schedule(context, y, m, d, h, mi, off, p[2])
                }
            }
            c.add(Calendar.DAY_OF_MONTH, 1)
        }
    }
}
