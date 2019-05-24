package com.github.yuukis.mtfuji_locator

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.RuntimePermissions
import kotlin.math.*

@RuntimePermissions
class MainActivity : AppCompatActivity(), LocationListener {

    companion object {
        val MTFUJI_LOCATION = arrayOf(35.360496, 138.727284)
    }

    var locationManager: LocationManager? = null
    var bestProvider: String? = null
    var currentLocation: Location? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    @SuppressLint("NoDelegateOnResumeDetector")
    override fun onResume() {
        super.onResume()

        startUpdatingLocationWithPermissionCheck()
    }

    override fun onPause() {
        super.onPause()

        stopUpdatingLocation()
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
            Log.d("Distance", dist.toString())
            val azim = calcAzimuth(lng, lat)
            Log.d("Azimuth", azim.toString())
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
