package com.dafusshlosh.carphotos.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A car "session" — created the moment a license-plate photo is detected.
 * plateNumber is indexed + unique so lookups are instant even at huge scale.
 */
@Entity(
    tableName = "cars",
    indices = [Index(value = ["plateNumber"], unique = true)]
)
data class Car(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val plateNumber: String,
    val firstPhotoUri: String,
    val createdAt: Long
)

/**
 * Every photo (plate photo or damage photo) belongs to exactly one car.
 * carId is indexed so "all photos for car X" is a direct index lookup.
 */
@Entity(
    tableName = "photos",
    indices = [Index(value = ["carId"]), Index(value = ["mediaStoreDateAdded"])]
)
data class Photo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val carId: Long,
    val uri: String,
    val takenAt: Long,
    val isPlatePhoto: Boolean,
    // date_added from MediaStore, used to know what has already been scanned
    val mediaStoreDateAdded: Long
)

/** Single-row table holding scan progress, so re-scans only look at new photos. */
@Entity(tableName = "scan_state")
data class ScanState(
    @PrimaryKey val id: Int = 0,
    val lastScannedDateAdded: Long
)
