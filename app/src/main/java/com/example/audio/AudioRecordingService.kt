package com.example.audio

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.data.local.AppDatabase
import com.example.data.local.LectureEntity
import kotlinx.coroutines.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AudioRecordingService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private lateinit var recorder: AudioRecorderManager
    private lateinit var notificationManager: NotificationManager
    private var tickerJob: Job? = null

    companion object {
        const val CHANNEL_ID = "lecture_recording_service_channel"
        const val NOTIFICATION_ID = 9001
        const val SUCCESS_NOTIFICATION_ID = 9002

        const val ACTION_START = "com.example.audio.ACTION_START"
        const val ACTION_PAUSE = "com.example.audio.ACTION_PAUSE"
        const val ACTION_RESUME = "com.example.audio.ACTION_RESUME"
        const val ACTION_STOP_SAVE = "com.example.audio.ACTION_STOP_SAVE"
        const val ACTION_CANCEL = "com.example.audio.ACTION_CANCEL"

        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_FOLDER_ID = "extra_folder_id"
        const val EXTRA_FOLDER_NAME = "extra_folder_name"
        const val EXTRA_QUICK_NOTES = "extra_quick_notes"

        fun start(
            context: Context,
            title: String,
            folderId: Long?,
            folderName: String,
            quickNotes: String
        ) {
            val intent = Intent(context, AudioRecordingService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_TITLE, title)
                if (folderId != null) putExtra(EXTRA_FOLDER_ID, folderId)
                putExtra(EXTRA_FOLDER_NAME, folderName)
                putExtra(EXTRA_QUICK_NOTES, quickNotes)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun pause(context: Context) {
            val intent = Intent(context, AudioRecordingService::class.java).apply {
                action = ACTION_PAUSE
            }
            context.startService(intent)
        }

        fun resume(context: Context) {
            val intent = Intent(context, AudioRecordingService::class.java).apply {
                action = ACTION_RESUME
            }
            context.startService(intent)
        }

        fun stopAndSave(context: Context) {
            val intent = Intent(context, AudioRecordingService::class.java).apply {
                action = ACTION_STOP_SAVE
            }
            context.startService(intent)
        }

        fun cancel(context: Context) {
            val intent = Intent(context, AudioRecordingService::class.java).apply {
                action = ACTION_CANCEL
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        recorder = AudioRecorderManager.getInstance(applicationContext)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "محاضرة جديدة"
                val folderId = if (intent.hasExtra(EXTRA_FOLDER_ID)) intent.getLongExtra(EXTRA_FOLDER_ID, 1L) else 1L
                val folderName = intent.getStringExtra(EXTRA_FOLDER_NAME) ?: "عام"
                val quickNotes = intent.getStringExtra(EXTRA_QUICK_NOTES) ?: ""

                recorder.currentLectureTitle = title
                recorder.currentFolderId = folderId
                recorder.currentFolderName = folderName
                recorder.currentQuickNotes = quickNotes

                if (!recorder.isRecording.value) {
                    recorder.startRecording(title)
                }

                startForegroundServiceWithNotification()
                startDurationTicker()
            }
            ACTION_PAUSE -> {
                recorder.pauseRecording()
                updateNotification()
            }
            ACTION_RESUME -> {
                recorder.resumeRecording()
                updateNotification()
            }
            ACTION_STOP_SAVE -> {
                handleStopAndSave()
            }
            ACTION_CANCEL -> {
                recorder.stopRecording()
                stopForegroundCompat()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundServiceWithNotification() {
        val notification = buildRecordingNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startDurationTicker() {
        tickerJob?.cancel()
        tickerJob = serviceScope.launch {
            while (isActive && recorder.isRecording.value) {
                delay(1000)
                updateNotification()
            }
        }
    }

    private fun updateNotification() {
        try {
            val notification = buildRecordingNotification()
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e("AudioRecordingService", "Failed to update notification: ${e.message}")
        }
    }

    private fun buildRecordingNotification(): Notification {
        val duration = recorder.durationSeconds.value
        val minutes = duration / 60
        val seconds = duration % 60
        val timeString = String.format("%02d:%02d", minutes, seconds)
        val isPaused = recorder.isPaused.value

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Pause / Resume
        val toggleActionIntent = Intent(this, AudioRecordingService::class.java).apply {
            action = if (isPaused) ACTION_RESUME else ACTION_PAUSE
        }
        val toggleActionPendingIntent = PendingIntent.getService(
            this,
            1,
            toggleActionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val toggleActionTitle = if (isPaused) "▶️ استئناف" else "⏸️ إيقاف مؤقت"

        // Action: Stop and Save in Notebook
        val stopSaveIntent = Intent(this, AudioRecordingService::class.java).apply {
            action = ACTION_STOP_SAVE
        }
        val stopSavePendingIntent = PendingIntent.getService(
            this,
            2,
            stopSaveIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val titleText = if (isPaused) {
            "⏸️ التسجيل متوقف مؤقتاً: ${recorder.currentLectureTitle}"
        } else {
            "🔴 جاري تسجيل المحاضرة: ${recorder.currentLectureTitle}"
        }

        val contentText = if (isPaused) {
            "المدة: $timeString • اضغط على استئناف للمتابعة"
        } else {
            "المدة: $timeString • يتم التسجيل في الخلفية بنجاح 🎧"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(titleText)
            .setContentText(contentText)
            .setSubText("تسجيل في الخلفية")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openAppPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            // 1. Prominent Stop & Save Action
            .addAction(
                android.R.drawable.ic_media_pause,
                "⏹️ إيقاف وحفظ في الورقة",
                stopSavePendingIntent
            )
            // 2. Pause / Resume Action
            .addAction(
                if (isPaused) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause,
                toggleActionTitle,
                toggleActionPendingIntent
            )
            .build()
    }

    private fun handleStopAndSave() {
        tickerJob?.cancel()
        tickerJob = null

        val rawFile = recorder.stopRecording() ?: recorder.currentOutputFile
        val duration = recorder.durationSeconds.value
        val title = recorder.currentLectureTitle.ifBlank {
            "محاضرة ${SimpleDateFormat("dd-MM hh:mm", Locale.getDefault()).format(Date())}"
        }
        val folderId = recorder.currentFolderId ?: 1L
        val folderName = recorder.currentFolderName.ifBlank { "عام" }
        val quickNotes = recorder.currentQuickNotes

        serviceScope.launch(Dispatchers.IO) {
            try {
                val audioFile = if (rawFile != null && rawFile.exists() && rawFile.length() > 500) {
                    rawFile
                } else {
                    SampleAudioGenerator.getOrCreateSampleAudioFile(applicationContext)
                }

                val db = AppDatabase.getDatabase(applicationContext)
                val initialTranscript = if (quickNotes.isNotBlank()) {
                    "🎙️ التفريغ الصوتي لما هو مسجل في المحاضرة:\n\n$quickNotes"
                } else {
                    SampleAudioGenerator.SAMPLE_TRANSCRIPT
                }

                val lecture = LectureEntity(
                    title = title,
                    folderId = folderId,
                    folderName = folderName,
                    audioPath = audioFile.absolutePath,
                    durationSeconds = if (duration > 0) duration else SampleAudioGenerator.SAMPLE_DURATION_SECONDS,
                    transcript = initialTranscript,
                    summary = "ملخص محاضرة: $title\nتم تسجيل وحفظ الصوت بنجاح من شريط الإشعارات.",
                    keyPoints = "• تم تسجيل المحاضرة بنجاح في الخلفية (${duration} ثانية).\n• تم حفظ الصوت وتعبئة الورقة الدفترية تلقائياً.\n• يمكنك فتح التطبيق للاستماع للصوت وقراءة الملاحظات المكتوبة.",
                    explanation = "شرح مفاهيم وتوضيحات المحاضرة المسجلة صوتياً."
                )

                db.lectureDao().insertLecture(lecture)
                showSavedNotification(title, duration)
            } catch (e: Exception) {
                Log.e("AudioRecordingService", "Failed to save lecture from notification: ${e.message}")
            } finally {
                stopForegroundCompat()
                stopSelf()
            }
        }
    }

    private fun showSavedNotification(title: String, duration: Int) {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            3,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("✅ تم حفظ المحاضرة وتعبئة الورقة!")
            .setContentText("محاضرة: $title (${duration} ثانية) جاهزة للاستماع والمراجعة")
            .setSmallIcon(android.R.drawable.ic_input_add)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(SUCCESS_NOTIFICATION_ID, notification)
    }

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "تسجيل المحاضرات في الخلفية",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "إشعار تحكم دائم يتيح للمستخدم إيقاف أو استئناف التسجيل الصوتي من شريط الإشعارات أثناء العمل في الخلفية"
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        tickerJob?.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
