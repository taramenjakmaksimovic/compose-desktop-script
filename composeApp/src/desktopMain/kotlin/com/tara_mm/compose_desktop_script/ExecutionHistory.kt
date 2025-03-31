package com.tara_mm.compose_desktop_script

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import java.text.SimpleDateFormat
import java.util.*


@Composable
fun executionHistoryWindow(
    showHistoryWindow: MutableState<Boolean>,
    executionHistory: MutableState<MutableList<Pair<Long, List<String>>>>
) {
    if (showHistoryWindow.value) {
        Window(
            onCloseRequest = { showHistoryWindow.value = false },
            title = "Execution history",
            state = WindowState(
                position = WindowPosition(100.dp, 100.dp),
                width = 500.dp,
                height = 600.dp
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .background(Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(16.dp)
                ) {
                    Text(
                        "Execution history",
                        fontWeight = FontWeight.Bold,
                        color = DarkPurple
                    )
                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    if (executionHistory.value.isEmpty()) {
                        Text("No history available.", color = Color.Gray)
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxHeight(0.8f)
                                .verticalScroll(rememberScrollState())
                        ) {
                            executionHistory.value.forEachIndexed { index, (timestamp,items) ->
                                val formattedTime = SimpleDateFormat("dd-MM-yyyy HH:mm:ss")
                                    .format(Date(timestamp))
                                Text(
                                    formattedTime,
                                    color = LightPurple
                                )
                                items.forEach{ item ->
                                    Text(
                                        item,
                                        color = DarkPurple
                                    )
                                }
                                if (index < executionHistory.value.lastIndex) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Divider()
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Button(
                        onClick = { showHistoryWindow.value = false },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = DarkPurple,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}
