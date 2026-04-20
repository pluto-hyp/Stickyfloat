package ma.project.stickyfloat.services

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import ma.project.stickyfloat.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GeminiService {

    private const val API_KEY = BuildConfig.GEMINI_API_KEY
    private const val MODEL_NAME = "gemini-2.5-flash"

    private val model = GenerativeModel(
        modelName = MODEL_NAME,
        apiKey = API_KEY
    )

    suspend fun expandNote(noteContent: String): String = withContext(Dispatchers.IO) {
        try {
            val prompt = """
                You are a productivity assistant. The user has a note and wants you to expand it into a clear, actionable checklist.
                
                Note: "$noteContent"
                
                Rules:
                - Return ONLY the checklist, no introduction or explanation.
                - Each item starts with "☐ ".
                - 3 to 7 items maximum.
                - Items should be short and actionable (start with a verb).
                - Keep the same language as the note (Arabic, French, English, etc.).
            """.trimIndent()

            val response = model.generateContent(prompt)

            // Return the text or a fallback if something went wrong
            response.text?.trim() ?: "Could not generate checklist."

        } catch (e: Exception) {
            e.printStackTrace()
            "Error: ${e.localizedMessage}"
        }
    }
}