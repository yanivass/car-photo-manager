package com.dafusshlosh.carphotos.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Reads a license plate number out of an image using the on-device ML Kit
 * Latin text recognizer (works fully offline, no network calls).
 *
 * Israeli plates are numeric, in groups of 2-3-2 or 3-3-1 digits
 * (e.g. 12-345-67 or 123-45-678). We take the OCR's raw text, strip
 * everything that isn't a digit, and validate length (7 or 8 digits).
 */
object PlateReader {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    suspend fun recognizePlate(context: Context, imageUri: Uri): String? {
        val image = InputImage.fromFilePath(context, imageUri)
        val rawText = suspendCancellableCoroutine<String> { cont ->
            recognizer.process(image)
                .addOnSuccessListener { result -> cont.resume(result.text) }
                .addOnFailureListener { cont.resume("") }
        }
        return extractPlateNumber(rawText)
    }

    /** Pulls the most plate-like digit sequence out of arbitrary OCR text. */
    fun extractPlateNumber(rawText: String): String? {
        // Candidate = any run of digits (allowing embedded spaces/dashes) with length 7-8
        val candidates = Regex("[0-9][0-9\\-\\s]{5,9}[0-9]")
            .findAll(rawText)
            .map { it.value.filter { c -> c.isDigit() } }
            .filter { it.length == 7 || it.length == 8 }
            .toList()

        return candidates.maxByOrNull { it.length }
    }

    /** Formats a raw digit string as a standard Israeli plate for display, e.g. 12345678 -> 123-45-678 */
    fun formatForDisplay(digits: String): String = when (digits.length) {
        7 -> "${digits.substring(0, 2)}-${digits.substring(2, 5)}-${digits.substring(5)}"
        8 -> "${digits.substring(0, 3)}-${digits.substring(3, 5)}-${digits.substring(5)}"
        else -> digits
    }
}
