package com.chickenprawn

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.chickenprawn.background.FileSyncWorker
import com.chickenprawn.fileSelect.FileActivity
import com.chickenprawn.studip.StudIp
import com.chickenprawn.util.connectionIsWifi
import es.jlarriba.jrmapi.Jrmapi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.io.File
import java.nio.charset.Charset


class MainActivity : AppCompatActivity() {

    private var filesButton: ImageButton? = null
    private var syncButton: ImageButton? = null
    private var settingsButton: ImageButton? = null

    // TODO Fix connection leakage
    // TODO Test internet states java.net.SocketTimeoutException: timeout occurs sometimes
    // TODO Add info for sending email and time delay for refresh (60 minutes), Warn about battery optimization
    // TODO fix internet: remove onLost?

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        filesButton = findViewById(R.id.filesButton)
        syncButton = findViewById(R.id.syncButton)
        settingsButton = findViewById(R.id.settingsButton)

        // Check periodic worker status and start if necessary
        val preferences = PreferenceManager.getDefaultSharedPreferences(this.applicationContext)
        if (preferences.getBoolean("background_sync", false)){
            FileSyncWorker.checkAndStartPeriodicalWork(this)
        }

    }


    fun testing(view: View){
        val filesDir = this.filesDir
        MainScope().launch(Dispatchers.IO) {
            Jrmapi("a", filesDir)
        }
    }

    fun openSettings(view: View){
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    fun openFiles(view: View){
        if (!StudIp().loginSuccessful){
            Toast.makeText(this, resources.getString(R.string.not_logged_in), Toast.LENGTH_LONG).show()
            return
        }

        val intent = Intent(this, FileActivity::class.java)
        startActivity(intent)
    }

    fun runWorker(view: View){
        if (!StudIp().loginSuccessful){
            Toast.makeText(this, resources.getString(R.string.not_logged_in), Toast.LENGTH_LONG).show()
            return
        }

        val preferences = PreferenceManager.getDefaultSharedPreferences(this.applicationContext)

        // Check if the current network is over wifi and compare with preference
        if (!connectionIsWifi(this) and !preferences.getBoolean("download_cellular", false) ){
            // If network is not wifi return
            Toast.makeText(applicationContext, resources.getString(R.string.wifi_not_connected), Toast.LENGTH_SHORT).show()
            return
        }

        // Disable button
        syncButton!!.isEnabled = false

        Toast.makeText(applicationContext, resources.getString(R.string.sync_in_progress), Toast.LENGTH_SHORT).show()
        FileSyncWorker.executeOnce(this)

        // Re-enable button after one second to prevent spamming
        syncButton!!.postDelayed({syncButton!!.isEnabled = true}, 3*1000)
    }
}