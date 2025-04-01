package com.tara_mm.compose_desktop_script


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.*
import com.tara_mm.compose_desktop_script.Buttons.executeScript
import kotlinx.coroutines.delay

fun main() = application {
    val scriptText = mutableStateOf("")
    val outputText = mutableStateOf("")
    val isRunning = mutableStateOf(false)
    val lastExitCode = mutableStateOf<Int?>(null)
    val cursorPosition = mutableStateOf<Pair<Int, Any?>>(Pair(0, 0))
    val executionTime = mutableStateOf("")
    val showEditor = remember { mutableStateOf(false) }
    val showHomePage = remember { mutableStateOf(true) }
    val executionHistory = remember { mutableStateOf(mutableListOf<Pair<Long, List<String>>>()) }
    val showHistoryWindow = mutableStateOf(false)


    if (showHomePage.value) {
        Window(
            onCloseRequest = ::exitApplication,
            title = "ComposeDesktopScript Home",
            state = WindowState(
                position = WindowPosition(Alignment.Center),
                width = 500.dp,
                height = 500.dp
            )
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
                    .background(LightPurple)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                animationText(
                    "Welcome to ComposeDesktopScript!",
                    modifier = Modifier.padding(bottom = 48.dp),
                    fontSize = 25.sp,
                    color = Color.White,
                    typingSpeed = 70L
                )
                Spacer(modifier = Modifier.height(32.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = {
                            showEditor.value = true
                            showHomePage.value = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = DarkPurple,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Start")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = ::exitApplication,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = DarkPurple,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Exit")
                    }
                }
            }
        }
    }
    if (showEditor.value)
    {
        Window(
           onCloseRequest = ::exitApplication,
            title = "ComposeDesktopScript editor pane",
            state = WindowState(position = WindowPosition(100.dp, 100.dp), width = 500.dp, height = 600.dp)
        ) {
            editorPane(
                editorText = scriptText,
                onRun = {
                    executeScript(
                        scriptText.value,
                        outputText,
                        isRunning,
                        lastExitCode,
                        executionTime,
                        executionHistory
                    ) },
                onExit = {
                    exitApplication()
                         },
                cursorPosition = cursorPosition,
                outputText = outputText,
                lastExitCode = lastExitCode,
                isRunning = isRunning,
                executionHistory = executionHistory

            )
        }

        Window(
            onCloseRequest = ::exitApplication,
            title = "ComposeDesktopScript output pane",
            state = WindowState(position = WindowPosition(650.dp, 100.dp), width = 500.dp, height = 600.dp)
        ) {
            outputPane(
                outputText = outputText,
                isRunning = isRunning,
                lastExitCode = lastExitCode,
                executionTime = executionTime,
                cursorPosition = cursorPosition,
                executionHistory = executionHistory,
                showHistoryWindow = showHistoryWindow
            )
        }
    }
}

@Composable
fun animationText(
    inputText: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit,
    color: Color,
    typingSpeed: Long
){
    var visibleText by remember { mutableStateOf("") }
    LaunchedEffect(inputText){
        visibleText= ""
        for(i in inputText.indices){
            visibleText = inputText.substring(0, i + 1)
            delay(typingSpeed)
        }
    }
    Text(
        text = visibleText,
        fontSize = fontSize,
        color = color,
        modifier = modifier
    )
}
