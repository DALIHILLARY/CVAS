package com.bsse6.cvasmobile

import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.bsse6.cvasmobile.services.ExploratoryService

class MainActivity : AppCompatActivity() {

    private val TAG = javaClass.simpleName
    private lateinit var settingsButton : ImageButton
    private lateinit var navigationButton : ImageButton
    private lateinit var trackingButton : ImageButton
    private lateinit var backgroundButton : ImageButton
    private lateinit var exploreButton : ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        settingsButton = findViewById(R.id.settings_option)
        navigationButton = findViewById(R.id.navigation_option)
        trackingButton = findViewById(R.id.track_option)
        backgroundButton = findViewById(R.id.switch_background)
        exploreButton = findViewById(R.id.exploratory_option)

        //set click listeners here
        settingsButton.setOnClickListener {

        }
        navigationButton.setOnClickListener {

        }
        trackingButton.setOnClickListener {

        }
        backgroundButton.setOnClickListener {

        }
        exploreButton.setOnClickListener {
            if(!ExploratoryService.isServiceRunning){
                ExploratoryService.startService(this)
            }else{
                ExploratoryService.stopService(this)
            }
        }
    }
}