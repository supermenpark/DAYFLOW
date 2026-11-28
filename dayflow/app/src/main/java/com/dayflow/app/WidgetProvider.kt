package com.dayflow.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import java.util.Calendar

class WidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        for (id in ids) manager.updateAppWidget(id, build(context))
    }

    companion object {

        fun build(c: Context): RemoteViews {
            Fortune.load(c)
            val now = Calendar.getInstance()
            val y = now.get(Calendar.YEAR)
            val m = now.get(Calendar.MONTH) + 1
            val d = now.get(Calendar.DAY_OF_MONTH)
            val rv = RemoteViews(c.packageName, R.layout.widget_day)
            rv.setTextViewText(
                R.id.w_title,
                m.toString() + "월 " + d + "일 · 운세 " + Fortune.score(y, m, d, 0) + "점"
            )
            rv.setTextViewText(R.id.w_body, Extra.brief(c))
            val pi = PendingIntent.getActivity(
                c, 0, Intent(c, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            rv.setOnClickPendingIntent(R.id.w_root, pi)
            return rv
        }

        fun refresh(c: Context) {
            try {
                val mgr = AppWidgetManager.getInstance(c)
                val ids = mgr.getAppWidgetIds(ComponentName(c, WidgetProvider::class.java))
                for (id in ids) mgr.updateAppWidget(id, build(c))
            } catch (e: Exception) {
            }
        }
    }
}
