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














import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;

import com.smartdialer.dialer.phone.call.R;

import java.util.ArrayList;
import java.util.List;

import static android.content.ContentValues.TAG;

public class RoundWaveView extends View {

    private static final float LINE_SMOOTHNESS = 0.16f;
    private Path path;
    private Paint paint;

    int pointCount;
    float innerSizeRatio = 0.75f;
    float[] firstRadius;
    float[] secondRadius;   //
    float[] animationOffset;    //
    float[] xs; //
    float[] ys;

    float[] fractions;

    ValueAnimator[] animators;

    float angleOffset;

    float currentCx;
    float currentCy;
    float translationRadius;
    float translationRadiusStep;

    boolean useAnimation;

    Context context;

    public RoundWaveView(Context context) {
        this(context, null);
        this.context = context;
    }

    public RoundWaveView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.RoundWaveView);
        pointCount = typedArray.getInt(R.styleable.RoundWaveView_pointCount, 6);
        useAnimation = typedArray.getBoolean(R.styleable.RoundWaveView_useAnimation, true);
        typedArray.recycle();
        init(pointCount);

        path = new Path();
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
//        paint.setColor(Color.MAGENTA);
        paint.setStyle(Paint.Style.FILL);

        float delta = 1.0f / pointCount;
        for (int i = 0; i < fractions.length; i++) {
            fractions[i] = delta * i;
        }

        for (int i = 0; i < pointCount; i++) {
            int pos = (int) (Math.random() * pointCount);
            float[] dest = new float[pointCount * 2 + 1];
            int inc = 1;
            for (int j = 0; j < dest.length; j++) {
                dest[j] = fractions[pos];
                pos += inc;
                if (pos < 0 || pos >= fractions.length) {
                    inc = -inc;
                    pos += inc * 2;
                }
            }

            if (i == 0) {
                List<Float> list = new ArrayList<>();
                for (float f : dest) {
                    list.add(f);
                }
                Log.d(TAG, "DEST " + list);
            }

            if (useAnimation) {
                ValueAnimator animator = ValueAnimator.ofFloat(dest).setDuration((long) (2000));
                animator.setInterpolator(new LinearInterpolator());
                animator.setRepeatMode(ValueAnimator.RESTART);
                animator.setRepeatCount(ValueAnimator.INFINITE);
                animator.start();
                if (i == 0) {
                    animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator valueAnimator) {
                            randomTranslate();
                            invalidate();
                        }
                    });
                }
                animators[i] = animator;
                animator.start();
            }
        }
    }

    private float lastTranslationAngle;

    private void randomTranslate() {
        float r = translationRadiusStep;
        float R = translationRadius;

        float cx = getWidth() / 2;
        float cy = getHeight() / 2;
        float vx = currentCx - cx;
        float vy = currentCy - cy;
        float ratio = 1 - r / R;
        float wx = vx * ratio;
        float wy = vy * ratio;
        lastTranslationAngle = (float) ((Math.random() - 0.5) * Math.PI / 4 + lastTranslationAngle);
        float distRatio = (float) Math.random();

        currentCx = (float) (cx + wx + r * distRatio * Math.cos(lastTranslationAngle));
        currentCy = (float) (cy + wy + r * distRatio * Math.sin(lastTranslationAngle));

    }

    private void init(int pointCount) {
        firstRadius = new float[pointCount];
        secondRadius = new float[pointCount];
        animationOffset = new float[pointCount];
        xs = new float[pointCount];
        ys = new float[pointCount];

        fractions = new float[pointCount + 1];

        animators = new ValueAnimator[pointCount];

        angleOffset = (float) (Math.PI * 2 / pointCount * Math.random());
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        float radius = Math.min(w, h) / 2;
        float innerRadius = radius * innerSizeRatio;
        float ringWidth = radius - innerRadius;

        for (int i = 0; i < pointCount; i++) {
            firstRadius[i] = (float) (innerRadius + ringWidth * Math.random());
            secondRadius[i] = (float) (innerRadius + ringWidth * Math.random());
            animationOffset[i] = (float) Math.random();
        }

        paint.setShader(new LinearGradient(0, 0, 0, h, ContextCompat.getColor(context, R.color.teal_700), ContextCompat.getColor(context, R.color.teal_700), Shader.TileMode.MIRROR));
        paint.setAlpha((int) (0.2f * 255));

        currentCx = w / 2;
        currentCy = h / 2;
        translationRadius = radius / 6;
        translationRadiusStep = radius / 4000;
    }

    float[] temp = new float[2];

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

