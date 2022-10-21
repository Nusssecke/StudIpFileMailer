package com.chickenprawn.background

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.preference.PreferenceManager
import com.chickenprawn.R
import androidx.work.*
import com.chickenprawn.mail.MailSender
import com.chickenprawn.studip.StudIp
import com.chickenprawn.util.FILE_NOTIFICATION_CHANNEL_ID
import com.chickenprawn.util.readableFileSize
import java.util.concurrent.TimeUnit

class FileSyncWorker(appContext: Context, workerParams: WorkerParameters): Worker(appContext, workerParams) {

    override fun doWork(): Result {
        Log.d("StudIpFileMailer", "StudIp Download Background Worker started")

        val preferences = PreferenceManager.getDefaultSharedPreferences(this.applicationContext)
        StudIp.username = preferences.getString("username", "")!!
        StudIp.password = preferences.getString("password", "")!!

        val studIp = StudIp()
        if (!studIp.loginSuccessful){
            Log.d("StudIpFileMailer: Worker", "StudIpLogin not successful")
            return Result.failure()
        }

        val studIpCourses = studIp.getCourses()
        for (course in studIpCourses){
            course.setCourseAsKnown(preferences)
        }

        for (course in studIpCourses){
            // Check if the courses is selected and its files should be downloaded
            if (course.name in preferences.getStringSet("selected_courses", setOf())!!){

                for (file in course.allFiles){

                    // Check the file has the correct file ending e.g pdf (no videos)
                    if (file.mime_type in preferences.getStringSet("download_file_types", setOf())!!){

                        // Check if the file has already been seen
                        if ("${course.identifier}@${file.identifier}" in preferences.getStringSet("known_files", setOf())!!){
                            Log.d("StudIpFileMailer: Worker", "Known File: ${course.name}@${file.name}; Mime Type ${file.mime_type}")
                        } else {
                            Log.d("StudIpFileMailer: Worker", "Unknown File: ${file.name}; Mime Type ${file.mime_type}")

                            if (preferences.getBoolean("download_local", false)) {
                                file.download(this.applicationContext)
                            }
                            if (preferences.getBoolean("email_address_1_sync", false)){
                                val emailAddress = preferences.getString("email_address_1", "")!!
                                if (android.util.Patterns.EMAIL_ADDRESS.matcher(emailAddress).matches()){
                                    MailSender().downloadAndSend(this.applicationContext, emailAddress, file)
                                }
                            }
                            if (preferences.getBoolean("email_address_2_sync", false)){
                                val emailAddress = preferences.getString("email_address_2", "")!!
                                if (android.util.Patterns.EMAIL_ADDRESS.matcher(emailAddress).matches()){
                                    MailSender().downloadAndSend(this.applicationContext, emailAddress, file)
                                }
                            }
                            if (preferences.getBoolean("file_notification", false)){
                                val notificationTitle = this.applicationContext.getString(R.string.notification_file_title).format(file.name)
                                val notificationBody = this.applicationContext.getString(R.string.notification_file_body).format(course.name, readableFileSize(file.size))

                                // Setting up intents for buttons on notification
                                // Download file from notification
                                val downloadFileIntent = Intent(applicationContext, FileBroadcastReceiver::class.java).apply {
                                    action = ACTION_DOWNLOAD_FILE_TO_PHONE
                                    putExtra(EXTRA_STUD_IP_FILE, file)
                                }
                                val downloadFilePendingIntent: PendingIntent =
                                    PendingIntent.getBroadcast(this.applicationContext, System.currentTimeMillis().toInt(), downloadFileIntent, FLAG_IMMUTABLE)

                                // Send file to email1
                                val sendFileToEmail1Intent = Intent(applicationContext, FileBroadcastReceiver::class.java).apply {
                                    action = ACTION_SEND_TO_EMAIL_1
                                    putExtra(EXTRA_STUD_IP_FILE, file)
                                }
                                val sendFileToEmail1PendingIntent: PendingIntent =
                                    PendingIntent.getBroadcast(this.applicationContext, System.currentTimeMillis().toInt(), sendFileToEmail1Intent, FLAG_IMMUTABLE)

                                // Send file to email2
                                val sendFileToEmail2Intent = Intent(applicationContext, FileBroadcastReceiver::class.java).apply {
                                    action = ACTION_SEND_TO_EMAIL_2
                                    putExtra(EXTRA_STUD_IP_FILE, file)
                                }
                                val sendFileToEmail2PendingIntent: PendingIntent =
                                    PendingIntent.getBroadcast(this.applicationContext, System.currentTimeMillis().toInt(), sendFileToEmail2Intent, FLAG_IMMUTABLE)

                                // Build notification
                                val notificationBuilder = NotificationCompat.Builder(this.applicationContext, FILE_NOTIFICATION_CHANNEL_ID)
                                    .setSmallIcon(R.drawable.studip_mail)
                                    .setContentTitle(notificationTitle)
                                    .setContentText(notificationBody)

                                // Depending on the settings not all buttons should be displayed. Sending the file via email twice is unnecessary
                                if (!preferences.getBoolean("download_local", true)){
                                    notificationBuilder.addAction(R.drawable.download, this.applicationContext.getString(R.string.notification_download_button), downloadFilePendingIntent)
                                }
                                if (!preferences.getBoolean("email_address_1_sync", true)){
                                    notificationBuilder.addAction(R.drawable.email, this.applicationContext.getString(R.string.send_to_email_1), sendFileToEmail1PendingIntent)
                                }
                                if (!preferences.getBoolean("email_address_2_sync", true)){
                                    notificationBuilder.addAction(R.drawable.email, this.applicationContext.getString(R.string.send_to_email_2), sendFileToEmail2PendingIntent)
                                }

                                // notificationId and requestCode are unique ids for later reference they are not needed here
                                with(NotificationManagerCompat.from(this.applicationContext)) {
                                    notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
                                }

                            }
                        }
                    }
                }
                course.setFilesAsKnown(preferences)
            }
        }

        return Result.success()
    }


