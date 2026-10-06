package bo.saludencasa.features.verification.presentation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import bo.saludencasa.features.verification.domain.vo.DocumentImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.math.max

class DocumentImageCompressor(
    private val context: Context,
) {
    sealed interface Result {
        data class Success(
            val bytes: ByteArray,
        ) : Result

        data object Unreadable : Result

        data object TooLarge : Result
    }

    // Decode + EXIF rotation + up to four JPEG passes is heavy enough for a
    // 12 MP camera picture to block the main thread. The suspend body runs on
    // Default so the UI stays responsive.
    suspend fun compress(uri: Uri): Result =
        withContext(Dispatchers.Default) {
            try {
                val decoded = decode(uri) ?: return@withContext Result.Unreadable
                val oriented = applyExifRotation(uri, decoded)
                val scaled = scaleToTarget(oriented)
                val bytes = encodeUnder(scaled)
                if (scaled !== oriented) oriented.recycle()
                if (oriented !== decoded) decoded.recycle()
                scaled.recycle()
                if (bytes == null) Result.TooLarge else Result.Success(bytes)
            } catch (io: IOException) {
                Result.Unreadable
            }
        }

    private fun decode(uri: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val options =
            BitmapFactory.Options().apply {
                inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight)
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
        return context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }
    }

    private fun applyExifRotation(
        uri: Uri,
        bitmap: Bitmap,
    ): Bitmap {
        val rotation =
            context.contentResolver.openInputStream(uri)?.use { stream ->
                ExifInterface(stream).rotationDegrees()
            } ?: 0
        if (rotation == 0) return bitmap
        val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    // inSampleSize is power-of-two, so a 4000x3000 photo decodes to 1000x750 and
    // not to the 1600 px lado largo the design promises. This step closes the gap.
    private fun scaleToTarget(bitmap: Bitmap): Bitmap {
        val longest = max(bitmap.width, bitmap.height)
        if (longest <= TARGET_LONGEST_SIDE) return bitmap
        val ratio = TARGET_LONGEST_SIDE.toFloat() / longest
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * ratio).toInt(),
            (bitmap.height * ratio).toInt(),
            true,
        )
    }

    private fun encodeUnder(bitmap: Bitmap): ByteArray? {
        for (quality in 80 downTo 50 step 10) {
            val output = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
            val bytes = output.toByteArray()
            if (bytes.size <= DocumentImage.MAX_BYTES) return bytes
        }
        return null
    }

    private fun sampleSizeFor(
        width: Int,
        height: Int,
    ): Int {
        val longest = max(width, height)
        var sample = 1
        while (longest / sample > TARGET_LONGEST_SIDE) sample *= 2
        return sample
    }

    private fun ExifInterface.rotationDegrees(): Int =
        when (getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }

    private companion object {
        const val TARGET_LONGEST_SIDE = 1600
    }
}
