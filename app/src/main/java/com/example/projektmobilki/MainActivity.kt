package com.example.projektmobilki

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var locationManager: LocationManager
    private lateinit var locationTextView: TextView
    private lateinit var StrefaCzasowaData: TextView
    private lateinit var PrzesuniecieUTCData: TextView
    private lateinit var GodzinaData: TextView
    private lateinit var mapView: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(applicationContext, androidx.preference.PreferenceManager.getDefaultSharedPreferences(applicationContext))
        setContentView(R.layout.activity_main)

        locationTextView = findViewById(R.id.locationTextView)
        StrefaCzasowaData = findViewById(R.id.StrefaCzasowaData)
        PrzesuniecieUTCData = findViewById(R.id.PrzesuniecieUTCData)
        GodzinaData = findViewById(R.id.GodzinaData)
        mapView = findViewById(R.id.mapView)

        // Konfiguracja mapy
        mapView.setMultiTouchControls(true)

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        // Sprawdź uprawnienia i uzyskaj lokalizację
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        } else {
            getLocation()
        }
    }

    private fun getLocation() {
        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000L,
                1f
            ) { location ->
                updateLocationText(location)
                updateMapLocation(location)
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun updateLocationText(location: Location?) {
        location?.let {
            val latitude = it.latitude
            val longitude = it.longitude
            locationTextView.text = "Lat: $latitude, Lon: $longitude"
            fetchIPGeolocationData(latitude, longitude)
        }
    }

    private fun updateMapLocation(location: Location?) {
        location?.let {
            val latitude = it.latitude
            val longitude = it.longitude

            // Ustawienie mapy na danej lokalizacji
            val geoPoint = GeoPoint(latitude, longitude)
            mapView.controller.setZoom(15.0)
            mapView.controller.setCenter(geoPoint)

            // Dodanie markera
            val marker = Marker(mapView)
            marker.position = geoPoint
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.title = "Twoja lokalizacja"
            mapView.overlays.clear()
            mapView.overlays.add(marker)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getLocation()
        } else {
            locationTextView.text = "Brak uprawnień do uzyskania lokalizacji"
        }
    }

    private fun fetchIPGeolocationData(latitude: Double, longitude: Double) {
        thread {
            try {
                val apiKey = "5da52d7151834ecbba079aa4ab4d836b"
                val url = URL("https://api.ipgeolocation.io/timezone?apiKey=$apiKey&lat=$latitude&long=$longitude")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val jsonObject = JSONObject(response)

                    val timezone = jsonObject.getString("timezone")
                    val utcOffset = jsonObject.getString("timezone_offset")
                    val currentTime = jsonObject.getString("time_24")

                    runOnUiThread {
                        StrefaCzasowaData.text = " $timezone"
                        PrzesuniecieUTCData.text = "$utcOffset"
                        GodzinaData.text = "$currentTime"
                    }
                } else {
                    runOnUiThread {
                        StrefaCzasowaData.text = "Błąd API: $responseCode"
                    }
                }
                connection.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    StrefaCzasowaData.text = "Błąd podczas połączenia z API"
                }
            }
        }
    }
}
