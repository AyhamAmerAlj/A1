package com.ayham.player

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import android.graphics.Color
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class PlayerActivity : Activity() {
    private var player: ExoPlayer? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
        }
        val title = TextView(this).apply {
            text = intent.getStringExtra("title") ?: "Ayham Player"
            textSize = 20f; setTextColor(Color.WHITE); gravity = Gravity.CENTER_VERTICAL
            setPadding(18, 8, 18, 8)
        }
        val view = PlayerView(this).apply { useController = true; setShowSubtitleButton(true) }
        root.addView(title, LinearLayout.LayoutParams(-1, 56))
        root.addView(view, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        player = ExoPlayer.Builder(this).build().also {
            view.player = it
            val url = intent.getStringExtra("url") ?: return@also
            it.setMediaItem(MediaItem.fromUri(url))
            it.prepare()
            it.playWhenReady = true
        }
    }
    override fun onStop() {
        super.onStop()
        player?.release()
        player = null
    }
}
