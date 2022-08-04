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
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.bsse6.cvasmobile.MainActivity
import com.bsse6.cvasmobile.R
import com.bsse6.cvasmobile.util.NotifyChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.vision.detector.ObjectDetector
import java.net.URI
import java.nio.ByteBuffer
import java.util.*

class ExploratoryService : Service(), TextToSpeech.OnInitListener {
    companion object {
        private const val EXPLORATORY_MODEL = "efficientdet_lite0.tflite"
        var isExploratoryRunning = false
        private val uri = URI("ws://192.168.4.1:86/")
        @SuppressLint("StaticFieldLeak")
        private lateinit var mContext : Context
        fun startService( context: Context){
            mContext = context
            if(!isExploratoryRunning){
                isExploratoryRunning = true
                val startIntent = Intent(context, ExploratoryService::class.java)
                ContextCompat.startForegroundService(context,startIntent)
            }
        }

        fun stopService(context: Context){
            mContext = context
            isExploratoryRunning = false
            val stopIntent = Intent(context, ExploratoryService::class.java)
            context.stopService(stopIntent)
        }
    }
    private val TAG = javaClass.simpleName
    private lateinit var objectDetector: ObjectDetector
    private lateinit var imageProcessor: ImageProcessor
    private lateinit var tensorImage: TensorImage
    @Volatile private var isModelRunning = false
    private var startedTtsEngine = false
    private lateinit var tts: TextToSpeech
    private val job = Job()
    private val scope = CoroutineScope(job + Dispatchers.Default)

    /**
     * Run inference using the ML model
     */
    private fun runModel(image : Bitmap) {
        if(!isModelRunning) {
            isModelRunning = true //capture the flag
            Log.e(TAG,System.currentTimeMillis().toString())
            tensorImage.load(image)
            tensorImage = imageProcessor.process(tensorImage)
            val results = objectDetector.detect(tensorImage)
            Log.e(TAG,System.currentTimeMillis().toString())
            results.forEach {
                if(startedTtsEngine) {
                    tts.speak(it.categories[0].label, TextToSpeech.QUEUE_FLUSH, null,"")
                }
                Log.e("prediction", it.toString())
            }
            isModelRunning = false //release the model flag
        }
    }
    /**
     *  listen to websocket events and perform the corresponding required actions
     */
    private val mWebSocketClient = object : WebSocketClient(uri) {
        override fun onOpen(handshakedata: ServerHandshake?) {
            Log.d(TAG, "SOCKET CONNECTION OPENED")
        }

        override fun onMessage(message: String?) {
            Log.d(TAG,"Receive")

        }

        override fun onMessage(message: ByteBuffer) {
            Log.i(TAG,"NEW IMAGE RECEIVED")
            val imageBytes = message.array()
            //create a bitmap
            val bmp = BitmapFactory.decodeByteArray(imageBytes,0,imageBytes.size)
            bmp?.let{ image ->

                val matrix = Matrix()
//                      matrix.postRotate(90F)
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
                scope.launch(Dispatchers.IO) {
                    runModel(image)
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
        }

    }

    override fun onCreate() {
        super.onCreate()

        val optionsBuilder =
            ObjectDetector.ObjectDetectorOptions.builder()
                .setBaseOptions(BaseOptions.builder().useGpu().build())
                .setScoreThreshold(0.6F)
                .setMaxResults(6)
        objectDetector = ObjectDetector.createFromFileAndOptions(this, EXPLORATORY_MODEL,optionsBuilder.build())
        imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(320, 320, ResizeOp.ResizeMethod.BILINEAR))
            .build()
        tensorImage = TensorImage(DataType.UINT8)
        tts = TextToSpeech(this, this)
        
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        isExploratoryRunning = true
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
        isExploratoryRunning = false
        mWebSocketClient.close()
        objectDetector.close()

        // Shutdown TTS
        tts.stop()
        tts.shutdown()
        startedTtsEngine = true

        super.onDestroy()
//        TODO("Stop everything")
    }
    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onInit(p0: Int) {
        if (p0 == TextToSpeech.SUCCESS) {
            // set US English as language for tts
            val result = tts.setLanguage(Locale.US)

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS","The Language specified is not supported!")
            } else {
                startedTtsEngine = true
            }

        } else {
            Log.e("TTS", "Initilization Failed!")
        }
    }

}