package com.projet3a.smartspectro

import android.app.Activity
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.camera2.CameraCaptureSession.CaptureCallback
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.view.TextureView.SurfaceTextureListener
import android.view.View
import android.widget.Button
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.floor

/**
 * Created by Rémy Cordeau-Mirani on 20/09/2019.
 */
open class CameraActivity : Activity() {
    private var takePictureButton: Button? = null
    private var saveReferenceButton: Button? = null
    private var saveDataButton: Button? = null
    private var clearGraphButton: Button? = null
    private var calibrateButton: Button? = null
    private var calibrationLeft: SeekBar? = null
    private var calibrationRight: SeekBar? = null
    private var calibrationTop: SeekBar? = null
    private var calibrationBottom: SeekBar? = null
    private var lastSeekBarValLeft = 0
    private var lastSeekBarValRight = 0
    private var lastSeekBarValBottom = 0
    private var lastSeekBarValTop = 0
    private var isDefaultCalibrationDone = false
    private var isReferenceSaved = false
    private var isSampleSaved = false
    private var isCalibrating = false
    private var cameraCalibrationView: CameraCalibrationView? = null
    private var textureView: TextureView? = null
    private var cameraId: String? = null
    private var cameraDevice: CameraDevice? = null
    private var cameraCaptureSession: CameraCaptureSession? = null
    private var captureRequestBuilder: CaptureRequest.Builder? = null
    private var imageDimension: Size? = null
    private val backgroundHandler: Handler? = null
    private var contextWrapper: ContextWrapper? = null
    private var cameraHandler: CameraHandler? = null
    private var graphData: DoubleArray? = null
    private val definitiveMeasures = HashMap<String, DoubleArray?>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.camera_layout)
        contextWrapper = ContextWrapper(applicationContext)
        cameraHandler = CameraHandler()

        //adding custom surface view above the texture view
        val calibrationViewLayout: ConstraintLayout = findViewById(R.id.calibrationViewLayout)
        cameraCalibrationView = CameraCalibrationView(this)
        calibrationViewLayout.addView(cameraCalibrationView)
        enableListeners()
    }

    /**
     * Adds listeners on various UI components, the listener to the texture is added in the onResume method
     */
    private fun enableListeners() {

        /* Adding listeners to the buttons */
        takePictureButton = findViewById(R.id.btn_takepicture)
        takePictureButton!!.setOnClickListener(View.OnClickListener {
            if (isCalibrating) return@OnClickListener
            if (isDefaultCalibrationDone) {
                displayCreationMessage()
                setImagesCapture()
            } else {
                Toast.makeText(
                    this@CameraActivity,
                    "Please calibrate before taking picture",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
        saveReferenceButton = findViewById(R.id.save_reference_button)
        saveReferenceButton!!.setOnClickListener(View.OnClickListener {
            try {
                if (isCalibrating || isReferenceSaved) return@OnClickListener
                savePicture("Reference")
            } catch (e: IOException) {
                Log.e(TAG, e.toString())
            }
        })
        saveDataButton = findViewById(R.id.save_picture_button)
        saveDataButton!!.setOnClickListener(View.OnClickListener {
            if (isCalibrating) return@OnClickListener
            try {
                val graphView: GraphView = findViewById(R.id.intensityGraph)
                if (isReferenceSaved && graphView.series.size >= 2) {
                    savePicture("Sample")
                    if (graphData != null && isReferenceSaved && isSampleSaved) {
                        val intent = Intent(this@CameraActivity, AnalysisActivity::class.java)
                        if (definitiveMeasures.containsKey("Reference") && definitiveMeasures.containsKey(
                                "Sample"
                            )
                        ) {
                            intent.putExtra(GRAPH_DATA_KEY, definitiveMeasures)
                            startActivity(intent)
                        } else {
                            Toast.makeText(
                                applicationContext,
                                "Missing measurement",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } else {
                    Toast.makeText(
                        this@CameraActivity,
                        "You must first save the reference",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: IOException) {
                Log.e(TAG, e.toString())
            }
        })
        clearGraphButton = findViewById(R.id.clearButton)
        clearGraphButton!!.setOnClickListener(View.OnClickListener {
            if (isCalibrating) return@OnClickListener
            clearGraph()
        })
        calibrateButton = findViewById(R.id.calibrationButton)
        calibrateButton!!.setOnClickListener(View.OnClickListener {
            if (!isDefaultCalibrationDone) {
                isDefaultCalibrationDone = true
            }
            if (!isCalibrating) {
                enableCalibration()
            } else {
                endCalibration()
            }
        })
    }

    /**
     * Adds listeners on seekbars when calibrationCameraView is fully initialized
     */
    fun enableSeekBarsListeners() {
        calibrationBottom = findViewById(R.id.calibrationBottom)
        calibrationBottom!!.max = cameraCalibrationView!!.height
        calibrationBottom!!.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, b: Boolean) {
                val shift = abs(progress - lastSeekBarValBottom)
                if (progress > lastSeekBarValBottom) { /*Seekbar moved to the right*/
                    cameraCalibrationView!!.moveLine("Bottom line", shift)
                } else if (progress < lastSeekBarValBottom) { /*Seekbar moved to the left*/
                    cameraCalibrationView!!.moveLine("Bottom line", -shift)
                }
                lastSeekBarValBottom = progress
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        calibrationTop = findViewById(R.id.calibrationTop)
        calibrationTop!!.max = cameraCalibrationView!!.height
        calibrationTop!!.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, b: Boolean) {
                val shift = abs(progress - lastSeekBarValTop)
                if (progress > lastSeekBarValTop) { /*Seekbar moved to the right*/
                    cameraCalibrationView!!.moveLine("Top line", shift)
                } else if (progress < lastSeekBarValTop) { /*Seekbar moved to the left*/
                    cameraCalibrationView!!.moveLine("Top line", -shift)
                }
                lastSeekBarValTop = progress
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        calibrationRight = findViewById(R.id.calibrationRight)
        calibrationRight!!.max = cameraCalibrationView!!.width
        calibrationRight!!.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, b: Boolean) {
                val shift = abs(progress - lastSeekBarValRight)
                if (progress > lastSeekBarValRight) { /*Seekbar moved to the right*/
                    cameraCalibrationView!!.moveLine("Right line", shift)
                } else if (progress < lastSeekBarValRight) { /*Seekbar moved to the left*/
                    cameraCalibrationView!!.moveLine("Right line", -shift)
                }
                lastSeekBarValRight = progress
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        calibrationLeft = findViewById(R.id.calibrationLeft)
        calibrationLeft!!.max = cameraCalibrationView!!.width
        calibrationLeft!!.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, b: Boolean) {
                val shift = abs(progress - lastSeekBarValLeft)
                if (progress > lastSeekBarValLeft) { /*Seekbar moved to the right*/
                    cameraCalibrationView!!.moveLine("Left line", shift)
                } else if (progress < lastSeekBarValLeft) { /*Seekbar moved to the left*/
                    cameraCalibrationView!!.moveLine("Left line", -shift)
                }
                lastSeekBarValLeft = progress
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
    }

    /**
     * Called when capture zone button is pressed. Modifies the UI accordingly and saves current capture zone.
     */
    private fun endCalibration() {
        isCalibrating = false
        cameraCalibrationView!!.eraseLines()
        findViewById<View>(R.id.intensityGraph).visibility = View.VISIBLE
        findViewById<View>(R.id.maxInGraph).visibility = View.VISIBLE
        findViewById<View>(R.id.calibrationBottom).visibility = View.INVISIBLE
        findViewById<View>(R.id.calibrationTop).visibility = View.INVISIBLE
        findViewById<View>(R.id.calibrationLeft).visibility = View.INVISIBLE
        findViewById<View>(R.id.calibrationRight).visibility = View.INVISIBLE
        findViewById<View>(R.id.TextRight).visibility = View.INVISIBLE
        findViewById<View>(R.id.TextTop).visibility = View.INVISIBLE
        findViewById<View>(R.id.TextLeft).visibility = View.INVISIBLE
        findViewById<View>(R.id.TextBottom).visibility = View.INVISIBLE
        cameraCalibrationView!!.setCaptureZone()
    }

    /**
     * Called when capture zone button is pressed. Modifies the UI accordingly.
     */
    private fun enableCalibration() {
        isCalibrating = true
        val graphView: GraphView = findViewById(R.id.intensityGraph)
        if (graphView.visibility == View.VISIBLE) {
            graphView.visibility = View.INVISIBLE
        }
        findViewById<View>(R.id.maxInGraph).visibility = View.INVISIBLE
        findViewById<View>(R.id.calibrationBottom).visibility = View.VISIBLE
        findViewById<View>(R.id.calibrationTop).visibility = View.VISIBLE
        findViewById<View>(R.id.calibrationLeft).visibility = View.VISIBLE
        findViewById<View>(R.id.calibrationRight).visibility = View.VISIBLE
        findViewById<View>(R.id.TextRight).visibility = View.VISIBLE
        findViewById<View>(R.id.TextTop).visibility = View.VISIBLE
        findViewById<View>(R.id.TextLeft).visibility = View.VISIBLE
        findViewById<View>(R.id.TextBottom).visibility = View.VISIBLE
        cameraCalibrationView!!.drawLines()
    }

    private var textureListener: SurfaceTextureListener = object : SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, i: Int, i1: Int) {
            openCamera()
        }

        override fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture, i: Int, i1: Int) {}
        override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
            return false
        }

        override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {}
    }

    // "listener" of the camera device, calls various method depending on the CameraDevice state
    private val stateCallback: CameraDevice.StateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            createCameraPreview()
        }

        override fun onDisconnected(camera: CameraDevice) {
            //cameraDevice.close();
        }

        override fun onError(camera: CameraDevice, i: Int) {
            if (cameraDevice != null) {
                cameraDevice!!.close()
                cameraDevice = null
            }
        }
    }

    /**
     * opens the camera (if allowed), sets image dimension for capture
     */
    @Throws(SecurityException::class)
    private fun openCamera() {
        try {
            val cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
            cameraId = cameraManager.cameraIdList[0]
            val cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId!!)
            val map =
                cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
            imageDimension = map.getOutputSizes(SurfaceTexture::class.java)[0]
            cameraManager.openCamera(cameraId!!, stateCallback, null)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    /**
     * Creates a camera preview, a CaptureSession and sets various parameters for this CaptureSession (calls disableAutmatics method)
     */
    private fun createCameraPreview() {
        try {
            val texture = textureView!!.surfaceTexture!!
            texture.setDefaultBufferSize(imageDimension!!.width, imageDimension!!.height)
            val surface = Surface(texture)
            captureRequestBuilder =
                cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            captureRequestBuilder!!.addTarget(surface)
            val captureListener: CaptureCallback = object : CaptureCallback() {
            }
            cameraDevice!!.createCaptureSession(
                listOf(surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(captureSession: CameraCaptureSession) {
                        if (cameraDevice == null) return
                        cameraCaptureSession = captureSession
                        captureRequestBuilder!!.set(
                            CaptureRequest.CONTROL_MODE,
                            CameraMetadata.CONTROL_MODE_AUTO
                        )
                        disableAutomatics(
                            captureRequestBuilder!!,
                            cameraCaptureSession,
                            captureListener
                        )
                    }

                    override fun onConfigureFailed(captureSession: CameraCaptureSession) {
                        Toast.makeText(
                            this@CameraActivity,
                            "Configuration change",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                null
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    /**
     * Processes camera's raw data to get intensity for each pixel in the capture zone
     */
    private fun setImagesCapture() {
        if (null == cameraDevice) {
            Log.e(TAG, "cameraDevice is null")
            openCamera()
            return
        }
        val width = textureView!!.width
        val height = textureView!!.height
        val bitmap = textureView!!.getBitmap(width, height) // getting raw data
        val captureZone = AppParameters.getInstance().captureZone
        val captureZoneBitmap = Bitmap.createBitmap(
            bitmap,
            captureZone[0], captureZone[1], captureZone[2], captureZone[3]
        ) // getting raw data inside capture zone only
        val rgb = RGBDecoder.getRGBCode(captureZoneBitmap, captureZone[2], captureZone[3])
        val intensity = RGBDecoder.getImageIntensity(rgb)
        //this.graphData = RGBDecoder.computeIntensityMean(intensity,captureZone[2],captureZone[3]);
        graphData = RGBDecoder.getMaxIntensity(intensity, captureZone[2])
        saveCurrentMeasure()
        updateUIGraph()
    }

    /**
     * Updates the UI graph when a new picture is taken
     */
    private fun updateUIGraph() {
        val graphView: GraphView = findViewById(R.id.intensityGraph)
        val updateGraphThread = Thread { // checking if the graphic contains series
            if (graphView.series.isNotEmpty() && !isReferenceSaved) {
                graphView.removeAllSeries()
            }

            //adding series to graph
            val xAxisTitle: String
            val maxText: String
            var maxValue =
                DataPoint(0.0, 0.0)
            val values = arrayOfNulls<DataPoint>(
                graphData!!.size
            )
            val slope = AppParameters.getInstance().slope
            val intercept = AppParameters.getInstance().intercept
            var begin =
                AppParameters.getInstance().captureZone[0] // xBegin for the capture zone (pixel 0 by default)
            val xMin = begin
            if (slope != 0.0 && intercept != 0.0) { // if both are not equal to zero, it means that wavelength calibration has been done
                xAxisTitle = "Wavelength (nm)"
                for (i in graphData!!.indices) { //getting wavelength from position
                    val x = begin * slope + intercept
                    values[i] = DataPoint(x, graphData!![i])
                    if (graphData!![i] > maxValue.y) {
                        maxValue = values[i]!!
                    }
                    begin++
                }
                maxText =
                    "Peak found at " + floor(maxValue.x) + " nm and is " + floor(
                        maxValue.y
                    )

                //setting manually X axis max and min bounds to see all points on graph
                graphView.viewport.isXAxisBoundsManual = true
                graphView.viewport.setMaxX((graphData!!.size - 1) * slope + intercept)
                graphView.viewport.setMinX(xMin * slope + intercept)
            } else {
                xAxisTitle = "Pixel position"
                for (i in graphData!!.indices) {
                    values[i] = DataPoint(begin.toDouble(), graphData!![i])
                    if (graphData!![i] > maxValue.y) {
                        maxValue = values[i]!!
                    }
                    begin++
                }
                maxText = "Peak found at " + maxValue.x + " px and is " + floor(
                    maxValue.y
                )

                //setting manually X axis bound to see all points on graph
                graphView.viewport.isXAxisBoundsManual = true
                graphView.viewport.setMaxX(graphData!!.size.toDouble() - 1)
                graphView.viewport.setMinX(xMin.toDouble())
            }

            //if the reference is saved and not the data, we remove previous data
            if (isReferenceSaved) {
                if (graphView.series.size > 1) {
                    graphView.series.removeAt(1)
                }
            }

            //setting up X and Y axis title
            val gridLabelRenderer = graphView.gridLabelRenderer
            gridLabelRenderer.horizontalAxisTitle = xAxisTitle
            gridLabelRenderer.verticalAxisTitle = "Intensity"

            //adding points to graph
            val series = LineGraphSeries(values)
            if (isReferenceSaved) {
                series.color = Color.RED
            }
            graphView.addSeries(series)
            val maxInGraph = findViewById<TextView>(R.id.maxInGraph)
            if (maxInGraph != null && maxValue.y != 0.0) {
                maxInGraph.text = maxText
                maxInGraph.visibility = View.VISIBLE
            }
            runOnUiThread {
                if (graphView.visibility != View.VISIBLE) {
                    graphView.visibility = View.VISIBLE
                }
            }
        }
        updateGraphThread.start()
    }

    /**
     * Clears graph and updates UI accordingly
     */
    private fun clearGraph() {
        if (graphData == null) {
            return
        } else {
            val graphView: GraphView = findViewById(R.id.intensityGraph)
            if (graphView.series.size > 0) graphView.removeAllSeries()
            isReferenceSaved = false
            isSampleSaved = false
        }
    }

    /**
     * saves the frame's data in a map
     */
    private fun saveCurrentMeasure() {
        if (!isReferenceSaved && !isSampleSaved) { // we are trying to capture the reference
            definitiveMeasures["Reference"] = graphData
        } else if (isReferenceSaved && !isSampleSaved) {
            definitiveMeasures["Sample"] = graphData
        }
    }

    /**
     * disables some camera automatics (such as auto focus, lens stabilization) for the specified CameraCaptureSession
     */
    private fun disableAutomatics(
        captureBuilder: CaptureRequest.Builder,
        session: CameraCaptureSession?,
        callback: CaptureCallback
    ) {
        try {
            captureBuilder.set(
                CaptureRequest.CONTROL_MODE,
                CaptureRequest.CONTROL_MODE_OFF_KEEP_STATE
            )
            captureBuilder.set(
                CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
                CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_OFF
            )
            captureBuilder.set(
                CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,
                CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_OFF
            )
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
            val captureRequest = captureBuilder.build()
            session!!.setRepeatingRequest(captureRequest, callback, backgroundHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    /**
     * Displays a message in the UI
     */
    private fun displayCreationMessage() {
        runOnUiThread {
            Toast.makeText(this@CameraActivity, "Creating graph...", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Saves picture data in a file (stored in Documents folder)
     */
    @Throws(IOException::class)
    private fun savePicture(text: String) {
        if (graphData == null) {
            Toast.makeText(
                this@CameraActivity,
                "Please press Take Picture button before saving",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val currentDateAndTime = sdf.format(Date())
            var outputStream: OutputStream? = null
            val directory =
                File(Environment.getExternalStorageDirectory().toString() + "/Documents")
            //check whether Documents directory exists, if not, we create it
            if (!directory.exists()) {
                val result = directory.mkdirs()
                if (!result) {
                    Toast.makeText(this, "Unable to create directory", Toast.LENGTH_SHORT).show()
                    return
                }
            }
            val file = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                text + "_" + currentDateAndTime + ".txt"
            )
            try {
                outputStream = FileOutputStream(file, false)
                val slope = AppParameters.getInstance().slope
                val intercept = AppParameters.getInstance().intercept
                var begin = AppParameters.getInstance().captureZone[0]
                if (slope != 0.0 && intercept != 0.0) { //if wavelength calibration has been done
                    for (i in graphData!!.indices) { //getting wavelength from position
                        val x = begin * slope + intercept
                        outputStream.write("$x,".toByteArray())
                        outputStream.write(
                            """${graphData!![i]}
    """.toByteArray()
                        )
                        begin++
                    }
                } else {
                    for (i in graphData!!.indices) {
                        outputStream.write(("$begin,").toByteArray())
                        outputStream.write(
                            ("""${graphData!![i]}
""").toByteArray()
                        )
                        begin++
                    }
                }
            } finally {
                if (outputStream != null) {
                    outputStream.close()
                    if (text == "Reference") {
                        isReferenceSaved = true
                    } else if (text == "Sample") {
                        isSampleSaved = true
                    }
                    Toast.makeText(
                        this@CameraActivity,
                        "File successfully saved",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    public override fun onPause() {
        super.onPause()
        Log.e(TAG, "On Pause")
        textureView = null
        cameraDevice!!.close()
    }

    public override fun onResume() {
        super.onResume()
        Log.e(TAG, "On Resume")
        if (textureView == null) { // prevents camera preview from freezing when app is resuming
            textureView = findViewById(R.id.texture)
            textureView!!.surfaceTextureListener = textureListener
            if (textureView!!.isAvailable) {
                textureListener.onSurfaceTextureAvailable(
                    textureView!!.surfaceTexture,
                    textureView!!.width,
                    textureView!!.height
                )
            }
        }
    }

    companion object {
        private const val TAG = "Camera Activity"
        const val GRAPH_DATA_KEY = "Graph Data"
    }
}