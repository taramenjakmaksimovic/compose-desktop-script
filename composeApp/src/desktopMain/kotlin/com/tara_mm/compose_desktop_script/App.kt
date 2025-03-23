package com.tara_mm.compose_desktop_script
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun editorPane(
    editorText: MutableState<String>,
    onRun: () -> Unit,
    onExit: () -> Unit,
    cursorPosition: MutableState<Pair<Int, Any?>>,
    outputText: MutableState<String>,
    lastExitCode: MutableState<Int?>
) {
    val scrollState = rememberScrollState()
    val keywords = listOf("val", "var", "fun", "class", "if", "else", "for", "when", "return", "try", "catch",
        "throw", "package", "import", "finally", "private", "public", "object", "null", "do", "while", "break", "continue")
    val keywordColor = Color.Magenta
    val errorMessages = remember { mutableStateOf(mutableMapOf<Int, String>()) }
    val textFieldValue = remember { mutableStateOf(TextFieldValue(editorText.value)) }
    val errorLine = remember { mutableStateOf(-1) }

    LaunchedEffect(cursorPosition.value) {
        val (line, error) = cursorPosition.value
        if (line > 0) {
            val lines = editorText.value.lines()
            if (line <= lines.size) {
                var offset = 0
                for (i in 0 until line - 1) {
                    offset += lines[i].length + 1
                }

                if (error is Int) {
                    textFieldValue.value = textFieldValue.value.copy(selection = TextRange(offset, offset))
                } else if (error is String) {
                    errorMessages.value = errorMessages.value.toMutableMap().apply {
                        this[line] = error
                    }
                }
                errorLine.value = line
            }
        } else {
            errorLine.value = -1
        }
    }


    val visualTransformation = VisualTransformation { text ->
        val newString = buildAnnotatedString {
            var currentIndex = 0
            while (currentIndex < text.text.length) {
                var foundKeyword = false
                for (keyword in keywords) {
                    val regex = "\\b$keyword\\b".toRegex()
                    val match = regex.find(text.text, currentIndex)

                    if (match != null && match.range.first == currentIndex) {
                        val endIndex = currentIndex + keyword.length
                        withStyle(style = SpanStyle(color = keywordColor, fontWeight = FontWeight.Bold)) {
                            append(text.text.substring(currentIndex, endIndex))
                        }
                        currentIndex = endIndex
                        foundKeyword = true
                        break
                    }
                }
                if (!foundKeyword) {
                    val lines = text.text.lines()
                    val currentLineNumber = lines.take(currentIndex).size + 1
                    if (currentLineNumber == errorLine.value) {
                        withStyle(style = SpanStyle(background = Color(0xFFFFCDD2))) {
                            append(text.text[currentIndex])
                        }
                    } else {
                        append(text.text[currentIndex])
                    }
                    currentIndex++
                }
            }
        }
        TransformedText(newString, OffsetMapping.Identity)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Enter your Kotlin script:", fontWeight = FontWeight.Bold)

        Text(
            text = if (cursorPosition.value.second is String)
                "Cursor Position: Line ${cursorPosition.value.first} - Error: ${cursorPosition.value.second}"
            else
                "Cursor Position: Line ${cursorPosition.value.first}",
            color = if (cursorPosition.value.second is String) Color.Red else Color.Gray,
            fontSize = 12.sp
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                .background(Color.White)
                .verticalScroll(scrollState)
                .padding(8.dp)
        ) {
            Column {
                BasicTextField(
                    value = textFieldValue.value,
                    onValueChange = { newText ->
                        textFieldValue.value = newText
                        editorText.value = newText.text
                    },
                    textStyle = TextStyle(color = Color.Black, fontSize = 16.sp),
                    modifier = Modifier.fillMaxSize(),
                    visualTransformation = visualTransformation
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = {
                cursorPosition.value = Pair(0, null)
                outputText.value = ""
                lastExitCode.value = null
                onRun()
            }) {
                Text("Run")

            }
            Button(onClick = {
                editorText.value = ""
                outputText.value = ""
                textFieldValue.value = TextFieldValue("")
                errorMessages.value = mutableMapOf()
                errorLine.value = -1
                cursorPosition.value = Pair(0, null)
            }) {
                Text("Delete")
            }
            Button(onClick = onExit) {
                Text("Exit")
            }
        }
    }
}

@Composable
fun outputPane(
    outputText: MutableState<String>,
    isRunning: MutableState<Boolean>,
    lastExitCode: MutableState<Int?>,
    onDelete: () -> Unit,
    cursorPosition: MutableState<Pair<Int, Any?>>
) {
    val errorRegex = """(.+):(\d+):(\d+): error: (.+)""".toRegex()
    val scrollState = rememberScrollState()

    val lines = outputText.value.lines()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.Top
            ) {
                Text("Execution output:", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                if (isRunning.value) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(70.dp)
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
                        Text(line)
                    }
                }
            }


            VerticalScrollbar(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .fillMaxHeight(),
                adapter = rememberScrollbarAdapter(scrollState)
            )
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

        Button(
            onClick = onDelete,
            modifier = Modifier
                .padding(top = 16.dp)
                .align(Alignment.CenterHorizontally)
        ) {
            Text("Delete output")
        }
    }
}
