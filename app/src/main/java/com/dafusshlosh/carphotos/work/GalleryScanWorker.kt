package com.dafusshlosh.carphotos.work

import android.content.Context
import android.provider.MediaStore
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dafusshlosh.carphotos.data.AppDatabase
import com.dafusshlosh.carphotos.data.Car
import com.dafusshlosh.carphotos.data.Photo
import com.dafusshlosh.carphotos.data.ScanState
import com.dafusshlosh.carphotos.ocr.PlateReader

/**
 * Scans only photos added to the device gallery *after* the last successful
 * scan (MediaStore.Images.Media.DATE_ADDED > lastScanned). Existing cars/photos
 * are never re-processed, which is what keeps this fast even at huge scale.
 */
class GalleryScanWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val db = AppDatabase.getInstance(applicationContext)
        val carDao = db.carDao()
        val photoDao = db.photoDao()
        val scanStateDao = db.scanStateDao()

        val lastScanned = scanStateDao.get()?.lastScannedDateAdded ?: 0L

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_ADDED
        )
        val selection = "${MediaStore.Images.Media.DATE_ADDED} > ?"
        val selectionArgs = arrayOf(lastScanned.toString())
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} ASC"

        val cursor = applicationContext.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection, selection, selectionArgs, sortOrder
        ) ?: return Result.retry()

        var activeCarId: Long? = null
        var maxDateSeen = lastScanned

        cursor.use {
            val idCol = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val dateCol = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

            while (it.moveToNext()) {
                val id = it.getLong(idCol)
                val dateAdded = it.getLong(dateCol)
                val uri = "content://media/external/images/media/$id"

                val plate = PlateReader.recognizePlate(applicationContext, android.net.Uri.parse(uri))

                if (plate != null) {
                    // New plate detected -> starts a new group, per spec.
                    val existing = carDao.findByPlate(plate)
                    val carId = existing?.id ?: carDao.insert(
                        Car(plateNumber = plate, firstPhotoUri = uri, createdAt = dateAdded * 1000)
                    )
                    activeCarId = carId
                    photoDao.insert(
                        Photo(carId = carId, uri = uri, takenAt = dateAdded * 1000, isPlatePhoto = true, mediaStoreDateAdded = dateAdded)
                    )
                } else {
                    activeCarId?.let { carId ->
                        photoDao.insert(
                            Photo(carId = carId, uri = uri, takenAt = dateAdded * 1000, isPlatePhoto = false, mediaStoreDateAdded = dateAdded)
                        )
                    }
                    // If no active car yet and no plate found, the photo is skipped —
                    // it doesn't belong to any known group.
                }

                if (dateAdded > maxDateSeen) maxDateSeen = dateAdded
            }
        }

        scanStateDao.upsert(ScanState(lastScannedDateAdded = maxDateSeen))
        return Result.success()
    }
    }
