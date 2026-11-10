package com.dayflow.app

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object Weather {

    private fun prefs(c: Context) =
        c.getSharedPreferences("dayflow_weather", Context.MODE_PRIVATE)

    private fun key(y: Int, m: Int, d: Int): String {
        return String.format("%04d-%02d-%02d", y, m, d)
    }

    private fun name(code: Int): String {
        if (code == 0) return "맑음"
        if (code <= 3) return "구름"
        if (code == 45 || code == 48) return "안개"
        if (code in 51..67) return "비"
        if (code in 71..77) return "눈"
        if (code in 80..82) return "소나기"
        if (code >= 95) return "뇌우"
        return "흐림"
    }

    private fun icon(code: Int): String {
        if (code == 0) return "☀"
        if (code <= 3) return "⛅"
        if (code == 45 || code == 48) return "🌫"
        if (code in 51..67 || code in 80..82) return "☔"
        if (code in 71..77) return "❄"
        if (code >= 95) return "⚡"
        return "☁"
    }

    private fun line(c: Context, y: Int, m: Int, d: Int): List<String>? {
        val raw = prefs(c).getString("data", "") ?: ""
        val k = key(y, m, d)
        for (l in raw.split("\n")) {
            if (l.startsWith(k + "|")) return l.split("|")
        }
        return null
    }

    fun label(c: Context, y: Int, m: Int, d: Int): String {
        val p = line(c, y, m, d) ?: return " "
        val code = p[1].toIntOrNull() ?: return " "
        return icon(code) + p[2] + "°"
    }

    fun detail(c: Context, y: Int, m: Int, d: Int): String {
        val p = line(c, y, m, d) ?: return "예보 없음 (오늘부터 16일 이내만 제공)"
        val code = p[1].toIntOrNull() ?: 0
        return name(code) + "  최고 " + p[2] + "° / 최저 " + p[3] + "°  (부산)"
    }

    fun refresh(c: Context, done: () -> Unit) {
        val last = prefs(c).getLong("time", 0L)
        if (System.currentTimeMillis() - last < 30 * 60 * 1000L) return
        Thread {
            try {
                val u = "https://api.open-meteo.com/v1/forecast?latitude=35.18&longitude=129.08" +
                    "&daily=weather_code,temperature_2m_max,temperature_2m_min" +
                    "&timezone=Asia%2FSeoul&forecast_days=16"
                val con = URL(u).openConnection() as HttpURLConnection
                con.connectTimeout = 8000
                con.readTimeout = 8000
                val text = con.inputStream.bufferedReader().readText()
                con.disconnect()
                val daily = JSONObject(text).getJSONObject("daily")
                val t = daily.getJSONArray("time")
                val w = daily.getJSONArray("weather_code")
                val mx = daily.getJSONArray("temperature_2m_max")
                val mn = daily.getJSONArray("temperature_2m_min")
                val sb = StringBuilder()
                for (i in 0 until t.length()) {
                    sb.append(t.getString(i)).append("|").append(w.getInt(i)).append("|")
                    sb.append(Math.round(mx.getDouble(i))).append("|")
                    sb.append(Math.round(mn.getDouble(i))).append("\n")
                }
                prefs(c).edit().putString("data", sb.toString())
                    .putLong("time", System.currentTimeMillis()).apply()
                done()
            } catch (e: Exception) {
            }
        }.start()
    }
}
