package com.example.testradar

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private var wifiManager: WifiManager? = null
    private var scanResults: List<ScanResult>? = null
    private var wifiReceiver: BroadcastReceiver? = null

    companion object {
        private const val TAG = "WifiScanner"
        private const val PERMISSION_REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize WifiManager
        wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager

        // Enable WiFi if it's disabled
        if (!wifiManager!!.isWifiEnabled) {
            Toast.makeText(this, "Enabling WiFi...", Toast.LENGTH_SHORT).show()
            wifiManager!!.setWifiEnabled(true)
        }

        // Setup the scan button to start WiFi scanning
        val scanButton = findViewById<Button>(R.id.scanButton)
        scanButton.setOnClickListener {
            if (checkPermissions()) {
                startWifiScan()
            } else {
                requestPermissions()
            }
        }
    }

    // Start the WiFi scan and save results
    private fun startWifiScan() {
        try {
            wifiReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    // Create a local copy of scanResults to avoid the smart cast issue
                    val results: List<ScanResult>? = wifiManager?.scanResults
                    unregisterReceiver(this)

                    // Check if the scan results are empty or null
                    if (results.isNullOrEmpty()) {
                        Log.d(TAG, "No WiFi networks found.")
                        Toast.makeText(this@MainActivity, "No WiFi networks found.", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.d(TAG, "WiFi networks found: ${results.size}")
                        Toast.makeText(this@MainActivity, "Found ${results.size} networks.", Toast.LENGTH_SHORT).show()

                        // Save scan results to file
                        try {
                            saveScanResultsToFile(results)
                        } catch (e: IOException) {
                            e.printStackTrace()
                            Toast.makeText(
                                this@MainActivity,
                                "Failed to save scan results",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }

            // Register the receiver and start the scan
            registerReceiver(wifiReceiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))

            // Check if permission is granted before starting the scan
            if (checkPermissions()) {
                wifiManager!!.startScan()
                Toast.makeText(this, "Scanning for WiFi networks...", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Location permission required to scan WiFi networks.", Toast.LENGTH_SHORT).show()
                requestPermissions()
            }
        } catch (e: SecurityException) {
            // Handle case where permissions are not granted
            Log.e(TAG, "Permission denied: ${e.message}")
            Toast.makeText(this, "Permission denied. Cannot scan WiFi networks.", Toast.LENGTH_SHORT).show()
        }
    }

    // Save scan results to a file in app-specific external storage
    @Throws(IOException::class)
    private fun saveScanResultsToFile(scanResults: List<ScanResult>) {
        // Use app-specific external storage
        val file = File(getExternalFilesDir(null), "wifi_scan_logs.txt")

        // Check if the file exists and create if necessary
        if (!file.exists()) {
            file.createNewFile()
            Toast.makeText(this, "File Created: " + file.path, Toast.LENGTH_SHORT).show()
            Log.d(TAG, "File Created: " + file.path)
        }

        val fos = FileOutputStream(file, true) // Append mode

        // Get the current timestamp
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        // Log the scan results and write to the file
        for (scanResult in scanResults) {
            val log = String.format(
                Locale.getDefault(), "%s, SSID: %s, BSSID: %s, Signal Level: %d\n",
                timestamp, scanResult.SSID, scanResult.BSSID, scanResult.level
            )
            Log.d(TAG, "Writing to file: $log")  // Log the data being written to the file
            fos.write(log.toByteArray())  // Write to file
        }

        fos.close()
        Toast.makeText(this, "Scan results saved to: " + file.path, Toast.LENGTH_LONG).show()
        Log.d(TAG, "WiFi scan results saved to: " + file.path)
    }

    // Check if the app has the necessary permissions
    private fun checkPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Request necessary permissions if not granted
    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            PERMISSION_REQUEST_CODE
        )
    }

    // Handle the permissions request result
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Location permission granted", Toast.LENGTH_SHORT).show()
                startWifiScan() // Now that permission is granted, we can start the WiFi scan
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (wifiReceiver != null) {
            unregisterReceiver(wifiReceiver)
        }
    }
}

