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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tara_mm.compose_desktop_script.Buttons.abortExecution

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
                saveUndoState()
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


