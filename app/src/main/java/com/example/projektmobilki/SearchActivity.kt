package com.example.projektmobilki

import android.location.Address
import android.location.Geocoder
import android.location.LocationManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
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
        // Input fields
        addressInput = findViewById(R.id.search_address_input)
        longitudeInput = findViewById(R.id.longitude_input)
        latitudeInput = findViewById(R.id.latitude_input)

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

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


    private fun displayErrorMessageUI(message: String) {
        runOnUiThread {
            locationDisplay.text = message
        }
    }

    private fun displayTitleUI(text: String) {
        locationDisplay.text = text
    }

}