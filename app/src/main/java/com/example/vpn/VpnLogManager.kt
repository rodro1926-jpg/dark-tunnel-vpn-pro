package com.example.vpn

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class LogLevel {
    INFO,
    SUCCESS,
    WARNING,
    ERROR
}

data class LogEntry(
    val id: String = "log-" + System.currentTimeMillis() + "-" + (0..999).random(),
    val timestamp: String = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date()),
    val level: LogLevel = LogLevel.INFO,
    val message: String
)

object VpnLogManager {
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    fun log(message: String, level: LogLevel = LogLevel.INFO) {
        val entry = LogEntry(
            id = "log-" + System.currentTimeMillis() + "-" + (0..9999).random(),
            timestamp = dateFormat.format(Date()),
            level = level,
            message = message
        )
        _logs.update { current ->
            // Keep latest 300 logs
            val updated = current.toMutableList()
            updated.add(0, entry)
            if (updated.size > 300) {
                updated.subList(0, 300)
            } else {
                updated
            }
        }
    }

    fun clearLogs() {
        _logs.value = emptyList()
        log("Logs cleared by user", LogLevel.INFO)
    }
}
