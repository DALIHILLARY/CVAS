package com.bsse6.cvasmobile

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bsse6.cvasmobile.services.DaemonService

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
            if(!DaemonService.isDaemonRunning){
                DaemonService.startService(this, DaemonService.NAVIGATION)
            }else if(DaemonService.getDaemon() == DaemonService.NAVIGATION){
                DaemonService.stopService(this)
            }else{
                DaemonService.setDaemonMode(DaemonService.NAVIGATION)
            }
        }
        trackingButton.setOnClickListener {
            if(!DaemonService.isDaemonRunning){
                DaemonService.startService(this, DaemonService.TRACKING)
            }else if(DaemonService.getDaemon() == DaemonService.TRACKING){
                DaemonService.stopService(this)
            }else{
                DaemonService.setDaemonMode(DaemonService.TRACKING)
            }
        }
        backgroundButton.setOnClickListener {
            Toast.makeText(this,"CVAS in now running in background",Toast.LENGTH_LONG).show()
            //Send application to background
            val home = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK

            }
            startActivity(home)
        }
        exploreButton.setOnClickListener {
            if(!DaemonService.isDaemonRunning){
                DaemonService.startService(this, DaemonService.EXPLORE)
            }else if(DaemonService.getDaemon() == DaemonService.EXPLORE){
                DaemonService.stopService(this)
            }else{
                DaemonService.setDaemonMode(DaemonService.EXPLORE)
            }
        }
    }
}