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
import android.os.Handler
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.view.TextureView.SurfaceTextureListener
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import kotlinx.android.synthetic.main.choices.*
import kotlinx.android.synthetic.main.wavelength_cal_layout.*
import org.opencv.android.Utils
import org.opencv.core.Mat
import java.util.*

class WavelengthCalibrationActivity : Activity() {

    private var cameraId: String? = null
    private var intensity: DoubleArray? = null
    private var contextWrapper: ContextWrapper? = null
    private var cameraHandler: CameraHandler? = null
    private var graphData: DoubleArray? = null
    private var currentButton: Button? = null
    private var currentIndex = 0
    private var wavelengthCalibrationView: WavelengthCalibrationView? = null
    private var textureView: TextureView? = null
    private var cameraDevice: CameraDevice? = null
    private var cameraCaptureSession: CameraCaptureSession? = null
    private var captureRequestBuilder: CaptureRequest.Builder? = null
    private var imageDimension: Size? = null
    private val wavelengthRaysPositions = IntArray(4)
    private val backgroundHandler: Handler? = null
    private var originShift: Int? = null
    private var captureZone: IntArray = IntArray(5)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.wavelength_cal_layout)
        contextWrapper = ContextWrapper(applicationContext)
        cameraHandler = CameraHandler()
        //adding custom surface view above the texture view
        val calibrationViewLayout: ConstraintLayout = findViewById(R.id.calibrationViewLayout)
        wavelengthCalibrationView = WavelengthCalibrationView(this)
        calibrationViewLayout.addView(wavelengthCalibrationView)
        enableListeners()
        when (AppParameters.getInstance().button) {
            "Manuel" -> enableManuelCalibration()
            "Automatique" -> enableAutoCalibration()
            "Semi-automatique" -> enableSemiAutoCalibration()
            else  -> Toast.makeText(
                applicationContext,
                "problem of button clicked",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun enableSemiAutoCalibration() {
        /* Adding listeners to the buttons */
        Button436.setTextColor(Color.BLUE)
        Button436.setOnClickListener {
            if (currentButton != null) {
                currentButton!!.setBackgroundColor(Color.argb(100, 187, 222, 251))
            }
            currentButton = Button436
            currentButton!!.setBackgroundColor(Color.CYAN)
            currentIndex = 0
            updateWaveLengthPositions()
        }
        Button488.setTextColor(Color.argb(100, 30, 144, 255))
        Button488.setOnClickListener {
            if (currentButton != null) {
                currentButton!!.setBackgroundColor(Color.argb(100, 187, 222, 251))
            }
            currentButton = Button488
            currentButton!!.setBackgroundColor(Color.CYAN)
            currentIndex = 1
            updateWaveLengthPositions()
        }
        Button546.setTextColor(Color.GREEN)
        Button546.setOnClickListener {
            if (currentButton != null) {
                currentButton!!.setBackgroundColor(Color.argb(100, 187, 222, 251))
            }
            currentButton = Button546
            currentButton!!.setBackgroundColor(Color.CYAN)
            currentIndex = 2
            updateWaveLengthPositions()
        }
        Button612.setTextColor(Color.RED)
        Button612.setOnClickListener {
            if (currentButton != null) {
                currentButton!!.setBackgroundColor(Color.argb(100, 187, 222, 251))
            }
            currentButton = Button612
            currentButton!!.setBackgroundColor(Color.CYAN)
            currentIndex = 3
            updateWaveLengthPositions()
        }
    }

    private fun enableAutoCalibration() {
        /* Adding listeners to the buttons */
        Button436.setTextColor(Color.BLUE)
        Button436.setOnClickListener {
            if (currentButton != null) {
                currentButton!!.setBackgroundColor(Color.argb(100, 187, 222, 251))
            }
            currentButton = Button436
            currentButton!!.setBackgroundColor(Color.CYAN)
            currentIndex = 0
            updateWaveLengthPositions()
        }
        Button488.setTextColor(Color.argb(100, 30, 144, 255))
        Button488.setOnClickListener {
            if (currentButton != null) {
                currentButton!!.setBackgroundColor(Color.argb(100, 187, 222, 251))
            }
            currentButton = Button488
            currentButton!!.setBackgroundColor(Color.CYAN)
            currentIndex = 1
            updateWaveLengthPositions()
        }
        Button546.setTextColor(Color.GREEN)
        Button546.setOnClickListener {
            if (currentButton != null) {
                currentButton!!.setBackgroundColor(Color.argb(100, 187, 222, 251))
            }
            currentButton = Button546
            currentButton!!.setBackgroundColor(Color.CYAN)
            currentIndex = 2
            updateWaveLengthPositions()
        }
        Button612.setTextColor(Color.RED)
        Button612.setOnClickListener {
            if (currentButton != null) {
                currentButton!!.setBackgroundColor(Color.argb(100, 187, 222, 251))
            }
            currentButton = Button612
            currentButton!!.setBackgroundColor(Color.CYAN)
            currentIndex = 3
            updateWaveLengthPositions()
        }
    }

    private fun enableManuelCalibration() {
        /* Adding listeners to the buttons */
        Button436.setTextColor(Color.BLUE)
        Button436.setOnClickListener {
            if (currentButton != null) {
                currentButton!!.setBackgroundColor(Color.argb(100, 187, 222, 251))
            }
            currentButton = Button436
            currentButton!!.setBackgroundColor(Color.CYAN)
            currentIndex = 0
            updateWaveLengthPositions()
        }
        Button488.setTextColor(Color.argb(100, 30, 144, 255))
        Button488.setOnClickListener {
            if (currentButton != null) {
                currentButton!!.setBackgroundColor(Color.argb(100, 187, 222, 251))
            }
            currentButton = Button488
            currentButton!!.setBackgroundColor(Color.CYAN)
            currentIndex = 1
            updateWaveLengthPositions()
        }
        Button546.setTextColor(Color.GREEN)
        Button546.setOnClickListener {
            if (currentButton != null) {
                currentButton!!.setBackgroundColor(Color.argb(100, 187, 222, 251))
            }
            currentButton = Button546
            currentButton!!.setBackgroundColor(Color.CYAN)
            currentIndex = 2
            updateWaveLengthPositions()
        }
        Button612.setTextColor(Color.RED)
        Button612.setOnClickListener {
            if (currentButton != null) {
                currentButton!!.setBackgroundColor(Color.argb(100, 187, 222, 251))
            }
            currentButton = Button612
            currentButton!!.setBackgroundColor(Color.CYAN)
            currentIndex = 3
            updateWaveLengthPositions()
        }
    }

    /**
     * Adds listeners on various UI components
     */
    private fun enableListeners() {
        /* Adding listeners to the buttons */
        buttonPicture!!.setOnClickListener {
            setImagesCapture()
            allowWavelengthCalibration()
        }
        clearPicture!!.setOnClickListener {
            clear()
        }
        validateCalButton.setOnClickListener {
            for (ent in wavelengthRaysPositions) {
                if (ent == 0) {
                    Toast.makeText(
                        applicationContext,
                        "Missing calibration for one wavelength value",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }
            }
            val intent =
                Intent(this@WavelengthCalibrationActivity, CameraActivity::class.java)
            val lineData = findSlopeAndIntercept()
            AppParameters.getInstance().slope = lineData[0]
            AppParameters.getInstance().intercept = lineData[1]
            captureZone[0] = ((400 - lineData[1]) / lineData[0]).toInt()
            captureZone[2] = ((700 - lineData[1]) / lineData[0]).toInt() - captureZone[0]
            AppParameters.getInstance().captureZone = captureZone
            startActivity(intent)
        }
    }

    private fun findZone(): IntArray {
        val result = IntArray(4)

        return result
    }

    private fun clear() {
        image.visibility = View.INVISIBLE
        textureView?.visibility ?: View.VISIBLE
        Button436.visibility = View.INVISIBLE
        Button488.visibility = View.INVISIBLE
        Button546.visibility = View.INVISIBLE
        Button612.visibility = View.INVISIBLE
        validateCalButton.visibility = View.INVISIBLE
        clearPicture.visibility = View.INVISIBLE
        buttonPicture.visibility = View.VISIBLE

    }

    private fun allowWavelengthCalibration() {
        Button436.visibility = View.VISIBLE
        Button488.visibility = View.VISIBLE
        Button546.visibility = View.VISIBLE
        Button612.visibility = View.VISIBLE
        validateCalButton.visibility = View.VISIBLE
        clearPicture.visibility = View.VISIBLE
        buttonPicture.visibility = View.INVISIBLE
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
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        val imageopen = Image(mat)
        val mat2 = imageopen.Canny()
        originShift = imageopen.rectOrigin
        captureZone[1] = imageopen.yOrigin
        captureZone[3] = mat2.height()
        captureZone[4] = height
            mat.release()
        val subimage = Bitmap.createBitmap(mat2.width(), mat2.height(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(mat2, subimage)
        mat2.release()
        val rgb = RGBDecoder.getRGBCode(subimage, subimage.width, subimage.height)
        intensity = RGBDecoder.getImageIntensity(rgb)
        graphData = RGBDecoder.computeIntensityMean(intensity!!, subimage.width, subimage.height)
        //graphData = RGBDecoder.getMaxIntensity(intensity, intensity!!.size)
        textureView!!.visibility = View.INVISIBLE
        image.visibility = View.VISIBLE
        image.setImageBitmap(subimage)
    }

    /**
     * Computes slopes and intercept from calibration lines position and known wavelength values
     */
    private fun findSlopeAndIntercept(): DoubleArray {
        val result = DoubleArray(2)
        val wavelengths = intArrayOf(436, 488, 546, 612)
        val slopes = DoubleArray(6)
        var count = 0
        run {
            var i = 0
            while (i < wavelengths.size && count < slopes.size) {
                var j = i + 1
                while (j < wavelengths.size && count < slopes.size) {
                    slopes[count] =
                        (wavelengths[i] - wavelengths[j]).toDouble() / (wavelengthRaysPositions[i] - wavelengthRaysPositions[j])
                    count++
                    j++
                }
                i++
            }
        }
        var slopeMean = 0.0
        for (slope in slopes) {
            slopeMean += slope
        }
        slopeMean /= slopes.size.toDouble()
        var intercept = 0.0
        for (i in wavelengths.indices) {
            intercept += wavelengths[i] - slopeMean * wavelengthRaysPositions[i]
        }
        result[0] = slopeMean
        result[1] = intercept / wavelengths.size
        return result
    }

    /**
     * Displays the calibration line
     */
    fun displayLine() {
        wavelengthCalibrationView!!.drawLine()
    }

    /**
     * stores the current wavelength ray position. If the calibration line is misplaced, the value is not stored and an error message is displayed
     */
    private fun updateWaveLengthPositions() {
        if (currentButton != null) {
            val currentLinePosition =
                searchMaxIntensity(wavelengthCalibrationView!!.xPositionOfDrawnLine)
            for (i in wavelengthRaysPositions.indices) {
                if (i < currentIndex && wavelengthRaysPositions[i] > currentLinePosition) { // check if values in the tab before the currentIndex value are <= to the currentIndex value
                    Toast.makeText(this, "The selected order is not correct", Toast.LENGTH_SHORT)
                        .show()
                    return
                } else if (i > currentIndex && wavelengthRaysPositions[i] > 0 && wavelengthRaysPositions[i] <= currentLinePosition) { // check if values in the tab after the currentIndex value are >= to the currentIndex value
                    Toast.makeText(this, "The selected order is not correct", Toast.LENGTH_SHORT)
                        .show()
                    return
                }
            }
            wavelengthCalibrationView!!.changeXLine(
                currentLinePosition -
                        (originShift?.minus(image.x.toInt())!!)
            )
            wavelengthRaysPositions[currentIndex] = currentLinePosition
        }
    }

    private fun searchMaxIntensity(currentLinePosition: Int): Int {
        val viewShift = image.x
        var res = 0
        var max = 0.0
        val dim = 25
        val j = (currentLinePosition - viewShift.toInt() - dim / 2).coerceAtLeast(0)
        val k = (currentLinePosition - viewShift.toInt() + dim / 2).coerceAtMost(intensity?.size!!-1)
        for (i in j until k) {
            if (graphData?.get((i))!! > max) {
                res = (i + originShift!!)
                max = graphData?.get((i))!!
                Log.w("merde!!!!!", "" + res)
            }
        }
        val position = "x : $res"
        positionValue!!.text = position
        val inten = "i : $max"
        intensityValue!!.text = inten
        val line = "x0 : $currentLinePosition"
        positionLine!!.text = line
        val min = "min : ${j + originShift!!}"
        minSearch!!.text = min
        val max0 = "max : ${k + originShift!!}"
        maxSearch!!.text = max0
        return res
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
            cameraDevice!!.close()
        }

        override fun onError(cameraDevice: CameraDevice, i: Int) {
            cameraDevice.close()
        }
    }

    /**
     * opens the camera (if allowed), activates flash light and sets image dimension for capture
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
                            cameraCaptureSession!!,
                            captureListener
                        )
                    }

                    override fun onConfigureFailed(captureSession: CameraCaptureSession) {
                        Toast.makeText(
                            this@WavelengthCalibrationActivity,
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
     * disables some camera automatics (such as auto focus, lens stabilization,auto exposure...) for the specified CameraCaptureSession
     */
    private fun disableAutomatics(
        captureBuilder: CaptureRequest.Builder,
        session: CameraCaptureSession,
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
            session.setRepeatingRequest(captureRequest, callback, backgroundHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    public override fun onPause() {
        super.onPause()
        Log.e(TAG, "On Pause")
        cameraDevice!!.close()
        textureView = null
    }

    public override fun onResume() {
        super.onResume()
        Log.e(TAG, "On Resume")
        if (textureView == null) { // prevents camera preview from freezing when app is resumed
            textureView = findViewById(R.id.texture)
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
        private const val TAG = "Wavelength Calibration"
    }

    /*private fun saveinfo(text: String, graph: Bitmap) {
        if (graph == null) {
            Toast.makeText(
                this@WavelengthCalibrationActivity,
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
                for (i in graphData!!.indices) {
                    outputStream.write("0,".toByteArray())
                    outputStream.write("""${graphData!![i]}""".toByteArray())
                }
            } finally {
                if (outputStream != null) {
                    outputStream.close()
                    Toast.makeText(
                        this@WavelengthCalibrationActivity,
                        "File successfully saved",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }*/
}