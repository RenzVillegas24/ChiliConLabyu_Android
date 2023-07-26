package com.mooncode.chiliconlabyu


import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Size
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mooncode.chiliconlabyu.Tools.QRScanner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class WiFiScan : Fragment() {
    private var imageCapture: ImageCapture? = null

    private lateinit var wifiManager: WifiManager
    private lateinit var connectivityManager: ConnectivityManager
    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}
    private var hasScanned = false

    private lateinit var cameraExecutor: ExecutorService

    private inline fun<T> is29AndAbove(
        func: () -> T
    ) :T?{
        return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            func.invoke()
        else
            null
    }
    private val NetworkCapabilities.hasInternet
        get() = hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            else -> true
        }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun connect29AndAbove(ssid: String, passPhrase: String) {
        if (!wifiManager.isWifiEnabled) {
            Toast.makeText(this.context, "Wifi is disabled please enable it to proceed", Toast.LENGTH_SHORT).show()
            val panelIntent = Intent(Settings.Panel.ACTION_WIFI)
            launcher.launch(panelIntent)
        }
        connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val builder =   WifiNetworkSpecifier.Builder()
            .setSsid(ssid)
            .setWpa2Passphrase(passPhrase)
            .build()

        val netBuilder = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .setNetworkSpecifier(builder)
            .build()

        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                connectivityManager.bindProcessToNetwork(network)

                Handler(Looper.getMainLooper()).post {
                    MaterialAlertDialogBuilder(requireActivity())
                        .setTitle("Connection successful")
                        .setMessage("The controller successfully connected to the local! Would you like to pair the controller to your phone to be able to control this on WiFi?")
                        .setPositiveButton("Connect to WiFi") { _, _ ->
                            findNavController().navigate(R.id.action_wiFiScan_to_wiFiConnect)
                        }
                        .setNegativeButton("Cancel") { _, _ ->
                            findNavController().popBackStack();
                        }
                        .show()
                }
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                // This is to stop the looping request for OnePlus & Xiaomi models
                connectivityManager.bindProcessToNetwork(null)
                connectivityManager.unregisterNetworkCallback(this)
                // Here you can have a fallback option to show a 'Please connect manually' page with an Intent to the Wifi settings

                findNavController().navigate(R.id.action_wiFiConnect_to_mainMenu)
                Toast.makeText(requireContext(), "Wifi turns off!", Toast.LENGTH_SHORT).show()
            }

        }
        connectivityManager.requestNetwork(netBuilder, networkCallback)


    }

    @SuppressLint("MissingPermission")
    fun connectBelow29(ssid: String, passPhrase: String) {
        if (!wifiManager.isWifiEnabled) {
            Toast.makeText(this.context, "Enabling wifi...", Toast.LENGTH_SHORT).show()
            wifiManager.isWifiEnabled = true
        }

        val conf = WifiConfiguration()
        conf.SSID = String.format("\"%s\"", ssid)
        conf.preSharedKey = String.format("\"%s\"", passPhrase)
        wifiManager.addNetwork(conf)
        val netId = wifiManager.addNetwork(conf)
        wifiManager.disconnect()
        wifiManager.enableNetwork(netId, true)
        wifiManager.reconnect()
        Toast.makeText(this.context, "Connecting...", Toast.LENGTH_SHORT).show()
        hasScanned = false
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_wifi_scan, container, false)
        val viewFinder_v = view.findViewById<PreviewView>(R.id.viewFinder)

        wifiManager = requireContext().applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        connectivityManager = requireContext().applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val previewUseCase = Preview.Builder()
            .build().also {
                it.setSurfaceProvider(viewFinder_v.surfaceProvider)
            }

        val imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(viewFinder_v.width, viewFinder_v.height))
            .setBackpressureStrategy(STRATEGY_KEEP_ONLY_LATEST)
            .build().apply {
                setAnalyzer(
                    ContextCompat.getMainExecutor(requireContext()),
                    QRScanner.WiFiScanner(
                        {ssid, pw ->
                            if (!hasScanned) {
                                hasScanned = true
                                is29AndAbove {
                                    connect29AndAbove(ssid, pw)
                                } ?: {
                                    connectBelow29(ssid, pw)
                                }
                            }

                        }, {
                            if (!hasScanned) {
                                hasScanned = true
                                Toast.makeText(
                                    context,
                                    "Error's occurred ${it.localizedMessage}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )
                )
            }

        try {
            ProcessCameraProvider.getInstance(requireContext()).get().apply {
                unbindAll()

                val camera = bindToLifecycle(
                    viewLifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    previewUseCase,
                    imageAnalysis
                )


                viewFinder_v.setOnTouchListener(View.OnTouchListener { view: View, motionEvent: MotionEvent ->
                    when (motionEvent.action) {
                        MotionEvent.ACTION_DOWN -> return@OnTouchListener true
                        MotionEvent.ACTION_UP -> {
                            val factory = viewFinder_v.meteringPointFactory
                            val point = factory.createPoint(motionEvent.x, motionEvent.y)
                            val action = FocusMeteringAction.Builder(point).build()

                            camera.cameraControl.startFocusAndMetering(action)

                            return@OnTouchListener true
                        }

                        else -> return@OnTouchListener false
                    }
                })
            }


        } catch (e: Exception) {
            e.printStackTrace()
        }
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Inflate the layout for this fragment
        return view
    }

}