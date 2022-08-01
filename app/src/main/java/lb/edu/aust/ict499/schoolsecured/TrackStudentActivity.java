package lb.edu.aust.ict499.schoolsecured;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;


import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import lb.edu.aust.ict499.schoolsecured.databinding.ActivityTrackStudentBinding;

public class TrackStudentActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityTrackStudentBinding binding;
    private String ModuleID, StudentName, CurrentUsername;
    FirebaseFirestore firestoredb = FirebaseFirestore.getInstance();
    FirebaseDatabase realTimedb = FirebaseDatabase.getInstance();
    DatabaseReference mRealTimeDatabase = FirebaseDatabase.getInstance().getReference();
    private float GEOFENCE_RADIUS = 50;
    private LatLng studentLatLng;
    private String schoolOpenTime, schoolCloseTime;
    private double schoolLat;
    private double schoolLongi;
    private NotificationHelper notificationHelper;
    private int positiveNotification = 0;
    private int negativeNotification = 0;
    private MarkerOptions markerOptions;
    private Snackbar snackbar;
    private View custom;
    private final String POLICE_NUM = "Emergency Services Number Here";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityTrackStudentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        Bundle extras = getIntent().getExtras();

        if (extras != null) {
            ModuleID = extras.getString("Student Module ID");
            StudentName = extras.getString("Student Name");
            CurrentUsername = extras.getString("Username");
        }

        notificationHelper = new NotificationHelper(this);

    }
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        googleMap.clear();
        loadMapData();
    }

    public void loadMapData(){
        try {
            mRealTimeDatabase.child("Users").child(CurrentUsername).get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DataSnapshot> task) {
                    if (!task.isSuccessful()) {
                        Log.d("Load Map Data", "onComplete Task Not Successful: " + task.getException());
                    } else {
                        Map<String, Object> dataFromFirestore = (Map<String, Object>) task.getResult().getValue();
                        System.out.println(dataFromFirestore);
                        Map<String, Object> studentData = (Map<String, Object>) dataFromFirestore.get("Student: " + StudentName);
                        Map<String, String> schoolData = (Map<String, String>) studentData.get("SchoolData");

                        String coordinateStringUnfiltered = schoolData.get("Coordinates");
                        String[] coordinateStringArray = coordinateStringUnfiltered.substring(coordinateStringUnfiltered.indexOf('(') + 1, coordinateStringUnfiltered.indexOf(')')).split(",");

                        schoolLat = Double.parseDouble(coordinateStringArray[0]);
                        schoolLongi = Double.parseDouble(coordinateStringArray[1]);

                        schoolOpenTime = schoolData.get("OpeningTime");
                        schoolCloseTime = schoolData.get("ClosingTime");


                        markerOptions = new MarkerOptions().position(new LatLng(0, 0)).title("Student");
                        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.studentmarker));
                        Marker marker = mMap.addMarker(markerOptions);
                        marker.setVisible(false);
                        if (!isDuringSchoolTime())
                            loadSnackbar(-1);

                        mapSetupGeofence(new LatLng(schoolLat, schoolLongi), GEOFENCE_RADIUS);
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(schoolLat, schoolLongi), 18f));

                        mRealTimeDatabase.child("ModuleID").child(ModuleID).addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot){
                                try {
                                    Map<String, String> dataFromRealtimeDB = (Map<String, String>) snapshot.getValue();
                                    System.out.println(dataFromRealtimeDB);
                                    double lat = Double.parseDouble(dataFromRealtimeDB.get("lat"));
                                    double lng = Double.parseDouble(dataFromRealtimeDB.get("longi"));

                                    if (dataFromRealtimeDB != null) {
                                        studentLatLng = new LatLng(lat, lng);
                                        if (isDuringSchoolTime()) {
                                            marker.setVisible(true);
                                            marker.setPosition(studentLatLng);
                                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(studentLatLng, 18f));
                                            monitorStudentLocation(studentLatLng);
                                        }
                                    }
                                } catch (Exception e){
                                    Log.d("Monitor Student", "onDataChangeError: " + e.getMessage().toString());
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });
                    }
                }
            });


        }
        catch (Exception e){Log.d("Load Map Data Error", "loadMapData: " + e.toString()); }
    }

    public void addGeofenceCircle(LatLng latLng, float radius){
        CircleOptions circleOptions = new CircleOptions();
        circleOptions.center(latLng);
        circleOptions.radius(radius);
        circleOptions.fillColor(Color.argb(100, 63,193,201));
        circleOptions.strokeColor(Color.argb(255, 63,193,201));
        circleOptions.strokeWidth(4);
        mMap.addCircle(circleOptions);
    }

    public void addSchoolMarker(LatLng latLng){
        MarkerOptions markerOptions = new MarkerOptions().position(latLng);
        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.schoolmarker));
        mMap.addMarker(markerOptions);
    }

    public void mapSetupGeofence(LatLng latLng, float radius){
        addSchoolMarker(latLng);
        addGeofenceCircle(latLng, radius);
    }

    // Monitoring Student Activity
    public void monitorStudentLocation(LatLng latLng){
        System.out.println(isDuringSchoolTime());
        double distanceBetweenStudentAndSchool = distance(schoolLat, schoolLongi, studentLatLng.latitude, studentLatLng.longitude);
        if (isDuringSchoolTime()){
            if (distanceBetweenStudentAndSchool <= GEOFENCE_RADIUS){
                // Display a bottom snack bar that the student is in the school
                // Keep a persistent notification on the phone

                if ((positiveNotification == 0)) {
                    notificationHelper.sendHighPriorityNotification("Student Still in School", "Student In School", TrackStudentActivity.class);
                    positiveNotification=1;
                    loadSnackbar(0);
                    if (negativeNotification==1){
                        negativeNotification=0;
                    }
                }

            }
            else if ((negativeNotification == 0)){
                    notificationHelper.sendHighPriorityNotification("Student Exited School", "Student Exited School", TrackStudentActivity.class);
                    negativeNotification=1;
                    loadSnackbar(1);
                    if (positiveNotification==1){
                        positiveNotification=0;
                    }
                }
            }
        else
            loadSnackbar(-1);
        }


    private double distance(double lat1, double lon1, double lat2, double lon2) {
        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1))
                * Math.sin(deg2rad(lat2))
                + Math.cos(deg2rad(lat1))
                * Math.cos(deg2rad(lat2))
                * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;
        return (dist*1609.344);
    }

    private double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    private double rad2deg(double rad) {
        return (rad * 180.0 / Math.PI);
    }

    private boolean isDuringSchoolTime(){
        final Calendar instance = GregorianCalendar.getInstance();
        final int i = instance.get(Calendar.HOUR_OF_DAY);
        int day = instance.get(Calendar.DAY_OF_WEEK);
        System.out.println("DAY of the week: " + day);
        int schoolhourOpen = Integer.parseInt(schoolOpenTime.split(":")[0]);
        int schoolhourClose = Integer.parseInt(schoolCloseTime.split(":")[0]);

//        return i <= schoolhourClose && i >= schoolhourOpen && day !=7 && day!=1;
        return true;
    }

    private void loadSnackbar(int statusCode){
        // Status Code 0 Means Student is inside the School
        // Status code 1 Means Student Left School
        snackbar = Snackbar.make(binding.getRoot(), "", BaseTransientBottomBar.LENGTH_INDEFINITE);
        if (snackbar.isShown()){
            snackbar.dismiss();
        }
        if (!isDuringSchoolTime()){
            custom = getLayoutInflater().inflate(R.layout.status_snackbar_schoolclosed, null);
            snackbar.getView().setBackgroundColor(Color.TRANSPARENT);
            Snackbar.SnackbarLayout snackbarLayout = (Snackbar.SnackbarLayout) snackbar.getView();
            snackbarLayout.setPadding(0,0,0,0);
            snackbarLayout.addView(custom, 0);
            snackbar.show();
        }
        else if (statusCode ==0) {
            custom = getLayoutInflater().inflate(R.layout.status_snackbar_custom, null);
            snackbar.getView().setBackgroundColor(Color.TRANSPARENT);
            Snackbar.SnackbarLayout snackbarLayout = (Snackbar.SnackbarLayout) snackbar.getView();
            snackbarLayout.setPadding(0,0,0,0);
            snackbarLayout.addView(custom, 0);
            snackbar.show();
        }
        else if (statusCode == 1) {
            custom = getLayoutInflater().inflate(R.layout.status_snackbar_alert, null);
            snackbar.getView().setBackgroundColor(Color.TRANSPARENT);
            Snackbar.SnackbarLayout snackbarLayout = (Snackbar.SnackbarLayout) snackbar.getView();
            snackbarLayout.setPadding(0,0,0,0);

            custom.findViewById(R.id.dismiss).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    snackbar.dismiss();
                }
            });

            custom.findViewById(R.id.call_police).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent policeCallIntent = new Intent(Intent.ACTION_CALL);
                    policeCallIntent.setData(Uri.parse("tel:"+POLICE_NUM));
                    startActivity(policeCallIntent);
                }
            });


            snackbarLayout.addView(custom, 0);
            snackbar.show();

        }




    }

}