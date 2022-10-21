package com.chickenprawn.util

import android.content.Intent
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.activity.OnBackPressedCallback
import com.chickenprawn.LoginActivity
import com.chickenprawn.R

class NoInternetActivity : AppCompatActivity() {

    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: NetworkCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_no_internet)

        // Remove back press
        onBackPressedDispatcher.addCallback(this , object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Back is pressed do nothing
            }
        })

        connectivityManager = getSystemService(ConnectivityManager::class.java)
        // If network is gained move to the main activity
        networkCallback = object : NetworkCallback(){
            override fun onCapabilitiesChanged(network : Network, networkCapabilities : NetworkCapabilities) {
                if(phoneIsOnline(this@NoInternetActivity)){
                    Log.d("NoInternetActivity", "Phone back online!")
                    val loginActivityIntent = Intent(this@NoInternetActivity, LoginActivity::class.java)
                    startActivity(loginActivityIntent)
                }
            }
        }
        connectivityManager!!.registerDefaultNetworkCallback(networkCallback!!)

    }

    override fun onDestroy() {
        super.onDestroy()
        networkCallback?.let { connectivityManager?.unregisterNetworkCallback(it) }
    }
}