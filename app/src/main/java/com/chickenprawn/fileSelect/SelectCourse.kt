package com.chickenprawn.fileSelect

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.chickenprawn.studip.StudIpCourse

@Composable
fun Course(course: StudIpCourse) {
    val context = LocalContext.current
    Card(
        elevation = elevation,
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val intent = Intent(context, FileActivity::class.java)
                intent.putExtra("course", course)
                context.startActivity(intent)
            },
    ) {
        Row(modifier = Modifier.padding(all = 8.dp)) {

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = course.name,
                color = MaterialTheme.colors.primary,
                fontSize = textSizeCourse
            )
        }
    }
}