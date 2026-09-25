package com.example.audio

import android.content.Context
import android.os.Build
import android.speech.tts.TextToSpeech
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.Locale
import kotlin.math.sin

object SampleAudioGenerator {

    const val SAMPLE_LECTURE_TITLE = "محاضرة تجريبية: أساسيات الذكاء الاصطناعي وتعلم الآلة"
    const val SAMPLE_FOLDER_NAME = "علوم الحاسب والبرمجة"
    const val SAMPLE_DURATION_SECONDS = 24

    const val SAMPLE_TRANSCRIPT = """🎙️ التفريغ الصوتي لما هو مسجل في المحاضرة:

مرحباً بكم يا أعزائي الطلاب والطالبات في محاضرة اليوم لمادة علوم الحاسب.
سنتناول اليوم مفهوماً جوهرياً في هندسة البرمجيات: وهو الفرق بين البرمجة التقليدية وخوارزميات تعلم الآلة والذكاء الاصطناعي الحديث.
في البرمجة التقليدية (Traditional Programming): يقوم المبرمج بكتابة القواعد الصريحة والمنطق خطوة بخطوة، وعند إدخال البيانات نحصل على النتائج.
أما في نماذج تعلم الآلة (Machine Learning): فإننا نغذي النموذج بالبيانات الضخمة والنتائج السابقة، ليقوم النموذج باستنتاج القواعد والأنماط الرياضية ذاتياً.
وينقسم تعلم الآلة إلى ثلاثة أنماط رئيسية:
1. التعلم الخاضع للإشراف (Supervised Learning): يعتمد على بيانات مصنفة مسبقاً.
2. التعلم غير الخاضع للإشراف (Unsupervised Learning): يكتشف المجموعات والأنماط تلقائياً دون تسميات.
3. التعلم التعزيزي (Reinforcement Learning): يعتمد على التجربة والمكافأة.
ركزوا جيداً على هذه النقاط لأنها ستكون موضوع سؤال في الاختبار الفصلي القادم."""

    const val SAMPLE_SUMMARY = """ملخص المحاضرة المسجلة صوتياً:
• المحور الأول: المقارنة بين البرمجة التقليدية وتعلم الآلة؛ فالأولى تعتمد على قواعد مبرمجة يدوياً بينما الثانية تستنتج القواعد من البيانات.
• المحور الثاني: أقسام تعلم الآلة الثلاثة (الموجه، غير الموجه، والتعزيزي).
• المحور الثالث: تطبيقات نماذج التعلم في استخراج الأنماط ومعالجة اللغات الطبيعية."""

    const val SAMPLE_KEY_POINTS = """• البرمجة التقليدية: بيانات + قواعد = مخرجات.
• تعلم الآلة: بيانات + مخرجات = قواعد وأنماط مستنتجة.
• التعلم الخاضع للإشراف (Supervised Learning): بيانات موسومة (Labeled Data).
• التعلم غير الخاضع للإشراف (Unsupervised Learning): تصنيف وتجميع ذاتي (Clustering).
• التعلم التعزيزي (Reinforcement Learning): محاكاة التجربة ونظام النقاط/المكافأة."""

    const val SAMPLE_EXPLANATION = """شرح توضيحي وتبسيط للمفاهيم:
تخيل طفلاً صغيراً يتعلم التمييز بين الفواكه:
إذا أريته تفاحة وقلت له 'هذه تفاحة' فهذا تعلم خاضع للإشراف.
إذا تركته في حديقة بمفرده ليجمع الثمار المتشابهة في اللون والشكل دون أن تخبره بأسمائها، فهذا تعلم غير خاضع للإشراف.
أما إذا حاول لمس موقد ساخن فشعر بالحرارة وتراجع، فهذا تعلم تعزيزي قائم على التجربة والخطأ. هذه الركائز الثلاث هي عصب ثورة الذكاء الاصطناعي الحالية."""

    const val SAMPLE_EXAM_QUESTIONS = """س1: قارن بين البرمجة الكلاسيكية وخوارزميات تعلم الآلة من حيث المدخلات والتعامل مع القواعد.
س2: ما الفرق بين التعلم الموجه (Supervised) والتعلم غير الموجه (Unsupervised) مع إعطاء مثال لكل منهما؟
س3: اذكر كيف تستفيد الشبكات العصبية الحديثة من التعلم غير الخاضع للإشراف في معالجة الصوت والنصوص."""

