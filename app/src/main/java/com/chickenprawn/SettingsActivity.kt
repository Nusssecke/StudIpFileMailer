package com.chickenprawn

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.preference.*
import androidx.work.WorkManager
import com.chickenprawn.background.FileSyncWorker
import com.chickenprawn.studip.StudIp
import com.chickenprawn.util.PermissionActivity
import com.chickenprawn.util.createFileNotificationChannel
import es.jlarriba.jrmapi.Authentication
import es.jlarriba.jrmapi.Jrmapi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class SettingsActivity : AppCompatActivity() {

    // TODO Order settings into better categories
    // TODO Add preference to disable/enable background worker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())

            // Set the real name from StudIp
            val realName: EditTextPreference = findPreference("real_name")!!
            this.lifecycleScope.launch(Dispatchers.IO){
                realName.text = StudIp().realName
            }

            // Set onclick for logout preference
            val logOutPreference: Preference = findPreference("logout")!!
            logOutPreference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                // Remove stored username and password
                sharedPreferences.edit().putString("username", "").apply()
                sharedPreferences.edit().putString("password", "").apply()

                // Move to login activity to force new login
                this.lifecycleScope.launch(Dispatchers.Main) {
                    val loginActivityIntent = Intent(requireContext(), LoginActivity::class.java)
                    startActivity(loginActivityIntent)
                }

                true // Click was handled
            }

            // Verify email fields
            val emailPreference1: EditTextPreference = findPreference("email_address_1")!!
            val emailPreference2: EditTextPreference = findPreference("email_address_2")!!

            // Set email suitable keyboard
            emailPreference1.setOnBindEditTextListener { editText ->
                editText.inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            }
            emailPreference2.setOnBindEditTextListener { editText ->
                editText.inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            }

            val summaryProvider = Preference.SummaryProvider<EditTextPreference>{ preference ->
                val emailAddress = preference.text
                // Check if the email is valid and return error or email address accordingly
                if (android.util.Patterns.EMAIL_ADDRESS.matcher(emailAddress.toString()).matches()){
                    emailAddress
                } else {
                    resources.getString(R.string.email_address_invalid)
                }
            }
            emailPreference1.summaryProvider = summaryProvider
            emailPreference2.summaryProvider = summaryProvider

            // Remarkable
            val remarkablePreference: EditTextPreference = findPreference("login_remarkable")!!
            remarkablePreference.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, _ ->
                this.lifecycleScope.launch(Dispatchers.IO){
                    val oneTimeCode = remarkablePreference.text
                    val deviceToken = Authentication().registerDevice(oneTimeCode, UUID.randomUUID())
                    Log.d("SettingsActivty", "remarkable device token is: $deviceToken")

                    if (!deviceToken.isNullOrBlank()){
                        remarkablePreference.summary = resources.getString(R.string.remarkable_logged_in)
                    } else {
                        Toast.makeText(context, getString(R.string.remarkable_code_invalid), Toast.LENGTH_SHORT).show()
                    }
                }
                true
            }

            // Set all known courses to be selected, for which files should be downloaded
            val coursesPreference: MultiSelectListPreference = findPreference("selected_courses")!!
            coursesPreference.entryValues = sharedPreferences.getStringSet("known_courses", setOf())!!.toTypedArray()
            coursesPreference.entries = sharedPreferences.getStringSet("known_courses", setOf())!!.toTypedArray()

            val downloadCellularPreference: CheckBoxPreference = findPreference("download_cellular")!!
            downloadCellularPreference.onPreferenceChangeListener = Preference.OnPreferenceChangeListener{
                _, _ ->
                // Restart worker job if the network preferences have changed
                FileSyncWorker.executePeriodically(this.requireContext().applicationContext)
                true
            }

            val synchroniseBackgroundPreference: CheckBoxPreference = findPreference("background_sync")!!
            synchroniseBackgroundPreference.onPreferenceChangeListener = Preference.OnPreferenceChangeListener{
                _: Preference, _: Any ->
                // isChecked holds the status before the change
                if(synchroniseBackgroundPreference.isChecked){
                    // The box was checked and is now unchecked, therefore disable all Workers
                    WorkManager.getInstance(requireContext()).cancelAllWorkByTag(FileSyncWorker.PERIODIC_UPDATE_WORKER_TAG)
                    Log.d("SettingsActivity", "Canceled all background work")
                } else {
                    // The box was not checked and will now be enabled, start background work
                    FileSyncWorker.checkAndStartPeriodicalWork(requireContext())
                    Log.d("SettingsActivity", "Started background work")
                }
                true
            }

            val notificationsPreference: CheckBoxPreference = findPreference("file_notification")!!
            notificationsPreference.onPreferenceChangeListener = Preference.OnPreferenceChangeListener{
                _,_ ->
                val notificationManager: NotificationManager =
                    requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                // For Android 12 request permission at runtime
                if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)) {
                    val intent = Intent(requireContext(), PermissionActivity::class.java)
                    startActivity(intent)
                }
                // If the permission was denied uncheck the checkbox and notify the user
                if (!notificationManager.areNotificationsEnabled()){
                    Toast.makeText(requireContext(), resources.getString(R.string.notification_permission_not_given), Toast.LENGTH_LONG).show()
                    return@OnPreferenceChangeListener false
                }
                // Create notification channel
                createFileNotificationChannel(requireContext())

                true
            }

            // Once CoursesPreferences are selected, update known files form the course so that they don't get downloaded all at once
            coursesPreference.onPreferenceChangeListener = Preference.OnPreferenceChangeListener{ _: Preference, _: Any ->
                this.lifecycleScope.launch(Dispatchers.IO) {
                    val studIp = StudIp()
                    for (course in studIp.getCourses()){
                        if (course.name in coursesPreference.values){
                            Log.d("SettingsActivity", "Setting files to known for: ${course.name}")
                            course.setFilesAsKnown(preferenceManager.sharedPreferences!!)
                        }
                    }
                    Log.d("SettingsActivity", "Finished renewing files!")
                }
                true
            }

            // If the preference is clicked and no courses are know
            coursesPreference.onPreferenceClickListener = Preference.OnPreferenceClickListener{
                this.lifecycleScope.launch(Dispatchers.IO) {
                    if (coursesPreference.entries.isEmpty()){
                        Log.d("SettingsActivity", "No courses are known")
                        lifecycleScope.launch(Dispatchers.Main){
                            Toast.makeText(context, getString(R.string.starting_reloading_courses), Toast.LENGTH_LONG).show()
                        }
                        // Update courses from StudIp
                        val studIp = StudIp()
                        if (studIp.loginSuccessful){
                            for (course in studIp.getCourses()){
                                course.setCourseAsKnown(sharedPreferences)
                            }
                        }
                        Log.d("SettingsActivity", "Finished loading courses")
                        // TODO Is there no better way?
                        // Update values
                        coursesPreference.entryValues = sharedPreferences.getStringSet("known_courses", setOf())!!.toTypedArray()
                        coursesPreference.entries = sharedPreferences.getStringSet("known_courses", setOf())!!.toTypedArray()
                        lifecycleScope.launch(Dispatchers.Main){
                            Toast.makeText(context, getString(R.string.finished_reloading_courses), Toast.LENGTH_LONG).show()
                        }
                    } else {
                        // Make the user reopen the dialog
                        Log.d("SettingsActivity", "There are known courses")
                    }
                }
                true
            }

            // Clear courses and known file data
            val clearCoursesPreference: Preference = findPreference("clear_courses")!!
            clearCoursesPreference.onPreferenceClickListener = Preference.OnPreferenceClickListener {

                Log.d("StudIpMailer","Cleared ALL Courses and Files")

                preferenceManager.sharedPreferences?.edit()
                    ?.putStringSet("known_courses", setOf())
                    ?.putStringSet("selected_courses", setOf())
                    ?.putStringSet("known_files", setOf())
                    ?.apply()

                coursesPreference.entryValues = arrayOf()
                coursesPreference.entries = arrayOf()
                true
            }

        }
    }
}