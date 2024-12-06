package com.example.projektmobilki
import android.location.Geocoder
import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var locationManager: LocationManager
    private lateinit var locationTextView: TextView



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        locationTextView = findViewById(R.id.locationTextView)



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




}
