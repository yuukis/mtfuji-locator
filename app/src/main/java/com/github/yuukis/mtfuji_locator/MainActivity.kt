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

@RuntimePermissions
class MainActivity : AppCompatActivity(), LocationListener {

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

}
