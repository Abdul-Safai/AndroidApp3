package com.trios2025dej.androidapp3

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.MenuItem
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import kotlin.math.roundToInt

class CompassActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var rotationVectorSensor: Sensor? = null

    private lateinit var compassImage: ImageView
    private lateinit var bearingText: TextView
    private lateinit var backButton: Button

    private var currentAzimuth: Float = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_compass)

        // Use the toolbar from activity_compass.xml
        val toolbar = findViewById<Toolbar>(R.id.toolbarCompass)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.compass_title)

        // Views
        compassImage = findViewById(R.id.compassImage)
        bearingText = findViewById(R.id.bearingText)
        backButton = findViewById(R.id.btnBackHome)

        // Back button at bottom (white) → return to main page
        backButton.setOnClickListener {
            finish()  // closes CompassActivity and returns to MainActivity
        }

        // Sensor setup
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        // Prefer rotation vector (modern), fall back to orientation if needed
        rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (rotationVectorSensor == null) {
            rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION)
        }

        if (rotationVectorSensor == null) {
            Toast.makeText(this, "No compass sensor found on this device.", Toast.LENGTH_LONG).show()
        }
    }

    // Handle toolbar back arrow
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        rotationVectorSensor?.let {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_GAME
            )
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR ||
            event.sensor.type == Sensor.TYPE_ORIENTATION
        ) {
            val rotationMatrix = FloatArray(9)
            val orientation = FloatArray(3)

            if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientation)
            } else {
                // Fallback for older TYPE_ORIENTATION
                orientation[0] = event.values[0]
            }

            var azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
            azimuth = (azimuth + 360) % 360

            // Rotate the compass image smoothly
            val rotateAnimation = RotateAnimation(
                currentAzimuth,
                -azimuth,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
            )
            rotateAnimation.duration = 250
            rotateAnimation.fillAfter = true
            compassImage.startAnimation(rotateAnimation)
            currentAzimuth = -azimuth

            val bearingInt = azimuth.roundToInt()
            bearingText.text = "Bearing: $bearingInt°"
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for now
    }
}
