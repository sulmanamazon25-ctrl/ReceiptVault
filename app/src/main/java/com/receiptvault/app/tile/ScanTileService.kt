package com.receiptvault.app.tile

import android.content.Intent
import android.os.Build
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.receiptvault.app.ScanEntryActivity

@RequiresApi(Build.VERSION_CODES.N)
class ScanTileService : TileService() {
    override fun onClick() {
        val intent = Intent(this, ScanEntryActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivityAndCollapse(intent)
    }

    override fun onStartListening() {
        qsTile?.label = getString(com.receiptvault.app.R.string.tile_scan_label)
        qsTile?.state = android.service.quicksettings.Tile.STATE_ACTIVE
        qsTile?.updateTile()
    }
}
