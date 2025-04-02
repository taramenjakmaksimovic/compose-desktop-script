package com.tara_mm.compose_desktop_script

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun outputPane(
    outputText: MutableState<String>,
    isRunning: MutableState<Boolean>,
    lastExitCode: MutableState<Int?>,
    executionTime: MutableState<String>,
    cursorPosition: MutableState<Pair<Int, Any?>>,
    executionHistory: MutableState<MutableList<Pair<Long, List<String>>>>,
    showHistoryWindow: MutableState<Boolean>

) {
    val errorRegex = """(.+):(\d+):(\d+): error: (.+)""".toRegex()
    val scrollState = rememberScrollState()

    val lines = outputText.value.lines()
    var progressText by remember { mutableStateOf("In progress.") }

    LaunchedEffect(isRunning.value){
        while (isRunning.value) {
            delay(550L)
            progressText = when (progressText){
                "In progress." -> "In progress.."
                "In progress.." -> "In progress..."
                else -> "In progress."
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "Execution output:",
            fontWeight = FontWeight.Bold,
            color = DarkPurple
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .weight(1f)
                .border(2.dp, DarkPurple, RoundedCornerShape(8.dp))
                .padding(12.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.Top
            ) {
                if (isRunning.value) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(70.dp),
                            color = DarkPurple
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = progressText,
                            color = DarkPurple,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                lines.forEach { line ->
                    val match = errorRegex.find(line)
                    if (match != null) {
                        val (filePath, lineNum, colNum, errorMessage) = match.destructured
                        val errorLocation = "$filePath:$lineNum:$colNum"

                        Text(
                            text = "Error at: $errorLocation - $errorMessage",
                            modifier = Modifier.clickable {
                                cursorPosition.value = Pair(lineNum.toInt(), errorMessage)
                            },
                            color = Color.Red,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            line,
                            color = when {
                                line == "Your script is empty! Please enter a valid Kotlin script." ||
                                        line == "Execution aborted." ||
                                        line.contains("java") ||
                                        line.contains("foo") ||
                                        line == "Script executed for more than 60 seconds." -> Color.Red

                                line.startsWith("Script finished with exit code:") ||
                                        line.startsWith("Execution time:") ||
                                        line.startsWith("Temporary script file size:") -> DarkPurple

                                else -> Color.Unspecified
                            },
                            fontWeight = when {
                                line == "Your script is empty! Please enter a valid Kotlin script." ||
                                        line.contains("java") ||
                                        line.contains("foo") ||
                                        line == "Execution aborted." ||
                                        line == "Script executed for more than 60 seconds." -> FontWeight.Bold

                                else -> FontWeight.Normal
                            }
                        )
                    }
                }
            }

            VerticalScrollbar(
                modifier = Modifier
                    .fillMaxHeight(),
                adapter = rememberScrollbarAdapter(
                    scrollState
                ),
                style = ScrollbarStyle(
                    thickness = 6.dp,
                    minimalHeight = 8.dp,
                    hoverDurationMillis = 400,
                    shape = RoundedCornerShape(4.dp),
                    unhoverColor = LightPurple,
                    hoverColor = DarkPurple
                )
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = { showHistoryWindow.value = !showHistoryWindow.value },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = DarkPurple,
                    contentColor = Color.White
                ),
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text("Show execution history")
            }
        }

        lastExitCode.value?.let { exitCode ->
            if (exitCode != 0) {
                Text(
                    "The last script finished with exit code: $exitCode",
                    color = Color.Red,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        if (executionTime.value.isNotEmpty()) {
            Text(
                executionTime.value,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        executionHistoryWindow(showHistoryWindow, executionHistory, isRunning)
    }
}