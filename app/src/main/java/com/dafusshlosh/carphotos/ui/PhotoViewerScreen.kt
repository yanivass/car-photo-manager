package com.dafusshlosh.carphotos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.dafusshlosh.carphotos.data.AppDatabase
import com.dafusshlosh.carphotos.data.Photo

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PhotoViewerScreen(carId: Long, startIndex: Int) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    var photos by remember { mutableStateOf<List<Photo>>(emptyList()) }

    LaunchedEffect(carId) {
        photos = db.photoDao().allForCar(carId)
    }

    if (photos.isEmpty()) return

    val pagerState = rememberPagerState(initialPage = startIndex.coerceIn(0, photos.size - 1)) { photos.size }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            AsyncImage(
                model = photos[page].uri,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }

        Text(
            text = "${pagerState.currentPage + 1} / ${photos.size}",
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
}
