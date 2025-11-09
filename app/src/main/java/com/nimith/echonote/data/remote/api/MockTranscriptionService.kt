package com.nimith.echonote.data.remote.api

import com.nimith.echonote.data.remote.models.TranscriptionResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody

class MockTranscriptionService : TranscriptionService {
    private val transcripts = listOf(
        "The quick brown fox jumps over the lazy dog.",
        "She sells seashells by the seashore.",
        "How much wood would a woodchuck chuck if a woodchuck could chuck wood?",
        "Peter Piper picked a peck of pickled peppers.",
        "I have a dream that one day this nation will rise up and live out the true meaning of its creed: 'We hold these truths to be self-evident, that all men are created equal.'",
        "The only thing we have to fear is fear itself.",
        "Ask not what your country can do for you – ask what you can do for your country.",
        "That's one small step for a man, one giant leap for mankind.",
        "In the beginning God created the heavens and the earth.",
        "It was the best of times, it was the worst of times."
    )

    private var currentIndex = 0

    override suspend fun transcribe(
        authorization: String,
        file: MultipartBody.Part,
        model: RequestBody
    ): TranscriptionResponse {
        val transcript = transcripts[currentIndex]
        currentIndex = (currentIndex + 1) % transcripts.size
        return TranscriptionResponse(transcript)
    }

}
