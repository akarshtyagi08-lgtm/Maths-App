package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiMathService {
    private const val TAG = "GeminiMathService"
    private const val MODEL = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * General purpose suspend function to call Gemini with text and an optional image.
     */
    suspend fun callGemini(
        prompt: String,
        systemInstruction: String? = null,
        imageBase64: String? = null,
        mimeType: String = "image/jpeg",
        forceJsonResponse: Boolean = false
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "API_KEY_MISSING"
        }

        try {
            val requestJson = JSONObject()
            
            // Build contents array
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()

            // Prompt text
            val textPart = JSONObject()
            textPart.put("text", prompt)
            partsArray.put(textPart)

            // Inline Image Data (if present)
            if (!imageBase64.isNullOrEmpty()) {
                val imagePart = JSONObject()
                val inlineDataObj = JSONObject()
                inlineDataObj.put("mimeType", mimeType)
                inlineDataObj.put("data", imageBase64)
                imagePart.put("inlineData", inlineDataObj)
                partsArray.put(imagePart)
            }

            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            requestJson.put("contents", contentsArray)

            // Generation Config
            val generationConfig = JSONObject()
            if (forceJsonResponse) {
                generationConfig.put("responseMimeType", "application/json")
            }
            generationConfig.put("temperature", 0.2) // Low temperature for factual math logic
            requestJson.put("generationConfig", generationConfig)

            // System Instructions
            if (!systemInstruction.isNullOrEmpty()) {
                val systemInstructionObj = JSONObject()
                val systemPartsArray = JSONArray()
                val systemTextPart = JSONObject()
                systemTextPart.put("text", systemInstruction)
                systemPartsArray.put(systemTextPart)
                systemInstructionObj.put("parts", systemPartsArray)
                requestJson.put("systemInstruction", systemInstructionObj)
            }

            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val body = requestJson.toString().toRequestBody(mediaType)
            val url = "$BASE_URL?key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val respBody = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                Log.e(TAG, "API Error: HTTP ${response.code} - $respBody")
                return@withContext "Error: HTTP ${response.code}\n$respBody"
            }

            val jsonResponse = JSONObject(respBody)
            val candidates = jsonResponse.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.optJSONObject("content")
                if (content != null) {
                    val parts = content.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).optString("text", "No text part found")
                    }
                }
            }
            return@withContext "Error: No text content returned from the AI model."
        } catch (e: Exception) {
            Log.e(TAG, "Exception during callGemini: ", e)
            return@withContext "Exception: ${e.message}"
        }
    }

    /**
     * Solves a single math question. Supports text and optional image base64.
     */
    suspend fun solveMathQuestion(
        questionText: String,
        imageBase64: String? = null
    ): String {
        val prompt = if (imageBase64 != null) {
            "Carefully analyze the mathematical question in this image. Solve it step-by-step with complete, clear explanations for every mathematical step. Conclude with the final answer boxed or in bold."
        } else {
            "Solve this math problem step-by-step with clear, mathematical logical explanations:\n\n$questionText\n\nConclude with the final answer bolded."
        }

        val systemInstruction = "You are a warm, encouraging, and elite AI Math Coach. Your goal is to guide students through a thorough step-by-step understanding of the problem. Use Markdown layout, bullet points, and neat spacing to present equations cleanly. Avoid using raw HTML."

        return callGemini(prompt = prompt, systemInstruction = systemInstruction, imageBase64 = imageBase64)
    }

    /**
     * Generates a fully structured question paper in JSON format.
     */
    suspend fun generateQuestionPaper(
        grade: String,
        topic: String,
        difficulty: String,
        count: Int
    ): String {
        val prompt = """
            Generate an exam/practice paper with exactly $count questions.
            Grade Level: $grade
            Topic: $topic
            Difficulty: $difficulty
            
            Each question must have exactly 4 multiple-choice options.
            Ensure that one option matches the 'correctAnswer' field exactly.
            Include a detailed 'stepByStepSolution' explaining how to reach the correct answer.
            
            Return ONLY a valid JSON array of question objects without any markdown formatting wrappers (no ```json or similar).
            
            Expected JSON Schema:
            [
              {
                "question": "A clear mathematical question",
                "options": ["Option A", "Option B", "Option C", "Option D"],
                "correctAnswer": "The exact string of the correct option",
                "stepByStepSolution": "Explain how to solve this question step-by-step."
              }
            ]
        """.trimIndent()

        val systemInstruction = "You are an expert math curriculum creator. You must output a 100% syntactically correct JSON array following the requested schema. Do not output any notes, introductory text, or markdown code blocks. Just start with [ and end with ]."

        return callGemini(prompt = prompt, systemInstruction = systemInstruction, forceJsonResponse = true)
    }

    /**
     * Grades a set of answers submitted by a student.
     */
    suspend fun gradeExamSubmission(
        title: String,
        questionsJson: String,
        answersJson: String
    ): String {
        val prompt = """
            Analyze a student's completed math exam and provide a highly detailed step-by-step performance grading report in a clean, human-readable layout.
            
            Exam Title: $title
            
            Questions and Solutions:
            $questionsJson
            
            Student's Answers:
            $answersJson
            
            Provide:
            1. An overall summary score and score percentage.
            2. For EACH question:
               - State the original question.
               - Show the correct answer.
               - Show what the student selected.
               - State if they were Correct or Incorrect.
               - Provide a brief, supportive, step-by-step explanation of the correct solution, especially emphasizing the logical path if the student made an error.
            3. A encouraging concluding summary or personalized study tip tailored to their performance.
            
            Format the response in a beautiful markdown structure using headers, bold text, and neat spacing.
        """.trimIndent()

        val systemInstruction = "You are a supportive, high-fidelity AI Math grader. Be objective with mathematical correctness, but highly constructive and motivating in your comments. Present calculations in neat layouts."

        return callGemini(prompt = prompt, systemInstruction = systemInstruction)
    }
}
