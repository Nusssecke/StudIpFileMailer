package com.chickenprawn.util

import android.app.DownloadManager
import android.content.*
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.core.net.toFile
import androidx.core.net.toUri
import com.chickenprawn.studip.StudIp
import com.chickenprawn.studip.StudIpFile
import okhttp3.Request
import okio.BufferedSource
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.time.ZoneOffset


fun saveStudIpFile(context: Context, file: StudIpFile) {
    saveFile(context,
        downloadUrl = file.download_url,
        fileName = file.name,
        folderName = "", // TODO Add dynamic folder
        mime_type = file.mime_type,
        dateCreated = file.date.toEpochSecond(ZoneOffset.of("+02:00")),
        downloadTitle = file.name
        )
}

fun saveStudIpAsTempFile(context: Context, file: StudIpFile): File {
    return saveAsTempFile(context,
        downloadUrl = file.download_url,
        fileName = file.name
    )
}

private fun saveFile(context: Context, downloadUrl: String, fileName: String, folderName: String, mime_type: String, dateCreated: Long, downloadTitle: String) {
    val fileData = download(context, downloadUrl)

    val externalUri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    val relativeLocation = Environment.DIRECTORY_DOCUMENTS + "/" + folderName

    val contentValues = ContentValues()
    contentValues.put(MediaStore.Files.FileColumns.DISPLAY_NAME, fileName)
    contentValues.put(MediaStore.Files.FileColumns.MIME_TYPE, mime_type)
    contentValues.put(MediaStore.Files.FileColumns.TITLE, "Test")
    contentValues.put(MediaStore.Files.FileColumns.DATE_ADDED, System.currentTimeMillis() / 1000)
    contentValues.put(MediaStore.Files.FileColumns.RELATIVE_PATH, relativeLocation)
    contentValues.put(MediaStore.Files.FileColumns.DATE_TAKEN, System.currentTimeMillis())

    val fileUri: Uri = context.contentResolver.insert(externalUri, contentValues)!!
    try {
        val outputStream: OutputStream = context.contentResolver.openOutputStream(fileUri)!!
        fileData.saveTo(outputStream)
        outputStream.close()
    } catch (e: IOException) {
        e.printStackTrace()
    }
}

private fun saveAsTempFile(context: Context, downloadUrl: String, fileName: String): File {
    val fileData = download(context, downloadUrl)
    val tempFile = File.createTempFile(fileName.split(".")[0], fileName.split(".")[0], context.cacheDir)
    try {
        fileData.saveTo(tempFile)
    } catch (e: IOException) {
        e.printStackTrace()
    }
    return tempFile
}

private fun download(context: Context, downloadUrl: String): BufferedSource {
    val fileRequest = Request.Builder().url(downloadUrl).build()
    val fileResponse = StudIp().client.newCall(fileRequest).execute()
    return fileResponse.body!!.source()
}