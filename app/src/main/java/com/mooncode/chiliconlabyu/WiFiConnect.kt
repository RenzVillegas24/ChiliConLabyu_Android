package com.mooncode.chiliconlabyu

import androidx.fragment.app.Fragment
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.navigation.fragment.findNavController


import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder


class WiFiConnect : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_wi_fi_connect, container, false)
        val btnConnect_v = view.findViewById<Button>(R.id.btnConnect)
        val txtBoxSSID_v = view.findViewById<TextInputEditText>(R.id.txtBxSSID)
        val txtBoxPassword_v = view.findViewById<TextInputEditText>(R.id.txtBxPassword)

        btnConnect_v.setOnClickListener{
            GlobalScope.launch {
                val success = submitFormData(txtBoxSSID_v.text.toString(), txtBoxPassword_v.text.toString())
                launch(Dispatchers.Main) {
                    if (success == HttpURLConnection.HTTP_OK) {
                        Toast.makeText(requireContext(), "Wifi connected to the controller", Toast.LENGTH_SHORT).show()
                    }
                    else if (success == 401) {
                        Toast.makeText(requireContext(), "Incorrect password. Please try again.", Toast.LENGTH_SHORT).show()
                    }
                    else if (success == 400)  {
                        Toast.makeText(requireContext(), "SSID not found. Please enter a valid SSID.", Toast.LENGTH_SHORT).show()
                    }
                    else if (success == 404)  {
                        Toast.makeText(requireContext(), "Bad Request", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        return view
    }



    private suspend fun submitFormData(SSID: String, pass: String): Int {
        return try {
            // Construct the form data in the desired format, e.g., URL-encoded parameters
            val formDataString = "SSID=" + URLEncoder.encode(SSID, "UTF-8") +
                    "&password=" + URLEncoder.encode(pass, "UTF-8")

            // Replace "your_form_endpoint" with the actual URL of the form submission endpoint
            val url = URL("http://8.8.4.4/setup/wificonnect.html")

            Log.d("DATA STRING", formDataString)

            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")


            // Write the form data to the request body
            val writer = OutputStreamWriter(conn.outputStream)
            writer.write(formDataString)
            writer.flush()
            writer.close()


            // Get the response code
            val responseCode = conn.responseCode
            conn.disconnect()

            responseCode
        } catch (e: Exception) {
            e.printStackTrace()

            -1
        }
    }


}