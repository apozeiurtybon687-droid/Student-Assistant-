package com.example.data.remote

import android.content.Context
import android.util.Base64
import android.util.Log
import com.example.data.local.ApiKeyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

data class LectureAnalysisResult(
    val transcript: String,
    val summary: String,
    val keyPoints: String,
    val explanation: String,
    val examQuestions: String,
    val detectedLanguage: String = "ar"
)

class GeminiService(context: Context? = null) {

    private val apiKeyManager = context?.let { ApiKeyManager(it) }

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun getActiveApiKey(): String {
        val userKey = apiKeyManager?.getApiKey()?.trim() ?: ""
        if (userKey.isNotEmpty() && userKey != "MY_GEMINI_API_KEY") {
            return userKey
        }
        val buildKey = com.example.BuildConfig.GEMINI_API_KEY.trim()
        if (buildKey.isNotEmpty() && buildKey != "MY_GEMINI_API_KEY") {
            return buildKey
        }
        return ""
    }

    fun hasValidApiKey(): Boolean {
        val key = getActiveApiKey()
        return key.isNotEmpty() && key != "MY_GEMINI_API_KEY"
    }

    /**
     * Transcribe, summarize, organize notes, and explain the lecture from recorded audio.
     */
    suspend fun analyzeLectureAudio(
        audioFile: File,
        customPromptHint: String = ""
    ): LectureAnalysisResult = withContext(Dispatchers.IO) {
        val audioBytes = audioFile.readBytes()
        if (audioBytes.isEmpty()) {
            throw IllegalArgumentException("الملف الصوتي فارغ، لم يتم تسجيل أي صوت.")
        }
        val base64Audio = Base64.encodeToString(audioBytes, Base64.NO_WRAP)
        val mimeType = if (audioFile.name.endsWith(".wav", ignoreCase = true)) "audio/wav" else "audio/mp4"

        val systemPrompt = """
            أنت مساعد أكاديمي ذكي متخصص في تحليل وتفريغ وتلخيص المحاضرات الجامعية.
            مهمتك:
            1. تفريغ صوت الأستاذ بدقة بالغة (Transcript) باللغة المنطوقة كاملة دون اختصار مخل.
            2. صياغة ملخص شامل وموجز لأهم محاور المحاضرة (Summary).
            3. تدوين ملاحظات دراسية منظمة ومنسقة في نقاط (Key Points / Organized Notes).
            4. تقديم شرح تفصيلي ومبسط للمفاهيم والمصطلحات الصعبة التي تطرق لها الأستاذ (In-depth Explanations).
            5. صياغة أهم الأسئلة الامتحانية المتوقعة والمصطلحات الأساسية (Exam Questions & Key Terms).
            
            يجب أن يكون الرد بتنسيق JSON حصرياً كالتالي:
            {
              "transcript": "...",
              "summary": "...",
              "key_points": "...",
              "explanation": "...",
              "exam_questions": "...",
              "detected_language": "ar"
            }
        """.trimIndent()

        val jsonRequest = JSONObject().apply {
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()

            val audioPart = JSONObject().apply {
                put("inlineData", JSONObject().apply {
                    put("mimeType", mimeType)
                    put("data", base64Audio)
                })
            }
            partsArray.put(audioPart)

            val textPart = JSONObject().apply {
                val promptText = if (customPromptHint.isNotBlank()) {
                    "قم بتفريغ وتحليل هذه المحاضرة الصوتية بالكامل وكتابة كلام الأستاذ نصياً مع مراعاة: $customPromptHint"
                } else {
                    "قم بتفريغ صوت الأستاذ كاملاً إلى نص مكتوب وتحليل وشرح وتلخيص هذه المحاضرة الجامعية بدقة بالغة."
                }
                put("text", promptText)
            }
            partsArray.put(textPart)

            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            put("contents", contentsArray)

            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", systemPrompt) })
                })
            })

            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.3)
            })
        }

        // Use valid models starting with gemini-3.5-transcribe
        val rawResponse = callGenerateContentWithFallback(
            listOf("gemini-3.5-transcribe", "gemini-3.5-flash", "gemini-2.5-flash", "gemini-flash-latest"),
            jsonRequest
        )
        parseAnalysisResponse(rawResponse)
    }

    /**
     * Transcribe user audio input using model gemini-3.5-transcribe
     */
    suspend fun transcribeAudioWithGemini(audioFile: File): String = withContext(Dispatchers.IO) {
        val audioBytes = audioFile.readBytes()
        if (audioBytes.isEmpty()) {
            throw IllegalArgumentException("الملف الصوتي فارغ، لم يتم تسجيل أي صوت.")
        }
        val base64Audio = Base64.encodeToString(audioBytes, Base64.NO_WRAP)
        val mimeType = if (audioFile.name.endsWith(".wav", ignoreCase = true)) "audio/wav" else "audio/mp4"

        val jsonRequest = JSONObject().apply {
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()

            val audioPart = JSONObject().apply {
                put("inlineData", JSONObject().apply {
                    put("mimeType", mimeType)
                    put("data", base64Audio)
                })
            }
            partsArray.put(audioPart)

            val textPart = JSONObject().apply {
                put("text", "قم بتفريغ هذا الصوت بدقة تامة إلى نص مكتوب كلمة بكلمة.")
            }
            partsArray.put(textPart)

            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            put("contents", contentsArray)

            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", "You are an expert audio transcription model. You transcribe spoken audio accurately and completely word-for-word.")
                    })
                })
            })
        }

        val models = listOf("gemini-3.5-transcribe", "gemini-3.5-flash", "gemini-2.5-flash", "gemini-flash-latest")
        val rawResponse = callGenerateContentWithFallback(models, jsonRequest)
        extractTextFromResponse(rawResponse)
    }

    /**
     * Analyze text lecture (if user enters text, notes, or edits transcript).
     */
    suspend fun analyzeLectureText(
        lectureContent: String
    ): LectureAnalysisResult = withContext(Dispatchers.IO) {
        val systemPrompt = """
            أنت مساعد أكاديمي ذكي متخصص في المحاضرات الجامعية.
            المطلوب: تحليل هذا النص للمحاضرة وتلخيصه وتنظيم ملاحظاته وشرحه ووضع أسئلة اختبار.
            أعد JSON حصرياً بالشكل:
            {
              "transcript": "...",
              "summary": "...",
              "key_points": "...",
              "explanation": "...",
              "exam_questions": "...",
              "detected_language": "ar"
            }
        """.trimIndent()

        val jsonRequest = JSONObject().apply {
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()
            partsArray.put(JSONObject().apply { put("text", lectureContent) })
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            put("contents", contentsArray)

            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", systemPrompt) })
                })
            })

            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.3)
            })
        }

        val rawResponse = callGenerateContentWithFallback(
            listOf("gemini-2.5-flash", "gemini-flash-latest", "gemini-3.5-flash"),
            jsonRequest
        )
        parseAnalysisResponse(rawResponse)
    }

    /**
     * Translate text accurately to target language.
     */
    suspend fun translateText(
        text: String,
        targetLanguageName: String
    ): String = withContext(Dispatchers.IO) {
        val systemPrompt = "You are a professional academic translator. Translate the provided lecture text accurately into $targetLanguageName. Maintain academic terminology, clear structure, and flawless grammar."

        val jsonRequest = JSONObject().apply {
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()
            partsArray.put(JSONObject().apply {
                put("text", "ترجم النص الأكاديمي التالي بدقة إلى ($targetLanguageName):\n\n$text")
            })
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            put("contents", contentsArray)

            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", systemPrompt) })
                })
            })
        }

        val rawResponse = callGenerateContentWithFallback(
            listOf("gemini-2.5-flash", "gemini-flash-latest", "gemini-3.5-flash"),
            jsonRequest
        )
        extractTextFromResponse(rawResponse)
    }

    /**
     * Multi-turn chat with conversation history and role system instruction.
     */
    suspend fun sendChatMessage(
        modelName: String,
        conversationHistory: List<Pair<String, String>>,
        systemInstructionRole: String
    ): String = withContext(Dispatchers.IO) {
        val jsonRequest = JSONObject().apply {
            val contentsArray = JSONArray()
            for ((role, content) in conversationHistory) {
                val turnObj = JSONObject()
                turnObj.put("role", if (role == "model" || role == "assistant") "model" else "user")
                val partsArray = JSONArray()
                partsArray.put(JSONObject().apply { put("text", content) })
                turnObj.put("parts", partsArray)
                contentsArray.put(turnObj)
            }
            put("contents", contentsArray)

            if (systemInstructionRole.isNotBlank()) {
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", systemInstructionRole) })
                    })
                })
            }

            put("generationConfig", JSONObject().apply {
                put("temperature", 0.7)
            })
        }

        val cleanModel = modelName.removePrefix("models/").let {
            if (it == "gemini-3.8-flash") "gemini-3.5-flash" else it
        }
        val fallbackModels = listOf(
            cleanModel,
            "gemini-3.5-flash",
            "gemini-3.1-pro-preview",
            "gemini-3.1-flash-lite",
            "gemini-2.5-flash"
        ).distinct()
        val rawResponse = callGenerateContentWithFallback(fallbackModels, jsonRequest)
        extractTextFromResponse(rawResponse)
    }

    /**
     * Text to Speech using gemini-2.5-flash-preview-tts
     */
    suspend fun generateSpeech(
        textToSpeak: String,
        voiceName: String = "Puck"
    ): ByteArray? = withContext(Dispatchers.IO) {
        val jsonRequest = JSONObject().apply {
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()
            partsArray.put(JSONObject().apply { put("text", textToSpeak) })
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            put("contents", contentsArray)

            put("generationConfig", JSONObject().apply {
                put("responseModalities", JSONArray().apply {
                    put("AUDIO")
                })
                put("speechConfig", JSONObject().apply {
                    put("voiceConfig", JSONObject().apply {
                        put("prebuiltVoiceConfig", JSONObject().apply {
                            put("voiceName", voiceName)
                        })
                    })
                })
            })
        }

        val ttsModels = listOf("gemini-2.5-flash-preview-tts")
        for (m in ttsModels) {
            try {
                val rawResponse = callGenerateContent(m, jsonRequest)
                val json = JSONObject(rawResponse)
                val candidates = json.optJSONArray("candidates") ?: continue
                val firstCandidate = candidates.optJSONObject(0) ?: continue
                val content = firstCandidate.optJSONObject("content") ?: continue
                val parts = content.optJSONArray("parts") ?: continue
                for (i in 0 until parts.length()) {
                    val part = parts.optJSONObject(i) ?: continue
                    val inlineData = part.optJSONObject("inlineData")
                    if (inlineData != null) {
                        val data = inlineData.optString("data", "")
                        if (data.isNotEmpty()) {
                            return@withContext Base64.decode(data, Base64.DEFAULT)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("GeminiService", "TTS model $m failed: ${e.message}")
            }
        }
        null
    }

    /**
     * Live Voice API conversational turn
     */
    suspend fun liveVoiceInteraction(
        userAudioBytes: ByteArray?,
        userText: String?,
        systemInstruction: String = "أنت مدرس ومساعد دراسي ذكي تتحدث مباشرة مع الطالب بالصوت لمساعدته في فهم المحاضرات وحل مشاكله الأكاديمية باختصار وبراعة."
    ): Pair<String, ByteArray?> = withContext(Dispatchers.IO) {
        val jsonRequest = JSONObject().apply {
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()

            if (userAudioBytes != null && userAudioBytes.isNotEmpty()) {
                val base64Audio = Base64.encodeToString(userAudioBytes, Base64.NO_WRAP)
                partsArray.put(JSONObject().apply {
                    put("inlineData", JSONObject().apply {
                        put("mimeType", "audio/mp4")
                        put("data", base64Audio)
                    })
                })
            }

            if (!userText.isNullOrBlank()) {
                partsArray.put(JSONObject().apply { put("text", userText) })
            }

            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            put("contents", contentsArray)

            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", systemInstruction) })
                })
            })

            put("generationConfig", JSONObject().apply {
                put("responseModalities", JSONArray().apply {
                    put("TEXT")
                    put("AUDIO")
                })
                put("speechConfig", JSONObject().apply {
                    put("voiceConfig", JSONObject().apply {
                        put("prebuiltVoiceConfig", JSONObject().apply {
                            put("voiceName", "Kore")
                        })
                    })
                })
            })
        }

        val liveModels = listOf("gemini-2.5-flash-native-audio-preview-12-2025", "gemini-2.5-flash", "gemini-3.5-flash")
        for (m in liveModels) {
            try {
                val rawResponse = callGenerateContent(m, jsonRequest)
                val json = JSONObject(rawResponse)
                val candidates = json.optJSONArray("candidates") ?: continue
                val firstCandidate = candidates.optJSONObject(0) ?: continue
                val content = firstCandidate.optJSONObject("content") ?: continue
                val parts = content.optJSONArray("parts") ?: continue

                var replyText = ""
                var audioData: ByteArray? = null

                for (i in 0 until parts.length()) {
                    val part = parts.optJSONObject(i) ?: continue
                    if (part.has("text")) {
                        replyText += part.optString("text")
                    }
                    if (part.has("inlineData")) {
                        val inlineData = part.getJSONObject("inlineData")
                        val data = inlineData.optString("data", "")
                        if (data.isNotEmpty()) {
                            audioData = Base64.decode(data, Base64.DEFAULT)
                        }
                    }
                }
                if (replyText.isNotEmpty() || audioData != null) {
                    return@withContext Pair(replyText.ifEmpty { "تم استلام الصوت بنجاح." }, audioData)
                }
            } catch (e: Exception) {
                Log.w("GeminiService", "Live model $m error: ${e.message}")
            }
        }

        // Final text fallback
        Pair("عذراً، حدث خطأ أثناء الاتصال بنموذج الصوت المباشر. تأكد من إعداد مفتاح API بشكل صحيح.", null)
    }

    private fun callGenerateContentWithFallback(models: List<String>, requestPayload: JSONObject): String {
        var lastException: Exception? = null
        for (m in models) {
            try {
                return callGenerateContent(m, requestPayload)
            } catch (e: Exception) {
                lastException = e
                Log.w("GeminiService", "Model $m failed: ${e.message}, trying next fallback...")
                // If it's a 403 authorization error, don't keep trying fallback models as all will fail for the same bad key
                if (e.message?.contains("403") == true) {
                    throw e
                }
            }
        }
        throw lastException ?: IllegalStateException("فشلت جميع محاولات الاتصال بالنموذج.")
    }

    private fun callGenerateContent(model: String, requestPayload: JSONObject): String {
        val key = getActiveApiKey()
        if (key.isEmpty() || key == "MY_GEMINI_API_KEY") {
            throw IllegalStateException("مفتاح Gemini API غير معين! يرجى إدخال مفتاح API الخاص بك من أيقونة المفتاح 🔑 أعلى الشاشة.")
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$key"
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = requestPayload.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            val errorMsg = try {
                val errJson = JSONObject(responseBody)
                errJson.optJSONObject("error")?.optString("message") ?: response.message
            } catch (_: Exception) {
                response.message
            }

            if (response.code == 403) {
                throw IllegalStateException("خطأ 403 (تصريح غير صالح): مفتاح Gemini API غير صالح أو لم يتم تفعيله أو مقيد بنطاق مختلف.\nالرسالة: $errorMsg\nيرجى تعديل مفتاح API من أيقونة 🔑 بأعلى الشاشة.")
            } else if (response.code == 404) {
                throw IllegalStateException("خطأ 404: النموذج $model غير متاح لهذا المفتاح ($errorMsg).")
            } else if (response.code == 429) {
                throw IllegalStateException("خطأ 429: تم تجاوز الحد المسموح من الطلبات مؤقتاً، يرجى المحاولة بعد قليل.")
            }

            throw IllegalStateException("فشل الاتصال بـ Gemini ($model): $errorMsg (كود ${response.code})")
        }

        return responseBody
    }

    private fun extractTextFromResponse(rawJson: String): String {
        return try {
            val json = JSONObject(rawJson)
            val candidates = json.optJSONArray("candidates") ?: return "لم يتم استلام رد."
            val firstCandidate = candidates.optJSONObject(0) ?: return "لم يتم استلام رد."
            val content = firstCandidate.optJSONObject("content") ?: return "لم يتم استلام رد."
            val parts = content.optJSONArray("parts") ?: return "لم يتم استلام رد."
            val sb = java.lang.StringBuilder()
            for (i in 0 until parts.length()) {
                val part = parts.optJSONObject(i) ?: continue
                if (part.has("text")) {
                    sb.append(part.getString("text"))
                }
            }
            sb.toString().ifEmpty { "لم يتوفر نص في الرد." }
        } catch (e: Exception) {
            "خطأ في معالجة الرد: ${e.message}"
        }
    }

    private fun parseAnalysisResponse(rawJson: String): LectureAnalysisResult {
        return try {
            val rawText = extractTextFromResponse(rawJson)
            val cleaned = rawText
                .replace(Regex("^```json\\s*", RegexOption.MULTILINE), "")
                .replace(Regex("^```\\s*", RegexOption.MULTILINE), "")
                .trim()

            val json = JSONObject(cleaned)
            LectureAnalysisResult(
                transcript = json.optString("transcript", "تم تفريغ المحاضرة بنجاح."),
                summary = json.optString("summary", "ملخص المحاضرة جاهز."),
                keyPoints = json.optString("key_points", "ملاحظات المحاضرة."),
                explanation = json.optString("explanation", "شرح تفصيلي لكلام الأستاذ."),
                examQuestions = json.optString("exam_questions", "أسئلة مقترحة للاختبار."),
                detectedLanguage = json.optString("detected_language", "ar")
            )
        } catch (e: Exception) {
            Log.e("GeminiService", "JSON parsing fallback: ${e.message}", e)
            val fallbackText = extractTextFromResponse(rawJson)
            LectureAnalysisResult(
                transcript = fallbackText,
                summary = "ملخص المحاضرة:\n" + fallbackText.take(300) + "...",
                keyPoints = "• ملاحظات المحاضرة الرئيسية\n• توضيحات الأستاذ",
                explanation = "شرح مفاهيم المحاضرة مستخلص من التسجيل الصوتي.",
                examQuestions = "1. ما هي المفاهيم الأساسية التي تم شرحها في المحاضرة؟",
                detectedLanguage = "ar"
            )
        }
    }
}