//        paint.setColor(Color.MAGENTA);
//        float cx = getWidth() / 2;
//        float cy = getHeight() / 2;
//        float fraction = (float) valueAnimator.getAnimatedValue();
        for (int i = 0; i < pointCount; i++) {
//            float currentFraction = animationOffset[i] + fraction;
//            if (currentFraction >= 1) {
//                currentFraction = currentFraction - 1;
//            }
            float currentFraction = useAnimation ? (float) animators[i].getAnimatedValue() : 0;
            float radius = firstRadius[i] * (1 - currentFraction) + secondRadius[i] * currentFraction;
            float angle = (float) (Math.PI * 2 / pointCount * i) + angleOffset;
            xs[i] = (float) (currentCx + radius * Math.cos(angle));
            ys[i] = (float) (currentCy + radius * Math.sin(angle));
        }

        path.reset();
        path.moveTo(xs[0], ys[0]);
        for (int i = 0; i < pointCount; i++) {
            float currX = getFromArray(xs, i);
            float currY = getFromArray(ys, i);
            float nextX = getFromArray(xs, i + 1);
            float nextY = getFromArray(ys, i + 1);

            getVector(xs, ys, i, temp);

            float vx = temp[0];
            float vy = temp[1];

            getVector(xs, ys, i + 1, temp);
            float vxNext = temp[0];
            float vyNext = temp[1];

            path.cubicTo(currX + vx, currY + vy, nextX - vxNext, nextY - vyNext, nextX, nextY);
        }

        canvas.drawPath(path, paint);
//        float firstFraction = (float) animators[0].getAnimatedValue();
//        paint.setColor(Color.BLACK);
//        float r = 30;
//        canvas.drawCircle((getWidth() - r * 2) * firstFraction + r, getHeight() / 2, r, paint);

    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (useAnimation) {
            for (ValueAnimator animator : animators) {
                animator.end();
            }

        }
    }

    static float getFromArray(float[] arr, int pos) {
        return arr[(pos + arr.length) % arr.length];
    }

    static void getVector(float[] xs, float[] ys, int i, float[] out) {
        float nextX = getFromArray(xs, i + 1);
        float nextY = getFromArray(ys, i + 1);
        float prevX = getFromArray(xs, i - 1);
        float prevY = getFromArray(ys, i - 1);

        float vx = (nextX - prevX) * LINE_SMOOTHNESS;
        float vy = (nextY - prevY) * LINE_SMOOTHNESS;

        out[0] = vx;
        out[1] = vy;

    }

}




<declare-styleable name="RoundWaveView">
        <attr name="pointCount" format="integer" />
        <attr name="useAnimation" format="boolean" />
    </declare-styleable>






    fun Activity.setAsIntent(path: String, applicationId: String) {
    ensureBackgroundThread {
        val newUri = getFinalUriFromPath(path, applicationId) ?: return@ensureBackgroundThread
        Intent().apply {
            action = Intent.ACTION_ATTACH_DATA
            setDataAndType(newUri, getUriMimeType(path, newUri))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val chooser =
                Intent.createChooser(this, getString(com.simplemobiletools.commons.R.string.set_as))

            try {
                startActivityForResult(chooser, REQUEST_SET_AS)
            } catch (e: ActivityNotFoundException) {
                toast(com.simplemobiletools.commons.R.string.no_app_found)
            } catch (e: Exception) {
                showErrorToast(e)
            }
        }
    }
}

fun isOnMainThread() = Looper.myLooper() == Looper.getMainLooper()

fun ensureBackgroundThread(callback: () -> Unit) {
    if (isOnMainThread()) {
        Thread {
            callback()
        }.start()
    } else {
        callback()
    }
}
