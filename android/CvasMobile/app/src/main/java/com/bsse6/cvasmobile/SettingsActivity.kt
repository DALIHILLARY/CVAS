package com.bsse6.cvasmobile

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
    }
}

val specifier = WifiNetworkSpecifier.Builder()
    .setSsidPattern(PatternMatcher("test", PatternMatcher.PATTERN_PREFIX))
    .setBssidPattern(MacAddress.fromString("10:03:23:00:00:00"), MacAddress.fromString("ff:ff:ff:00:00:00"))
    .build()

val request = NetworkRequest.Builder()
    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
    .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    .setNetworkSpecifier(specifier)
    .build()

val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

val networkCallback = object : ConnectivityManager.NetworkCallback() {
    ...
    override fun onAvailable(network: Network?) {
        // do success processing here..
    }

    override fun onUnavailable() {
        // do failure processing here..
    }
    ...
}
connectivityManager.requestNetwork(request, networkCallback)
...
// Release the request when done.
connectivityManager.unregisterNetworkCallback(networkCallback)

