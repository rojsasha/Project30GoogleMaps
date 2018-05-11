package com.example.rojsa.project30googlemaps;


import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.animation.LinearInterpolator;

import com.akexorcist.googledirection.DirectionCallback;
import com.akexorcist.googledirection.GoogleDirection;
import com.akexorcist.googledirection.constant.TransportMode;
import com.akexorcist.googledirection.model.Direction;
import com.akexorcist.googledirection.util.DirectionConverter;
import com.example.rojsa.project30googlemaps.utils.AndroidUtils;
import com.example.rojsa.project30googlemaps.utils.AppConstants;
import com.example.rojsa.project30googlemaps.utils.PermissionUtils;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.maps.android.SphericalUtil;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mGoogleMap;
    private Marker mCarMarker;
    private LatLng startLocation = new LatLng(46, 73);
    private LatLng endLocation = new LatLng(42.86, 74.60);
    float fraction;
    private ArrayList<LatLng> mRouteList = new ArrayList<>();
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private Handler mHandler = new Handler();
    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            animateCar();
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initMap();


    }

    private void initMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

    }

    private void callServicesDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Location GPS is turned off")
                .setMessage("Please, turn on GPS your app")
                .setCancelable(false)
                .setPositiveButton("SETTINGS", mOnClickListener)
                .create()
                .show();

    }

    private void callServicesDialogPermissions() {
        new AlertDialog.Builder(this)

                .setTitle("Go to Permissions App")
                .setMessage("Please, turn on GPS to HIGH ACCURACY mode")
                .setCancelable(false)
                .setPositiveButton("SETTINGS", mOnClickListenerAppPermission)
                .create()
                .show();

    }

    private final DialogInterface.OnClickListener mOnClickListenerAppPermission = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
//            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package",getPackageName(),null)));
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setData(Uri.fromParts("package", getPackageName(), null));
            startActivity(intent);
            dialog.dismiss();
        }
    };
    private final DialogInterface.OnClickListener mOnClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            dialog.dismiss();
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        checkForLocationService();
    }

    private void checkForLocationService() {
        enableMyLocation();

    }

    private void enableMyLocation() {
        if (PermissionUtils.checkLocationPermission(this))
            if (mGoogleMap != null) {
                mGoogleMap.setMyLocationEnabled(true);
            }
    }


    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == AppConstants.REQUEST_CODE_LOCATION_PERMISSION) {
            for (int result : grantResults) {
                if (result == PackageManager.PERMISSION_GRANTED) {
                    getMyCurrentLocation();
                } else {
                    callServicesDialogPermissions();
                }
            }
        }

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
//        drawCarMarker();
        getMyCurrentLocation();

    }

    private void drawCarMarker() {
        LatLng bishkek = new LatLng(42, 73);
        MarkerOptions options = new MarkerOptions().position(startLocation).title("Bishkek")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_car));
        mCarMarker = mGoogleMap.addMarker(options);
//        animateToBishkek(bishkek);
    }

    private void animateToBishkek(LatLng bishkekLitLng) {
        CameraUpdate cu = CameraUpdateFactory.newLatLngZoom(bishkekLitLng, 40);
        mGoogleMap.animateCamera(cu);
        //        mHandler.post(mRunnable);

    }

    private void animateCar() {


        ValueAnimator carAnimator = ValueAnimator.ofFloat(0, 1);
        carAnimator.setDuration(2000);
        carAnimator.setInterpolator(new LinearInterpolator());
        carAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                fraction = animation.getAnimatedFraction();
                LatLng newLocation = SphericalUtil.interpolate(startLocation, endLocation, fraction);

                mCarMarker.setPosition(newLocation);
                float hearing = (float) SphericalUtil.computeHeading(startLocation, endLocation);
                mCarMarker.setRotation(hearing);
                CameraUpdate cu = CameraUpdateFactory.newCameraPosition(
                        new CameraPosition.Builder()
                                .target(newLocation)
                                .zoom(10)
                                .build());
                mGoogleMap.moveCamera(cu);
            }
        });
        carAnimator.start();


        mHandler.postDelayed(mRunnable, 3000);
    }

    private void getMyCurrentLocation() {
        if (PermissionUtils.checkLocationPermission(this)) {
            Task<Location> locationTask = mFusedLocationProviderClient.getLastLocation();
            locationTask.addOnCompleteListener(new OnCompleteListener<Location>() {
                @Override
                public void onComplete(@NonNull Task<Location> task) {
                    if (task.getResult() != null) {
                        startLocation = new LatLng(
                                task.getResult().getLatitude(),
                                task.getResult().getLongitude());
                        CameraUpdate cu = CameraUpdateFactory.newLatLngZoom(startLocation, 80);
                        mGoogleMap.animateCamera(cu);
//                        getRouteList();


                    } else {
                        AndroidUtils.showShortTost(MainActivity.this, "no Location detected");
                    }
                }
            });
        }

    }

    private void getRouteList() {
        GoogleDirection.withServerKey(getString(R.string.google_api_key))
                .from(startLocation)
                .to(endLocation)
                .transportMode(TransportMode.DRIVING)
                .execute(new DirectionCallback() {
                    @Override
                    public void onDirectionSuccess(Direction direction, String rawBody) {
                        if (direction.isOK()) {
                            mRouteList = direction
                                    .getRouteList()
                                    .get(0)
                                    .getLegList()
                                    .get(0)
                                    .getDirectionPoint();
                            drawRoute(mRouteList);

                        } else {
                            AndroidUtils.showShortTost(MainActivity.this, "direction not ok" + direction.getStatus());
                        }
                    }

                    @Override
                    public void onDirectionFailure(Throwable t) {
                        AndroidUtils.showShortTost(MainActivity.this, t.getMessage());

                    }
                });
    }

    private void drawRoute(ArrayList<LatLng> routeList) {
        PolylineOptions options = DirectionConverter.createPolyline(this, routeList
                , 5, Color.parseColor("#303F9F"));
        mGoogleMap.addPolyline(options);
        for (int i = 0; i < 2; i++) {
            MarkerOptions mMarkerOptions = new MarkerOptions()
                    .position(i == 0 ? startLocation : endLocation)
                    .icon(BitmapDescriptorFactory.defaultMarker(
                            i == 0 ? BitmapDescriptorFactory.HUE_GREEN :
                                    BitmapDescriptorFactory.HUE_RED
                    ));
            mGoogleMap.addMarker(mMarkerOptions);


        }
    }
    private void saveLocation(){

    }
}
