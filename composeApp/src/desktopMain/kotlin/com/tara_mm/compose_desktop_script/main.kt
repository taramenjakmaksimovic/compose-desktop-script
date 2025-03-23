package com.tara_mm.compose_desktop_script

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.tara_mm.compose_desktop_script.Buttons.executeScript



fun main() = application {
    val scriptText = mutableStateOf("")
    val outputText = mutableStateOf("")
    val isRunning = mutableStateOf(false)
    val lastExitCode = mutableStateOf<Int?>(null)
    val cursorPosition = mutableStateOf<Pair<Int, Any?>>(Pair(0, 0))
    val executionTime = mutableStateOf("")


    Window(
        onCloseRequest = ::exitApplication,
        title = "ComposeDesktopScript",
        state = WindowState(position = WindowPosition(100.dp, 100.dp), width = 500.dp, height = 600.dp)
    ) {
        editorPane(
            editorText = scriptText,
            onRun = { executeScript(scriptText.value, outputText, isRunning, lastExitCode, executionTime) },
            onExit = { exitApplication() },
            cursorPosition = cursorPosition,
            outputText = outputText,
            lastExitCode = lastExitCode

        )
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "ComposeDesktopScriptOutput",
        state = WindowState(position = WindowPosition(650.dp, 100.dp), width = 500.dp, height = 600.dp)
    ) {
        outputPane(
            outputText = outputText,
            isRunning = isRunning,
            lastExitCode = lastExitCode,
            executionTime = executionTime,
            cursorPosition = cursorPosition
        )
    }
}