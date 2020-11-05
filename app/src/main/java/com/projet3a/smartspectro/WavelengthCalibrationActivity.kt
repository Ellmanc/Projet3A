package com.projet3a.smartspectro

import android.app.Activity
import android.content.Intent
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
import kotlinx.android.synthetic.main.wavelength_cal_layout.*

//private val backgroundHandler: Handler
class WavelengthCalibrationActivity : Activity() {
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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.wavelength_cal_layout)

        //adding custom surface view above the texture view
        val calibrationViewLayout: ConstraintLayout = findViewById(R.id.calibrationViewLayout)
        wavelengthCalibrationView = WavelengthCalibrationView(this)
        calibrationViewLayout.addView(wavelengthCalibrationView)
        enableListeners()
    }

    /**
     * Adds listeners on various UI components
     */
    private fun enableListeners() {

        /* Adding listeners to the buttons */
        val validateButton = findViewById<Button>(R.id.validateCalButton)!!
        validateButton.setOnClickListener { view: View? ->
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
            startActivity(intent)
        }
        Button436.setTextColor(Color.BLUE)
        Button436.setOnClickListener(View.OnClickListener {
            if (currentButton != null) {
                currentButton!!.setBackgroundColor(Color.argb(100, 187, 222, 251))
            }
            currentButton = Button436
            currentButton!!.setBackgroundColor(Color.CYAN)
            currentIndex = 0
        })
        Button488.setTextColor(Color.argb(100, 30, 144, 255))
        Button488.setOnClickListener(View.OnClickListener {
            if (currentButton != null) {
                currentButton!!.setBackgroundColor(Color.argb(100, 187, 222, 251))
            }
            currentButton = Button488
            currentButton!!.setBackgroundColor(Color.CYAN)
            currentIndex = 1
        })
        Button546.setTextColor(Color.GREEN)
        Button546.setOnClickListener(View.OnClickListener {
            if (currentButton != null) {
                currentButton!!.setBackgroundColor(Color.argb(100, 187, 222, 251))
            }
            currentButton = Button546
            currentButton!!.setBackgroundColor(Color.CYAN)
            currentIndex = 2
        })
        Button612.setTextColor(Color.RED)
        Button612.setOnClickListener(View.OnClickListener {
            if (currentButton != null) {
                currentButton!!.setBackgroundColor(Color.argb(100, 187, 222, 251))
            }
            currentButton = Button612
            currentButton!!.setBackgroundColor(Color.CYAN)
            currentIndex = 3
        })
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
    fun updateWaveLengthPositions() {
        if (currentButton != null) {
            val currentLinePosition = wavelengthCalibrationView!!.xPositionOfDrawnLine
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
            wavelengthRaysPositions[currentIndex] = currentLinePosition
        }
    }

    var textureListener: SurfaceTextureListener = object : SurfaceTextureListener {
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
            val cameraId = cameraManager.cameraIdList[0]
            val cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId)
            val map =
                cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
            imageDimension = map.getOutputSizes(SurfaceTexture::class.java)[0]
            cameraManager.openCamera(cameraId, stateCallback, null)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    /**
     * Creates a camera preview, a CaptureSession and sets various parameters for this CaptureSession (calls disableAutmatics method)
     */
    protected fun createCameraPreview() {
        try {
            val texture = textureView!!.surfaceTexture!!
            texture.setDefaultBufferSize(imageDimension!!.width, imageDimension!!.height)
            val surface = Surface(texture)
            captureRequestBuilder =
                cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            captureRequestBuilder!!.addTarget(surface)
            val captureListener: CaptureCallback = object : CaptureCallback() {
                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult
                ) {
                    super.onCaptureCompleted(session, request, result)
                }
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
                            cameraCaptureSession!!, captureListener
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
            textureView!!.setSurfaceTextureListener(textureListener)
            if (textureView!!.isAvailable()) {
                textureListener.onSurfaceTextureAvailable(
                    textureView!!.getSurfaceTexture(),
                    textureView!!.getWidth(),
                    textureView!!.getHeight()
                )
            }
        }
    }

    companion object {
        private const val TAG = "Wavelength Calibration"
    }
}