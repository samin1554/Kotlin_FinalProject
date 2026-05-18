package com.example.student_pomodoro

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class TimerTileService : TileService() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        updateTile()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onClick() {
        super.onClick()
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("AUTO_START_TIMER", true)
        }
        startActivityAndCollapse(intent)
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    private fun updateTile() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            qsTile?.let { tile ->
                tile.state = Tile.STATE_INACTIVE
                tile.label = "Start Pomodoro"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = "Student Pomodoro"
                }
                tile.updateTile()
            }
        }
    }
}
