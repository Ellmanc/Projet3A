package com.projet3a.smartspectro

import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private var contextWrapper: ContextWrapper? = null
    private var permissionsGranted: Boolean = false
    private val REQUEST_PERMISSIONS = 200

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        enableGPS()
        if (Build.VERSION.SDK_INT <= 22) {
            permissionsGranted = true
        } else {
            askPermissions()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        if(item.title == "Quit"){
            finish()
        }
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * asks app permissions to user
     * */
    @RequiresApi(Build.VERSION_CODES.M)
    fun askPermissions() {
        val version = Build.VERSION.SDK_INT
        if (version >= 23) {
            if (this.contextWrapper?.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(
                        Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION), REQUEST_PERMISSIONS)
            } else {
                this.permissionsGranted = true
            }
        }
    }

    /**
     * Checks whether user has granted permissions for the app
     * */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_PERMISSIONS -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    this.permissionsGranted = true
                } else {
                    Toast.makeText(this@MainActivity, "You can't use this app without granting permissions", Toast.LENGTH_LONG).show()
                }
                return
            }
        }
    }

    /**
     * Displays GPS activation window on phone if it is not turned on
     * */
    private fun enableGPS() {
        val lm = applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if(lm.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            return
        }else{
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        }
    }
}