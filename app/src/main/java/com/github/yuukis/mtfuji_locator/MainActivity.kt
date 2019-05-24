package com.github.yuukis.mtfuji_locator

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.RuntimePermissions
import kotlin.math.*

@RuntimePermissions
class MainActivity : AppCompatActivity(), LocationListener, SensorEventListener {

    companion object {
        val MTFUJI_LOCATION = arrayOf(35.360496, 138.727284)
    }

    var locationManager: LocationManager? = null
    var sensorManager: SensorManager? = null
    var bestProvider: String? = null
    var currentLocation: Location? = null
    var distanceToMtFuji: Float = 0f
    var azimuthToMtFuji: Int = 0
    var azimuthBySensor: Int = 0
    var accelerometerValues: FloatArray? = null
    var magneticFieldValues: FloatArray? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    @SuppressLint("NoDelegateOnResumeDetector")
    override fun onResume() {
        super.onResume()

        startUpdatingLocationWithPermissionCheck()
        sensorManager?.let { manager ->
            val delay = SensorManager.SENSOR_DELAY_UI
            manager.registerListener(this, manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), delay)
            manager.registerListener(this, manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), delay)
        }
    }

    override fun onPause() {
        super.onPause()

        stopUpdatingLocation()
        sensorManager?.unregisterListener(this)
    }

    fun initLocationManager() {
        val manager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val criteria = Criteria()
        criteria.powerRequirement = Criteria.POWER_MEDIUM
        criteria.isSpeedRequired = false
        criteria.isAltitudeRequired = false
        criteria.isBearingRequired = false
        val provider = manager.getBestProvider(criteria, true)
        locationManager = manager
        bestProvider = provider
    }

    @SuppressLint("MissingPermission")
    @NeedsPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    fun startUpdatingLocation() {
        initLocationManager()

        if (bestProvider != null) {
            locationManager?.requestLocationUpdates(bestProvider, 60000, 10f, this)
        }
    }

    fun stopUpdatingLocation() {
        locationManager?.removeUpdates(this)
    }

    override fun onLocationChanged(location: Location?) {
        Log.d("onLocationChanged", "${location?.toString()}")

        currentLocation = location
        location?.let {
            val lat = it.latitude.toFloat()
            val lng = it.longitude.toFloat()

            val dist = calcDistance(lng, lat)
            val azim = calcAzimuth(lng, lat)

            distanceToMtFuji = dist
            azimuthToMtFuji = azim
            showResult(distanceToMtFuji, azimuthToMtFuji, azimuthBySensor)
        }
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
        //
    }

    override fun onProviderEnabled(provider: String?) {
        //
    }

    override fun onProviderDisabled(provider: String?) {
        //
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        //
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            when (it.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    accelerometerValues = it.values.clone()
                }
                Sensor.TYPE_MAGNETIC_FIELD -> {
                    magneticFieldValues = it.values.clone()
                }
                else -> return
            }
            if (accelerometerValues != null && magneticFieldValues != null) {
                val rotationMatrix = FloatArray(16)
                val inclinationMatrix = FloatArray(16)
                val remapedMatrix = FloatArray(16)
                val orientationValues = FloatArray(3)
                SensorManager.getRotationMatrix(rotationMatrix, inclinationMatrix, accelerometerValues, magneticFieldValues)
                SensorManager.remapCoordinateSystem(rotationMatrix, SensorManager.AXIS_X, SensorManager.AXIS_Z, remapedMatrix)
                SensorManager.getOrientation(remapedMatrix, orientationValues)
                azimuthBySensor = rad2deg(orientationValues[0].toDouble())
                showResult(distanceToMtFuji, azimuthToMtFuji, azimuthBySensor)
            }
        }
    }

    fun showResult(distance: Float, azimuth: Int, sensorAzimuth: Int) {
        val textView = findViewById<TextView>(R.id.text1)
        val distanceKm = (distance / 1000)
        textView.text = """
DST: $distanceKm KM
AZ: $azimuth
SENSOR_AZ $sensorAzimuth
"""
    }

    private fun calcDistance(x0: Float, y0: Float): Float {
        val (y, x) = MTFUJI_LOCATION
        val r = 6378.137 * 1000
        val rad = 180/PI
        val d = r * acos(sin(y0/rad)*sin(y/rad) + cos(y0/rad)*cos(y/rad)*cos((x - x0)/rad))
        return d.toFloat()
    }

    private fun calcAzimuth(x0: Float, y0: Float): Int {
        val (y, x) = MTFUJI_LOCATION
        val rad = 180/PI
        val a = atan2(sin((x - x0)/rad), cos(y0/rad)*tan(y/rad) - sin(y0/rad)*cos((x - x0)/rad))
        return rad2deg(a)
    }

    private fun rad2deg(angrad: Double): Int {
        return Math.floor(
            if (angrad >= 0)
                Math.toDegrees(angrad)
            else
                360 + Math.toDegrees(angrad)
        ).toInt()
    }
}
