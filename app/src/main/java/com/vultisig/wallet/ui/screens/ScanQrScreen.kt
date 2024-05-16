@file:OptIn(ExperimentalPermissionsApi::class)

package com.vultisig.wallet.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.vultisig.wallet.R
import com.vultisig.wallet.ui.components.UiBarContainer
import timber.log.Timber

internal const val ARG_QR_CODE = "qr_code"

@Composable
internal fun ScanQrScreen(
    navController: NavHostController,
) {
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
    val context = LocalContext.current

    var isScanned by remember { mutableStateOf(false) }

    val onSuccess: (List<Barcode>) -> Unit = { barcodes ->
        if (barcodes.isNotEmpty()) {
            isScanned = true
            val barcode = barcodes.first()
            navController.previousBackStackEntry
                ?.savedStateHandle
                ?.set(ARG_QR_CODE, barcode.rawValue)
            navController.popBackStack()
        }
    }

    val pickMedia = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        if (uri != null) {
            createScanner()
                .process(InputImage.fromFilePath(context, uri))
                .addOnSuccessListener(onSuccess)
        }
    }

    UiBarContainer(
        navController = navController,
        title = stringResource(R.string.scan_qr_default_title),
        endIcon = R.drawable.ic_gallery,
        onEndIconClick = {
            pickMedia.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
        }
    ) {
        if (cameraPermissionState.status.isGranted) {
            QrCameraScreen(
                onSuccess = onSuccess,
            )
        } else if (cameraPermissionState.status.shouldShowRationale) {
            Text("Camera Permission permanently denied")
        } else {
            SideEffect {
                cameraPermissionState.launchPermissionRequest()
            }
            Text("No Camera Permission")
        }
    }
}

@Composable
private fun QrCameraScreen(
    onSuccess: (List<Barcode>) -> Unit,

    ) {
    val localContext = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember {
        ProcessCameraProvider.getInstance(localContext)
    }
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            val previewView = PreviewView(context)
            val preview = Preview.Builder().build()
            val selector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()

            preview.setSurfaceProvider(previewView.surfaceProvider)

            val imageAnalysis = ImageAnalysis.Builder().build()
            imageAnalysis.setAnalyzer(
                ContextCompat.getMainExecutor(context),
                BarcodeAnalyzer(onSuccess)
            )

            try {
                cameraProviderFuture.get().bindToLifecycle(
                    lifecycleOwner,
                    selector,
                    preview,
                    imageAnalysis,
                )
            } catch (e: Throwable) {
                Timber.e("Camera bind error ${e.localizedMessage}", e)
            }

            previewView
        }
    )
}

private class BarcodeAnalyzer(
    private val onSuccess: (List<Barcode>) -> Unit,
) : ImageAnalysis.Analyzer {

    private val scanner = createScanner()

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        imageProxy.image?.let { image ->
            scanner.process(
                InputImage.fromMediaImage(
                    image, imageProxy.imageInfo.rotationDegrees
                )
            ).addOnSuccessListener { barcode ->
                barcode?.takeIf { it.isNotEmpty() }
                    ?.let(onSuccess)
            }.addOnCompleteListener {
                imageProxy.close()
            }
        }
    }
}

private fun createScanner() = BarcodeScanning.getClient(
    BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
        .build()
)