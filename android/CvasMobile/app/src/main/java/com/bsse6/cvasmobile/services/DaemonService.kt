package com.bsse6.cvasmobile.services

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.RectF
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
import kotlinx.coroutines.*
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.task.vision.detector.ObjectDetector
import java.net.URI
import java.nio.ByteBuffer
import java.util.*
import java.util.Collections.min

class DaemonService : Service(), TextToSpeech.OnInitListener {
    companion object {
        private const val EXPLORATORY_MODEL = "efficientdet_lite0.tflite"
        var isDaemonRunning = false
        private val uri = URI("ws://192.168.4.1:86/")
        @SuppressLint("StaticFieldLeak")
        private lateinit var mContext : Context

        //Agent MODES
        const val EXPLORE = 1
        const val TRACKING = 2
        const val NAVIGATION = 3

        private var runningMode = 0

        fun startService( context: Context,daemonMode: Int){
            mContext = context
            if(!isDaemonRunning){
                isDaemonRunning = true
                val startIntent = Intent(context, DaemonService::class.java).apply{
                    putExtra("mode", daemonMode)
                }
                ContextCompat.startForegroundService(context,startIntent)
            }
        }

        fun stopService(context: Context){
            mContext = context
            isDaemonRunning = false
            val stopIntent = Intent(context, DaemonService::class.java)
            context.stopService(stopIntent)
        }
        fun setDaemonMode(daemonMode: Int) {
            runningMode = daemonMode
        }
        fun getDaemon() = runningMode
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
     * Speak the input string
     * @param "text"
     */
    private fun speakOut(text : String) {
        if(startedTtsEngine) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null,"")
        }
    }
    /**
     * Run inference using the ML model
     */
    private suspend fun runModel(image : Bitmap) {
        if(!isModelRunning) {
            isModelRunning = true //capture the flag
            tensorImage.load(image)
            tensorImage = imageProcessor.process(tensorImage)
            val results = objectDetector.detect(tensorImage)

//            Choose a particular daemon mode to run
            when(runningMode) {
                TRACKING -> {
                    val partitionFrameSize = (320 / 3).toFloat()

                    results.forEach {   detected ->
                        val detectedFrame = detected.boundingBox
                        val detectedName = detected.categories[0].label
                        val rightCoordinate = 320 - detectedFrame.right // relative to the right of the frame
                        val leftCoordinate = detectedFrame.left
                        val bottomCoordinate = 320 - detectedFrame.bottom // relative to the bottom of the frame
                        val topCoordinate  = detectedFrame.top

//                        handle the rows first i.e. top, middle, bottom
                        when {
                            topCoordinate  <= partitionFrameSize -> {
//                                objects at top of the screen
                                when{
                                    leftCoordinate <= partitionFrameSize -> {
//                                        objects on the left
                                        speakOut("$detectedName in the top left")
                                    }
                                    rightCoordinate <= partitionFrameSize -> {
//                                        objects on the right
                                        speakOut("$detectedName in the top right")
                                    }
                                    else -> {
//                                        objects in the centre
                                        speakOut("$detectedName at the top")

                                    }
                                }
                            }
                            bottomCoordinate <= partitionFrameSize -> {
//                                objects at bottom of the screen
                                when{
                                    leftCoordinate <= partitionFrameSize -> {
//                                        objects on the left
                                        speakOut("$detectedName in the bottom left")

                                    }
                                    rightCoordinate <= partitionFrameSize -> {
//                                        objects on the right
                                        speakOut("$detectedName in the bottom right")

                                    }
                                    else -> {
//                                        objects in the centre
                                        speakOut("$detectedName at the bottom")

                                    }
                                }
                            }
                            else ->{
//                                objects in the middle
                                when{
                                    leftCoordinate <= partitionFrameSize -> {
//                                        objects on the left
                                        speakOut("$detectedName on the left")
                                    }
                                    rightCoordinate <= partitionFrameSize -> {
//                                        objects on the right
                                        speakOut("$detectedName on the right")

                                    }
                                    else -> {
//                                        objects in the centre
                                        speakOut("$detectedName in front")
                                    }
                                }
                            }
                        }
                    }
                }
                EXPLORE -> {
                    results.forEach {
                        if(startedTtsEngine) {
                            tts.speak(it.categories[0].label, TextToSpeech.QUEUE_FLUSH, null,"")
                        }
                        Log.e("prediction", it.toString())
                    }
                }
                NAVIGATION -> {
//                    TODO("Divide the object rectf in 3 section frames check frames")
//                    get area in each frame
//                    avoid all with object with biggest obstacle area
//                    consider some objects like doors stairs and any that you can walk through, tell the person about it and its location
//                    tell person about closest obstacle and location

                    val safeObjects = listOf("door","staircase","corridor","bottle")
                    val partitionFrameSize = (320 / 3).toFloat()

                    val firstPartition = RectF(0f, 0f, partitionFrameSize, 320f)
                    var firstOccupiedArea = 0f
                    val secondPartition = RectF(partitionFrameSize + 1, 0f, partitionFrameSize * 2, 320f)
                    var secondOccupiedArea = 0f
                    val thirdPartition = RectF((partitionFrameSize * 2) + 1, 0f, partitionFrameSize * 3, 320f)
                    var thirdOccupiedArea = 0f

                    results.forEach { detected ->
                        scope.launch{
                            val detectedFrame = detected.boundingBox
                            val detectedName = detected.categories[0].label
                            val rightCoordinate = detectedFrame.right
                            val leftCoordinate = detectedFrame.left

//                            find the partition the detected item belongs and sum the areas

                            when {
                                leftCoordinate <= firstPartition.right && rightCoordinate <= firstPartition.right -> {

                                    if(detectedName in safeObjects) {
                                        //Tell the user the location
                                        speakOut("$detectedName on your far left")

                                    }else {
                                        // get area occupied in first partition
                                        firstOccupiedArea += detectedFrame.height() * detectedFrame.width()
                                    }

                                }
                                leftCoordinate <= firstPartition.right && rightCoordinate  >= secondPartition.left -> {
                                    if(detectedName in safeObjects) {
                                        //Tell the user the location
                                        speakOut("$detectedName on your left")

                                    }else {
//                                    get area occupied in 1st and 2nd partitions
                                        firstOccupiedArea += detectedFrame.height() * (firstPartition.right - leftCoordinate)
                                        secondOccupiedArea += detectedFrame.height() * (rightCoordinate - secondPartition.left)
                                    }
                                }
                                leftCoordinate <= firstPartition.right && rightCoordinate >= thirdPartition.left -> {
                                    if(detectedName in safeObjects) {
                                        //Tell the user the location
                                        speakOut("$detectedName closely upfront")

                                    }else {

//                                    get areas occupied in each partition
                                        firstOccupiedArea += detectedFrame.height() * (firstPartition.right - leftCoordinate)
                                        secondOccupiedArea += detectedFrame.height() * partitionFrameSize
                                        thirdOccupiedArea += detectedFrame.height() * (rightCoordinate - thirdPartition.left)
                                    }

                                }
                                leftCoordinate >= secondPartition.left && rightCoordinate < thirdPartition.left -> {
                                    if(detectedName in safeObjects) {
                                        //Tell the user the location
                                        speakOut("$detectedName in front")

                                    }else {
//                                    get area occupied in second partition
                                        secondOccupiedArea += detectedFrame.height() * detectedFrame.width()
                                    }
                                }
                                leftCoordinate >= secondPartition.left && leftCoordinate <= secondPartition.right && rightCoordinate >= thirdPartition.left -> {
                                    if(detectedName in safeObjects) {
                                        //Tell the user the location
                                        speakOut("$detectedName on your right")

                                    }else {
//                                    get area occupied in 2nd and 3rd
                                        secondOccupiedArea += detectedFrame.height() * (secondPartition.right - leftCoordinate)
                                        thirdOccupiedArea += detectedFrame.height() * (rightCoordinate - thirdPartition.left)
                                    }
                                }
                                else -> {
                                    if(detectedName in safeObjects) {
                                        //Tell the user the location
                                        speakOut("$detectedName on your far right")

                                    }else {
//                                    get area occupied in the third partition
                                        thirdOccupiedArea += detectedFrame.height() * detectedFrame.width()
                                    }
                                }
                            }
                        }
                    }
                    val minOccupiedPartition = min(listOf(firstOccupiedArea, secondOccupiedArea, thirdOccupiedArea))
                    Log.e(TAG,"$firstOccupiedArea  $secondOccupiedArea  $thirdOccupiedArea")
                    if (minOccupiedPartition < (0.5 * 320 * 320)) {
                        //get least occupied partition move forward if nothing in all partitions
                        when(minOccupiedPartition) {
                            secondOccupiedArea -> {
                                speakOut("Move straight")
                            }
                            firstOccupiedArea -> {
                                speakOut("Move left")
                            }
                            thirdOccupiedArea -> {
                                speakOut("Move right")
                            }
                        }
                    }else{
                        speakOut("Pathway blocked")
                    }

                }
                else -> {
                    Log.e(TAG, "UNKNOWN DAEMON MODE")
                }
            }
            if(isDaemonRunning) {
                isModelRunning = false //release the model flag

            }else {
                objectDetector.close()
            }
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
                    if(isActive) {
                        runModel(image)
                    }
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
        intent?.let {
            runningMode = it.getIntExtra("mode", 0)
        }
        isDaemonRunning = true
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotifyChannel.createNotificationChannel(this@DaemonService)
        }
        val notificationIntent = Intent(this@DaemonService, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this@DaemonService,0,notificationIntent,0)
        val notification = NotificationCompat.Builder(this@DaemonService,NotifyChannel.CHANNEL_ID)
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
        isDaemonRunning = false
        mWebSocketClient.close()

        // Shutdown TTS
        tts.stop()
        tts.shutdown()
        startedTtsEngine = true

        super.onDestroy()
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