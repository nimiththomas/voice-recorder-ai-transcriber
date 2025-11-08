package com.nimith.echonote.presentation.common

import android.content.Context
import android.os.StatFs

object StorageUtils {
    private const val FIFTY_MB = 50 * 1024 * 1024
    fun isStorageAvailable(context: Context): Boolean {
        val stat = StatFs(context.filesDir.absolutePath)
        val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
        return availableBytes > FIFTY_MB
    }
}
