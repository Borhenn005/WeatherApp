package com.example.weatherapp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.example.weatherapp.POJO.ModelClass
import com.example.weatherapp.Utilities.ApiUtitlities
import com.example.weatherapp.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.*
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var activityMainBinding: ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activityMainBinding = DataBindingUtil.setContentView(this,R.layout.activity_main)
        supportActionBar?.hide()

        /*
            Hide Main Layout when start activity until getting the location from user
         */
        activityMainBinding.rlMainLayout.visibility = View.GONE

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        getCurrentLocation()

        activityMainBinding.etGetCityName.setOnEditorActionListener { v, actionId, keyEvent ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                getCityWeather(activityMainBinding.etGetCityName.text.toString())
                val view = this.currentFocus
                if (view != null) {
                    val imm: InputMethodManager =
                        getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(view.windowToken, 0)
                    activityMainBinding.etGetCityName.clearFocus()
                }
                true

            } else false
            }
    }

    private fun getCityWeather(cityName: String) {
        activityMainBinding.pbLoading.visibility = View.VISIBLE
        ApiUtitlities.getApiInterface()?.getCityWeatherData(cityName, API_KEY)?.enqueue(object : Callback<ModelClass>
        {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onResponse(call: Call<ModelClass>, response: Response<ModelClass>) {
                setDataOnView(response.body())
            }

            override fun onFailure(call: Call<ModelClass>, t: Throwable) {
                Toast.makeText(applicationContext,"Not a valid city Name ... ",Toast.LENGTH_SHORT).show()
            }

        })
    }


    private fun getCurrentLocation() {
        if(checkPermissions()){
            if(isLocationEnabled()){

                /*
                    EveryThing is Well
                    Display Result Here ...
                */
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    requestPermissionsFromUser()
                    return
                }
                fusedLocationProviderClient.lastLocation.addOnCompleteListener(this){ task ->
                    val location : Location? = task.result

                    if(location == null){
                        Toast.makeText(this,"Null Result Recieved", Toast.LENGTH_SHORT).show()
                    }
                    else{
                            /*
                                FETCH THE WEATHER AFTER GETTING THE LOCATION
                            */

                            fetchCurrentLocationWeather(location.latitude.toString(),location.longitude.toString())
                    }
                }
            }
            else{
                // Settings open Here ...
                Toast.makeText(this,"Turn On Location", Toast.LENGTH_SHORT).show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        }
        else{
            //request Permission From User ...

            requestPermissionsFromUser()
        }
    }

    private fun fetchCurrentLocationWeather(latitude: String, longitude: String) {
        activityMainBinding.pbLoading.visibility = View.VISIBLE

        ApiUtitlities.getApiInterface()?.getCurrentWeatherData(latitude,longitude,API_KEY)?.enqueue(object : Callback<ModelClass>{
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onResponse(call: Call<ModelClass>, response: Response<ModelClass>) {
                if(response.isSuccessful){
                    setDataOnView(response.body())
                }
            }

            override fun onFailure(call: Call<ModelClass>, t: Throwable) {
                Toast.makeText(applicationContext,"ERROR",Toast.LENGTH_SHORT).show()
            }

        })
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setDataOnView(response: ModelClass?) {
        val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm")
        val currentDate = sdf.format(Date())

        activityMainBinding.tvDateAndTime.text = currentDate

        activityMainBinding.tvDayMaxTemp.text = "Day " + kelvinToCelsius(response!!.main.temp_max) + "°"
        activityMainBinding.tvDayMinTemp.text = "Night " + kelvinToCelsius(response!!.main.temp_min) + "°"
        activityMainBinding.tvTemp.text = "" + kelvinToCelsius(response!!.main.temperature) + "°"
        activityMainBinding.tvFeelsLike.text = "Feels alike " + kelvinToCelsius(response!!.main.feels_like) + "°"


        activityMainBinding.tvWeatherType.text = response.weather[0].main

        activityMainBinding.tvSunset.text = timeStampToLocalDate(response.sys.sunset.toLong())
        activityMainBinding.tvSunrise.text = timeStampToLocalDate(response.sys.sunrise.toLong())


        activityMainBinding.tvPressure.text = response.main.pressure.toString()
        activityMainBinding.tvHumidity.text = response.main.humidity.toString() + " %"
        activityMainBinding.tvWindSpeed.text = response.wind.speed.toString() + " m/s"

        activityMainBinding.tvTempFarenhite.text = "" + ((kelvinToCelsius(response.main.temperature)).times(1.8).plus(32).roundToInt())

        activityMainBinding.etGetCityName.setText(response.name)


        updateUI(response.weather[0].id)


    }

    private fun updateUI(id : Int){
        if(id in 200..232){
            /*
                THUNDERSTORM WEATHER
            */
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)

            window.statusBarColor = resources.getColor(R.color.thunderstorm)
            activityMainBinding.rlToolbar.setBackgroundColor(resources.getColor(R.color.thunderstorm))

            activityMainBinding.rlSubLayout.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.thunderstorm_bg
            )

            activityMainBinding.llBgBelow.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.thunderstorm_bg
            )

            activityMainBinding.llBgAbove .background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.thunderstorm_bg
            )

            activityMainBinding.imgWeatherBg.setImageResource(R.drawable.thunderstorm_bg)
            activityMainBinding.imgWeatherIcon.setImageResource(R.drawable.ic_clear)
        }else if(id in 300..321){
            /*
                Drizzle WEATHER
            */
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)

            window.statusBarColor = resources.getColor(R.color.drizzle)
            activityMainBinding.rlToolbar.setBackgroundColor(resources.getColor(R.color.drizzle))

            activityMainBinding.rlSubLayout.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.drizzle_bg
            )

            activityMainBinding.llBgBelow.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.drizzle_bg
            )

            activityMainBinding.llBgAbove .background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.drizzle_bg
            )

            activityMainBinding.imgWeatherBg.setImageResource(R.drawable.drizzle_bg)
            activityMainBinding.imgWeatherIcon.setImageResource(R.drawable.ic_clear)
        }else if(id in 500..531){
            /*
                Rain WEATHER
            */
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)

            window.statusBarColor = resources.getColor(R.color.rain)
            activityMainBinding.rlToolbar.setBackgroundColor(resources.getColor(R.color.rain))

            activityMainBinding.rlSubLayout.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.rain_bg
            )

            activityMainBinding.llBgBelow.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.rain_bg
            )

            activityMainBinding.llBgAbove .background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.rain_bg
            )

            activityMainBinding.imgWeatherBg.setImageResource(R.drawable.rain_bg)
            activityMainBinding.imgWeatherIcon.setImageResource(R.drawable.ic_clear)
        }else if(id in 600..622){
            /*
                Snow WEATHER
            */
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)

            window.statusBarColor = resources.getColor(R.color.snow)
            activityMainBinding.rlToolbar.setBackgroundColor(resources.getColor(R.color.snow))

            activityMainBinding.rlSubLayout.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.snow_bg
            )

            activityMainBinding.llBgBelow.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.snow_bg
            )

            activityMainBinding.llBgAbove .background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.snow_bg
            )

            activityMainBinding.imgWeatherBg.setImageResource(R.drawable.snow_bg)
            activityMainBinding.imgWeatherIcon.setImageResource(R.drawable.ic_clear)
        }else if(id in 701..781){
            /*
                Atmosphere WEATHER
            */
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)

            window.statusBarColor = resources.getColor(R.color.atmosphere)
            activityMainBinding.rlToolbar.setBackgroundColor(resources.getColor(R.color.atmosphere))

            activityMainBinding.rlSubLayout.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.mist_bg
            )

            activityMainBinding.llBgBelow.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.mist_bg
            )

            activityMainBinding.llBgAbove .background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.mist_bg
            )

            activityMainBinding.imgWeatherBg.setImageResource(R.drawable.mist_bg)
            activityMainBinding.imgWeatherIcon.setImageResource(R.drawable.ic_clear)
        }else if(id == 800){
            /*
                Clear WEATHER
            */
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)

            window.statusBarColor = resources.getColor(R.color.clear)
            activityMainBinding.rlToolbar.setBackgroundColor(resources.getColor(R.color.clear))

            activityMainBinding.rlSubLayout.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.clear_bg
            )

            activityMainBinding.llBgBelow.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.clear_bg
            )

            activityMainBinding.llBgAbove .background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.clear_bg
            )

            activityMainBinding.imgWeatherBg.setImageResource(R.drawable.clear_bg)
            activityMainBinding.imgWeatherIcon.setImageResource(R.drawable.ic_clear)
        }else if(id in 801..804){
            /*
                Clouds WEATHER
            */
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)

            window.statusBarColor = resources.getColor(R.color.clouds)
            activityMainBinding.rlToolbar.setBackgroundColor(resources.getColor(R.color.clouds))

            activityMainBinding.rlSubLayout.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.clouds_bg
            )

            activityMainBinding.llBgBelow.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.clouds_bg
            )

            activityMainBinding.llBgAbove .background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.clouds_bg
            )

            activityMainBinding.imgWeatherBg.setImageResource(R.drawable.clouds_bg)
            activityMainBinding.imgWeatherIcon.setImageResource(R.drawable.ic_clear)
        }


        activityMainBinding.pbLoading.visibility = View.GONE
        activityMainBinding.rlMainLayout.visibility = View.VISIBLE
    }

    private fun kelvinToCelsius(temp : Double) : Double{
        var intTemp = temp
        intTemp = intTemp.minus(273)

        return intTemp.toBigDecimal().setScale(1, RoundingMode.UP).toDouble()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun timeStampToLocalDate(timeStamp : Long) : String{
        val localTime = timeStamp.let {
            Instant.ofEpochSecond(it)
                .atZone(ZoneId.systemDefault())
                .toLocalTime()
        }

        return localTime.toString()
    }


    private fun requestPermissionsFromUser() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ),
            PERMISSION_REQUEST_ACCESS_LOCATION
        )
    }

    companion object {
        private const val PERMISSION_REQUEST_ACCESS_LOCATION = 100

        const val API_KEY = "0feffc0b4bd0ed4ebc87fcedf3d6d96e"
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager : LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER)
    }

    private fun checkPermissions(): Boolean {
        val coarseLocation = ActivityCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val fineLocation = ActivityCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if( coarseLocation && fineLocation){
            return true
        }
        return false
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(requestCode == PERMISSION_REQUEST_ACCESS_LOCATION){
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(applicationContext,"Granted", Toast.LENGTH_SHORT).show()
                getCurrentLocation()
            }
            else{
                Toast.makeText(applicationContext,"Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}