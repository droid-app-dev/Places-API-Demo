package com.droid_app_dev.polyline

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.droid_app_dev.PermissionUtils
import com.droid_app_dev.polyline.databinding.ActivityMapsBinding
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.internal.ViewUtils
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.maps.android.SphericalUtil

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding

    // lateinit var latlong:Place
    var placesClient: PlacesClient? = null
    private lateinit var locationCallback: LocationCallback

    companion object {
        private const val TAG = "MapsActivity"
        private const val LOCATION_PERMISSION_REQUEST_CODE = 999
        private const val PICKUP_REQUEST_CODE = 1
        private const val DROP_REQUEST_CODE = 2
    }

    private var pickUpLatLng: LatLng? = null
    private var dropLatLng: LatLng? = null
    private var currentLatLng: LatLng? = null
    private var originMarker: Marker? = null
    private var greyPolyLine: Polyline? = null
    private var blackPolyline: Polyline? = null
    private lateinit var fusedLocationProviderClient:FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        com.droid_app_dev.ViewUtils.enableTransparentStatusBar(window)

        val apiKey = getString(R.string.google_maps_key)
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, apiKey)
        }

        // Create a new Places client instance.
        placesClient = Places.createClient(this)


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.pickUpTextView.setOnClickListener {
            launchLocationAutoCompleteActivity(1)
        }

        binding.dropTextView.setOnClickListener {
            launchLocationAutoCompleteActivity(2)
        }
        binding.search.setOnClickListener{
            drawPolyline()
        }

    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
    }

    private fun launchLocationAutoCompleteActivity(requestCode: Int) {
        val fields: List<Place.Field> =
            listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG)
        val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
            .build(this)
        startActivityForResult(intent, requestCode)
    }

    /*fun showPath(latLngList: List<LatLng>) {
       val builder = LatLngBounds.Builder()
       for (latLng in latLngList) {
           builder.include(latLng)
       }
       val bounds = builder.build()
       mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 2))
       val polylineOptions = PolylineOptions()
       polylineOptions.color(Color.GRAY)
       polylineOptions.width(5f)
       polylineOptions.addAll(latLngList)
       greyPolyLine = mMap.addPolyline(polylineOptions)

       val blackPolylineOptions = PolylineOptions()
       blackPolylineOptions.width(5f)
       blackPolylineOptions.color(Color.BLACK)
       blackPolyline = mMap.addPolyline(blackPolylineOptions)

       originMarker = addOriginDestinationMarkerAndGet(latLngList[0])
       originMarker?.setAnchor(0.5f, 0.5f)
       destinationMarker = addOriginDestinationMarkerAndGet(latLngList[latLngList.size - 1])
       destinationMarker?.setAnchor(0.5f, 0.5f)

       val polylineAnimator = AnimationUtils.polyLineAnimator()
       polylineAnimator.addUpdateListener { valueAnimator ->
           val percentValue = (valueAnimator.animatedValue as Int)
           val index = (greyPolyLine?.points!!.size * (percentValue / 100.0f)).toInt()
           blackPolyline?.points = greyPolyLine?.points!!.subList(0, index)
       }
       polylineAnimator.start()
   }
*/


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICKUP_REQUEST_CODE || requestCode == DROP_REQUEST_CODE) {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    val place = Autocomplete.getPlaceFromIntent(data!!)
                    Log.d(TAG, "Place: " + place.name + ", " + place.id + ", " + place.latLng)
                    when (requestCode) {
                        PICKUP_REQUEST_CODE -> {
                            binding.pickUpTextView.text = place.name
                            pickUpLatLng = place.latLng
                            //  checkAndShowRequestButton()
                        }

                        DROP_REQUEST_CODE -> {
                            binding.dropTextView.text = place.name
                            dropLatLng = place.latLng
                            // checkAndShowRequestButton()
                        }
                    }
                }

                AutocompleteActivity.RESULT_ERROR -> {
                    val status: Status = Autocomplete.getStatusFromIntent(data!!)
                    Log.d(TAG, status.statusMessage!!)
                }

                Activity.RESULT_CANCELED -> {
                    Log.d(TAG, "Place Selection Canceled")
                }
            }
        }
    }

    fun drawPolyline() {
        // inside on map ready method
        // we will be displaying polygon on Google Maps.
        // on below line we will be adding polyline on Google Maps.
        // inside on map ready method
        // we will be displaying polygon on Google Maps.
        // on below line we will be adding polyline on Google Maps.

      var  distance = String.format("%.2f", SphericalUtil.computeDistanceBetween(pickUpLatLng, dropLatLng)/1000)+" KM"

        Toast.makeText(this,"Distance :- $distance",Toast.LENGTH_LONG).show()

        mMap.clear()

        pickUpLatLng?.let {
            MarkerOptions().position(it).title("0 KM")
        }?.let { mMap.addMarker(it) }


         dropLatLng?.let {
            MarkerOptions().position(it).title(distance)
        }?.let { mMap.addMarker(it) }





        mMap.addPolyline(
            PolylineOptions().add(pickUpLatLng, dropLatLng)
                .width // below line is use to specify the width of poly line.
                    (10f) // below line is use to add color to our poly line.
                .color(Color.RED) // below line is to make our poly line geodesic.
                .geodesic(true)

        )

        dropLatLng?.let { CameraUpdateFactory.newLatLng(it) }?.let { mMap.moveCamera(it) }

        // on below line we will be starting the drawing of polyline.
        // on below line we will be starting the drawing of polyline.

        moveCamera(dropLatLng)


    }

    override fun onStart() {
        super.onStart()


        if (currentLatLng == null) {
            when {
                PermissionUtils.isAccessFineLocationGranted(this) -> {
                    when {
                        PermissionUtils.isLocationEnabled(this) -> {
                            setUpLocationListener()
                        }
                        else -> {
                            PermissionUtils.showGPSNotEnabledDialog(this)
                        }
                    }
                }
                else -> {
                    PermissionUtils.requestAccessFineLocationPermission(
                        this,
                        LOCATION_PERMISSION_REQUEST_CODE
                    )
                }
            }
        }
    }

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val location = locationResult.lastLocation
            Log.d("LocationCall", "onLocationResult: $location")
            if (location != null) {
                //lat = location.latitude.toString()
                //lng = location.longitude.toString()
               // saveLocation()
            }
        }
    }
    private fun setUpLocationListener() {

        val locationRequest=LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY,1000)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(1000)
            .setMinUpdateIntervalMillis(2000)
            .build()

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest, mLocationCallback,
            Looper.myLooper()!!
        )


          locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                if (currentLatLng == null) {
                    for (location in locationResult.locations) {
                        if (currentLatLng == null) {
                            currentLatLng = LatLng(location.latitude, location.longitude)
                            setCurrentLocationAsPickUp()
                            enableMyLocationOnMap()
                            moveCamera(currentLatLng)
                            animateCamera(currentLatLng)
                        }
                    }
                }
                // Few more things we can do here:
                // For example: Update the location of user on server
            }
        }
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        fusedLocationProviderClient?.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.myLooper()
        )
    }

    private fun enableMyLocationOnMap() {
        mMap.setPadding(0, com.droid_app_dev.ViewUtils.dpToPx(48f), 0, 0)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        mMap.isMyLocationEnabled = true


    }
    private fun moveCamera(latLng: LatLng?) {
        latLng?.let { CameraUpdateFactory.newLatLng(it) }?.let { mMap.moveCamera(it) }
    }

    private fun animateCamera(latLng: LatLng?) {
        val cameraPosition = latLng?.let { CameraPosition.Builder().target(it).zoom(15.5f).build() }
        cameraPosition?.let { CameraUpdateFactory.newCameraPosition(it) }
            ?.let { mMap.animateCamera(it) }
    }

    private fun setCurrentLocationAsPickUp() {
        pickUpLatLng = currentLatLng
        binding.pickUpTextView.text = getString(R.string.current_location)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    when {
                        PermissionUtils.isLocationEnabled(this) -> {
                            setUpLocationListener()
                        }
                        else -> {
                            PermissionUtils.showGPSNotEnabledDialog(this)
                        }
                    }
                } else {
                    Toast.makeText(
                        this,
                        getString(R.string.location_permission_not_granted),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

}