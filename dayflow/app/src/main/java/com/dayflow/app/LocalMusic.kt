
package com.dayflow.app

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri

object LocalMusic {

    private var player: MediaPlayer? = null
    private var prepared = false

    @Synchronized
    fun play(context: Context, rawUri: String): Boolean {
        return try {
            stop()

            val uri = Uri.parse(rawUri)
            val newPlayer = MediaPlayer()
            player = newPlayer
            prepared = false

            newPlayer.setOnPreparedListener { mp ->
                if (player === mp) {
                    prepared = true
                    mp.start()
                }
            }

            newPlayer.setOnCompletionListener {
                // 곡 재생 완료
            }

            newPlayer.setOnErrorListener { mp, _, _ ->
                if (player === mp) {
                    prepared = false
                    mp.release()
                    player = null
                }
                true
            }

            newPlayer.setDataSource(context.applicationContext, uri)
            newPlayer.prepareAsync()

            true
        } catch (e: Exception) {
            stop()
            false
        }
    }

    @Synchronized
    fun toggle(): Boolean {
        val p = player ?: return false

        if (!prepared) return false

        return try {
            if (p.isPlaying) {
                p.pause()
            } else {
                p.start()
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    @Synchronized
    fun isPlaying(): Boolean {
        return try {
            prepared && player?.isPlaying == true
        } catch (e: Exception) {
            false
        }
    }

    @Synchronized
    fun stop() {
        val old = player
        player = null
        prepared = false

        try {
            old?.setOnPreparedListener(null)
            old?.setOnCompletionListener(null)
            old?.setOnErrorListener(null)
            old?.stop()
        } catch (_: Exception) {
        }

        try {
            old?.release()
        } catch (_: Exception) {
        }
    }
}
