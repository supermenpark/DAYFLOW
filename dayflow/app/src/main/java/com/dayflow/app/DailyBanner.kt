package com.dayflow.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar

object DailyBanner {

    private val tops = arrayOf("#7EC8F5", "#9AD0F0", "#F6B98A", "#A8C8F0", "#8FD3C7", "#C3B1F0", "#F2A7B8")
    private val bottoms = arrayOf("#CDE9FA", "#E3F2FD", "#FBE3CF", "#DCE8FA", "#D4F1EA", "#E8E0FA", "#FBDDE4")

    private fun today(): String {
        val c = Calendar.getInstance()
        return String.format("%04d-%02d-%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
    }

    private fun file(c: Context): File {
        return File(c.filesDir, "daily_banner.jpg")
    }

    private fun prefs(c: Context) = c.getSharedPreferences("dayflow_banner", Context.MODE_PRIVATE)

    private fun gradient(): GradientDrawable {
        val k = Calendar.getInstance().get(Calendar.DAY_OF_YEAR) % tops.size
        return GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(Color.parseColor(tops[k]), Color.parseColor(bottoms[k]))
        )
    }

    private fun load(c: Context): Bitmap? {
        val f = file(c)
        if (!f.exists()) return null
        return try {
            BitmapFactory.decodeFile(f.absolutePath)
        } catch (e: Exception) {
            null
        }
    }

    private fun download(c: Context, day: String): Boolean {
        return try {
            val u = "https://picsum.photos/seed/dayflow-" + day + "/1080/600"
            val con = URL(u).openConnection() as HttpURLConnection
            con.connectTimeout = 10000
            con.readTimeout = 15000
            con.instanceFollowRedirects = true
            val bytes = con.inputStream.readBytes()
            con.disconnect()
            if (bytes.size < 2000) return false
            val out = FileOutputStream(file(c))
            out.write(bytes)
            out.close()
            prefs(c).edit().putString("date", day).apply()
            true
        } catch (e: Exception) {
            false
        }
    }

    // 배너 FrameLayout 맨 뒤에 오늘의 사진을 깔아요
    fun attach(box: FrameLayout) {
        val c = box.context
        val iv = ImageView(c)
        iv.scaleType = ImageView.ScaleType.CENTER_CROP
        iv.setImageDrawable(gradient())
        box.addView(iv, 0, FrameLayout.LayoutParams(-1, -1))

        val scrim = View(c)
        scrim.background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(Color.parseColor("#00000000"), Color.parseColor("#55000000"))
        )
        box.addView(scrim, 1, FrameLayout.LayoutParams(-1, -1))

        val day = today()
        val saved = prefs(c).getString("date", "") ?: ""
        val old = load(c)
        if (old != null) iv.setImageBitmap(old)
        if (saved == day && old != null) return

        Thread {
            if (download(c, day)) {
                val bmp = load(c)
                if (bmp != null) iv.post { iv.setImageBitmap(bmp) }
            }
        }.start()
    }
}
