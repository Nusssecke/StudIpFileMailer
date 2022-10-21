package com.chickenprawn.fileSelect

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.chickenprawn.studip.StudIp
import com.chickenprawn.studip.StudIpCourse
import com.chickenprawn.studip.StudIpFile
import com.chickenprawn.studip.StudIpFolder
import com.chickenprawn.ui.theme.StudIpTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO Add semester selector
        // TODO Add empty text if there are no files

        // Show ProgressIndicator while loading from the internet
        setContent {
            StudIpTheme{
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(50.dp),
                    )
                }
            }
        }

        // Check what course / file / folder to open
        if (intent.getSerializableExtra("course") != null) {
            val course: StudIpCourse = intent.getSerializableExtra("course") as StudIpCourse

            this.lifecycleScope.launch(Dispatchers.IO) {

                val folders = course.folders
                val files = course.files

                withContext(Dispatchers.Main) {
                    setContent {
                        StudIpTheme {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                FilesAndFolders(folders = folders, files = files)
                            }
                        }
                    }
                }
            }

        } else if (intent.getSerializableExtra("folder") != null) {
            val folder: StudIpFolder = intent.getSerializableExtra("folder") as StudIpFolder
            this.lifecycleScope.launch(Dispatchers.IO) {
                val folders = folder.folders
                val files = folder.files

                withContext(Dispatchers.Main) {
                    setContent {
                        StudIpTheme {
                            Surface(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                FilesAndFolders(folders = folders, files = files)
                            }
                        }
                    }
                }
            }
        } else {
            // Show courses overview if nothing has been passed
            this.lifecycleScope.launch(Dispatchers.IO) {
                val studIp = StudIp()
                val courses = studIp.getCourses()

                withContext(Dispatchers.Main) {
                    setContent {
                        StudIpTheme {
                            Surface(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Courses(courses = courses)
                            }
                        }
                    }
                }
            }
        }
    }
}

val imageSize = 40.dp
val textSizeCourse = 30.sp
val textSizeHeader = 20.sp
val textSizeSmall = 15.sp
val outer_padding = 5.dp
val inner_padding = 8.dp
val elevation = 5.dp

@Composable
fun Courses(courses: List<StudIpCourse>) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = outer_padding, vertical = outer_padding),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(courses) { course ->
            Course(course)
        }
    }
}

@Composable
fun FilesAndFolders(folders: List<StudIpFolder>, files: List<StudIpFile>) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = outer_padding, vertical = outer_padding),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(folders) { folder ->
            Folder(folder)
        }
        items(files) { file ->
            File(file)
        }
    }
}

fun Context.getActivity(): AppCompatActivity? = when (this) {
    is AppCompatActivity -> this
    is ContextWrapper -> baseContext.getActivity()
    else -> null
}


@Preview(
    name = "Light Mode",
    showBackground = true,
)
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    name = "Dark Mode"
)

@Composable
fun DefaultPreview() {
    val file = StudIpFile(
        "fc2db39d57ccb6de7483a55e07526dcb", "Aufgabe2.pdf",
        "https://studip.uni-giessen.de/sendfile.php?type=0&file_id=fc2db39d57ccb6de7483a55e07526dcb&file_name=Blatt2.pdf",
        354, "application/pdf", "file-pdf", 101623,
        "https://studip.uni-giessen.de/dispatch.php/profile?username=gcm7",
        "Lani-Wayda, Bernhard",
        "15b7ab8aeb1fb45d9898c8ba871aa95a", 1650386119, listOf(),
        "https://studip.uni-giessen.de/dispatch.php/file/details/fc2db39d57ccb6de7483a55e07526dcb?cid=b82f714a39aa509857dc23e5cf1b6ba8&file_navigation=1",
        false, "", new = false, isEditable = false
    )

    val folder = StudIpFolder(
        "e5f5582d96876f65658e3698ac0922cd", "folder-full",
        "Übungsblätter",
        "https://studip.uni-giessen.de/dispatch.php/course/files/index/e5f5582d96876f65658e3698ac0922cd?cid=5238da07f3fdd71a207467b174036554",
        "a04df9897fcbe19378804cdc48d0d018", "Chatterjee, Sangam",
        "https://studip.uni-giessen.de/dispatch.php/profile?cid=5238da07f3fdd71a207467b174036554&username=gd1769",
        1650636515, "", "StandardFolder", "rd",
        listOf()
    )

    val folders = listOf(folder, folder)
    val files = listOf(file, file)


    StudIpTheme {
        FilesAndFolders(folders = folders, files = files)
    }
}