package com.dafusshlosh.carphotos.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.dafusshlosh.carphotos.data.CarPhotoRepository
import com.dafusshlosh.carphotos.work.GalleryScanWorker
import kotlinx.coroutines.launch

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
    val scope = rememberCoroutineScope()

    var plateInput by remember { mutableStateOf("") }
    var notFound by remember { mutableStateOf(false) }
    var isSearching by remember { mutableStateOf(false) }

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
        if (granted) {
            WorkManager.getInstance(context).enqueue(
                OneTimeWorkRequestBuilder<GalleryScanWorker>().build()
            )
        }
    }
    LaunchedEffect(Unit) {
        if (!hasMediaPermission) {
            mediaPermissionLauncher.launch(mediaPermissionName())
        } else {
            WorkManager.getInstance(context).enqueue(
                OneTimeWorkRequestBuilder<GalleryScanWorker>().build()
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text("ניהול תמונות רכבים", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(32.dp))

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
    }
}
