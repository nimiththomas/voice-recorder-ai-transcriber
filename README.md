# EchoNote - Voice Recorder AI Transcriber

EchoNote is an Android application that records voice notes, transcribes them into text, and provides a summary of the transcription using AI.

## Features

*   **Voice Recording:** Simple and intuitive voice recording.
*   **Transcription:** Converts recorded audio into text.
*   **AI Summarization:** Get key points from your transcriptions using OpenAI's GPT models.
*   **Clean Architecture:** Built with modern Android development practices.

## Getting Started

To build and run this project, you will need to provide your own OpenAI API key.

1.  Open the `local.properties` file in the root of the project. If it doesn't exist, create one.
2.  Add the following line to your `local.properties` file, replacing `"YOUR_OPENAI_API_KEY"` with your actual key:

    ```
    OPENAI_API_KEY="YOUR_OPENAI_API_KEY"
    ```

3.  Sync the project with Gradle.

## How to Build

1.  Clone the repository.
2.  Open the project in Android Studio.
3.  Follow the "Getting Started" steps to add your API key.
4.  Build and run the app on an emulator or a physical device.

## Technologies Used

*   Kotlin
*   Jetpack Compose
*   Hilt for Dependency Injection
*   Retrofit for network calls
*   Kotlin Coroutines & Flow
*  MVVM
