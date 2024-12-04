package com.example.projektmobilki
import android.location.Geocoder
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var locationManager: LocationManager
    private lateinit var locationTextView: TextView
    private lateinit var Pogoda: TextView
    private lateinit var Opis: TextView

    private lateinit var GodzinaPrognozy: TextView
    private lateinit var TempPrognozy: TextView
    private lateinit var OpisPrognozy: TextView
    private val apiKey = "68b83895141bb2f9f99a65f7055049d9"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        locationTextView = findViewById(R.id.locationTextView)
        Pogoda = findViewById(R.id.Pogoda)
        Opis = findViewById(R.id.Opis)


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
            getLocationName(latitude, longitude)

            getWeather(latitude, longitude)
            getWeatherForecast(latitude, longitude)
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
                    Pogoda.text = "Temperatura: $temp°C"
                    Opis.text ="Opis: $description"
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
    private fun getLocationName(latitude: Double, longitude: Double) {
        try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val addressList = geocoder.getFromLocation(latitude, longitude, 1)

            if (addressList != null && addressList.isNotEmpty()) {
                val address = addressList[0]
                val locationName = address.getAddressLine(0) // pełna nazwa lokalizacji

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
    private fun getWeatherForecast(latitude: Double, longitude: Double) {
        val urlString = "https://api.openweathermap.org/data/2.5/forecast?lat=$latitude&lon=$longitude&appid=$apiKey&units=metric"

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
                val list = jsonResponse.getJSONArray("list") // Lista prognoz dla kolejnych godzin
                val forecastContainer = findViewById<GridLayout>(R.id.forecastContainer)
                runOnUiThread {
                    forecastContainer.removeAllViews()
                }

                for (i in 0 until 6) {
                    val forecastItem = list.getJSONObject(i)
                    val dtTxt = forecastItem.getString("dt_txt")
                    val main = forecastItem.getJSONObject("main")
                    val temp = main.getDouble("temp")
                    val weatherArray = forecastItem.getJSONArray("weather")
                    val description = weatherArray.getJSONObject(0).getString("description")
                    val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

                    // Parsowanie daty na godzinę i minutę
                    val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(dtTxt)
                    val formattedTime = dateFormat.format(date) // Formatowanie do HH:mm
                    val forecastView = TextView(this)
                    forecastView.text = "Godzina: $formattedTime\nTemperatura: $temp°C\n $description"
                    forecastView.setPadding(16, 16, 16, 16)
                    forecastView.textSize = 14f
                    forecastView.setBackgroundColor(Color.WHITE)
                    forecastView.setTextColor(Color.BLACK)
                    runOnUiThread {
                        forecastContainer.addView(forecastView)
                    }
                }

                reader.close()
                inputStream.close()
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {

                }
            }
        }
    }



}
