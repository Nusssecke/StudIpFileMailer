package com.chickenprawn.fileSelect

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.preference.PreferenceManager
import com.chickenprawn.R
import com.chickenprawn.mail.MailSender
import com.chickenprawn.studip.StudIpFile
import com.chickenprawn.util.readableFileSize
import java.time.format.DateTimeFormatter

@Composable
fun File(file: StudIpFile) {
    // Get the activity to be able to download files
    val context: Context? = LocalContext.current
    // We keep track if the file is expanded or not in this variable
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        elevation = elevation,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }
        // animateContentSize will change the Surface size gradually
        // .animateContentSize(),
    ) {
        val iconId = when (file.icon) {
            "file-pdf" -> R.drawable.file_pdf
            "file-text" -> R.drawable.file_text
            "file-word" -> R.drawable.file_word
            else -> R.drawable.clear
        }

        Row(modifier = Modifier.padding(all = 8.dp)) {

            Image(
                painter = painterResource(iconId),
                contentDescription = "File type picture",
                modifier = Modifier.size(imageSize)
            )

            // Add a horizontal space
            Spacer(modifier = Modifier.width(8.dp))

            Column() {
                Row() {
                    Text(
                        text = file.name,
                        color = MaterialTheme.colors.primary,
                        fontSize = textSizeHeader,
                    )

                }

                Row() {
                    val studIpFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

                    Text(
                        text = file.date.format(studIpFormatter),
                        color = MaterialTheme.colors.onSurface,
                        fontSize = textSizeSmall
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = readableFileSize(file.size),
                        color = MaterialTheme.colors.onSurface,
                        fontSize = textSizeSmall
                    )
                }

                if (isExpanded) {

                    // The buttons are shown when the file is clicked
                    Row() {
                        // Prevent the button from being clicked twice to not spam emails or downloads
                        var enabledDownload by remember { mutableStateOf(true) }
                        var enabledEmail1 by remember { mutableStateOf(true) }
                        var enabledEmail2 by remember { mutableStateOf(true) }

                        OutlinedButton(
                            onClick = {
                                enabledDownload = !enabledDownload
                                file.download(context = context!!.applicationContext)
                                Toast.makeText(context, context.applicationContext.resources.getString(R.string.downloading_file), Toast.LENGTH_LONG).show()
                            },
                            enabled = enabledDownload
                        ) {
                            Image(
                                painter = painterResource(R.drawable.download),
                                contentDescription = "Download file",
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                enabledEmail1 = !enabledEmail1
                                val preferences = PreferenceManager.getDefaultSharedPreferences(context!!.applicationContext)
                                val emailAddress = preferences.getString("email_address_1", "")!!
                                if (android.util.Patterns.EMAIL_ADDRESS.matcher(emailAddress).matches()){
                                    MailSender().downloadAndSend(context.applicationContext, emailAddress, file)

                                    Toast.makeText(context, context.applicationContext.resources.getString(R.string.email_sent), Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, context.applicationContext.resources.getString(R.string.email_address_invalid), Toast.LENGTH_LONG).show()
                                }
                                      },
                            enabled = enabledEmail1
                        ) {
                            Image(
                                painter = painterResource(R.drawable.email),
                                contentDescription = "Download file"
                            )
                            Text(text = stringResource(R.string.send_to_email_1))
                        }
                        OutlinedButton(
                            onClick = {
                                enabledEmail2 = !enabledEmail2
                                val preferences = PreferenceManager.getDefaultSharedPreferences(context!!.applicationContext)
                                val emailAddress = preferences.getString("email_address_2", "")!!
                                if (android.util.Patterns.EMAIL_ADDRESS.matcher(emailAddress).matches()){
                                    MailSender().downloadAndSend(context.applicationContext, emailAddress, file)

                                    Toast.makeText(context, context.applicationContext.resources.getString(R.string.email_sent), Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, context.applicationContext.resources.getString(R.string.email_address_invalid), Toast.LENGTH_LONG).show()
                                }


                                      },
                            enabled = enabledEmail2
                        ) {
                            Image(
                                painter = painterResource(R.drawable.email),
                                contentDescription = "Download file"
                            )
                            Text(text = stringResource(R.string.send_to_email_2))
                        }
                    }
                }

            }

        }

    }
}