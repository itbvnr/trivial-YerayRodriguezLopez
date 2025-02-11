import androidx.compose.foundation.layout.*
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay

fun main() = application {
    Window(
        title = "Trivial App",
        state = rememberWindowState(width = 540.dp, height = 960.dp),
        onCloseRequest = ::exitApplication,
    ) {
        trivialApp()
    }
}

enum class EScreen {
    Menu, Game, Result, Settings
}

@Composable
fun trivialApp(gameViewModel: GameViewModel = viewModel()) {
    var currentScreen by remember { mutableStateOf(EScreen.Menu) }

    when (currentScreen) {
        EScreen.Menu -> menuScreen(onStartGame = { currentScreen = EScreen.Game }, onSettings = { currentScreen = EScreen.Settings })
        EScreen.Game -> gameScreen(onGameEnd = { currentScreen = EScreen.Result }, viewModel = gameViewModel)
        EScreen.Result -> resultScreen(score = gameViewModel.score, onBackToMenu = { currentScreen = EScreen.Menu }, viewModel = gameViewModel)
        EScreen.Settings -> settingsScreen(onBack = { currentScreen = EScreen.Menu }, viewModel = gameViewModel)
    }
}

@Composable
fun menuScreen(onStartGame: () -> Unit, onSettings: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(onClick = onStartGame, modifier = Modifier.padding(8.dp)) {
            Text("Start Game")
        }
        Button(onClick = onSettings, modifier = Modifier.padding(8.dp)) {
            Text("Settings")
        }
    }
}

@Composable
fun gameScreen(onGameEnd: () -> Unit, viewModel: GameViewModel) {
    val question = viewModel.questions.getOrNull(viewModel.currentQuestionIndex)
    var isAnswerSelected by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel.timeLeft) {
        if (viewModel.timeLeft > 0 && !viewModel.isGameOver) {
            delay(1000)
            viewModel.timeLeft -= 1
        } else if (viewModel.timeLeft <= 0 && !viewModel.isGameOver) {
            viewModel.nextQuestion()
        }
    }

    LaunchedEffect(viewModel.isGameOver) {
        if (viewModel.isGameOver) {
            onGameEnd()
        }
    }

    val progress = (viewModel.timeLeft.toFloat() / viewModel.settings.timePerRound).coerceIn(0f, 1f)
    val currentRound = viewModel.currentQuestionIndex + 1
    val totalRounds = viewModel.settings.rounds

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Time Left: ${viewModel.timeLeft}", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(16.dp))

        question?.let {
            Text(it.text, style = MaterialTheme.typography.bodyLarge, overflow = TextOverflow.Ellipsis, maxLines = 2)
            Spacer(modifier = Modifier.height(16.dp))

            it.options.forEach { option ->
                Button(
                    onClick = {
                        if (!isAnswerSelected) {
                            viewModel.answerQuestion(option)
                            isAnswerSelected = true
                        }
                    },
                    modifier = Modifier.padding(8.dp),
                    enabled = !isAnswerSelected
                ) {
                    Text(option)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Round $currentRound of $totalRounds", style = MaterialTheme.typography.bodyLarge)
        }
    }

    // Reiniciar el estado de isAnswerSelected después del cambio de pregunta
    if (isAnswerSelected) {
        LaunchedEffect(viewModel.currentQuestionIndex) {
            isAnswerSelected = false
        }
    }
}

@Composable
fun resultScreen(score: Int, onBackToMenu: () -> Unit, viewModel: GameViewModel) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Your Score: $score", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            viewModel.resetGame()
            onBackToMenu()
        }) {
            Text("Back to Menu")
        }
    }
}

