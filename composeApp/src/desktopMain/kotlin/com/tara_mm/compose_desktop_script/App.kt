package com.tara_mm.compose_desktop_script

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tara_mm.compose_desktop_script.Buttons.abortExecution
import kotlinx.coroutines.delay
import androidx.compose.foundation.rememberScrollbarAdapter as FoundationScrollbarAdapter

@Composable
fun editorPane(
    editorText: MutableState<String>,
    onRun: () -> Unit,
    onExit: () -> Unit,
    cursorPosition: MutableState<Pair<Int, Any?>>,
    outputText: MutableState<String>,
    lastExitCode: MutableState<Int?>,
    isRunning: MutableState<Boolean>,
    executionHistory: MutableState<MutableList<Pair<Long, List<String>>>>
) {
    val scrollState = rememberScrollState()
    val showExitDialog = remember { mutableStateOf(false) }
    val keywords = listOf(
        "val", "var", "fun", "class",
        "if", "else", "while", "for", "when",
        "return", "try", "catch",
        "throw", "package", "import",
        "finally", "private", "public",
        "object", "null", "do",
        "while", "break", "continue"
    )
    val keywordColor = Purple
    val commentColor = Color.Gray
    val errorMessages = remember { mutableStateOf(mutableMapOf<Int, String>()) }
    val textFieldValue = remember { mutableStateOf(TextFieldValue(editorText.value)) }
    val errorLine = remember { mutableStateOf(-1) }
    val undoList = remember { mutableListOf<String>() }
    val redoList = remember { mutableListOf<String>() }

    fun saveUndoState(){
        undoList.add(editorText.value)
        redoList.clear()
        if (undoList.size > 30) undoList.removeAt(0)
    }

    fun undo(){
        if (undoList.isNotEmpty()){
            redoList.add(editorText.value)
            editorText.value = undoList.removeLast()
            textFieldValue.value = TextFieldValue(editorText.value)
        }
    }

    fun redo(){
        if (redoList.isNotEmpty()){
            undoList.add(editorText.value)
            editorText.value = redoList.removeLast()
            textFieldValue.value = TextFieldValue(editorText.value)
        }
    }

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
            val textValue = text.text

            while (currentIndex < textValue.length) {
                var foundMatch = false

                for (keyword in keywords) {
                    val regex = "\\b$keyword\\b".toRegex()
                    val match = regex.find(textValue, currentIndex)

                    if (match != null && match.range.first == currentIndex) {
                        val endIndex = currentIndex + keyword.length
                        withStyle(style = SpanStyle(color = keywordColor, fontWeight = FontWeight.Bold)) {
                            append(textValue.substring(currentIndex, endIndex))
                        }
                        currentIndex = endIndex
                        foundMatch = true
                        break
                    }
                }

                if (!foundMatch) {
                    val singleLineCommentMatch = "//.*".toRegex().find(textValue, currentIndex)
                    if (singleLineCommentMatch != null && singleLineCommentMatch.range.first == currentIndex) {
                        val endIndex = singleLineCommentMatch.range.last + 1
                        withStyle(style = SpanStyle(color = commentColor, fontStyle = FontStyle.Italic)) {
                            append(textValue.substring(currentIndex, endIndex))
                        }
                        currentIndex = endIndex
                        foundMatch = true
                    }
                }

                if (!foundMatch) {
                    val multiLineCommentMatch = "/\\*.*?\\*/".toRegex(RegexOption.DOT_MATCHES_ALL).find(textValue, currentIndex)
                    if (multiLineCommentMatch != null && multiLineCommentMatch.range.first == currentIndex) {
                        val endIndex = multiLineCommentMatch.range.last + 1
                        withStyle(style = SpanStyle(color = commentColor, fontStyle = FontStyle.Italic)) {
                            append(textValue.substring(currentIndex, endIndex))
                        }
                        currentIndex = endIndex
                        foundMatch = true
                    }
                }

                if (!foundMatch) {
                    append(textValue[currentIndex])
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ){
        Text(
            "Enter your Kotlin script:",
            fontWeight = FontWeight.Bold,
            color = DarkPurple
        )
            Row {
                Button(
                    onClick = { undo() },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = DarkPurple,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Undo")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { redo() },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = DarkPurple,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Redo")
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (cursorPosition.value.second is String)
                "Cursor Position: Line ${cursorPosition.value.first} \n" +
                        "Error: ${cursorPosition.value.second}"
            else
                "Cursor Position: Line ${cursorPosition.value.first}",
            color = if (cursorPosition.value.second is String) Color.Red else LightPurple,
            fontSize = 12.sp
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .border(2.dp, DarkPurple, RoundedCornerShape(4.dp))
                .background(Color.White)
                .verticalScroll(scrollState)
                .padding(8.dp)
        ) {
            Column {
                BasicTextField(
                    value = textFieldValue.value,
                    onValueChange = { newText ->
                        saveUndoState()
                        textFieldValue.value = newText
                        editorText.value = newText.text
                    },
                    textStyle = TextStyle(
                        color = Color.Black,
                        fontSize = 16.sp
                    ),
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
            },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = DarkPurple,
                    contentColor = Color.White
                )
            ) {
                Text("Run")

            }
            Button(onClick = {
                abortExecution(outputText, isRunning, executionHistory)
            },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = DarkPurple,
                    contentColor = Color.White
                )
            ) {
                Text("Abort")

            }
            Button(
                onClick = {
                editorText.value = ""
                outputText.value = ""
                textFieldValue.value = TextFieldValue("")
                errorMessages.value = mutableMapOf()
                errorLine.value = -1
                cursorPosition.value = Pair(0, null)
            },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = DarkPurple,
                    contentColor = Color.White
                )

            ) {
                Text("Delete")
            }
            Button(
                onClick = { showExitDialog.value = true },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = DarkPurple,
                    contentColor = Color.White
                )
                ) {
                Text("Exit")
            }
        }

        if (showExitDialog.value) {
            AlertDialog(
                onDismissRequest = { showExitDialog.value = false },
                title = {
                    Text(
                    "Confirm exit",
                    fontSize = 20.sp,
                    color = Color.White
                ) },
                text = {
                    animationText(
                    inputText = "Do you really want to exit?",
                    color = DarkPurple,
                    fontSize = 16.sp,
                    typingSpeed = 40L
                )},
                backgroundColor  = LightPurple,
                dismissButton = {
                    Button(
                        onClick = { showExitDialog.value = false },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = DarkPurple,
                            contentColor = Color.White
                        )
                        ) {
                        Text("No")
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showExitDialog.value = false
                            onExit()
                        },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = DarkPurple,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Yes")
                    }
                }
            )
        }
    }
}

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
                                        line == "Script executed for more than 60 seconds." -> Color.Red

                                        line.startsWith("Script finished with exit code:") ||
                                        line.startsWith("Execution time:") ||
                                        line.startsWith("Temporary script file size:") -> DarkPurple

                                        else -> Color.Unspecified
                            },
                            fontWeight = when {
                                        line == "Your script is empty! Please enter a valid Kotlin script." ||
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
                    adapter = FoundationScrollbarAdapter(
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
        executionHistoryWindow(showHistoryWindow, executionHistory)
    }
}

@Composable
fun animationText(
    inputText: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit ,
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
