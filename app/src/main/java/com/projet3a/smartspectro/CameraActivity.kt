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
import android.view.TextureView.*
import android.view.View
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.Toast
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import kotlinx.android.synthetic.main.camera_layout.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.floor

/**
 * Created by Rémy Cordeau-Mirani on 20/09/2019.
 */
open class CameraActivity : Activity() {
    private var isReferenceSaved = false
    private var isSampleSaved = false
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
    private var x: DoubleArray? = null
    private var captureZoneIsAdjusted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.camera_layout)
        contextWrapper = ContextWrapper(applicationContext)
        cameraHandler = CameraHandler()
        enableListeners()
    }

    /**
     * Adds listeners on various UI components, the listener to
     * the texture is added in the onResume method
     */
    private fun enableListeners() {

        /* Adding listeners to the buttons */
        btn_takepicture!!.setOnClickListener {
            displayCreationMessage()
            setImagesCapture()
        }
        save_reference_button!!.setOnClickListener {
            try {
                savePicture("Reference")
                allowSample()
            } catch (e: IOException) {
                Log.e(TAG, e.toString())
            }
        }
        save_picture_button!!.setOnClickListener {
            try {
                savePicture("Sample")
                if (graphData != null && isReferenceSaved && isSampleSaved) {
                    val intent = Intent(
                        this@CameraActivity,
                        AnalysisActivity::class.java
                    )
                    if (definitiveMeasures.containsKey("Reference") &&
                        definitiveMeasures.containsKey(
                            "Sample"
                        )
                    ) {
                        AppParameters.getInstance().reference = definitiveMeasures["Reference"]
                            ?.toList() as ArrayList<Double>?
                        AppParameters.getInstance().sample = definitiveMeasures["Sample"]
                            ?.toList() as ArrayList<Double>?
                        startActivity(intent)
                    } else {
                        Toast.makeText(
                            applicationContext,
                            "Missing measurement",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, e.toString())
            }
        }
        clearButton!!.setOnClickListener {
            clearGraph()
            clearSample()
        }
    }

    private fun clearSample() {
        save_reference_button.visibility = VISIBLE
        save_picture_button.visibility = INVISIBLE
    }

    private fun allowSample() {
        save_reference_button.visibility = INVISIBLE
        save_picture_button.visibility = VISIBLE
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
            cameraDevice?.close()
        }

        override fun onError(camera: CameraDevice, i: Int) {
            cameraDevice!!.close()
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
     * Creates a camera preview, a CaptureSession and sets various parameters for
     * this CaptureSession (calls disableAutmatics method)
     */
    private fun createCameraPreview() {
        try {
            val texture = textureView!!.surfaceTexture!!
            texture.setDefaultBufferSize(imageDimension!!.width, imageDimension!!.height)
            val surface = Surface(texture)
            captureRequestBuilder =
                cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            captureRequestBuilder!!.addTarget(surface)
            val captureListener: CaptureCallback = object : CaptureCallback() {}
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
            bitmap!!,
            captureZone[0], captureZone[1], captureZone[2], captureZone[3]
        ) // getting raw data inside capture zone only
        val rgb = RGBDecoder.getRGBCode(captureZoneBitmap, captureZone[2], captureZone[3])
        val intensity = RGBDecoder.getImageIntensity(rgb)
        graphData = RGBDecoder.computeIntensityMean(intensity, captureZone[2], captureZone[3])
        //graphData = RGBDecoder.getMaxIntensity(intensity, captureZone[2])
        saveCurrentMeasure()
        updateUIGraph()
    }

    /**
     * Updates the UI graph when a new picture is taken
     */
    private fun updateUIGraph() {
        val updateGraphThread = Thread { // checking if the graphic contains series
            if (intensityGraph.series.isNotEmpty() && !isReferenceSaved) {
                intensityGraph.removeAllSeries()
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
                AppParameters.getInstance().captureZone[0]
            // xBegin for the capture zone (pixel 0 by default)
            val xMin = begin
            if (slope != 0.0 && intercept != 0.0) {
                // if both are not equal to zero, it means that wavelength calibration has been done
                xAxisTitle = "Wavelength (nm)"
                x = DoubleArray(graphData!!.size)
                for (i in graphData!!.indices) { //getting wavelength from position
                    x!![i] = (begin * slope + intercept).toInt().toDouble()
                    values[i] = DataPoint(x!![i], graphData!![i])
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
                intensityGraph.viewport.isXAxisBoundsManual = true
                intensityGraph.viewport.setMaxX(700.0)
                intensityGraph.viewport.setMinX(400.0)
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
                intensityGraph.viewport.isXAxisBoundsManual = true
                intensityGraph.viewport.setMaxX(graphData!!.size.toDouble() - 1)
                intensityGraph.viewport.setMinX(xMin.toDouble())
            }

            //if the reference is saved and not the data, we remove previous data
            if (isReferenceSaved) {
                if (intensityGraph.series.size > 1) {
                    intensityGraph.series.removeAt(1)
                }
            }

            //setting up X and Y axis title
            val gridLabelRenderer = intensityGraph.gridLabelRenderer
            gridLabelRenderer.horizontalAxisTitle = xAxisTitle
            gridLabelRenderer.verticalAxisTitle = "Intensity"

            //adding points to graph
            val series = LineGraphSeries(values)
            if (isReferenceSaved) {
                series.color = Color.RED
            }
            intensityGraph.addSeries(series)
            if (maxInGraph != null && maxValue.y != 0.0) {
                maxInGraph.text = maxText
                maxInGraph.visibility = View.VISIBLE
            }
            runOnUiThread {
                if (intensityGraph.visibility != View.VISIBLE) {
                    intensityGraph.visibility = View.VISIBLE
                }
            }
        }
        updateGraphThread.start()
    }

    /**
     * Clears graph and updates UI accordingly
     */
    private fun clearGraph() {
        intensityGraph.removeAllSeries()
        isReferenceSaved = false
        isSampleSaved = false
        graphData = null
    }

    /**
     * saves the frame's data in a map
     */
    private fun saveCurrentMeasure() {
        if (!isReferenceSaved) { // we are trying to capture the reference
            definitiveMeasures["Reference"] = graphData
        } else {
            definitiveMeasures["Sample"] = graphData
        }
    }

    /**
     * disables some camera automatics (such as auto focus, lens stabilization) for
     * the specified CameraCaptureSession
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
            Toast.makeText(
                this@CameraActivity, "Creating graph...",
                Toast.LENGTH_SHORT
            ).show()
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
                    Toast.makeText(
                        this, "Unable to create directory",
                        Toast.LENGTH_SHORT
                    ).show()
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
                if (slope != 0.0 && intercept != 0.0) { //if wavelength calibration has been
                    for (i in graphData!!.indices) {
                        outputStream.write("${x!![i]},".toByteArray())
                        outputStream.write(
                            """${graphData!![i]}
                                |
                            """.trimMargin().toByteArray()
                        )
                        begin++
                    }
                } else {
                    for (i in graphData!!.indices) {
                        outputStream.write(("$begin,").toByteArray())
                        outputStream.write(
                            ("""${graphData!![i]}
                                |
                            """.trimMargin()).toByteArray()
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
        if (cameraDevice != null) {
            cameraDevice!!.close()
        }
    }

    public override fun onResume() {
        super.onResume()
        Log.e(TAG, "On Resume")
        if (textureView == null) { // prevents camera preview from freezing when app is resuming
            textureView = texture
            textureView!!.surfaceTextureListener = textureListener
            if (textureView!!.isAvailable) {
                textureListener.onSurfaceTextureAvailable(
                    textureView!!.surfaceTexture!!,
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