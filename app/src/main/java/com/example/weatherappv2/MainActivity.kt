package com.example.weatherappv2

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.weatherappv2.models.WeatherResponse
import com.example.weatherappv2.network.WeatherService
import com.google.android.gms.location.*
import com.google.gson.Gson
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private var customProgressDialog: Dialog? = null

    private lateinit var ivWeather: ImageView
    private lateinit var tvMainWeather: TextView
    private lateinit var tvWeatherDescription: TextView

    private lateinit var ivMinMax: ImageView
    private lateinit var tvMin: TextView
    private lateinit var tvMax: TextView

    private lateinit var ivHumidity: ImageView
    private lateinit var tvTemp: TextView
    private lateinit var tvHumidity: TextView

    private lateinit var ivWind: ImageView
    private lateinit var tvWindSpeed: TextView
    private lateinit var tvSpeedUnit: TextView

    private lateinit var ivLocation: ImageView
    private lateinit var tvName: TextView
    private lateinit var tvCountry: TextView

    private lateinit var ivSunrise: ImageView
    private lateinit var tvSunriseTime: TextView

    private lateinit var ivSunset: ImageView
    private lateinit var tvSunsetTime: TextView

    private var latitude: Double = 0.0
    private var longitude: Double = 0.0

    private lateinit var mSharedPreferences: SharedPreferences

    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ivWeather = findViewById(R.id.ivWeather)
        tvMainWeather = findViewById(R.id.tvMainWeather)
        tvWeatherDescription = findViewById(R.id.tvWeatherDescription)

        ivMinMax = findViewById(R.id.ivMinMax)
        tvMin = findViewById(R.id.tvMin)
        tvMax = findViewById(R.id.tvMax)

        ivHumidity = findViewById(R.id.ivHumidity)
        tvTemp = findViewById(R.id.tvTemp)
        tvHumidity = findViewById(R.id.tvHumidity)

        ivWind = findViewById(R.id.ivWind)
        tvWindSpeed = findViewById(R.id.tvWindSpeed)
        tvSpeedUnit = findViewById(R.id.tvSpeedUnit)

        ivLocation = findViewById(R.id.ivLocation)
        tvName = findViewById(R.id.tvName)
        tvCountry = findViewById(R.id.tvCountry)

        ivSunrise = findViewById(R.id.ivSunrise)
        tvSunriseTime = findViewById(R.id.tvSunriseTime)

        ivSunset = findViewById(R.id.ivSunset)
        tvSunsetTime = findViewById(R.id.tvSunsetTime)

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this@MainActivity)
        mSharedPreferences = getSharedPreferences(Constants.PREFERENCE_NAME, Context.MODE_PRIVATE)

        setUpUI()

        if (!isLocationEnabled()) {
            Toast.makeText(
                this,
                "Your location provider is turned off. Please turn it on.",
                Toast.LENGTH_SHORT
            ).show()
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        } else {
            Dexter.withActivity(this)
                .withPermissions(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ).withListener(object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(p0: MultiplePermissionsReport?) {
                        if (p0!!.areAllPermissionsGranted()) {

                            requestLocationData()

                        }
                        if (p0.isAnyPermissionPermanentlyDenied) {
                            Toast.makeText(
                                this@MainActivity,
                                "You have denied location permission. They are mandatory for the app to work, please enable them.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        p0: MutableList<PermissionRequest>?,
                        p1: PermissionToken?
                    ) {
                        showRationalDialogForPermissions()
                    }

                }).onSameThread()
                .check()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                requestLocationData()
                getLocationWeatherDetails()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    @SuppressLint("MissingPermission")
    private fun requestLocationData() {
        val mLocationRequest = LocationRequest()
        mLocationRequest.priority =
            LocationRequest.PRIORITY_HIGH_ACCURACY

        mFusedLocationClient.requestLocationUpdates(
            mLocationRequest, mLocationCallback, Looper.myLooper()
        )
    }

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(p0: LocationResult) {
            super.onLocationResult(p0)
            val mLastLocation: Location? = p0.lastLocation
            latitude = mLastLocation!!.latitude
            Log.i("Current Latitude", "$latitude")

            longitude = mLastLocation.longitude
            Log.i("Current Longitude", "$longitude")
            getLocationWeatherDetails()
        }
    }

    private fun getLocationWeatherDetails() {
        if (Constants.isNetworkAvailable(this@MainActivity)) {
            val retrofit: Retrofit = Retrofit.Builder().baseUrl(Constants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create()).build()

            val service: WeatherService =
                retrofit.create<WeatherService>(WeatherService::class.java)

            val listCall: Call<WeatherResponse> = service.getWeather(
                latitude, longitude, Constants.METRIC_UNIT, Constants.APP_ID
            )

            showProgressDialog()

            listCall.enqueue(object : Callback<WeatherResponse> {
                @RequiresApi(Build.VERSION_CODES.N)
                @SuppressLint("SetTextI18n")
                override fun onResponse(
                    call: Call<WeatherResponse>,
                    response: Response<WeatherResponse>
                ) {
                    if (response.isSuccessful) {
                        hideProgressDialog()

                        val weatherList: WeatherResponse? = response.body()
                        val weatherResponseJsonString = Gson().toJson(weatherList)
                        val editor = mSharedPreferences.edit()
                        editor.putString(Constants.WEATHER_RESPONSE_DATA, weatherResponseJsonString)
                            .apply()

                        setUpUI()

                        Log.i("Response Result", "$weatherList")
                    } else {
                        when (response.code()) {
                            400 -> {
                                Log.e("Error 400", "Bad Connection")
                            }
                            404 -> {
                                Log.e("Error 404", "Not Found")
                            }
                            else -> {
                                Log.e("Error ", "Generic Error")
                            }
                        }
                    }
                }

                override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                    Log.e(TAG , t.message.toString())
                    hideProgressDialog()
                }
            })
        }
    }

    private fun showRationalDialogForPermissions() {
        AlertDialog.Builder(this)
            .setMessage("It looks like you have turned off permission required for this feature. It can be enabled under Application Settings")
            .setPositiveButton("GO TO SETTINGS") { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }.show()
    }

    private fun isLocationEnabled(): Boolean {

        // This provides access to the system location services
        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun showProgressDialog() {
        customProgressDialog = Dialog(this@MainActivity)
        customProgressDialog!!.setContentView(R.layout.dialog_custom_progress)
        customProgressDialog!!.show()
    }

    private fun hideProgressDialog() {
        customProgressDialog?.dismiss()
    }

    @SuppressLint("SetTextI18n")
    private fun setUpUI() {

        val weatherResponseJsonString =
            mSharedPreferences.getString(Constants.WEATHER_RESPONSE_DATA, null)

        if (!weatherResponseJsonString.isNullOrEmpty()) {
            val weatherList =
                Gson().fromJson(weatherResponseJsonString, WeatherResponse::class.java)

            for (i in weatherList.weather.indices) {
                Log.i("Weather Name", weatherList.weather.toString())

                tvMainWeather.text = weatherList.weather[i].main
                tvWeatherDescription.text = weatherList.weather[i].description

                tvTemp.text =
                    weatherList.main.temp.toString() + getUnit(application.resources.configuration.locales.toString())
                tvHumidity.text = weatherList.main.humidity.toString() + " per cent"

                tvMin.text = weatherList.main.temp_min.toString() + " min"
                tvMax.text = weatherList.main.temp_max.toString() + " max"

                tvWindSpeed.text = weatherList.wind.speed.toString()

                tvName.text = weatherList.name
                tvCountry.text = weatherList.sys.country

                tvSunriseTime.text = unixTime(weatherList.sys.sunrise)
                tvSunsetTime.text = unixTime(weatherList.sys.sunset)

                when (weatherList.weather[i].icon) {
                    "01d" -> ivWeather.setImageResource(R.drawable.sunny)
                    "02d" -> ivWeather.setImageResource(R.drawable.cloud)
                    "03d" -> ivWeather.setImageResource(R.drawable.cloud)
                    "04d" -> ivWeather.setImageResource(R.drawable.cloud)
                    "04n" -> ivWeather.setImageResource(R.drawable.cloud)
                    "10d" -> ivWeather.setImageResource(R.drawable.rain)
                    "11d" -> ivWeather.setImageResource(R.drawable.storm)
                    "13d" -> ivWeather.setImageResource(R.drawable.snowflake)
                    "01n" -> ivWeather.setImageResource(R.drawable.cloud)
                    "02n" -> ivWeather.setImageResource(R.drawable.cloud)
                    "03n" -> ivWeather.setImageResource(R.drawable.cloud)
                    "10n" -> ivWeather.setImageResource(R.drawable.cloud)
                    "11n" -> ivWeather.setImageResource(R.drawable.rain)
                    "13n" -> ivWeather.setImageResource(R.drawable.snowflake)
                }

            }
        }
    }

    private fun getUnit(unit: String): String {
        Log.i("Unit", unit)
        var unit = "°C"
        if ("US" == unit || "LR" == unit || "MM" == unit) {
            unit = "°F"
        }
        return unit
    }

    @SuppressLint("SimpleDateFormat")
    private fun unixTime(timex: Long): String? {
        val date = Date(timex * 1000L)
        val sdf = SimpleDateFormat("HH:mm")
        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(date)
    }
}