package com.nimith.echonote.data.remote.models

import com.squareup.moshi.Json

data class TranscriptionResponse(
    @param:Json(name = "text")
    val text: String
)