@Composable
fun settingsScreen(onBack: () -> Unit, viewModel: GameViewModel) {
    var difficulty by remember { mutableStateOf(viewModel.settings.difficulty) }
    var rounds by remember { mutableStateOf(viewModel.settings.rounds) }
    var timePerRound by remember { mutableStateOf(viewModel.settings.timePerRound) }

    var isDifficultyDropdownExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(16.dp))

        // Difficulty Dropdown
        Text("Difficulty", style = MaterialTheme.typography.bodyLarge)
        Box {
            Button(onClick = { isDifficultyDropdownExpanded = true }) {
                Text(difficulty)
            }
            DropdownMenu(
                expanded = isDifficultyDropdownExpanded,
                onDismissRequest = { isDifficultyDropdownExpanded = false }
            ) {
                listOf("Easy", "Normal", "Hard").forEach { level ->
                    DropdownMenuItem(onClick = {
                        difficulty = level
                        isDifficultyDropdownExpanded = false
                    }) {
                        Text(level)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Rounds RadioGroup
        Text("Rounds", style = MaterialTheme.typography.bodyLarge)
        Row {
            listOf(5, 10, 15).forEach { round ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    RadioButton(
                        selected = rounds == round,
                        onClick = { rounds = round }
                    )
                    Text(text = round.toString())
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Time per Round Slider
        Text("Time Per Round (seconds)", style = MaterialTheme.typography.bodyLarge)
        Slider(
            value = timePerRound.toFloat(),
            onValueChange = { timePerRound = it.toInt() },
            valueRange = 10f..60f,
            steps = 50
        )
        Text("$timePerRound sec", style = MaterialTheme.typography.bodyLarge)

        Spacer(modifier = Modifier.height(16.dp))

        // Save Button
        Button(onClick = {
            viewModel.updateSettings(
                difficulty = difficulty,
                rounds = rounds,
                timePerRound = timePerRound
            )
            onBack()
        }) {
            Text("Save")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Back Button
        Button(onClick = onBack) {
            Text("Back")
        }
    }
}

class GameViewModel : ViewModel() {
    var score by mutableStateOf(0)
    var currentQuestionIndex by mutableStateOf(0)
    var timeLeft by mutableStateOf(30)
    var settings by mutableStateOf(GameSettings("Easy", 15, 30))
    val questions: List<Question> get() = when (settings.difficulty) {
        "Easy" -> easyQuestions
        "Normal" -> normalQuestions
        "Hard" -> hardQuestions
        else -> easyQuestions
    }

    val isGameOver get() = currentQuestionIndex >= settings.rounds

    fun answerQuestion(answer: String) {
        val currentQuestion = questions[currentQuestionIndex]
        if (answer == currentQuestion.correctAnswer) {
            score++
        }
        nextQuestion()
    }

    fun nextQuestion() {
        if (currentQuestionIndex < settings.rounds - 1) {
            currentQuestionIndex++
            timeLeft = settings.timePerRound
        } else {
            currentQuestionIndex++
        }
    }

    fun updateSettings(difficulty: String, rounds: Int, timePerRound: Int) {
        settings = GameSettings(difficulty, rounds, timePerRound)
        resetGame()
    }

    fun resetGame() {
        score = 0
        currentQuestionIndex = 0
        timeLeft = settings.timePerRound
    }
}

data class Question(val text: String, val options: List<String>, val correctAnswer: String)

data class GameSettings(var difficulty: String, var rounds: Int, var timePerRound: Int)

val easyQuestions = listOf(
    Question("¿Cuál es la capital de Afganistán?", listOf("Kabul", "Tirana", "Argel", "Andorra la Vella"), "Kabul"),
    Question("¿Cuál es el elemento químico con el símbolo H?", listOf("Hidrógeno", "Oxígeno", "Nitrógeno", "Carbono"), "Hidrógeno"),
    Question("¿Cuál es el planeta más grande del sistema solar?", listOf("Júpiter", "Saturno", "Urano", "Neptuno"), "Júpiter"),
    Question("¿Cuál es el órgano más grande del cuerpo humano?", listOf("Piel", "Corazón", "Hígado", "Riñón"), "Piel"),
    Question("¿Quién descubrió América?", listOf("Cristóbal Colón", "Marco Polo", "Américo Vespucio", "Fernando de Magallanes"), "Cristóbal Colón"),
    Question("¿Qué gas es necesario para la respiración humana?", listOf("Oxígeno", "Hidrógeno", "Nitrógeno", "Dióxido de carbono"), "Oxígeno"),
    Question("¿Cuál es el metal más ligero?", listOf("Litio", "Hierro", "Oro", "Plata"), "Litio"),
    Question("¿Cuál es el continente más grande?", listOf("Asia", "África", "América", "Europa"), "Asia"),
    Question("¿Cuál es el océano más grande?", listOf("Océano Pacífico", "Océano Atlántico", "Océano Índico", "Océano Ártico"), "Océano Pacífico"),
    Question("¿Cuál es el desierto más grande del mundo?", listOf("Desierto del Sahara", "Desierto de Gobi", "Desierto de Kalahari", "Desierto de Atacama"), "Desierto del Sahara"),
    Question("¿Cuál es la montaña más alta del mundo?", listOf("Monte Everest", "Monte Kilimanjaro", "Monte Aconcagua", "Monte McKinley"), "Monte Everest"),
    Question("¿Cuál es el río más largo del mundo?", listOf("Río Amazonas", "Río Nilo", "Río Yangtsé", "Río Misisipi"), "Río Amazonas"),
    Question("¿Cuál es el lago más grande del mundo?", listOf("Lago Superior", "Lago Victoria", "Lago Baikal", "Lago Tanganica"), "Lago Superior"),
    Question("¿Cuál es el país más grande del mundo?", listOf("Rusia", "Canadá", "China", "Estados Unidos"), "Rusia"),
    Question("¿Cuál es el país más pequeño del mundo?", listOf("Ciudad del Vaticano", "Mónaco", "Nauru", "San Marino"), "Ciudad del Vaticano")
)

val normalQuestions = listOf(
    Question("¿Cuál es la capital de Andorra?", listOf("Andorra la Vella", "Luanda", "Buenos Aires", "Ereván"), "Andorra la Vella"),
    Question("¿Qué gas es esencial para la respiración humana?", listOf("Oxígeno", "Hidrógeno", "Nitrógeno", "Helio"), "Oxígeno"),
    Question("¿Qué sustancia se utiliza para medir la acidez?", listOf("pH", "Hidrógeno", "Oxígeno", "Carbono"), "pH"),
    Question("¿Cuál es el segundo planeta más cercano al sol?", listOf("Venus", "Mercurio", "Tierra", "Marte"), "Venus"),
    Question("¿Quién pintó la Mona Lisa?", listOf("Leonardo da Vinci", "Pablo Picasso", "Vincent van Gogh", "Claude Monet"), "Leonardo da Vinci"),
    Question("¿En qué año cayó el Muro de Berlín?", listOf("1989", "1991", "1987", "1993"), "1989"),
    Question("¿Cuál es el idioma oficial de Brasil?", listOf("Portugués", "Español", "Inglés", "Francés"), "Portugués"),
    Question("¿Quién escribió 'Cien años de soledad'?", listOf("Gabriel García Márquez", "Mario Vargas Llosa", "Julio Cortázar", "Pablo Neruda"), "Gabriel García Márquez"),
    Question("¿Cuál es el país más poblado del mundo?", listOf("China", "India", "Estados Unidos", "Indonesia"), "China"),
    Question("¿Cuál es el elemento químico con el símbolo O?", listOf("Oxígeno", "Oro", "Osmio", "Oganesón"), "Oxígeno"),
    Question("¿Cuál es el número pi?", listOf("3.1416", "3.1514", "3.1615", "3.1717"), "3.1416"),
    Question("¿Cuál es el animal terrestre más rápido?", listOf("Guepardo", "León", "Tigre", "Leopardo"), "Guepardo"),
    Question("¿Cuál es el edificio más alto del mundo?", listOf("Burj Khalifa", "Shanghai Tower", "Abraj Al Bait", "Ping An Finance Centre"), "Burj Khalifa"),
    Question("¿Cuál es el país con más medallas olímpicas?", listOf("Estados Unidos", "China", "Rusia", "Alemania"), "Estados Unidos"),
    Question("¿Quién fue el primer hombre en pisar la luna?", listOf("Neil Armstrong", "Buzz Aldrin", "Yuri Gagarin", "Michael Collins"), "Neil Armstrong")
)

val hardQuestions = listOf(
    Question("¿Cuál es el proceso por el cual las plantas producen energía?", listOf("Fotosíntesis", "Respiración", "Fermentación", "Digestión"), "Fotosíntesis"),
    Question("¿Cuál es el metal más ligero?", listOf("Litio", "Hierro", "Aluminio", "Plata"), "Litio"),
    Question("¿Quién formuló la teoría de la relatividad?", listOf("Albert Einstein", "Isaac Newton", "Galileo Galilei", "Nikola Tesla"), "Albert Einstein"),
    Question("¿En qué año comenzó la Primera Guerra Mundial?", listOf("1914", "1918", "1939", "1945"), "1914"),
    Question("¿Cuál es el país con el PIB más alto del mundo?", listOf("Estados Unidos", "China", "Japón", "Alemania"), "Estados Unidos"),
    Question("¿Quién escribió 'Don Quijote de la Mancha'?", listOf("Miguel de Cervantes", "William Shakespeare", "Lope de Vega", "Francisco de Quevedo"), "Miguel de Cervantes"),
    Question("¿Cuál es la capital de Mongolia?", listOf("Ulán Bator", "Astana", "Biskek", "Dusambé"), "Ulán Bator"),
    Question("¿Cuál es el río más largo de Europa?", listOf("Volga", "Danubio", "Dniéper", "Rin"), "Volga"),
    Question("¿Cuál es el desierto más árido del mundo?", listOf("Desierto de Atacama", "Desierto del Sahara", "Desierto de Gobi", "Desierto de Kalahari"), "Desierto de Atacama"),
    Question("¿Cuál es el lago más profundo del mundo?", listOf("Lago Baikal", "Lago Tanganica", "Lago Superior", "Lago Victoria"), "Lago Baikal"),
    Question("¿Cuál es la montaña más alta de América del Norte?", listOf("Monte McKinley", "Monte Logan", "Monte Whitney", "Monte Mitchell"), "Monte McKinley"),
    Question("¿Cuál es el océano más profundo del mundo?", listOf("Océano Pacífico", "Océano Atlántico", "Océano Índico", "Océano Ártico"), "Océano Pacífico"),
    Question("¿Cuál es el país más grande de África?", listOf("Argelia", "Sudán", "Libia", "Chad"), "Argelia"),
    Question("¿Cuál es la ciudad más poblada de Australia?", listOf("Sídney", "Melbourne", "Brisbane", "Perth"), "Sídney"),
    Question("¿Cuál es el idioma oficial de Irán?", listOf("Persa", "Árabe", "Turco", "Kurdo"), "Persa")
)