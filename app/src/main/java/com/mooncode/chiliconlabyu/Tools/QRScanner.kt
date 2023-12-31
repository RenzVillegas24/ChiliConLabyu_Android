package com.mooncode.chiliconlabyu.Tools

import android.annotation.SuppressLint
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

class QRScanner {
    class WiFiScanner(
        private val onSuccess: (ssid: String, password: String) -> Unit,
        private val onFailure: (e: Exception) -> Unit)  : ImageAnalysis.Analyzer {
        private val scanner: BarcodeScanner = BarcodeScanning.getClient(BarcodeScannerOptions.Builder().setBarcodeFormats(
            Barcode.FORMAT_QR_CODE).build())

        @SuppressLint("UnsafeOptInUsageError")
        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image
            if (mediaImage != null){
                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

                scanner.process(image)
                    .addOnSuccessListener {barcodes->
                        for (barcode in barcodes){
                            val valueType = barcode.valueType
                            if(valueType == Barcode.TYPE_WIFI){
                                val ssid = barcode.wifi!!.ssid
                                val password = barcode.wifi!!.password
                                if(ssid !=null && password!= null){
                                    onSuccess(ssid, password)
                                }else{
                                    onFailure(NullPointerException("WIFI credentials must not be null"))
                                }
                            }else{
                                onFailure(IllegalArgumentException("Barcode is not resembling a WIFI network"))
                            }
                        }
                    }
                    .addOnFailureListener{
                        onFailure(it)
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }

            }
        }
    }
}