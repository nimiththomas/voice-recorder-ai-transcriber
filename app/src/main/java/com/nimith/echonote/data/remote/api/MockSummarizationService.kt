package com.nimith.echonote.data.remote.api

import com.nimith.echonote.data.remote.models.Content
import com.nimith.echonote.data.remote.models.OutputMessage
import com.nimith.echonote.data.remote.models.SummarizationRequest
import com.nimith.echonote.data.remote.models.SummarizationResponse
import kotlinx.coroutines.delay

class MockSummarizationService : SummarizationService {

    private val summaries = listOf(
        "The meeting covered Q3 financial results, showing a 15% increase in revenue. Key action items include finalizing the marketing budget and preparing for the upcoming product launch.",
        "A discussion about the new project timeline. The team agreed to a two-week extension for the design phase. The next milestone is the prototype presentation on the 5th of next month.",
        "Brainstorming session for the new app feature. Ideas included gamification, social sharing, and personalized user profiles. A follow-up meeting is scheduled to review the top three concepts.",
        "Client call notes: The client requested a new report on user engagement. They are happy with the current progress but would like to see more detailed analytics on a weekly basis.",
        "A podcast about the future of AI. The host and guest discussed the impact of large language models on software development and the ethical considerations of using AI in creative fields.",
        "Lecture on ancient history: The topic was the rise and fall of the Roman Empire. The professor highlighted the economic and political factors that contributed to its decline.",
        "A recipe for a chocolate cake. The ingredients are flour, sugar, cocoa powder, eggs, and butter. The instructions are to mix the dry ingredients, then add the wet ingredients, and bake at 350 degrees for 30 minutes.",
        "A travel blog about a trip to Japan. The author describes their visits to Tokyo, Kyoto, and Osaka. They recommend trying the local street food and visiting the temples in Kyoto.",
        "A summary of a book on productivity. The main takeaway is the importance of setting clear goals and breaking them down into smaller, manageable tasks. The book also suggests using the Pomodoro Technique to stay focused."
    )

    private var currentIndex = 0

    override suspend fun summarize(
        authorization: String,
        request: SummarizationRequest
    ): SummarizationResponse {
        delay(1000) // Simulate network delay
        val summary = summaries[currentIndex]
        currentIndex = (currentIndex + 1) % summaries.size
        return SummarizationResponse(
            output = listOf(
                OutputMessage(
                    content = listOf(
                        Content(
                            type = "output_text",
                            text = summary
                        )
                    )
                )
            )
        )
    }
}
