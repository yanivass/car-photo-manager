package com.dafusshlosh.carphotos.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.dafusshlosh.carphotos.data.AppDatabase
import com.dafusshlosh.carphotos.data.Photo
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(navController: NavHostController, carId: Long) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    var photos by remember { mutableStateOf<List<Photo>>(emptyList()) }

    LaunchedEffect(carId) {
        photos = db.photoDao().allForCar(carId)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("תמונות הרכב") }) }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(4.dp)
        ) {
            items(photos.size) { index ->
                val photo = photos[index]
                AsyncImage(
                    model = photo.uri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .padding(2.dp)
                        .aspectRatio(1f)
                        .clickable { navController.navigate("viewer/$carId/$index") }
                )
            }
        }
    }
}
