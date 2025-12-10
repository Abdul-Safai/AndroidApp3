package com.trios2025dej.androidapp3

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var homeAddressText: TextView
    private lateinit var latLngText: TextView
    private lateinit var currentAddressText: TextView
    private lateinit var distanceText: TextView

    private lateinit var editAddressInput: EditText
    private lateinit var btnLocate: Button      // "Calculate Distance"
    private lateinit var btnSetHome: Button     // "Add Home Address"
    private lateinit var btnRefresh: Button
    private lateinit var btnCompass: Button

    private val LOCATION_PERMISSION_REQUEST = 100

    private var homeLocation: Location? = null
    private var currentLocation: Location? = null   // GPS location

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Bind views
        homeAddressText = findViewById(R.id.homeAddressText)
        latLngText = findViewById(R.id.latLngText)
        currentAddressText = findViewById(R.id.currentAddressText)
        distanceText = findViewById(R.id.distanceText)

        editAddressInput = findViewById(R.id.editAddressInput)
        btnLocate = findViewById(R.id.btnLocate)
        btnSetHome = findViewById(R.id.btnSetHome)
        btnRefresh = findViewById(R.id.btnRefresh)
        btnCompass = findViewById(R.id.btnCompass)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Initial text / hints from strings.xml
        homeAddressText.text = getString(R.string.home_address_placeholder)
        currentAddressText.text = getString(R.string.current_address_placeholder)
        distanceText.text = getString(R.string.distance_placeholder)
        editAddressInput.hint = getString(R.string.home_address_input_hint)
        latLngText.text = getString(R.string.latitude_longitude)

        // Handle system bars padding (edge-to-edge)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 🔹 Add Home Address button – only saves/updates home, no distance yet
        btnSetHome.setOnClickListener {
            val query = editAddressInput.text.toString().trim()
            if (query.isEmpty()) {
                Toast.makeText(this, "Please enter an address to add as home.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val geocoder = Geocoder(this, Locale.getDefault())
            val results = geocoder.getFromLocationName(query, 1)

            if (!results.isNullOrEmpty()) {
                val addr = results[0]

                // Save as home location
                homeLocation = Location("home").apply {
                    latitude = addr.latitude
                    longitude = addr.longitude
                }

                // Update only the home address text
                homeAddressText.text = "Home Address:\n${addr.getAddressLine(0)}"
                Toast.makeText(this, "Home address added successfully.", Toast.LENGTH_SHORT).show()

                // Hint for user to calculate distance
                distanceText.text = "Distance: tap \"Calculate Distance\" to update."
            } else {
                Toast.makeText(this, "Home address not found. Please try a different address.", Toast.LENGTH_SHORT).show()
            }
        }

        // 🔹 Calculate Distance button – uses GPS current location + saved home
        btnLocate.setOnClickListener {
            if (homeLocation == null) {
                Toast.makeText(this, "Add your home address first.", Toast.LENGTH_SHORT).show()
            } else {
                // Get GPS location and then update distance
                checkLocationPermission()
            }
        }

        // 🔹 Refresh button – clear address + reset UI + clear saved locations
        btnRefresh.setOnClickListener {
            // Clear manual input
            editAddressInput.text.clear()
            editAddressInput.hint = getString(R.string.home_address_input_hint)

            // Clear saved locations
            homeLocation = null
            currentLocation = null

            // Reset all labels
            homeAddressText.text = getString(R.string.home_address_placeholder)
            currentAddressText.text = getString(R.string.current_address_placeholder)
            latLngText.text = getString(R.string.latitude_longitude)
            distanceText.text = getString(R.string.distance_placeholder)

            // Optionally re-fetch GPS for current position (without calculating distance)
            checkLocationPermission()
        }

        // 🔹 Open Compass screen
        btnCompass.setOnClickListener {
            startActivity(Intent(this, CompassActivity::class.java))
        }

        // Get GPS location on startup (so currentLocation is ready later)
        checkLocationPermission()
    }

    // Options menu (top-right) with Compass item
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_compass -> {
                startActivity(Intent(this, CompassActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // Ask for location permission if needed
    private fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST
            )
        } else {
            getLastLocation()
        }
    }

    // Called when user responds to permission dialog
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            getLastLocation()
        } else {
            Toast.makeText(this, "Location permission denied.", Toast.LENGTH_SHORT).show()
        }
    }

    // Get last known GPS location and show it + update distance (if home is set)
    private fun getLastLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    currentLocation = location

                    val lat = String.format("%.6f", location.latitude)
                    val lon = String.format("%.6f", location.longitude)
                    latLngText.text = "Latitude: $lat\nLongitude: $lon"

                    val geocoder = Geocoder(this, Locale.getDefault())
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)

                    if (!addresses.isNullOrEmpty()) {
                        val line = addresses[0].getAddressLine(0)
                        currentAddressText.text = "Current Address:\n$line"
                    } else {
                        currentAddressText.text = "Current Address: not found"
                    }

                    // Now that currentLocation is updated, show distance if home is set
                    updateDistance()
                } else {
                    Toast.makeText(this, "Unable to get GPS location.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to get location: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Update distance text using homeLocation and currentLocation
    private fun updateDistance() {
        when {
            homeLocation == null -> {
                distanceText.text = "Distance: set your home first."
            }
            currentLocation == null -> {
                distanceText.text = "Distance: waiting for current location."
            }
            else -> {
                val distMeters = homeLocation!!.distanceTo(currentLocation!!)
                val distKm = distMeters / 1000.0
                distanceText.text = String.format(Locale.getDefault(), "Distance: %.2f km", distKm)
            }
        }
    }
}
