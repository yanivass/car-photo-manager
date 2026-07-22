package com.dafusshlosh.carphotos.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.dafusshlosh.carphotos.data.AppDatabase
import com.dafusshlosh.carphotos.data.CarPhotoRepository
import com.dafusshlosh.carphotos.work.GalleryScanWorker
import kotlinx.coroutines.launch

private const val SCAN_WORK_NAME = "gallery_scan"

private fun mediaPermissionName(): String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        Manifest.permission.READ_MEDIA_IMAGES
    else
        Manifest.permission.READ_EXTERNAL_STORAGE

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController) {
    val context = LocalContext.current
    val repo = remember { CarPhotoRepository(context) }
    val db = remember { AppDatabase.getInstance(context) }
    val workManager = remember { WorkManager.getInstance(context) }
    val scope = rememberCoroutineScope()

    var plateInput by remember { mutableStateOf("") }
    var notFound by remember { mutableStateOf(false) }
    var isSearching by remember { mutableStateOf(false) }
    var carCount by remember { mutableStateOf<Int?>(null) }

    fun startScan(replace: Boolean) {
        val request = OneTimeWorkRequestBuilder<GalleryScanWorker>().build()
        workManager.enqueueUniqueWork(
            SCAN_WORK_NAME,
            if (replace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
            request
        )
    }

    var hasMediaPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, mediaPermissionName()) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val mediaPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMediaPermission = granted
        if (granted) startScan(replace = false)
    }
    LaunchedEffect(Unit) {
        if (!hasMediaPermission) {
            mediaPermissionLauncher.launch(mediaPermissionName())
        } else {
            startScan(replace = false)
        }
    }

    val workInfos by workManager.getWorkInfosForUniqueWorkLiveData(SCAN_WORK_NAME)
        .observeAsState(emptyList())
    val currentState = workInfos.firstOrNull()?.state

    LaunchedEffect(currentState) {
        if (currentState == WorkInfo.State.SUCCEEDED) {
            carCount = db.carDao().countCars()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text("ניהול תמונות רכבים", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))

        when (currentState) {
            WorkInfo.State.RUNNING, WorkInfo.State.ENQUEUED ->
                Text("סורק תמונות מהגלריה...", style = MaterialTheme.typography.bodySmall)
            WorkInfo.State.SUCCEEDED ->
                Text(
                    "סריקה הושלמה — ${carCount ?: 0} רכבים במאגר",
                    style = MaterialTheme.typography.bodySmall
                )
            WorkInfo.State.FAILED ->
                Text(
                    "הסריקה נכשלה, נסה שוב",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            else -> {}
        }
        TextButton(onClick = { startScan(replace = true) }) {
            Text("סרוק שוב עכשיו")
        }

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = plateInput,
            onValueChange = { plateInput = it; notFound = false },
            label = { Text("מספר רכב") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                isSearching = true
                scope.launch {
                    val result = repo.searchByPlate(plateInput)
                    isSearching = false
                    if (result != null) {
                        navController.navigate("gallery/${result.first.id}")
                    } else {
                        notFound = true
                    }
                }
            },
            enabled = plateInput.isNotBlank() && !isSearching,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isSearching) "מחפש..." else "חיפוש")
        }

        if (notFound) {
            Spacer(Modifier.height(8.dp))
            Text("לא נמצא רכב עם מספר זה", color = MaterialTheme.colorScheme.error)
        }

        if (!hasMediaPermission) {
            Spacer(Modifier.height(8.dp))
            Text(
                "אין הרשאה לקרוא תמונות מהגלריה — תמונות ישנות לא ייסרקו",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(Modifier.height(24.dp))
        Divider()
        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { navController.navigate("capture") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("צלם רכב חדש")
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "גרסת בדיקה: v3-scan-status",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}
