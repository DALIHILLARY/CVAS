package com.bsse6.cvasmobile.services

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.bsse6.cvasmobile.MainActivity
import com.bsse6.cvasmobile.R
import com.bsse6.cvasmobile.util.NotifyChannel
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import java.nio.ByteBuffer

class ExploratoryService : Service() {
    companion object {
        var isServiceRunning = false
        val uri = URI("ws://192.168.4.1:86/")
        @SuppressLint("StaticFieldLeak")
        private lateinit var mContext : Context
        fun startService( context: Context){
            mContext = context
            if(!isServiceRunning){
                isServiceRunning = true
                val startIntent = Intent(context, ExploratoryService::class.java)
                ContextCompat.startForegroundService(context,startIntent)
            }
        }

        fun stopService(context: Context){
            mContext = context
            isServiceRunning = false
            val stopIntent = Intent(context, ExploratoryService::class.java)
            context.stopService(stopIntent)
        }
    }
    private val TAG = javaClass.simpleName

    private val mWebSocketClient = object : WebSocketClient(uri) {
        override fun onOpen(handshakedata: ServerHandshake?) {
            Log.d(TAG, "SOCKET CONNECTION OPENED")
        }

        override fun onMessage(message: String?) {
            Log.d(TAG,"Receive")

        }

        override fun onMessage(message: ByteBuffer) {
            val imageBytes = message.array()
            //create a bitmap
            val bmp = BitmapFactory.decodeByteArray(imageBytes,0,imageBytes.size)
            bmp?.let{image ->
                val matrix = Matrix()
                matrix.postRotate(90F)
                val bmp_transpose = Bitmap.createBitmap(image, 0, 0, image.width, image.height, matrix, true)
                val imageRatio = bmp_transpose.height.toFloat() / bmp_transpose.width.toFloat()
               
                try{
                    Handler(Looper.getMainLooper()).post {
                        val activity = mContext as MainActivity
                        val previewScreen = activity.findViewById<ImageView>(R.id.preview_screen)
                        val viewWidth = previewScreen.width
                        val dispViewH = (viewWidth * imageRatio).toInt()
                        previewScreen.setImageBitmap(Bitmap.createScaledBitmap(bmp_transpose,viewWidth,dispViewH, false))
                    }
                }catch (e: Exception) {
                    Log.e(TAG,"Error processing image",e)
                }

            }
        }

        override fun onClose(code: Int, reason: String?, remote: Boolean) {
            Log.d(TAG,"Socket connection was closed $reason")
        }

        override fun onError(ex: Exception?) {
            if (ex != null) {
                Log.e(TAG,"An error occurred ${ex.message}")
            }
//            TODO("Not yet implemented")
        }

    }

    override fun onCreate() {
        super.onCreate()
//        TODO("initialize all resources")
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        isServiceRunning = true
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotifyChannel.createNotificationChannel(this@ExploratoryService)
        }
        val notificationIntent = Intent(this@ExploratoryService, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this@ExploratoryService,0,notificationIntent,0)
        val notification = NotificationCompat.Builder(this@ExploratoryService,NotifyChannel.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_logo)
            .setContentTitle("CVAS")
            .setContentText("Exploratory Service Running")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        startForeground(23256, notification)
        mWebSocketClient.connect()
        return START_STICKY
    }

    override fun onDestroy() {
        isServiceRunning = false
        mWebSocketClient.close()
        super.onDestroy()
//        TODO("Stop everything")
    }
    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

}