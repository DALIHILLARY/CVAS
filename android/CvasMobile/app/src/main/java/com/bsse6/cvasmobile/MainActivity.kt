package com.bsse6.cvasmobile

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.viewModelScope
import com.bsse6.cvasmobile.services.DaemonService
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

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

        val viewModel: MainActivityViewModel by viewModels()

        viewModel.viewModelScope.launch {

            viewModel.getBitmap().collectLatest {image ->
                val matrix = Matrix()
//                      matrix.postRotate(90F)
                val bmp_transpose = Bitmap.createBitmap(image, 0, 0, image.width, image.height, matrix, true)
                val imageRatio = bmp_transpose.height.toFloat() / bmp_transpose.width.toFloat()

                try{
                        val previewScreen = this@MainActivity.findViewById<ImageView>(R.id.preview_screen)
                        val viewWidth = previewScreen.width
                        val dispViewH = (viewWidth * imageRatio).toInt()
                        previewScreen.setImageBitmap(Bitmap.createScaledBitmap(bmp_transpose,viewWidth,dispViewH, false))

                }catch (e: Exception) {
                    Log.e(TAG,"Error processing image",e)
                }
            }
        }

        //set click listeners here
        settingsButton.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
        navigationButton.setOnClickListener {
            if(!DaemonService.isDaemonRunning){
                DaemonService.startService(this, DaemonService.NAVIGATION)
                Toast.makeText(this,"Navigation mode on",Toast.LENGTH_LONG).show()
            }else if(DaemonService.getDaemon() == DaemonService.NAVIGATION){
                DaemonService.stopService(this)
                Toast.makeText(this,"Navigation mode stopped",Toast.LENGTH_LONG).show()
            }else{
                DaemonService.setDaemonMode(DaemonService.NAVIGATION)
                Toast.makeText(this,"Navigation mode on",Toast.LENGTH_LONG).show()
            }
        }
        trackingButton.setOnClickListener {
            if(!DaemonService.isDaemonRunning){
                DaemonService.startService(this, DaemonService.TRACKING)
                Toast.makeText(this,"Tracking Mode on",Toast.LENGTH_LONG).show()
            }else if(DaemonService.getDaemon() == DaemonService.TRACKING){
                DaemonService.stopService(this)
                Toast.makeText(this,"Tracking mode stopped",Toast.LENGTH_LONG).show()
            }else{
                DaemonService.setDaemonMode(DaemonService.TRACKING)
                Toast.makeText(this,"Tracking Mode on",Toast.LENGTH_LONG).show()

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
                Toast.makeText(this,"Explore mode on",Toast.LENGTH_LONG).show()

            }else if(DaemonService.getDaemon() == DaemonService.EXPLORE){
                DaemonService.stopService(this)
                Toast.makeText(this,"Explore mode stopped",Toast.LENGTH_LONG).show()
            }else{
                DaemonService.setDaemonMode(DaemonService.EXPLORE)
                Toast.makeText(this,"Explore mode on",Toast.LENGTH_LONG).show()
            }
        }
    }
}