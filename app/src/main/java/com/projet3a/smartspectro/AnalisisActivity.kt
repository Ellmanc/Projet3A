package com.projet3a.smartspectro

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Environment
import android.os.Looper
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import com.google.android.gms.location.*
import com.jjoe64.graphview.BuildConfig
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import kotlinx.android.synthetic.main.analysis_activity_layout.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.floor

class AnalysisActivity : Activity(), LocationListener {
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null
    private var locationRequest: LocationRequest? = null
    private var locationCallback: LocationCallback? = null
    private var latitude = 0.0
    private var longitude = 0.0
    private var dataSize = 0
    private lateinit var values: Array<DataPoint?>

    /**
     * Displays last known position
     */
    private var lastKnownPositionElement: TextView? = null
        get() {
            field = lastKnownPosition
            val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())
            val currentDateAndTime = sdf.format(Date())
            position =
                "Data measured at position ($latitude,$longitude) on $currentDateAndTime"
            field!!.text = position
            field!!.visibility = View.VISIBLE
            return null
        }
    private var position: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.analysis_activity_layout)
        drawGraph()
        enableShareButton()
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        buildGoogleApiClient()
    }

    /**
     * Adds listener to the share button
     */
    private fun enableShareButton() {
        shareButton.setOnClickListener { openEmail() }
    }

    /**
     * opens e-mail dialog box with file containing data as attachment
     */
    private fun openEmail() {
        val file = saveMeasurements()
        val emailIntent = Intent(Intent.ACTION_SEND)
        emailIntent.type = "vnd.android.cursor.dir/email"
        emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf(""))
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Smart Spectro measurements")
        if (position != null) {
            emailIntent.putExtra(Intent.EXTRA_TEXT, position)
        }
        if (file != null) {
            val uri = FileProvider.getUriForFile(
                this@AnalysisActivity,
                BuildConfig.APPLICATION_ID + ".provider",
                file
            )
            emailIntent.putExtra(Intent.EXTRA_STREAM, uri)
        }
        try {
            startActivity(emailIntent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "No email app found on this device", Toast.LENGTH_SHORT).show()
            return
        }
    }

    /**
     * Saves analysis activity's results in text file and returns file
     */
    private fun saveMeasurements(): File? {
        var res: File? = null
        val sdf = SimpleDateFormat("yyyyMMdd_HH:mm:ss", Locale.getDefault())
        val currentDateAndTime = sdf.format(Date())
        val outputStream: OutputStream
        val directory = File(
            Environment.DIRECTORY_DOCUMENTS
        ) //check whether Documents directory exists, if not, we create it
        if (!directory.exists()) {
            val result = directory.mkdirs()
            if (!result) {
                Toast.makeText(this, "Unable to create directory", Toast.LENGTH_SHORT).show()
                return null
            }
        }
        val file = File(
            Environment.DIRECTORY_DOCUMENTS,
            "Transmission_$currentDateAndTime.txt"
        )
        try {
            outputStream = FileOutputStream(file, false)
            for (dataPoint in values) {
                outputStream.write((dataPoint!!.x.toString() + ",").toByteArray())
                outputStream.write(
                    """${dataPoint.y}
                            |
                        """.trimMargin().toByteArray()
                )
            }
            outputStream.close()
            Toast.makeText(this@AnalysisActivity, "File successfully saved", Toast.LENGTH_SHORT)
                .show()
            res = file
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
        return res
    }

    /**
     * Draws transmission graph using data from camera activity
     */
    private fun drawGraph() {

        //getting sample and reference data
        val referenceData = AppParameters.getInstance().reference
        val sampleData = AppParameters.getInstance().sample
        dataSize = referenceData!!.size

        //creating graph series
        val xAxisTitle: String
        val maxTransmissionText: String
        var maxTransmissionValue: DataPoint? = DataPoint(0.0, 0.0)
        values = arrayOfNulls(dataSize)
        val slope = AppParameters.getInstance().slope
        val intercept = AppParameters.getInstance().intercept
        var begin = 400
        val xMin = begin
        if (slope != 0.0 && intercept != 0.0) {
            xAxisTitle = "Wavelength (nm)"
            for (i in referenceData.indices) {
                val x = slope * begin + intercept
                if (referenceData[i] != 0.0) {
                    values[i] = DataPoint(x, sampleData!![i] / referenceData[i])
                    if (values[i]!!.y > maxTransmissionValue!!.y) {
                        maxTransmissionValue = values[i]
                    }
                } else {
                    values[i] = DataPoint(x, 0.0)
                }
                begin++
            }
            maxTransmissionText =
                "Peak found at " + floor(maxTransmissionValue!!.x) + " nm and is " + Math.floor(
                    maxTransmissionValue.y
                )

            //setting manually X axis max and min bounds to see all points on graph
            resultGraph.viewport.isXAxisBoundsManual = true
            resultGraph.viewport.setMaxX((xMin+300).toDouble())
            resultGraph.viewport.setMinX(xMin.toDouble())
        } else {
            xAxisTitle = "Pixel position"
            for (i in referenceData.indices) {
                if (referenceData[i] != 0.0) {
                    values[i] = DataPoint(begin.toDouble(), sampleData!![i] / referenceData[i])
                    if (values[i]!!.y > maxTransmissionValue!!.y) {
                        maxTransmissionValue = values[i]
                    }
                } else {
                    values[i] = DataPoint(begin.toDouble(), 0.0)
                }
                begin++
            }
            maxTransmissionText =
                "Peak found at " + maxTransmissionValue!!.x + " px and is " + floor(
                    maxTransmissionValue.y
                )

            //setting manually X axis bound to see all points on graph
            resultGraph.viewport.isXAxisBoundsManual = true
            resultGraph.viewport.setMaxX((xMin+300).toDouble())
            resultGraph.viewport.setMinX(xMin.toDouble())
        }
        val series = LineGraphSeries(values)

        //setting up X and Y axis title
        val gridLabelRenderer = resultGraph.gridLabelRenderer
        gridLabelRenderer.horizontalAxisTitle = xAxisTitle
        gridLabelRenderer.verticalAxisTitle = "Transmission"
        resultGraph.addSeries(series)
        maxTransmission.text = maxTransmissionText
    }

    /**
     * Stops locations updates by google play services
     */
    private fun stopLocationUpdates() {
        fusedLocationProviderClient!!.removeLocationUpdates(locationCallback)
    }

    /**
     * Starts locations updates by google play services
     */
    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        fusedLocationProviderClient!!.requestLocationUpdates(
            locationRequest,
            locationCallback, Looper.getMainLooper()
        )
    }

    /**
     * Starts google api client to get location updates
     */
    private fun buildGoogleApiClient() {
    }

    /**
     * defines location request and callback to get device position and starts location updates
     */
    fun onConnected() {
        locationRequest = LocationRequest()
        locationRequest!!.interval = 1000
        locationRequest!!.fastestInterval = 1000
        locationRequest!!.priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    latitude = location.latitude
                    longitude = location.longitude
                }
                if (fusedLocationProviderClient != null) {
                    //stopLocationUpdates()
                }
                lastKnownPositionElement
            }
        }
        startLocationUpdates()
    }

    fun onConnectionSuspended() {}
    fun onConnectionFailed() {}
    override fun onLocationChanged(location: Location) {}
    public override fun onPause() {
        super.onPause()
        if (fusedLocationProviderClient != null) {
            //stopLocationUpdates()
        }
    }

    companion object {
        private const val TAG = "Analysis Activity"
    }
}