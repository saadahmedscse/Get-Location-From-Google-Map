package com.saadahmedsoft.locationfinder

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.saadahmedsoft.locationfinder.databinding.ActivityMapBinding
import com.saadahmedsoft.locationfinder.utils.delay
import java.io.IOException
import java.util.*

class MapActivity : AppCompatActivity(), OnMapReadyCallback, LocationListener,
    GoogleMap.OnCameraIdleListener, GoogleMap.OnCameraMoveListener,
    GoogleMap.OnCameraMoveStartedListener {

    private lateinit var binding: ActivityMapBinding

    private var mMap: GoogleMap? = null
    private lateinit var mapView: SupportMapFragment

    private val TAG = "location_debug"

    private val defaultZoom = 15f
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mapView = supportFragmentManager.findFragmentById(R.id.map_view) as SupportMapFragment
        mapView.getMapAsync(this)

        delay(1000) {
            mMap!!.setOnCameraIdleListener(this)
            mMap!!.setOnCameraMoveListener(this)
            mMap!!.setOnCameraMoveStartedListener(this)
        }
    }

    override fun onMapReady(p0: GoogleMap) {
        mapView.onResume()
        mMap = p0
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ), 20
            )
        } else {
            mMap!!.isMyLocationEnabled = true
            mMap!!.uiSettings.isZoomControlsEnabled = true
        }
    }

    override fun onLocationChanged(p0: Location) {
        Log.d(TAG, "onLocationChanged: ")
        val geoCoder = Geocoder(this, Locale.getDefault())
        val address: List<Address>?

        try {
            address = geoCoder.getFromLocation(p0.latitude, p0.longitude, 1)
            setAddress(address[0])
        } catch (e: Exception) {
        }
    }

    private fun setAddress(address: Address?) {
        if (address != null) {
            if (address.getAddressLine(0) != null) {
                binding.addressLine.text = address.getAddressLine(0)
                binding.country.text = address.countryName
                binding.state.text = address.adminArea
            }
        }
    }

    override fun onCameraIdle() {
        Log.d(TAG, "onCameraIdle: ")
        val geoCoder = Geocoder(this, Locale.getDefault())
        val address: List<Address>?

        try {
            address = geoCoder.getFromLocation(
                mMap!!.cameraPosition.target.latitude,
                mMap!!.cameraPosition.target.longitude,
                1
            )
            setAddress(address[0])
        } catch (e: Exception) {
        }
    }

    override fun onCameraMove() {
        Log.d(TAG, "onCameraMove: ")
    }

    override fun onCameraMoveStarted(p0: Int) {
        Log.d(TAG, "onCameraMoveStarted: ")
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 20) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation()
            } else {
                //AppConstants.showSnackBar(this, binding.root, "You need to allow permission", AppConstants.SNACK_LONG)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        try {
            val location = fusedLocationProviderClient!!.lastLocation

            location.addOnCompleteListener {
                if (it.isSuccessful) {
                    val currLocation = it.result
                    if (currLocation != null) {
                        moveCamera(
                            LatLng(currLocation.latitude, currLocation.longitude),
                            defaultZoom
                        )
                    }
                } else {
                    //AppConstants.showSnackBar(requireContext(), binding.root, "Location not found", AppConstants.SNACK_SHORT)
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "getCurrentLocation: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    private fun moveCamera(latLng: LatLng, mapZoom: Float) {
        mMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, mapZoom))

        val geoCoder = Geocoder(this, Locale.getDefault())
        try {
            val addresses: List<Address> =
                geoCoder.getFromLocation(latLng.latitude, latLng.longitude, 10)
            val address = addresses[1]
            binding.addressLine.text = address.getAddressLine(0)
            binding.country.text = address.countryName
            binding.state.text = address.adminArea
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}