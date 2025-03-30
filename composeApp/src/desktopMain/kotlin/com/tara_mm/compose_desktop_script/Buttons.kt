package com.tara_mm.compose_desktop_script

import androidx.compose.runtime.MutableState
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader


object Buttons {
    private var process: Process? = null

    fun executeScript(
        script: String,
        outputText: MutableState<String>,
        isRunning: MutableState<Boolean>,
        lastExitCode: MutableState<Int?>,
        executionTime: MutableState<String>,
        executionHistory: MutableState<MutableList<String>>
    ) {

        if (script.isBlank()) {
            outputText.value = "Your script is empty! Please enter a valid Kotlin script."
            executionHistory.value.add(outputText.value + "\n")
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
                        outputText.value += line + "\n"

                        if (System.currentTimeMillis() - startTime >= timeout) {
                            process?.destroy()
                            processTimedOut = true
                            break
                        }
                    }

                    if (processTimedOut) {
                        outputText.value +=
                            "\nScript executed for more than 60 seconds.\n"
                        executionHistory.value.add("Script executed for more than 60 seconds.\n")
                        isRunning.value = false
                    } else {
                        val exitCode = process?.waitFor()
                        isRunning.value = false
                        lastExitCode.value = exitCode


                        outputText.value += "\n"
                        outputText.value += "Script finished with exit code: $exitCode\n"
                        executionHistory.value.add("Script finished with exit code: $exitCode")

                        if (exitCode == 0) {
                            val endTime = System.currentTimeMillis()
                            val duration = endTime - startTime
                            val seconds = duration / 1000
                            val milliseconds = duration % 1000
                            outputText.value += "\nExecution time: ${seconds}s ${milliseconds}ms\n"
                            executionHistory.value.add("Execution time: ${seconds}s ${milliseconds}ms")
                        }

                        var scriptSizeString = ""
                         scriptSizeString += when {
                            scriptSize.toInt() == 1 -> "Temporary script file size: $scriptSize byte\n"
                            scriptSize < 1024 -> "Temporary script file size: $scriptSize bytes\n"
                            else -> "Temporary script file size: %.2f KB\n".format(scriptSizeKB)
                        }
                        outputText.value += scriptSizeString
                        executionHistory.value.add(scriptSizeString)

                    }

                } catch (e: Exception) {
                        e.printStackTrace()
                        isRunning.value = false
                        lastExitCode.value = -1
                        executionTime.value = ""
                        outputText.value = "Error: ${e.message}"
                        executionHistory.value.add("Error: ${e.message}\n")
                    } finally {
                        scriptFile.delete()
                    }
                }.start()

            } catch (e: Exception) {
                e.printStackTrace()
                isRunning.value = false
                lastExitCode.value = -1
                executionTime.value = ""
                outputText.value = "Error: ${e.message}"
                executionHistory.value.add("Error: ${e.message}\n")
            }
    }

    fun abortExecution(
        outputText: MutableState<String>,
        isRunning: MutableState<Boolean>,
        executionHistory: MutableState<MutableList<String>>
    ){
        if(process!= null && process!!.isAlive) {
            process?.destroy()
            process = null
            isRunning.value = false
            outputText.value += "\nExecution aborted.\n"
            executionHistory.value.add("Execution aborted.")
        }
    }
}
