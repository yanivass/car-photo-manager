package com.dafusshlosh.carphotos.ocr

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

/**
 * Reads a license plate number out of an image using the Plate Recognizer
 * Snapshot Cloud API (platerecognizer.com) — a service purpose-built for
 * ALPR (Automatic License Plate Recognition), far more accurate for plates
 * than general-purpose on-device OCR.
 *
 * Requires an internet connection and an API token from platerecognizer.com.
 */
object PlateReader {

    // ⚠️ REPLACE_WITH_YOUR_API_TOKEN — paste your Plate Recognizer token
    // between the quotes below, replacing the whole placeholder text.
    private const val API_TOKEN = "0f8757e89424a54b51447849259f334ca6e6ccd2"

    private const val API_URL = "https://api.platerecognizer.com/v1/plate-reader/"

    private val client = OkHttpClient()

    suspend fun recognizePlate(context: Context, imageUri: Uri): String? =
        withContext(Dispatchers.IO) {
            try {
                val tempFile = copyUriToTempFile(context, imageUri) ?: return@withContext null

                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                        "upload", tempFile.name,
                        tempFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    )
                    // Hint the API to prioritize Israeli plate formats
                    .addFormDataPart("regions", "il")
                    .build()

                val request = Request.Builder()
                    .url(API_URL)
                    .addHeader("Authorization", "Token $API_TOKEN")
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    tempFile.delete()
                    if (!response.isSuccessful) return@withContext null
                    val body = response.body?.string() ?: return@withContext null
                    parseBestPlate(body)
                }
            } catch (e: Exception) {
                null
            }
        }

    private fun parseBestPlate(jsonBody: String): String? {
        val json = JSONObject(jsonBody)
        val results = json.optJSONArray("results") ?: return null
        if (results.length() == 0) return null

        // Results are typically ordered by confidence; take the top one.
        val best = results.getJSONObject(0)
        val rawPlate = best.optString("plate", "")
        val digits = rawPlate.filter { it.isDigit() }
        return if (digits.length == 7 || digits.length == 8) digits else null
    }

    private fun copyUriToTempFile(context: Context, uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val tempFile = File.createTempFile("plate_upload", ".jpg", context.cacheDir)
            FileOutputStream(tempFile).use { output ->
                inputStream.copyTo(output)
            }
            inputStream.close()
            tempFile
        } catch (e: Exception) {
            null
        }
    }

    /** Formats a raw digit string as a standard Israeli plate for display, e.g. 12345678 -> 123-45-678 */
    fun formatForDisplay(digits: String): String = when (digits.length) {
        7 -> "${digits.substring(0, 2)}-${digits.substring(2, 5)}-${digits.substring(5)}"
        8 -> "${digits.substring(0, 3)}-${digits.substring(3, 5)}-${digits.substring(5)}"
        else -> digits
    }
}
