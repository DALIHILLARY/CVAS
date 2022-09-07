package com.bsse6.cvasmobile

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bsse6.cvasmobile.util.NetworkCardManager
import com.google.android.material.textfield.TextInputEditText

class SettingsActivity : AppCompatActivity() {

    private lateinit var networkCardManager : NetworkCardManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        val connectBtn = findViewById<Button>(R.id.connect_btn)
        val pass = findViewById<TextInputEditText>(R.id.pass_value)
        val ssid = findViewById<TextInputEditText>(R.id.ssid_value)

        networkCardManager = NetworkCardManager.getNetworkManagerInstance(this)

        connectBtn.setOnClickListener {
            //get values from the fields
            val ssidValue = ssid.text.toString()
            val passValue = pass.text.toString()
            //check if the fields are empty
            if (ssidValue.isEmpty() || passValue.isEmpty()) {
                Toast.makeText(this, "Please fill all the fields", Toast.LENGTH_SHORT).show()
            } else {
                //connect to the wifi
                networkCardManager.insertWifiConfigurations(ssidValue, passValue)
            }
        }
        if(!networkCardManager.isWifiEnabled()){
            Toast.makeText(this, "Wifi is disabled Please Enable it", Toast.LENGTH_LONG).show()
            networkCardManager.requestWifiOn()
        }
        //setup wifi listener and check if the wifi is connected
        networkCardManager.wifiListener{
            Toast.makeText(this, "Connected to CVAS camera", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}




