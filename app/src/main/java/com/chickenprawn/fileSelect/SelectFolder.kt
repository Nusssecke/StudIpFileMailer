package com.chickenprawn.fileSelect

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.chickenprawn.R
import com.chickenprawn.studip.StudIpFolder
import java.time.format.DateTimeFormatter

@Composable
fun Folder(folder: StudIpFolder) {

    val context = LocalContext.current
    Card(
        elevation = elevation,
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val intent = Intent(context, FileActivity::class.java)
                intent.putExtra("folder", folder)
                context.startActivity(intent)
            },
    ) {
        Row(modifier = Modifier.padding(all = 8.dp)) {

            val iconId = when (folder.icon) {
                "folder-full" -> R.drawable.folder_full
                "folder-lock-full" -> R.drawable.folder_lock_full
                "download" -> R.drawable.folder_download
                else -> R.drawable.clear
            }

            Image(
                painter = painterResource(iconId),
                contentDescription = "Folder type picture",
                modifier = Modifier.size(imageSize)
            )

            // Add a horizontal space
            Spacer(modifier = Modifier.width(8.dp))

            Column() {
                Text(
                    text = folder.name,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = textSizeHeader,
                )

                val studIpFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

                Text(
                    text = folder.date.format(studIpFormatter),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = textSizeSmall
                )
            }

        }

    }
}