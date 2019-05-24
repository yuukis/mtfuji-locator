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
            val sensor = manager.getDefaultSensor(Sensor.TYPE_ORIENTATION)
            manager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
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
            showResult(dist, azim)
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
            if (it.sensor.type == Sensor.TYPE_ORIENTATION) {
                val sensorAzimuth = it.values[0]
                Log.d("onSensorChanged", "Azimuth: $sensorAzimuth")
            }
        }
    }

    fun showResult(distance: Float, azimuth: Float) {
        val textView = findViewById<TextView>(R.id.text1)
        val distanceKm = (distance / 1000).toInt()
        textView.text = "DST: $distanceKm KM\nAZ: ${azimuth.toInt()}"
    }

    private fun calcDistance(x0: Float, y0: Float): Float {
        val (y, x) = MTFUJI_LOCATION
        val r = 6378.137 * 1000
        val rad = 180/PI
        val d = r * acos(sin(y0/rad)*sin(y/rad) + cos(y0/rad)*cos(y/rad)*cos((x - x0)/rad))
        return d.toFloat()
    }

    private fun calcAzimuth(x0: Float, y0: Float): Float {
        val (y, x) = MTFUJI_LOCATION
        val rad = 180/PI
        var a = atan2(sin((x - x0)/rad), cos(y0/rad)*tan(y/rad) - sin(y0/rad)*cos((x - x0)/rad))*rad
        if (a < 0) {
            a += 360
        }
        return a.toFloat()
    }
}
