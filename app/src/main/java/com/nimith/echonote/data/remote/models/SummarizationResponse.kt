package com.nimith.echonote.data.remote.models

data class SummarizationResponse(
    val output: List<OutputMessage>
) {
    fun getSummaryText(): String? {
        return output.firstOrNull()
            ?.content?.firstOrNull { it.type == "output_text" }
            ?.text
    }
}

data class OutputMessage(
    val content: List<Content>
)

data class Content(
    val type: String,
    val text: String?
)
