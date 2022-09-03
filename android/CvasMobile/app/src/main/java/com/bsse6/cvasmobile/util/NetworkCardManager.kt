package com.bsse6.cvasmobile.util

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.*
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import android.provider.Settings
import android.util.Log


class NetworkCardManager(context : Context) {
    //this class will handle all wifi associated operations
    private val mContext = context
    val TAG  = javaClass.simpleName

    private var hadConnection = false
    private var lowSignal  = false
    private var  netId : Int? = null


    private val mWifiManager = mContext.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val cm =      mContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val mWifiConfig   = WifiConfiguration()

    private val intentFilter = IntentFilter().apply {
        addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
        addAction(WifiManager.RSSI_CHANGED_ACTION)
    }


    //Wi-Fi Broadcaster
    private val wifiScanReceiver = object : BroadcastReceiver(){
        override fun onReceive(context: Context, intent: Intent) {
            when(intent.action){

                WifiManager.RSSI_CHANGED_ACTION -> {
                    val rssi = intent.getIntExtra(WifiManager.EXTRA_NEW_RSSI,1)
                    lowSignal = rssi < -80

                    Log.d(TAG, "new rssi is $rssi")
                }

                WifiManager.NETWORK_STATE_CHANGED_ACTION -> {
                    val info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO) as NetworkInfo?
                    info?.let {
                        if (info.isConnected) {
                            hadConnection = true


                            Log.d(TAG, "CONNECTED to wifi")

                        } else if (info.isConnectedOrConnecting) {
                            Log.d(TAG,"is connecting to WIFI")

                        } else {

                            Log.d(TAG,"Discoonected From WIFI")
                        }
                    }

                }

            }

        }
    }


    fun unregisterCard(){
        mContext.unregisterReceiver(wifiScanReceiver)
    }
    fun registerCard(){
        mContext.registerReceiver(wifiScanReceiver,intentFilter)
    }

    fun connectedToCamera() : Boolean{
        val ssid : String = mWifiManager.connectionInfo.ssid
        return ssid == "\"BSE22-6-FY-PROJECT\""
    }
    /**
     * Check if the wifi is enabled
     */
    fun isWifiEnabled(): Boolean {
        return mWifiManager.isWifiEnabled
    }
    /**
     * Request user to switch on wifi
      */
    fun requestWifiOn() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            val panelIntent = Intent(Settings.Panel.ACTION_WIFI)
            panelIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            mContext.startActivity(panelIntent)
        }else{
            val intent = Intent(WifiManager.ACTION_PICK_WIFI_NETWORK)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            mContext.startActivity(intent)
        }
    }

    /**
     * insert wifi configurations  and connect to the wifi
     */
    fun insertWifiConfigurations(ssid: String, password: String) {
        //if  android below 10
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val wifiConfig = WifiConfiguration()
            wifiConfig.SSID = String.format("\"%s\"", ssid)
            wifiConfig.preSharedKey = String.format("\"%s\"", password)
            val netId = mWifiManager.addNetwork(wifiConfig)
            mWifiManager.disconnect()
            mWifiManager.enableNetwork(netId, true)
            mWifiManager.reconnect()
        } else {
            val wifiNetworkSpecifier = WifiNetworkSpecifier.Builder()
                .setSsid(ssid)
                .setWpa2Passphrase(password)
                .build()
            val networkRequest = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .setNetworkSpecifier(wifiNetworkSpecifier)
                .build()
            val connectivityManager =
                mContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            connectivityManager.requestNetwork(networkRequest, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    connectivityManager.bindProcessToNetwork(network)
                }
            })
        }

    }

    companion object{
        @SuppressLint("StaticFieldLeak")
        var instance : NetworkCardManager? = null

        fun getNetworkManagerInstance(context : Context) : NetworkCardManager {
            if(instance == null){
                instance = NetworkCardManager(context)
            }

            return instance as NetworkCardManager
        }

        fun getAlreadyNetMan() : NetworkCardManager {
            return instance as NetworkCardManager
        }
    }

}