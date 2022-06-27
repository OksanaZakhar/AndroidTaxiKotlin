package com.ksusha.vel.taxik

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentSender.SendIntentException
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.ksusha.vel.taxik.databinding.ActivityDriverMapsBinding

class DriverMapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityDriverMapsBinding


    private val CHECK_SETTINGS_CODE = 111
    private val REQUEST_LOCATION_PERMISSION = 222

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var settingsClient: SettingsClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationSettingsRequest: LocationSettingsRequest
    private lateinit var locationCallback: LocationCallback
    private var currentLocation: Location = Location("LongPressLocationProvider")

    private var isLocationUpdatesActive = false
    private lateinit var context: Context


    private lateinit var auth: FirebaseAuth
    private lateinit var currentUser: FirebaseUser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        auth = FirebaseAuth.getInstance()
        currentUser = auth.currentUser!!

        binding = ActivityDriverMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.singOutButton.setOnClickListener {
            auth.signOut()
            signOutDriver()
        }


        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_driver) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices
            .getFusedLocationProviderClient(this)
        settingsClient = LocationServices.getSettingsClient(this)

        buildLocationRequest()
        buildLocationCallBack()
        buildLocationSettingsRequest()

        startLocationUpdates()

    }

    private fun signOutDriver() {
        val driverUserId = currentUser.uid
        val drivers = FirebaseDatabase.getInstance().reference
            .child(ChildDBFirebase.DRIVER.title)
        val geoFire = GeoFire(drivers)
        geoFire.removeLocation(driverUserId)
        val intent = Intent(
            this@DriverMapsActivity,
            UserSingInActivity::class.java
        )
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        updateLocationUi()
    }

    private fun buildLocationRequest() {
        locationRequest = LocationRequest.create()
        locationRequest.interval = 10000
        locationRequest.fastestInterval = 3000
        locationRequest.priority = Priority.PRIORITY_HIGH_ACCURACY
    }

    private fun buildLocationCallBack() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                currentLocation = locationResult.lastLocation!!
                updateLocationUi()
            }
        }
    }

    private fun buildLocationSettingsRequest() {
        val builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(locationRequest)
        locationSettingsRequest = builder.build()
    }

    private fun startLocationUpdates() {
        isLocationUpdatesActive = true
        settingsClient.checkLocationSettings(locationSettingsRequest)
            .addOnSuccessListener(this,
                OnSuccessListener {
                    if (ActivityCompat.checkSelfPermission(
                            this@DriverMapsActivity,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) !=
                        PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat
                            .checkSelfPermission(
                                this@DriverMapsActivity,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            ) !=
                        PackageManager.PERMISSION_GRANTED
                    ) {
                        return@OnSuccessListener
                    }
                    fusedLocationClient.requestLocationUpdates(
                        locationRequest,
                        locationCallback,
                        Looper.myLooper()
                    )
                    updateLocationUi()
                })
            .addOnFailureListener(this) { e ->
                val statusCode = (e as ApiException).statusCode
                when (statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> try {
                        val resolvableApiException = e as ResolvableApiException
                        resolvableApiException.startResolutionForResult(
                            this@DriverMapsActivity,
                            CHECK_SETTINGS_CODE
                        )
                    } catch (sie: SendIntentException) {
                        sie.printStackTrace()
                    }
                    LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                        val message = "Adjust location settings on your device"
                        Toast.makeText(
                            this@DriverMapsActivity, message,
                            Toast.LENGTH_LONG
                        ).show()
                        isLocationUpdatesActive = false
                    }
                }
                updateLocationUi()
            }
    }

    private fun stopLocationUpdates() {
        if (!isLocationUpdatesActive) {
            return
        }
        fusedLocationClient.removeLocationUpdates(locationCallback)
            .addOnCompleteListener(
                this
            ) { isLocationUpdatesActive = false }
    }

    private fun updateLocationUi() {
        if (currentLocation != null) {
            val driverLocation = LatLng(
                currentLocation.latitude,
                currentLocation.longitude
            )
            mMap.moveCamera(CameraUpdateFactory.newLatLng(driverLocation))
            mMap.animateCamera(CameraUpdateFactory.zoomTo(10f))
            mMap.addMarker(MarkerOptions().position(driverLocation).title("Driver Location"))
            val driverUserId = currentUser.uid
            val drivers = FirebaseDatabase.getInstance().reference
                .child(ChildDBFirebase.DRIVER.title)
            val geoFire = GeoFire(drivers)
            geoFire.setLocation(
                driverUserId,
                GeoLocation(currentLocation.latitude, currentLocation.longitude)
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            CHECK_SETTINGS_CODE -> when (resultCode) {
                RESULT_OK -> {
                    Log.d(
                        "MainActivity", "User has agreed to change location" +
                                "settings"
                    )
                    startLocationUpdates()
                }
                RESULT_CANCELED -> {
                    Log.d(
                        "MainActivity", "User has not agreed to change location" +
                                "settings"
                    )
                    isLocationUpdatesActive = false
                    updateLocationUi()
                }
            }
        }
    }

    private fun requestLocationPermission() {
        val shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (shouldProvideRationale) {
            showSnackBar(
                "Location permission is needed for " +
                        "app functionality",
                "OK",
                View.OnClickListener {
                    ActivityCompat.requestPermissions(
                        this@DriverMapsActivity, arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ),
                        REQUEST_LOCATION_PERMISSION
                    )
                }
            )
        } else {
            ActivityCompat.requestPermissions(
                this@DriverMapsActivity, arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION
                ),
                REQUEST_LOCATION_PERMISSION
            )
        }
    }

    private fun showSnackBar(
        mainText: String,
        action: String,
        listener: View.OnClickListener
    ) {
        Snackbar.make(
            findViewById(android.R.id.content),
            mainText,
            Snackbar.LENGTH_INDEFINITE
        )
            .setAction(
                action,
                listener
            )
            .show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.size <= 0) {
                Log.d(
                    "onRequestPermissions",
                    "Request was cancelled"
                )
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (isLocationUpdatesActive) {
                    startLocationUpdates()
                }
            } else {
                showSnackBar(
                    "Turn on location on settings",
                    "Settings"
                ) {
                    val intent = Intent()
                    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    val uri = Uri.fromParts(
                        "package",
                        context.packageName,
                        null
                    )
                    intent.data = uri
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                }
            }
        }
    }

    private fun checkLocationPermission(): Boolean {
        val permissionState = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        return permissionState == PackageManager.PERMISSION_GRANTED
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    override fun onResume() {
        super.onResume()
        if (isLocationUpdatesActive && checkLocationPermission()) {
            startLocationUpdates()
        } else if (!checkLocationPermission()) {
            requestLocationPermission()
        }
    }


}










