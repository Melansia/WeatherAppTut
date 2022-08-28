package com.example.weatherappv2

import android.app.Dialog
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.google.gson.Gson
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

class MainActivity : AppCompatActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_main)

            CallAPILoginAsyncTask().execute()
        }

    private inner class CallAPILoginAsyncTask() : AsyncTask<Any, Void, String>() {

        private lateinit var customProgressDialog: Dialog
        override fun onPreExecute() {
            super.onPreExecute()
            showProgressDialog()
        }

        override fun doInBackground(vararg p0: Any?): String {
            var result: String
            var connection: HttpURLConnection? = null

            try {
                val url = URL("https://run.mocky.io/v3/054b6253-aca8-4042-aec8-0c628e8b3062")
                connection = url.openConnection() as HttpURLConnection
                connection.doInput = true
                connection.doOutput = true

                val httpResult = connection.responseCode
                if (httpResult == HttpURLConnection.HTTP_OK) {
                    val inputStream = connection.inputStream
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val stringBuilder = StringBuilder()
                    var line: String?
                    try {
                        while (reader.readLine().also { line = it } != null) {
                            stringBuilder.append(line + "\n")
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    } finally {
                        try {
                            inputStream.close()
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                    result = stringBuilder.toString()
                } else {
                    result = connection.responseMessage
                }
            } catch (e: SocketTimeoutException) {
                result = "Connection Timeout"
            } catch (e: Exception) {
                result = "Error: ${e.message}"
            } finally {
                connection?.disconnect()
            }
            return result
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            cancelProgressDialog()

            Log.i("JSON RESPONSE RESULT", result?: "null")

            val responseData = Gson().fromJson(result, ResponseData::class.java)
            Log.i("City", "City Name: ${responseData.name}")
            Log.i("Temp", "Temperature: ${responseData.main.temp}")
            Log.i("Outside", "Feels like: ${responseData.main.feels_like}")
            Log.i("Weather", "Weather: ${responseData.weather[0].main}")
            Log.i("Description", "Description: ${responseData.weather[0].description}")
            Log.i("Wind", "Wind Speed: ${responseData.wind.speed}")
        }

        private fun showProgressDialog() {
            customProgressDialog = Dialog(this@MainActivity)
            customProgressDialog.setContentView(R.layout.dialog_custom_progress)
            customProgressDialog.show()
        }

        private fun cancelProgressDialog() {
            customProgressDialog.dismiss()
        }
    }
}