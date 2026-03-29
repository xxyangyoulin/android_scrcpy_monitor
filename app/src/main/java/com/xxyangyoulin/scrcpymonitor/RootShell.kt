package com.xxyangyoulin.scrcpymonitor

object RootShell {
    fun isAvailable(): Boolean {
        return succeeded(run("id"))
    }

    fun run(script: String): Result {
        return try {
            val process = Runtime.getRuntime().exec("su")
            process.outputStream.bufferedWriter().use { writer ->
                writer.write(script)
                writer.newLine()
                writer.write("exit")
                writer.newLine()
            }
            val output = process.inputStream.bufferedReader().readText().trim()
            if (process.waitFor() == 0) {
                Result.Success(output)
            } else {
                Result.Failure
            }
        } catch (_: Exception) {
            Result.Failure
        }
    }

    fun run(commands: List<String>): Result {
        return run(commands.joinToString("\n"))
    }

    sealed interface Result {
        data class Success(val output: String) : Result
        data object Failure : Result
    }

    fun succeeded(result: Result): Boolean {
        return result is Result.Success
    }

    fun outputOrNull(result: Result): String? {
        return (result as? Result.Success)?.output
    }
}
