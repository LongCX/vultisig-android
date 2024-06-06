package com.vultisig.wallet.presenter.common

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.core.content.FileProvider
import com.vultisig.wallet.R
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


fun Context.share(bitmap: Bitmap) {
    try {
        val cachePath = File(cacheDir, "images")
        cachePath.mkdirs()
        FileOutputStream("$cachePath/image.png").use { stream ->
            val resizedBitmap = bitmap.getResizedBitmap(500f, 500f)
            resizedBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
        val imagePath = File(cacheDir, "images")
        val newFile = File(imagePath, "image.png")
        val contentUri = FileProvider.getUriForFile(
            this, "$packageName.provider", newFile
        )
        if (contentUri != null) {
            val shareIntent = Intent()
            shareIntent.setAction(Intent.ACTION_SEND)
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            shareIntent.setDataAndType(contentUri, contentResolver.getType(contentUri))
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri)
            startActivity(
                Intent.createChooser(
                    shareIntent,
                    getString(R.string.share_qr_utils_choose_an_app)
                )
            )
        }
    } catch (e: IOException) {
        e.printStackTrace()
    }
}

private fun Bitmap.getResizedBitmap(
    newWidth: Float,
    newHeight: Float
): Bitmap {
    val scaleWidth = newWidth / width
    val scaleHeight = newHeight / height
    val matrix = Matrix()
    matrix.postScale(scaleWidth, scaleHeight)
    val resizedBitmap = Bitmap.createBitmap(
        this, 0, 0,
        width, height, matrix, false
    )
    recycle()
    return resizedBitmap
}