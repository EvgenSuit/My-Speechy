package com.myspeechy.myspeechy.domain

import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class DateFormatter {
    companion object {
        fun convertFromTimestamp(timestamp: Long): String {
            var targetDateFormat = ""
            val currentDate = LocalDateTime.now()
            val messageDateFormatted = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
            if (currentDate.year != messageDateFormatted.year) targetDateFormat += "yyyy "
            if (currentDate.dayOfMonth != messageDateFormatted.dayOfMonth) targetDateFormat += "MMM dd "
            targetDateFormat += "HH:mm"
            return SimpleDateFormat(targetDateFormat, Locale.getDefault()).format(timestamp)
        }
        fun convertFromUtcThoughtTracker(date: String): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val parsedDate = sdf.parse(date) ?: return ""
            var targetDateFormat = ""
            val currentDate = LocalDateTime.now()
            val dateFormatted = Instant.ofEpochMilli(parsedDate.time).atZone(ZoneId.systemDefault()).toLocalDate()
            if (currentDate.year != dateFormatted.year) targetDateFormat += "yyyy "
            return if (currentDate.year == dateFormatted.year &&
                currentDate.month == dateFormatted.month &&
                currentDate.dayOfMonth == dateFormatted.dayOfMonth) "Today"
            else {
                val targetFormatter = DateTimeFormatter.ofPattern("${targetDateFormat}MMM dd")
                dateFormatted.format(targetFormatter)
            }
        }


    }
}