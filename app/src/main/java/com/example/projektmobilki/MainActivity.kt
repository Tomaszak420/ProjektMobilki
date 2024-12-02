package com.example.projektmobilki

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var locationManager: LocationManager
    private lateinit var locationTextView: TextView
    private lateinit var Pogoda: TextView
    private val apiKey = "68b83895141bb2f9f99a65f7055049d9"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        locationTextView = findViewById(R.id.locationTextView)
        Pogoda = findViewById(R.id.Pogoda)
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
                1000L, // Minimalny czas między aktualizacjami (ms)
                1f // Minimalna odległość między aktualizacjami (metry)
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
            locationTextView.text = "Szerokość: $latitude\nDługość: $longitude"
            getWeather(latitude, longitude)
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
    private fun getWeather(latitude: Double, longitude: Double) {
        val urlString = "https://api.openweathermap.org/data/2.5/weather?lat=$latitude&lon=$longitude&appid=$apiKey&units=metric"

        thread {
            try {
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                val inputStream = connection.inputStream
                val reader = BufferedReader(InputStreamReader(inputStream))
                val response = reader.readText()

                // Przetwarzanie odpowiedzi JSON
                val jsonResponse = JSONObject(response)
                val main = jsonResponse.getJSONObject("main")
                val temp = main.getDouble("temp")
                val weatherArray = jsonResponse.getJSONArray("weather")
                val description = weatherArray.getJSONObject(0).getString("description")

                runOnUiThread {
                    Pogoda.text = "Temperatura: $temp°C\nOpis: $description"
                }

                reader.close()
                inputStream.close()
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Pogoda.text = "Nie udało się pobrać pogody"
                }
            }
        }
    }
}
