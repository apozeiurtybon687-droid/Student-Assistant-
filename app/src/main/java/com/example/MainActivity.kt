package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.LectureEntity
import com.example.ui.screens.GeminiChatScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LectureDetailScreen
import com.example.ui.screens.LiveVoiceScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ChatViewModel
import com.example.ui.viewmodel.LectureViewModel
import com.example.ui.viewmodel.LiveVoiceViewModel

enum class Screen {
    HOME,
    LECTURE_DETAIL,
    GEMINI_CHAT,
    LIVE_VOICE
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                SmartLectureApp()
            }
        }
    }
}

@Composable
fun SmartLectureApp(
    lectureViewModel: LectureViewModel = viewModel(),
    chatViewModel: ChatViewModel = viewModel(),
    liveVoiceViewModel: LiveVoiceViewModel = viewModel()
) {
    var currentScreen by remember { mutableStateOf(Screen.HOME) }
    val selectedLecture by lectureViewModel.selectedLecture.collectAsState()

    // Handle back button smoothly
    BackHandler(enabled = currentScreen != Screen.HOME) {
        when (currentScreen) {
            Screen.LECTURE_DETAIL -> {
                lectureViewModel.clearSelectedLecture()
                currentScreen = Screen.HOME
            }
            Screen.GEMINI_CHAT -> {
                currentScreen = if (selectedLecture != null) Screen.LECTURE_DETAIL else Screen.HOME
            }
            Screen.LIVE_VOICE -> {
                liveVoiceViewModel.stopAudioPlayback()
                currentScreen = Screen.HOME
            }
            Screen.HOME -> { /* Exit */ }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when (currentScreen) {
            Screen.HOME -> {
                HomeScreen(
                    viewModel = lectureViewModel,
                    onOpenLecture = { lecture ->
                        lectureViewModel.selectLecture(lecture)
                        currentScreen = Screen.LECTURE_DETAIL
                    },
                    onOpenLiveVoice = {
                        currentScreen = Screen.LIVE_VOICE
                    },
                    onOpenChat = {
                        chatViewModel.setLectureContext(null)
                        currentScreen = Screen.GEMINI_CHAT
                    }
                )
            }

            Screen.LECTURE_DETAIL -> {
                selectedLecture?.let { lecture ->
                    LectureDetailScreen(
                        lecture = lecture,
                        viewModel = lectureViewModel,
                        onBack = {
                            lectureViewModel.clearSelectedLecture()
                            currentScreen = Screen.HOME
                        },
                        onOpenChat = { targetLecture ->
                            chatViewModel.setLectureContext(targetLecture)
                            currentScreen = Screen.GEMINI_CHAT
                        }
                    )
                } ?: run {
                    currentScreen = Screen.HOME
                }
            }

            Screen.GEMINI_CHAT -> {
                GeminiChatScreen(
                    chatViewModel = chatViewModel,
                    onBack = {
                        currentScreen = if (selectedLecture != null) Screen.LECTURE_DETAIL else Screen.HOME
                    }
                )
            }

            Screen.LIVE_VOICE -> {
                LiveVoiceScreen(
                    liveVoiceViewModel = liveVoiceViewModel,
                    onBack = {
                        liveVoiceViewModel.stopAudioPlayback()
                        currentScreen = Screen.HOME
                    }
                )
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme { Greeting("Android") }
}
