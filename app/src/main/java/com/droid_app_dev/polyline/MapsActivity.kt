package com.droid_app_dev.polyline

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.droid_app_dev.polyline.databinding.ActivityMapsBinding
import com.google.android.gms.common.api.Status
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
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

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding

    // lateinit var latlong:Place
    var placesClient: PlacesClient? = null

    companion object {
        private const val TAG = "MapsActivity"
        private const val LOCATION_PERMISSION_REQUEST_CODE = 999
        private const val PICKUP_REQUEST_CODE = 1
        private const val DROP_REQUEST_CODE = 2
    }

    private var pickUpLatLng: LatLng? = null
    private var dropLatLng: LatLng? = null
    private var originMarker: Marker? = null
    private var greyPolyLine: Polyline? = null
    private var blackPolyline: Polyline? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

        mMap.clear()

        pickUpLatLng?.let {
            MarkerOptions().position(it).title("Marker in Sydney")
        }?.let { mMap.addMarker(it) }


         dropLatLng?.let {
            MarkerOptions().position(it).title("Marker in Sydney")
        }?.let { mMap.addMarker(it) }




        pickUpLatLng?.let { CameraUpdateFactory.newLatLng(it) }?.let { mMap.moveCamera(it) }

        mMap.addPolyline(
            PolylineOptions().add(pickUpLatLng, dropLatLng)
                .width // below line is use to specify the width of poly line.
                    (5f) // below line is use to add color to our poly line.
                .color(Color.RED) // below line is to make our poly line geodesic.
                .geodesic(true)
        )
        // on below line we will be starting the drawing of polyline.
        // on below line we will be starting the drawing of polyline.
        pickUpLatLng?.let { CameraUpdateFactory.newLatLngZoom(it, 13f) }
            ?.let { mMap.moveCamera(it) }


    }
}