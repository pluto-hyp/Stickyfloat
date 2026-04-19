package ma.project.stickyfloat.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object AnthropicService {

    private const val API_KEY = "YOUR_ANTHROPIC_API_KEY_HERE"
    private const val API_URL = "https://api.anthropic.com/v1/messages"
    private const val MODEL = "claude-haiku-4-5-20251001" // fast + cheap, perfect for this

    suspend fun expandNote(noteContent: String): String = withContext(Dispatchers.IO) {
        val url = URL(API_URL)
        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("x-api-key", API_KEY)
            connection.setRequestProperty("anthropic-version", "2023-06-01")
            connection.doOutput = true
            connection.connectTimeout = 15000
            connection.readTimeout = 30000

            val prompt = """
                You are a productivity assistant. The user has a note and wants you to expand it into a clear, actionable checklist.
                
                Note: "$noteContent"
                
                Rules:
                - Return ONLY the checklist, no introduction or explanation
                - Each item starts with "☐ " 
                - 3 to 7 items maximum
                - Items should be short and actionable (start with a verb)
                - Keep the same language as the note (Arabic, French, English, etc.)
                
                Example output:
                ☐ Research available options online
                ☐ Compare prices and features
                ☐ Make a final decision
                ☐ Place the order
            """.trimIndent()

            val body = JSONObject().apply {
                put("model", MODEL)
                put("max_tokens", 400)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                })
            }

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(body.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            val responseText = if (responseCode == 200) {
                connection.inputStream.bufferedReader().readText()
            } else {
                val error = connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
                throw Exception("API error $responseCode: $error")
            }

            val json = JSONObject(responseText)
            val content = json
                .getJSONArray("content")
                .getJSONObject(0)
                .getString("text")
                .trim()

            content

        } finally {
            connection.disconnect()
        }
    }
}