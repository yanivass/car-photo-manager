package com.dafusshlosh.carphotos.data

import androidx.paging.PagingSource
import androidx.room.*

@Dao
interface CarDao {
    // Exact-match lookup on the unique index -> effectively O(log n), instant.
    @Query("SELECT * FROM cars WHERE plateNumber = :plate LIMIT 1")
    suspend fun findByPlate(plate: String): Car?

    // Fallback partial search (e.g. user typed only part of the plate)
    @Query("SELECT * FROM cars WHERE plateNumber LIKE '%' || :partial || '%' ORDER BY createdAt DESC LIMIT 50")
    suspend fun searchByPartialPlate(partial: String): List<Car>

    @Insert
    suspend fun insert(car: Car): Long

    @Query("SELECT COUNT(*) FROM cars")
    suspend fun countCars(): Int
}

@Dao
interface PhotoDao {
    @Insert
    suspend fun insert(photo: Photo): Long

    @Insert
    suspend fun insertAll(photos: List<Photo>)

    // Used for the results gallery — ordered by capture order, paged for huge sets.
    @Query("SELECT * FROM photos WHERE carId = :carId ORDER BY takenAt ASC")
    fun pagingSourceForCar(carId: Long): PagingSource<Int, Photo>

    @Query("SELECT * FROM photos WHERE carId = :carId ORDER BY takenAt ASC")
    suspend fun allForCar(carId: Long): List<Photo>

    @Query("SELECT MAX(mediaStoreDateAdded) FROM photos")
    suspend fun maxDateAdded(): Long?
}

@Dao
interface ScanStateDao {
    @Query("SELECT * FROM scan_state WHERE id = 0")
    suspend fun get(): ScanState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: ScanState)
}
