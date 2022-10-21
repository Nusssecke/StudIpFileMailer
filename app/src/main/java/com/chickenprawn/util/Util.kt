package com.chickenprawn.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkCapabilities.NET_CAPABILITY_NOT_METERED
import android.net.NetworkCapabilities.TRANSPORT_WIFI
import android.util.Log
import com.chickenprawn.R
import okio.BufferedSource
import okio.buffer
import okio.sink
import java.io.File
import java.io.OutputStream
import java.text.DecimalFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.log10
import kotlin.math.pow

// Takes the size in bytes and converts it into human readable format
fun readableFileSize(size: Int): String {
    if (size <= 0) return "0"
    val units = arrayOf("B", "kB", "MB", "GB", "TB")
    val digitGroups = (log10(size.toDouble()) / log10(1024.0)).toInt()
    return DecimalFormat("#,##0.#").format(size / 1024.0.pow(digitGroups.toDouble()))
        .toString() + " " + units[digitGroups]
}

fun epochSecondsToLocalDateTime(chdate: Int): LocalDateTime {
    return Instant.ofEpochMilli(chdate.toLong() * 1000)
        .atZone(ZoneId.systemDefault()).toLocalDateTime()
}

fun phoneIsOnline(context: Context): Boolean {
    val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
    // If no network is connected return false
    val currentNetwork = connectivityManager.activeNetwork ?: return false

    val networkCapabilities = connectivityManager.getNetworkCapabilities(currentNetwork) ?: return false
    val linkProperties = connectivityManager.getLinkProperties(currentNetwork)

    return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) and
            networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}

// Check if the connection is wifi
fun connectionIsWifi(context: Context): Boolean {
    val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
    // If no network is connected return false
    val currentNetwork = connectivityManager.activeNetwork ?: return false

    val networkCapabilities = connectivityManager.getNetworkCapabilities(currentNetwork) ?: return false
    return networkCapabilities.hasTransport(TRANSPORT_WIFI)
}

const val FILE_NOTIFICATION_CHANNEL_ID = "FileNotifications"
fun createFileNotificationChannel(context: Context){
    val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // Check if the channel already exists and if so return
    if (notificationManager.getNotificationChannel(FILE_NOTIFICATION_CHANNEL_ID) != null){
        Log.d("Util", "NotificationChannel already exists")
        return
    }

    // Create the NotificationChannel this allows control to the user over one type of notification
    val name = context.getString(R.string.notification_channel_files_title)
    val descriptionText = context.getString(R.string.notification_channel_files_description)
    val importance = NotificationManager.IMPORTANCE_DEFAULT
    val channel = NotificationChannel(FILE_NOTIFICATION_CHANNEL_ID, name, importance).apply {
        description = descriptionText
    }
    // Register the channel with the system
    notificationManager.createNotificationChannel(channel)
}

/**
 * Stolen from Tachiyomi Util Storage
 * Saves the given source to a file and closes it. Directories will be created if needed.
 *
 * @param file the file where the source is copied.
 */
fun BufferedSource.saveTo(file: File) {
    try {
        // Create parent dirs if needed
        file.parentFile?.mkdirs()

        // Copy to destination
        saveTo(file.outputStream())
    } catch (e: Exception) {
        close()
        file.delete()
        throw e
    }
}

/**
 * Saves the given source to an output stream and closes both resources.
 *
 * @param stream the stream where the source is copied.
 */
fun BufferedSource.saveTo(stream: OutputStream) {
    use { input ->
        stream.sink().buffer().use {
            it.writeAll(input)
            it.flush()
        }
    }
}