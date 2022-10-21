package com.chickenprawn.background

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.preference.PreferenceManager
import com.chickenprawn.R
import com.chickenprawn.mail.MailSender
import com.chickenprawn.studip.StudIpFile

const val ACTION_DOWNLOAD_FILE_TO_PHONE = "DOWNLOAD_FILE_TO_PHONE"
const val ACTION_SEND_TO_EMAIL_1 = "SEND_TO_EMAIL_1"
const val ACTION_SEND_TO_EMAIL_2 = "SEND_TO_EMAIL_2"
const val EXTRA_STUD_IP_FILE = "STUD_IP_FILE_EXTRA"

class FileBroadcastReceiver: BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if ((intent != null) and (context != null)) {

            Log.d("FileBroadcastReceiver", "Broadcast Action: ${intent!!.action}")

            // Get the file out of the intent
            val studIpFile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getSerializableExtra(EXTRA_STUD_IP_FILE, StudIpFile::class.java)
            } else {
                intent.getSerializableExtra(EXTRA_STUD_IP_FILE) as StudIpFile
            }
            if (studIpFile == null){
                return
            }
            Log.d("FileBroadcastReceiver", "Broadcast Passed StudIpFile: ${studIpFile.name}")

            when (intent.action){
                ACTION_DOWNLOAD_FILE_TO_PHONE -> {
                    studIpFile.download(context!!)
                    Toast.makeText(context, context.getString(R.string.downloading_file), Toast.LENGTH_LONG).show()
                }
                ACTION_SEND_TO_EMAIL_1 -> {
                    val preferences = PreferenceManager.getDefaultSharedPreferences(context!!.applicationContext)
                    val emailAddress = preferences.getString("email_address_1", "")!!
                    if (android.util.Patterns.EMAIL_ADDRESS.matcher(emailAddress).matches()) {
                        MailSender().downloadAndSend(
                            context.applicationContext,
                            emailAddress,
                            studIpFile
                        )
                        Toast.makeText(context, context.getString(R.string.sending_email), Toast.LENGTH_LONG).show()
                    }
                }
                ACTION_SEND_TO_EMAIL_2 -> {
                    val preferences = PreferenceManager.getDefaultSharedPreferences(context!!.applicationContext)
                    val emailAddress = preferences.getString("email_address_2", "")!!
                    if (android.util.Patterns.EMAIL_ADDRESS.matcher(emailAddress).matches()) {
                        MailSender().downloadAndSend(
                            context.applicationContext,
                            emailAddress,
                            studIpFile
                        )
                        Toast.makeText(context, context.getString(R.string.sending_email), Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

}