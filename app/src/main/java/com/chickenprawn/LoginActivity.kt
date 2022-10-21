package com.chickenprawn

import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Network
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.preference.PreferenceManager
import com.chickenprawn.studip.StudIp
import com.chickenprawn.util.NoInternetActivity
import com.chickenprawn.util.phoneIsOnline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private var preferences: SharedPreferences? = null

    private var usernameEditText: EditText? = null
    private var passwordEditText: EditText? = null
    private var logInButton: Button? = null
    private var progressBar: ProgressBar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen() // Login activity is start into app so setup splash screen

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Register network callbacks
        setUpInternetCallbacks()
        // Check if network is connected
        if (!phoneIsOnline(this)){
            Log.d("LoginActivity", "No internet!")
            val noInternetActivity = Intent(this, NoInternetActivity::class.java)
            startActivity(noInternetActivity)
            return // Makes sure that no further code is run; Internet connection attempt crashes app
        }

        usernameEditText = findViewById(R.id.username)
        passwordEditText = findViewById(R.id.password)
        logInButton = findViewById(R.id.login)
        progressBar = findViewById(R.id.loading)

        // Load preferences
        preferences = PreferenceManager.getDefaultSharedPreferences(this)
        StudIp.username = preferences!!.getString("username", "")!!
        StudIp.password = preferences!!.getString("password", "")!!

        // Set last used values in text fields
        usernameEditText!!.setText(preferences!!.getString("username", ""), TextView.BufferType.EDITABLE)
        passwordEditText!!.setText(preferences!!.getString("password", ""), TextView.BufferType.EDITABLE)

        // Check if text was changed and update login button
        val textWatcher = object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {return}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {return}
            override fun afterTextChanged(s: Editable?) {
                updateLoginButton()
            }
        }
        // Set listener on both input fields
        usernameEditText!!.addTextChangedListener(textWatcher)
        passwordEditText!!.addTextChangedListener(textWatcher)

        // Initialize StudIp singleton
        MainScope().launch(Dispatchers.IO) {
            val studIp = StudIp(createNewSessionAnyway = true) // Create a NEW StudIp Session,
            if (studIp.loginSuccessful){
                // Launch MainActivity
                toMainActivity()
            } else {
                Log.d("LoginActivity", "Login unsuccessful; Showing Input Fields")
                switchLoadingState()
            }
        }

    }

    // Enable the login button if both text fields contain text
    fun updateLoginButton(){
        logInButton!!.isEnabled = (
                    usernameEditText!!.text.isNotEmpty()
                and passwordEditText!!.text.isNotEmpty()
                and usernameEditText!!.text.isNotBlank()
                and passwordEditText!!.text.isNotBlank())
    }

    fun login(view: View){
        StudIp.username = usernameEditText!!.text.toString()
        StudIp.password = passwordEditText!!.text.toString()
        Log.d("LoginActivity", "Login attempt: username: ${usernameEditText!!.text}; password ${passwordEditText!!.text}")
        switchLoadingState() // Show loading indicator to prevent repeated button presses
        MainScope().launch(Dispatchers.IO) {
            val studIp = StudIp()
            if (studIp.loginSuccessful){
                // Store input username and password
                preferences!!.edit().putString("username", StudIp.username).apply()
                preferences!!.edit().putString("password", StudIp.password).apply()

                // Launch MainActivity
                toMainActivity()
            } else {
                MainScope().launch(Dispatchers.Main) {
                    Toast.makeText(
                        this@LoginActivity,
                        getString(R.string.incorrect_credentials),
                        Toast.LENGTH_LONG
                    ).show()
                }
                switchLoadingState() // Login unsuccessful, show text fields again
            }
        }
    }

    private var loadingState: Boolean = true
    // Switch between showing the loading animation and the input fields
    private fun switchLoadingState(){
        MainScope().launch(Dispatchers.Main) {
            if (loadingState) {
                progressBar!!.visibility = View.GONE
                usernameEditText!!.visibility = View.VISIBLE
                passwordEditText!!.visibility = View.VISIBLE
                logInButton!!.visibility = View.VISIBLE
            } else {
                progressBar!!.visibility = View.VISIBLE
                usernameEditText!!.visibility = View.GONE
                passwordEditText!!.visibility = View.GONE
                logInButton!!.visibility = View.GONE
            }
            loadingState = !loadingState
        }
    }

    private fun toMainActivity(){
        MainScope().launch(Dispatchers.Main){
            val mainActivityIntent = Intent(this@LoginActivity.applicationContext, MainActivity::class.java)
            startActivity(mainActivityIntent)
        }
    }

    private fun setUpInternetCallbacks(){
        // No matter where in the app, if the network connection is lost move to the NoInternetActivity
        val connectivityManager = getSystemService(ConnectivityManager::class.java)

        connectivityManager.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onLost(network : Network) {
                if(!phoneIsOnline(this@LoginActivity.applicationContext)){
                    Log.d("NetworkCallback", "Internet connection lost")
                    val noInternetActivity = Intent(this@LoginActivity.applicationContext, NoInternetActivity::class.java)
                    startActivity(noInternetActivity)
                }
            }
        })
    }

}