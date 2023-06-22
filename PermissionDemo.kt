import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsStatusCodes
import com.timestampcamera.autodatetimestamp.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

abstract class LocationHelperActivity : AppCompatActivity() {
    private var jobLocation: Job? = null

    private val locationPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    
    private var isMandatory: Boolean? = true
    private var askCount = 0

    val permissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grantRes ->
            val granted = !grantRes.values.contains(false)
            if (!granted) {
                askCount++
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    shouldShowRequestPermissionRationale(locationPermissions.first())
                    && shouldShowRequestPermissionRationale(locationPermissions.last())
                ) {
                    if (isMandatory == true) {
                        askCount++
                        askForLocationPermission()
                    }
                } else {
                    if (isMandatory == true)
                        askCount = 2
                    showSettingsDialog()
                }
            } else {
                getUserLocation()
            }
        }

    private var permissionDialog: AlertDialog? = null

    private fun showSettingsDialog() {
        if (permissionDialog == null) {
            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            builder.setTitle(getString(R.string.permission_denied_title))
                .setCancelable(false)
                .setMessage(getString(R.string.allow_for_smooth))
                .setPositiveButton(R.string.action_settings) { dialog, _ ->
                    dialog.dismiss()
                    val intent = Intent()
                    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    val uri = Uri.fromParts(
                        "package", packageName,
                        null
                    )
                    intent.data = uri
                    startActivity(intent)
                }
            permissionDialog = builder.create()
            permissionDialog?.show()
        } else {
            if (permissionDialog?.isShowing == false) permissionDialog?.show()
        }
    }

    private fun askForLocationPermission() {
        permissionsLauncher.launch(locationPermissions)
    }

    private fun locationPermissionGranted() = locationPermissions.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    private var shownGPSDialog: Boolean? = false
    private val gpsLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            when (result.resultCode) {
                Activity.RESULT_OK -> {
                    getUserLocation()
                }

                Activity.RESULT_CANCELED -> {
                    if (isMandatory == true) {
                        shownGPSDialog = false
                        enableGPS()
                    }
                }

                else -> {
                    shownGPSDialog = true
                }
            }
        }

    private fun allowedGPS() = run {
        val manager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val gpsEnabled = manager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        gpsEnabled
    }

    private fun enableGPS() {
        val locationRequest by lazy {
            LocationRequest.create().apply {
                interval = TimeUnit.SECONDS.toMillis(LocationHelper.UPDATE_INTERVAL_SECS)
                fastestInterval =
                    TimeUnit.SECONDS.toMillis(LocationHelper.FASTEST_UPDATE_INTERVAL_SECS)
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            }
        }
        val mSettingsClient = LocationServices.getSettingsClient(this)
        val builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(locationRequest)
        val mLocationSettingsRequest = builder.build()
        mSettingsClient.checkLocationSettings(mLocationSettingsRequest)
            .addOnSuccessListener {
                shownGPSDialog = true
                getUserLocation()
            }.addOnFailureListener { e: Exception ->
                when ((e as ApiException).statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                        Log.e("TAG", "RESOLUTION_REQUIRED: ")
                        try {
                            if (shownGPSDialog == false) {
                                val rae: ResolvableApiException = e as ResolvableApiException
                                gpsLauncher.launch(
                                    IntentSenderRequest.Builder(rae.resolution).build()
                                )
                                shownGPSDialog = true
                            }
                        } catch (sie: IntentSender.SendIntentException) {
                            sie.printStackTrace()
                        }
                    }

                    LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                        val errorMessage: String =
                            "Location settings are inadequate, and cannot be " + "fixed here. Fix in Settings."
                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG)
                            .show()
                    }
                }
            }
    }

    fun alwaysAskPermissions(isMandatory: Boolean? = true) {
        this.isMandatory = isMandatory
    }
    
    private fun getUserLocation() {
        if (!locationPermissionGranted()) {
            jobLocation?.cancel()
            if (askCount == 0)
                askForLocationPermission()
            else if (askCount == 2)
                showSettingsDialog()
            return
        } else askCount = 2

        if (!allowedGPS()) {
            enableGPS()
            return
        }

        val locationHelper =
            LocationHelper(LocationServices.getFusedLocationProviderClient(this))
        jobLocation?.cancel()
        jobLocation = lifecycleScope.launch(Dispatchers.IO) {
            locationHelper
                .fetchUpdates()
                .cancellable()
                .collect {
                    launch(Dispatchers.Main) {
                        getLocationUpdates(it.locationVal)
                    }
                }
        }
    }

    abstract fun getLocationUpdates(location: Location?)

    override fun onResume() {
        super.onResume()
        getUserLocation()
    }

    override fun onPause() {
        jobLocation?.cancel()
        super.onPause()
    }

    override fun onDestroy() {
        jobLocation?.cancel()
        super.onDestroy()
    }
}

















import android.annotation.SuppressLint
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class LocationHelper @Inject constructor(
    private val client: FusedLocationProviderClient
) {
    @SuppressLint("MissingPermission")
    fun fetchUpdates(): Flow<LatLngLocation> = callbackFlow {
        val locationRequest by lazy {
            LocationRequest.create().apply {
                interval = TimeUnit.SECONDS.toMillis(UPDATE_INTERVAL_SECS)
                fastestInterval = TimeUnit.SECONDS.toMillis(FASTEST_UPDATE_INTERVAL_SECS)
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            }
        }

        val callBack = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                val location = locationResult.lastLocation
                val userLocation = LatLngLocation(
                    latitude = location.latitude,
                    longitude = location.longitude,
                ).apply {
                    locationVal = location
                }
                trySend(userLocation).isSuccess
            }
        }

        client.requestLocationUpdates(locationRequest, callBack, Looper.getMainLooper())
        awaitClose { client.removeLocationUpdates(callBack) }
    }

    companion object {
        const val UPDATE_INTERVAL_SECS = 5L
        const val FASTEST_UPDATE_INTERVAL_SECS = 2L
    }
}

data class LatLngLocation(var latitude: Double, var longitude: Double) {
    var address: String = ""
    var locationVal: Location? = null
}









public class GPS {
    private static StringBuilder sb = new StringBuilder(20);

    public static String latitudeRef(double latitude) {
        return latitude < 0.0d ? "S" : "N";
    }

    public static String longitudeRef(double longitude) {
        return longitude < 0.0d ? "W" : "E";
    }

    synchronized public static final String convert(double latitude) {
        latitude = Math.abs(latitude);
        int degree = (int) latitude;
        latitude *= 60;
        latitude -= (degree * 60.0d);
        int minute = (int) latitude;
        latitude *= 60;
        latitude -= (minute * 60.0d);
        int second = (int) (latitude * 1000.0d);

        sb.setLength(0);
        sb.append(degree);
        sb.append("/1,");
        sb.append(minute);
        sb.append("/1,");
        sb.append(second);
        sb.append("/1000");
        return sb.toString();
    }
}








private fun deleteFolder(deleteFile: File) {
        val documentFile = DocumentFile.fromTreeUri(ctx, fileDocPath.uri.toString().toUri())
        if (documentFile != null) {
            val documentFiles = documentFile.listFiles()
            for (file in documentFiles) {
                if (file.name == deleteFile.name) {
                    file.delete()
                    break
                }
            }
        }
    }