    /**
     * Returns a valid, playable WAV audio file representing the recorded lecture.
     * Generates a 24-second acoustic PCM audio file with speech cadences and tones.
     */
    fun getOrCreateSampleAudioFile(context: Context): File {
        val audioDir = File(context.filesDir, "sample_audio").apply { if (!exists()) mkdirs() }
        val audioFile = File(audioDir, "default_recorded_lecture.wav")

        if (audioFile.exists() && audioFile.length() > 1000) {
            return audioFile
        }

        try {
            generateSpeechWav(audioFile, durationSeconds = SAMPLE_DURATION_SECONDS)
        } catch (e: Exception) {
            Log.e("SampleAudioGenerator", "Failed to generate speech wav: ${e.message}", e)
        }
        return audioFile
    }

    private fun generateSpeechWav(outputFile: File, durationSeconds: Int = 24) {
        val sampleRate = 16000
        val channels = 1
        val bitsPerSample = 16
        val totalSamples = sampleRate * durationSeconds
        val totalAudioLen = totalSamples * (bitsPerSample / 8) * channels
        val totalDataLen = totalAudioLen + 36

        FileOutputStream(outputFile).use { fos ->
            writeWavHeader(
                fos,
                totalAudioLen.toLong(),
                totalDataLen.toLong(),
                sampleRate.toLong(),
                channels,
                (sampleRate * channels * bitsPerSample / 8).toLong()
            )

            // Generate modulated voice-like frequencies with speech pauses
            val buffer = ByteArray(1024)
            var bufferIndex = 0

            for (i in 0 until totalSamples) {
                val t = i.toDouble() / sampleRate

                // Speech rhythm envelope: active words for 0.4s-0.7s, brief pause for 0.15s
                val wordCycle = (t * 2.5) % 1.0
                val isPause = wordCycle > 0.75
                val sentencePause = (t % 4.0) > 3.4

                val amplitudeMultiplier = if (isPause || sentencePause) 0.05 else 0.85

                // Fundamental lecture voice pitch around 175 Hz with speech formants (450Hz, 1200Hz, 2100Hz)
                val pitch = 175.0 + 15.0 * sin(2.0 * Math.PI * 0.8 * t)
                val fundamental = sin(2.0 * Math.PI * pitch * t)
                val formant1 = 0.5 * sin(2.0 * Math.PI * (pitch * 2.8) * t)
                val formant2 = 0.25 * sin(2.0 * Math.PI * (pitch * 5.6) * t)
                val formant3 = 0.12 * sin(2.0 * Math.PI * (pitch * 9.2) * t)

                val mixed = (fundamental + formant1 + formant2 + formant3) * 0.45 * amplitudeMultiplier
                val sampleValue = (mixed * 32767.0).toInt().coerceIn(-32768, 32767).toShort()

                // Little-endian 16-bit PCM
                buffer[bufferIndex++] = (sampleValue.toInt() and 0xFF).toByte()
                buffer[bufferIndex++] = ((sampleValue.toInt() shr 8) and 0xFF).toByte()

                if (bufferIndex >= buffer.size) {
                    fos.write(buffer, 0, bufferIndex)
                    bufferIndex = 0
                }
            }

            if (bufferIndex > 0) {
                fos.write(buffer, 0, bufferIndex)
            }
        }
    }

    private fun writeWavHeader(
        out: OutputStream,
        totalAudioLen: Long,
        totalDataLen: Long,
        longSampleRate: Long,
        channels: Int,
        byteRate: Long
    ) {
        val header = ByteArray(44)
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16 // 16 for PCM
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // PCM format
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (longSampleRate and 0xff).toByte()
        header[25] = ((longSampleRate shr 8) and 0xff).toByte()
        header[26] = ((longSampleRate shr 16) and 0xff).toByte()
        header[27] = ((longSampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = (channels * 2).toByte() // block align
        header[33] = 0
        header[34] = 16 // bits per sample
        header[35] = 0
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (totalAudioLen and 0xff).toByte()
        header[41] = ((totalAudioLen shr 8) and 0xff).toByte()
        header[42] = ((totalAudioLen shr 16) and 0xff).toByte()
        header[43] = ((totalAudioLen shr 24) and 0xff).toByte()
        out.write(header, 0, 44)
    }
}
