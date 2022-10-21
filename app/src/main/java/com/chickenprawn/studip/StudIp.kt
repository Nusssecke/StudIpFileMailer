package com.chickenprawn.studip

import android.util.Log
import okhttp3.*
import org.jsoup.Jsoup
import java.net.*

// Represents a logged in StudIp session
class StudIp private constructor(private val username: String, private val password: String) {

    // StudIp Singleton
    companion object {
        private var serverName = "https://studip.uni-giessen.de"

        private var studIpSingleton: StudIp? = null
        // TODO Make non null?
        var username: String? = null
        var password: String? = null
        operator fun invoke(createNewSessionAnyway: Boolean = false): StudIp {
            // If the username and password weren't changed, return the already existing singleton
            if ( (studIpSingleton != null) and (studIpSingleton?.username == username) and (studIpSingleton?.password == password) and !createNewSessionAnyway){
                // Log.d("StudIp", "Returned already created singleton")
            } else {
                // Create a new StudIp session with the new session
                studIpSingleton = StudIp(username!!, password!!)
            }
            return studIpSingleton!!
        }

        fun courseUrlToFileUrl(courseUrl: String): String {
            // Extract course id (string of characters and numbers) from url
            val courseId = courseUrl.split("=")[1]
            // Make fileUrl
            return "$serverName/dispatch.php/course/files?cid=$courseId"
        }
    }

    var client: OkHttpClient
    private val cookieManager = CookieManager()

    var realName = "Default"
    val loginSuccessful: Boolean

    init {
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL)

        client = OkHttpClient.Builder()
            .cookieJar(JavaNetCookieJar(cookieManager))
            .build()

        cookieManager.cookieStore
            .add(URI("studip.uni-giessen.de"), HttpCookie("cache_session", System.currentTimeMillis().toString()))


        if ( Companion.username.isNullOrBlank() or Companion.password.isNullOrBlank()) {
            // Setup dummy object if no valid values have been set yet
            loginSuccessful = false
        } else if (Companion.username!!.contains("gop")) {
            serverName = "http://chickenprawn.com"
            // Use proxy
            loginSuccessful = login(username, password)
        } else {
            loginSuccessful = login(username, password)

        }
    }

    private fun login(username: String, password: String): Boolean {

        // Call StudIp login page to get session token and cookies
        val loginPageUrl = "$serverName/"
        val loginPageRequest = Request.Builder().url(loginPageUrl).build()
        val loginPageResponse: Response?
        try {
            loginPageResponse = client.newCall(loginPageRequest).execute()
        } catch (e: SocketTimeoutException){
            Log.d("StudIp", "SocketTimeout: username:$username, password:$password, Error Text:${e.message}")
            return false
        }

        var securityToken = ""
        var loginTicket = ""
        // Extract security token and login ticket from webpage
        val document = Jsoup.parse(loginPageResponse.body?.string()?: "")
        loginPageResponse.close()
        val inputFields = document.select("input[name]").select("[type=hidden]")
        for (inputField in inputFields) {
            if (inputField.attr("name") == "security_token"){
                securityToken = inputField.attr("value")
            }
            if (inputField.attr("name") == "login_ticket"){
                loginTicket = inputField.attr("value")
            }
        }

        // Make form post to log in url
        val loginPostUrl = "$serverName/index.php?logout=true&set_language=de_DE"
        val formBody = FormBody.Builder()
            .add("loginname", username)
            .add("password", password)
            .add("security_token", securityToken)
            .add("login_ticket", loginTicket)
            .add("resolution", "1280x1024")
            .add("device_pixel_ratio", "1")
            .add("Login", "")
            .build()

        val request = Request.Builder()
            .url(loginPostUrl)
            .post(formBody)
            .build()

        val response = client.newCall(request).execute()
        val websiteHtml = response.body?.string()?: "" // Receives the main StudIp page as a response to the login form
        response.close()
        nameFromMainPage(websiteHtml) // Extract the full name of the user

        // Check if the connection was successful
        return websiteHtml.contains("Meine Veranstaltungen")
    }

    // Gets the actual name for the username
    private fun nameFromMainPage(websiteHtml: String){
        val element = Jsoup.parse(websiteHtml).select("img[class]").select("[class=avatar-medium user-$username recolor]")
        realName = element.attr("title")
    }

    // Creates a list of course objects from the elements of
    fun getCourses(): List<StudIpCourse> {
        // TODO Optimize
        // Make request to courses page to read out semester selection
        val coursesUrlFirst = "$serverName/dispatch.php/my_courses/index"
        val coursesRequestFirst = Request.Builder().url(coursesUrlFirst).build()
        val coursesResponseFirst = client.newCall(coursesRequestFirst).execute()

        // Extract the semester key from the courses page
        val semesterKey = selectedSemesterFromCoursesPage(coursesResponseFirst.body?.string() ?: "")
        coursesResponseFirst.close()

        // Change semester selection to all to get all courses, returns the courses page
        val coursesResponse = selectSemester("all")
        val documentRoot = Jsoup.parse(coursesResponse.body?.string() ?: "")

        val tableRow = documentRoot.select("table").select("tbody").select("tr")
        coursesResponse.close()

        // Reset courses selection on StudIp page
        selectSemester(semesterKey)

        // Select wrapper element
        return tableRow
                .map { it.select("td[style=text-align: left]").select("a").first() }
                .map { StudIpCourse(it?.ownText() ?: "", it?.attr("href")!!) }
    }

    private fun selectedSemesterFromCoursesPage(websiteHtml: String): String{
        // Read out the selection from the sidebar dropdown
        val element = Jsoup.parse(websiteHtml).select("select.sidebar-selectlist").select("option[selected]")
        return element.attr("value")
    }

    private fun selectSemester(semesterKey: String): Response {
        // Possible values: all, current, future, last, lastandnext and semester values (length 32)
        val courseChangeUrl = "$serverName/dispatch.php/my_courses/set_semester?sem_select="
        val courseChangeRequest = Request.Builder().url(courseChangeUrl + semesterKey).build()
        return client.newCall(courseChangeRequest).execute() // The response from the course change request is the courses page
    }
    
}
