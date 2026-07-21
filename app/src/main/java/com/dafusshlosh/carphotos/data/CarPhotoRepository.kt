package com.dafusshlosh.carphotos.data

import android.content.Context
import com.dafusshlosh.carphotos.ocr.PlateReader

class CarPhotoRepository(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val carDao = db.carDao()
    private val photoDao = db.photoDao()
    private val scanStateDao = db.scanStateDao()

    // The car currently "open" during a live capture session.
    // Kept only in memory: no OCR re-run needed for subsequent shots.
    private var activeCarId: Long? = null

    /**
     * Called right after a photo is taken inside the app.
     * If it's a plate photo, OCR runs once, a new Car row is created,
     * and it becomes the active car for every following shot.
     * If it's a damage photo, it's simply attached to the active car.
     */
    suspend fun onPhotoCaptured(uri: String, isPlateShot: Boolean): String? {
        val now = System.currentTimeMillis()

        if (isPlateShot) {
            val plate = PlateReader.recognizePlate(context, android.net.Uri.parse(uri))
            if (plate != null) {
                val existing = carDao.findByPlate(plate)
                val carId = existing?.id ?: carDao.insert(
                    Car(plateNumber = plate, firstPhotoUri = uri, createdAt = now)
                )
                activeCarId = carId
                photoDao.insert(Photo(carId = carId, uri = uri, takenAt = now, isPlatePhoto = true, mediaStoreDateAdded = now / 1000))
                return plate
            }
            // OCR failed to read a plate — still open an "unknown" group so photos aren't lost.
            val carId = carDao.insert(Car(plateNumber = "לא_זוהה_${now}", firstPhotoUri = uri, createdAt = now))
            activeCarId = carId
            photoDao.insert(Photo(carId = carId, uri = uri, takenAt = now, isPlatePhoto = true, mediaStoreDateAdded = now / 1000))
            return null
        } else {
            val carId = activeCarId ?: return null // no open group yet — caller should prompt to shoot a plate first
            photoDao.insert(Photo(carId = carId, uri = uri, takenAt = now, isPlatePhoto = false, mediaStoreDateAdded = now / 1000))
            return null
        }
    }

    /** Instant search: hits the unique index on plateNumber. */
    suspend fun searchByPlate(plateRaw: String): Pair<Car, List<Photo>>? {
        val normalized = plateRaw.filter { it.isDigit() }
        val car = carDao.findByPlate(normalized) ?: return null
        return car to photoDao.allForCar(car.id)
    }

    suspend fun lastScannedDateAdded(): Long =
        scanStateDao.get()?.lastScannedDateAdded ?: 0L

    suspend fun updateScanProgress(dateAdded: Long) {
        scanStateDao.upsert(ScanState(lastScannedDateAdded = dateAdded))
    }

    fun daos() = Triple(carDao, photoDao, scanStateDao)
}
