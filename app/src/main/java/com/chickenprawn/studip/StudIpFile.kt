package com.chickenprawn.studip

import android.content.Context
import com.chickenprawn.util.epochSecondsToLocalDateTime
import com.chickenprawn.util.saveStudIpFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import okhttp3.Request
import org.jsoup.Jsoup
import java.time.LocalDateTime

@Serializable
data class StudIpFile(
    private val id: String,
    val name: String,
    val download_url: String = "",
    val downloads: Int,
    val mime_type: String,
    val icon: String,
    val size: Int = 0, // Size in bytes
    private val author_url: String,
    private val author_name: String,
    private val author_id: String,
    private val chdate: Int, // This is the creation date in Epoch-Seconds
    private val additionalColumns: List<String>,
    private val details_url: String,
    val restrictedTermsOfUse: Boolean,
    private val actions: String,
    val new: Boolean,
    val isEditable: Boolean ): java.io.Serializable {

    override fun toString(): String {
        return "StudIpFile(name='$name', download_url='$download_url', mime_type='$mime_type', new=$new)"
    }

    val date: LocalDateTime
        get() {
            return epochSecondsToLocalDateTime(chdate)
        }

    val identifier: String
        get() {
            return id
        }

    fun download(context: Context) {
        val file = this
        CoroutineScope(Dispatchers.IO).launch {
            saveStudIpFile(context, file)
        }
    }
}

@Serializable
open class StudIpFolder(
    private val id: String,
    val icon: String,
    val name: String,
    private val url: String,
    private val user_id: String,
    private val author_name: String,
    private val author_url: String,
    private val chdate: Int, // This is the creation date in Epoch-Seconds
    private val actions: String,
    private val mime_type: String,
    private val permissions: String,
    private val additionalColumns: List<String>): java.io.Serializable {

    // Returns all descendants of the course, all subfolders etc.
    var allFiles: List<StudIpFile> = listOf()
        get() {
            if (field.isEmpty()){
                allFiles = files + folders.map { it.allFiles }.flatten()
            }
            return field
        }

    var files: List<StudIpFile> = listOf()
        get() {
            if(field.isEmpty()) {
                getChildren()
            }
            return field
        }

    var folders: List<StudIpFolder> = listOf()
        get() {
            if(field.isEmpty()) {
                getChildren()
            }
            return field
        }

    val date: LocalDateTime
        get() {
            return epochSecondsToLocalDateTime(chdate)
        }

    val identifier: String
        get() {
            return id
        }

    override fun toString(): String {
        return "StudIpFolder(name='$name', url='$url', mime_type='$mime_type', permissions='$permissions')"
    }

    // Gets all direct descendants of the course. Meaning all files and folders.
    private fun getChildren(){
        if (permissions!="rd"){
            files = listOf()
            folders = listOf()
            return
        }

        // Make request to the directory/files page
        val fileOverviewRequest = Request.Builder().url(url).build()
        val fileOverviewResponse = StudIp().client.newCall(fileOverviewRequest).execute()

        // Extract form which contains the file and folder entries
        val filesForm = Jsoup.parse(fileOverviewResponse.body?.string()?: "").select("form")

        // Extract folder and file data from the form
        val filesJson = filesForm.attr("data-files")
        files = json.decodeFromString<List<StudIpFile>>(filesJson)

        val foldersJson = filesForm.attr("data-folders")
        folders = json.decodeFromString<List<StudIpFolder>>(foldersJson)

        fileOverviewResponse.close()
    }
}