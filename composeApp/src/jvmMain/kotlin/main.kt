
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import cat.itb.m78.exercices.HelloWorldApp
import co.touchlab.kermit.Message
import io.ktor.http.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.awt.Dimension
import m78exercices.composeapp.generated.resources.Res
import m78exercices.composeapp.generated.resources.generatedFace
import m78exercices.composeapp.generated.resources.resourceKey
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import sun.awt.X11.Screen
import kotlinx.serialization.Serializable

fun main() = application {
    Window(
        title = "M78Exercices",
        state = rememberWindowState(width = 800.dp, height = 600.dp),
        onCloseRequest = ::exitApplication,
    ) {
        window.minimumSize = Dimension(350, 600)
        TrivialApp()
    }
}
// Screen Enum
enum class EScreen {
    Menu, Game, Result, Settings
}

// App Entry Point
@Composable
fun TrivialApp() {
    var currentScreen by remember { mutableStateOf(EScreen.Menu) }
    val gameViewModel: GameViewModel = viewModel()

    when (currentScreen) {
        EScreen.Menu -> MenuScreen(onStartGame = { currentScreen = EScreen.Game }, onSettings = { currentScreen = EScreen.Settings })
        EScreen.Game -> GameScreen(onGameEnd = { currentScreen = EScreen.Result }, viewModel = gameViewModel)
        EScreen.Result -> ResultScreen(score = gameViewModel.score, onBackToMenu = { currentScreen = EScreen.Menu })
        EScreen.Settings -> SettingsScreen(onBack = { currentScreen = EScreen.Menu }, viewModel = gameViewModel)
    }
}

// Menu Screen
@Composable
fun MenuScreen(onStartGame: () -> Unit, onSettings: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        androidx.compose.material3.Button(onClick = onStartGame, modifier = Modifier.padding(8.dp)) {
            androidx.compose.material3.Text("Start Game")
        }
        androidx.compose.material3.Button(onClick = onSettings, modifier = Modifier.padding(8.dp)) {
            androidx.compose.material3.Text("Settings")
        }
    }
}

@Composable
fun GameScreen(onGameEnd: () -> Unit, viewModel: GameViewModel) {
    val question = viewModel.currentQuestion
    val timeLeft = viewModel.timeLeft

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        androidx.compose.material3.Text("Time Left: $timeLeft", style = MaterialTheme.typography.bodyLarge) // Cambié h6 por bodyLarge
        Spacer(modifier = Modifier.height(16.dp))
        androidx.compose.material3.Text(question.text, style = MaterialTheme.typography.headlineMedium) // Cambié h5 por headlineMedium
        Spacer(modifier = Modifier.height(16.dp))
        question.options.forEach { option ->
            androidx.compose.material3.Button(
                onClick = {
                    viewModel.answerQuestion(option)
                    if (viewModel.isGameOver) onGameEnd() // End game if all questions answered
                },
                modifier = Modifier.padding(8.dp)
            ) {
                androidx.compose.material3.Text(option)
            }
        }
    }
}

@Composable
fun ResultScreen(score: Int, onBackToMenu: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        androidx.compose.material3.Text("Your Score: $score", style = MaterialTheme.typography.displayMedium) // Cambié h4 por displayMedium
        Spacer(modifier = Modifier.height(16.dp))
        androidx.compose.material3.Button(onClick = onBackToMenu) {
            androidx.compose.material3.Text("Back to Menu")
        }
    }
}

@Composable
fun SettingsScreen(onBack: () -> Unit, viewModel: GameViewModel) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        androidx.compose.material3.Text("Settings", style = MaterialTheme.typography.displayMedium) // Cambié h4 por displayMedium
        Spacer(modifier = Modifier.height(16.dp))
        androidx.compose.material3.Button(onClick = onBack) {
            androidx.compose.material3.Text("Back to Menu")
        }
    }
}


// ViewModel
class GameViewModel : ViewModel() {
    private val questions = listOf(
        Question("What is 2 + 2?", listOf("4", "3", "5", "6"), "4"),
        Question("What is the capital of France?", listOf("Berlin", "Paris", "Rome", "Madrid"), "Paris")
    )

    var currentQuestionIndex by mutableStateOf(0)
    val currentQuestion: Question get() = questions[currentQuestionIndex]
    var score by mutableStateOf(0)
    var timeLeft by mutableStateOf(10)
    val isGameOver: Boolean get() = currentQuestionIndex >= questions.size

    init {
        startTimer()
    }

    fun answerQuestion(answer: String) {
        if (answer == currentQuestion.correctAnswer) {
            score++
        }
        currentQuestionIndex++
    }

    private fun startTimer() {
        viewModelScope.launch {
            while (timeLeft > 0) {
                delay(1000)
                timeLeft--
            }
            if (!isGameOver) currentQuestionIndex++
        }
    }
}

// Question Data Class
data class Question(val text: String, val options: List<String>, val correctAnswer: String)


