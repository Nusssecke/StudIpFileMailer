package com.chickenprawn.studip

import android.content.SharedPreferences
import android.util.Log
import com.chickenprawn.studip.StudIp.Companion.courseUrlToFileUrl
import com.chickenprawn.studip.StudIpFolder
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup

val json = Json { coerceInputValues = true }

// Treat Course as Folder without some information
class StudIpCourse(name: String, courseUrl: String):
    StudIpFolder(
        "", "folder-full", name, courseUrlToFileUrl(courseUrl), "",
        "", "", 0,
        "", "CourseFolder", "rd", listOf()
    ),
    java.io.Serializable {

    // Add course to the list of known courses
    fun setCourseAsKnown(preferences: SharedPreferences){
        val knownCourses = HashSet(preferences.getStringSet("known_courses", setOf())!!)
        if (name !in knownCourses){
            knownCourses.add(name)
        }
        preferences.edit().putStringSet("known_courses", knownCourses).apply()
    }

    // Adds all files of the course to the list of known files
    fun setFilesAsKnown(preferences: SharedPreferences) {
        // Copy already known files
        val knownFiles = HashSet(preferences.getStringSet("known_files", setOf())!!)

        // Check for each file if it is in the list of known files
        for (file in allFiles){
            // Add the file if it hasn't been seen yet
            if ("$identifier@${file.identifier}" !in preferences.getStringSet("known_files", setOf())!!){
                knownFiles.add("$identifier@${file.identifier}")
            }
        }
        preferences.edit().putStringSet("known_files", knownFiles).apply()
    }

}
