package com.tara_mm.compose_desktop_script

import androidx.compose.runtime.MutableState
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader


object Buttons {
    private var process: Process? = null
    private var isAborted: Boolean = false

    fun executeScript(
        script: String,
        outputText: MutableState<String>,
        isRunning: MutableState<Boolean>,
        lastExitCode: MutableState<Int?>,
        executionTime: MutableState<String>,
        executionHistory: MutableState<MutableList<Pair<Long, List<String>>>>
    ) {
        isAborted = false

        if (script.isBlank()) {
            val errorBlank = "Your script is empty! Please enter a valid Kotlin script."
            outputText.value = errorBlank
            executionHistory.value = executionHistory.value.toMutableList().apply {
                add(Pair(System.currentTimeMillis(), listOf(errorBlank)))
            }
            return
        }
        outputText.value = ""

        try {
            val tempDir = System.getProperty("java.io.tmpdir")
            val scriptName = "foo"
            val scriptFile = File(tempDir, "$scriptName.kts")

            if (scriptFile.exists()) {
                scriptFile.delete()
            }

            scriptFile.createNewFile()
            scriptFile.writeText(script)


            val command = listOf("kotlinc", "-script", scriptFile.absolutePath)
            isRunning.value = true
            executionTime.value = ""

            val scriptSize = scriptFile.length()
            val scriptSizeKB = scriptSize / 1024.0

            Thread {
                try {
                    val startTime = System.currentTimeMillis()
                    process = ProcessBuilder(command)
                        .redirectErrorStream(true)
                        .start()

                    val reader = BufferedReader(InputStreamReader(process!!.inputStream))
                    var line: String?

                    var processTimedOut = false
                    val timeout = 60000L

                    while (reader.readLine().also { line = it } != null) {
                        if (isAborted) break
                        outputText.value += line + "\n"

                        if (System.currentTimeMillis() - startTime >= timeout) {
                            process?.destroy()
                            processTimedOut = true
                            break
                        }
                    }
                    val scriptMessages = mutableListOf<String>()

                    if (processTimedOut) {
                        val timeoutMessage = "Script executed for more than 60 seconds."
                        outputText.value +=
                            "\n$timeoutMessage\n"
                        scriptMessages.add(timeoutMessage)
                        isRunning.value = false
                    } else if (!isAborted){
                        val exitCode = process?.waitFor()
                        isRunning.value = false
                        lastExitCode.value = exitCode

                        val exitCodeMessage = "Script finished with exit code: $exitCode"
                        outputText.value += "\n$exitCodeMessage\n"
                        scriptMessages.add(exitCodeMessage)

                        if (exitCode == 0) {
                            val endTime = System.currentTimeMillis()
                            val duration = endTime - startTime
                            val seconds = duration / 1000
                            val milliseconds = duration % 1000
                            val executionTimeMessage = "Execution time: ${seconds}s ${milliseconds}ms"
                            outputText.value += "\n$executionTimeMessage\n"
                            scriptMessages.add(executionTimeMessage)
                        }

                        var scriptSizeString = ""
                         scriptSizeString += when {
                            scriptSize.toInt() == 1 -> "Temporary script file size: $scriptSize byte"
                            scriptSize < 1024 -> "Temporary script file size: $scriptSize bytes"
                            else -> "Temporary script file size: %.2f KB".format(scriptSizeKB)
                        }

                        outputText.value += "\n$scriptSizeString\n"
                        scriptMessages.add(scriptSizeString)
                    }

                    if(!isAborted){
                        executionHistory.value.add(Pair(startTime, scriptMessages))
                    }
                } catch (e: Exception) {
                        e.printStackTrace()
                        isRunning.value = false
                        lastExitCode.value = -1
                        executionTime.value = ""
                        val errorMessage = "Error: ${e.message}"
                        outputText.value = errorMessage
                        executionHistory.value.add(Pair(System.currentTimeMillis(), listOf(errorMessage)))
                    } finally {
                        scriptFile.delete()
                    }
                }.start()

            } catch (e: Exception) {
                e.printStackTrace()
                isRunning.value = false
                lastExitCode.value = -1
                executionTime.value = ""
                val errorMessage = "Error: ${e.message}"
                outputText.value = errorMessage
                executionHistory.value.add(Pair(System.currentTimeMillis(), listOf(errorMessage)))
            }
    }

    fun abortExecution(
        outputText: MutableState<String>,
        isRunning: MutableState<Boolean>,
        executionHistory: MutableState<MutableList<Pair<Long, List<String>>>>
    ){
        if(process!= null && process!!.isAlive) {
            process?.destroy()
            process = null
            isAborted = true
            isRunning.value = false
            val abortMessage = "Execution aborted."
            outputText.value += "\n$abortMessage\n"
            executionHistory.value.add(Pair(System.currentTimeMillis(), listOf(abortMessage)))
        }
    }
}