    companion object {
        const val PERIODIC_UPDATE_WORKER_TAG = "periodicUpdate"

        fun executePeriodically(context: Context){
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)

            // The Worker needs Network connectivity and a network which is not metered (e.g Wifi)
            val constraintsBuilder: Constraints.Builder = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)

            // If download cellular is true the worker may run on a metered network
            if (!preferences.getBoolean("download_cellular", false)){
                constraintsBuilder.setRequiredNetworkType(NetworkType.UNMETERED)
            }
            val constraints = constraintsBuilder.build()

            // Remove all existing processes to avoid two running at once
            WorkManager.getInstance(context).cancelAllWorkByTag(PERIODIC_UPDATE_WORKER_TAG) // Hopefully redundant

            // Executes FileSyncWorker every 60 minutes
            val periodicWorkRequest = PeriodicWorkRequest.Builder(FileSyncWorker::class.java, 60, TimeUnit.MINUTES)
                .addTag(PERIODIC_UPDATE_WORKER_TAG)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueue(periodicWorkRequest)
        }

        fun executeOnce(context: Context){
            val fileSyncWorkRequest: WorkRequest = OneTimeWorkRequestBuilder<FileSyncWorker>().build()
            WorkManager.getInstance(context).enqueue(fileSyncWorkRequest)
        }

        // Check if the periodically running worker is running and start if not
        fun checkAndStartPeriodicalWork(context: Context){
            val workManager = WorkManager.getInstance(context)

            var oneActive = false
            for (work in workManager.getWorkInfosByTag(PERIODIC_UPDATE_WORKER_TAG).get()){
                if(work.state == WorkInfo.State.ENQUEUED){
                    oneActive = true
                    break
                }
            }
            if (!oneActive){
                Log.d("WorkManager", "Starting periodical worker")
                executePeriodically(context)
            }
        }
    }
}