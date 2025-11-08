package com.nimith.echonote.presentation.common

import android.content.Context
import com.nimith.echonote.presentation.common.Constants.RECORDING_DIRECTORY
import com.nimith.echonote.presentation.common.Constants.RECORDING_FILE_PREFIX
import com.nimith.echonote.presentation.common.Constants.RECORDING_FILE_SUFFIX
import java.io.File

object FileUtils {
    fun getRecordingFilePath(context: Context, recordingId: Long, chunkIndex: Int): String {
        val dir = File(context.filesDir, RECORDING_DIRECTORY)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val fileName = "$RECORDING_FILE_PREFIX${recordingId}_${chunkIndex}$RECORDING_FILE_SUFFIX"
        return File(dir, fileName).absolutePath
    }
}
