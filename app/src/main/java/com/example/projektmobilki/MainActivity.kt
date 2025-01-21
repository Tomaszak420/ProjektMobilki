package com.example.projektmobilki

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.concurrent.thread


class MainActivity : AppCompatActivity() {

    private lateinit var locationManager: LocationManager
    private lateinit var locationTextView: TextView
    private lateinit var StrefaCzasowaData: TextView
    private lateinit var PrzesuniecieUTCData: TextView
    private lateinit var GodzinaData: TextView
    private lateinit var LookupTimezone: TextView
    private lateinit var LookupUTCData: TextView
    private lateinit var LookupDateTime: TextView
    private lateinit var AddressInput: EditText
    private lateinit var SearchButton: Button
    private lateinit var GoSearchButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        locationTextView = findViewById(R.id.locationTextView)
        StrefaCzasowaData = findViewById(R.id.StrefaCzasowaData)
        PrzesuniecieUTCData = findViewById(R.id.PrzesuniecieUTCData)
        GodzinaData = findViewById(R.id.GodzinaData)

        LookupTimezone = findViewById(R.id.lookup_timezone_display)
        LookupUTCData = findViewById(R.id.lookup_UTC_display)
        LookupDateTime = findViewById(R.id.lookup_date_time_display)

        AddressInput = findViewById(R.id.address_input)
        SearchButton = findViewById(R.id.address_button)
        GoSearchButton = findViewById(R.id.button)

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        // Sprawdź uprawnienia i uzyskaj lokalizację
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        } else {
            getLocation()
        }

        SearchButton.setOnClickListener {
            val input = AddressInput.text.toString()
            val address = getAddressFromString(input)

            if (address != null) {
                val timeData = getIPGeolocationData(address.latitude, address.longitude)
                updateSearchedLocationUI(address.getAddressLine(0), timeData)
            }
            else {
                locationTextView.text = "Nie udało się pobrać danych lokalizacji."
            }
        }

        GoSearchButton.setOnClickListener {
            val intent = Intent(this, SearchActivity::class.java)
            startActivity(intent)
        }
    }

    private fun updateSearchedLocationUI(addressLine: String, timeData: Array<String>) {
        locationTextView.text = addressLine
        if (timeData.size == 3) {
            LookupTimezone.text = timeData[0]
            LookupUTCData.text = timeData[1]
            LookupDateTime.text = timeData[2]
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
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun updateLocationText(location: Location?) {
        location?.let {
            val latitude = it.latitude
            val longitude = it.longitude
            getLocationName(latitude, longitude)
            fetchIPGeolocationData(latitude, longitude)
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

    private fun getLocationName(latitude: Double, longitude: Double) {
        try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val addressList = geocoder.getFromLocation(latitude, longitude, 1)

            if (addressList != null && addressList.isNotEmpty()) {
                val address = addressList[0]
                val locationName = address.getAddressLine(0)

                runOnUiThread {
                    locationTextView.text = "Lokalizacja: $locationName"
                }
            } else {
                runOnUiThread {
                    locationTextView.text = "Nie udało się znaleźć lokalizacji"
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            runOnUiThread {
                locationTextView.text = "Błąd przy pobieraniu lokalizacji"
            }
        }
    }

    private fun getIPGeolocationData(latitude: Double, longitude: Double) : Array<String> {
        var timezone: String = ""
        var utcOffset: String = ""
        var currentTime: String = ""

            try {
                // Ustaw swój klucz API
                val apiKey = "5da52d7151834ecbba079aa4ab4d836b"
                val url =
                    URL("https://api.ipgeolocation.io/timezone?apiKey=$apiKey&lat=$latitude&long=$longitude")
                //'https://api.ipgeolocation.io/timezone?apiKey=API_KEY&tz=America/Los_Angeles'
                //https://ipgeolocation.io/timezone-api.html?state=TimeZone#documentation-overview
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val jsonObject = JSONObject(response)

                    // Pobieranie danych
                    timezone = jsonObject.getString("timezone")
                    utcOffset = jsonObject.getString("timezone_offset")
                    currentTime = jsonObject.getString("time_24")

                    Log.d("kod",timezone)
                    Log.d("kod",utcOffset)
                    Log.d("kod",currentTime)
                } else {
                    // Jesli cos sie z API wykrzaczy
                }
                connection.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }


        return arrayOf(timezone, utcOffset, currentTime)
    }

    private fun fetchIPGeolocationData(latitude: Double, longitude: Double) {
        val zmienna: String
        thread {
            try {
                // Ustaw swój klucz API
                val apiKey = "5da52d7151834ecbba079aa4ab4d836b"
                val url = URL("https://api.ipgeolocation.io/timezone?apiKey=$apiKey&lat=$latitude&long=$longitude")
                                    //'https://api.ipgeolocation.io/timezone?apiKey=API_KEY&tz=America/Los_Angeles'
                                    //https://ipgeolocation.io/timezone-api.html?state=TimeZone#documentation-overview
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val jsonObject = JSONObject(response)

                    // Pobieranie danych

                    val timezone = jsonObject.getString("timezone")
                    Log.d("kod",timezone)
                    val utcOffset = jsonObject.getString("timezone_offset")
                    Log.d("kod",utcOffset)
                    val currentTime = jsonObject.getString("time_24")
                    Log.d("kod",currentTime)

                    // Aktualizuj UI
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
