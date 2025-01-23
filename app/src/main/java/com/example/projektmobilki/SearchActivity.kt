package com.example.projektmobilki

import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class SearchActivity : AppCompatActivity() {
    private lateinit var locationManager: LocationManager
    private lateinit var locationDisplay : TextView
    private lateinit var timezoneDisplay : TextView
    private lateinit var utcDisplay : TextView
    private lateinit var timeDisplay : TextView
    private lateinit var searchAddressButton : Button
    private lateinit var searchCoordinatesButton : Button
    private lateinit var longitudeInput : EditText
    private lateinit var latitudeInput : EditText
    private lateinit var addressInput : EditText
    private lateinit var goMapButton : Button
    private lateinit var mapView: MapView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        // TextViews
        locationDisplay = findViewById(R.id.location_text_view)
        timezoneDisplay = findViewById(R.id.timezone_display)
        utcDisplay = findViewById(R.id.utc_display)
        timeDisplay = findViewById(R.id.time_display)
        // Buttons
        searchAddressButton = findViewById(R.id.search_address_button)
        searchCoordinatesButton = findViewById(R.id.search_coordinates_button)
        goMapButton = findViewById(R.id.button_go_map)
        // Input fields
        addressInput = findViewById(R.id.address_input)
        longitudeInput = findViewById(R.id.longitude_input)
        latitudeInput = findViewById(R.id.latitude_input)
        mapView = findViewById(R.id.mapView)

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        Configuration.getInstance().load(applicationContext, androidx.preference.PreferenceManager.getDefaultSharedPreferences(applicationContext))
        // Button setup
        searchAddressButton.setOnClickListener {
            val input = addressInput.text.toString()
            val address = getAddressFromString(input)

            if (address != null) {
                fetchIPGeolocationData(address.latitude, address.longitude)
                val addressDisplayedText = address.getAddressLine(0)
                displayTitleUI(addressDisplayedText)
            }
            else {
                displayTitleUI("Nie udało się pobrać danych lokalizacji.")
            }
        }

        searchCoordinatesButton.setOnClickListener {
            try {
                val longitude = longitudeInput.text.toString().toDouble()
                val latitude = latitudeInput.text.toString().toDouble()
                val locationName = getLocationName(latitude, longitude)

                if (locationName != null) {
                    displayTitleUI(locationName)
                    fetchIPGeolocationData(latitude, longitude)
                }
                else {
                    displayTitleUI("Nie udało się pobrać danych lokalizacji.")
                }
            }
            catch (e: NumberFormatException) {
                e.printStackTrace()
                displayTitleUI("Nie udało się pobrać danych lokalizacji.")
            }
        }

        goMapButton.setOnClickListener {
            val locationName = locationDisplay.text.toString()
            if (locationName.isNotEmpty()) {
                val address = getAddressFromString(locationName)
                if (address != null) {
                    val location = Location("").apply {
                        latitude = address.latitude
                        longitude = address.longitude
                    }
                    updateMapLocation(location)
                } else {
                    displayErrorMessageUI("Nie znaleziono lokalizacji: $locationName")
                }
            } else {
                displayErrorMessageUI("Pole lokalizacji jest puste.")
            }
        }


    }

    private fun getAddressFromString(addressString: String) : Address? {
        try {
            val geocoder = Geocoder(this)
            val addressList = geocoder.getFromLocationName(addressString, 1);

            if (!addressList.isNullOrEmpty()) {
                val address = addressList[0]
                return address
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun getLocationName(latitude: Double, longitude: Double) : String? {
        try {
            val geocoder = Geocoder(this)
            val addressList = geocoder.getFromLocation(latitude, longitude, 1)

            if (!addressList.isNullOrEmpty()) {
                val address = addressList[0]
                val locationName = address.getAddressLine(0)
                return locationName
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun fetchIPGeolocationData(latitude: Double, longitude: Double) {
        thread {
            try {
                //Klucz API
                val apiKey = "5da52d7151834ecbba079aa4ab4d836b"
                val url = URL("https://api.ipgeolocation.io/timezone?apiKey=$apiKey&lat=$latitude&long=$longitude")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val jsonObject = JSONObject(response)

                    // Pobieranie danych
                    val timezone = jsonObject.getString("timezone")
                    val utcOffset = jsonObject.getString("timezone_offset")
                    val currentTime = jsonObject.getString("time_24")

                    displayTimeDataUI(timezone, utcOffset, currentTime)
                } else {
                    displayErrorMessageUI("Błąd API: $responseCode")
                }
                connection.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
                displayErrorMessageUI("Błąd podczas połączenia z API")
            }
        }
    }

    private fun displayTimeDataUI(timezone: String, utcOffset: String, currentTime: String) {
        runOnUiThread {
            timezoneDisplay.text = timezone
            utcDisplay.text = utcOffset
            timeDisplay.text = currentTime
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


    private fun displayErrorMessageUI(message: String) {
        runOnUiThread {
            locationDisplay.text = message
        }
    }

    private fun displayTitleUI(text: String) {
        locationDisplay.text = text
    }

}