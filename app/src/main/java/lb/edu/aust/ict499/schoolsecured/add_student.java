package lb.edu.aust.ict499.schoolsecured;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lb.edu.aust.ict499.schoolsecured.databinding.ActivityMapsBinding;


public class add_student extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    EditText moduleID, studentName, studentAge, schoolName;
    TextView tv_schoolName;
    Spinner  openingTime, closingTime;
    EditText[] editTexts;
    public final LatLng[] latLngs = new LatLng[10];
    Button submit;
    HashMap<String, String> schoolData = new HashMap<>();
    private MapView mMapView;
    private static final String MAPVIEW_Bundle_KEY = "MapViewBundleKey";
    private GoogleMap map;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_student);

        moduleID = findViewById(R.id.et_moduleID1);
        studentName = findViewById(R.id.et_studentName);
        studentAge = findViewById(R.id.et_studentAge);

        openingTime = findViewById(R.id.spinner_openTime);
        closingTime = findViewById(R.id.spinner_closeTime);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.time_array,android.R.layout.simple_spinner_item );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        openingTime.setAdapter(adapter);
        closingTime.setAdapter(adapter);

        schoolName = findViewById(R.id.et_schoolSearch);
        tv_schoolName = findViewById(R.id.tv_schoolName);
        submit=findViewById(R.id.btn_addStudent);
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                submit();
            }
        });
        editTexts = new EditText[]{moduleID, studentName, studentAge};
        latLngs[0] = new LatLng(35,35);



        Intent i = getIntent();
        Bundle extras = i.getExtras();
        moduleID.setText(extras.get("ModuleID").toString());


        Bundle mapViewBundle = null;
        if (savedInstanceState != null)
            mapViewBundle = savedInstanceState.getBundle(MAPVIEW_Bundle_KEY);

        mMapView = findViewById(R.id.mapView1);
        mMapView.onCreate(mapViewBundle);
        mMapView.getMapAsync(this);


        // Initializing Places
        Places.initialize(getApplicationContext(), "AIzaSyCIobTRd-i8DoI-rpuSYpk48vV_v5DELWk");
        PlacesClient placesClient = Places.createClient(this);

        schoolName.setFocusable(false);
        schoolName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Initialize place field
                List<Place.Field> fieldList = Arrays.asList(Place.Field.ADDRESS
                , Place.Field.LAT_LNG, Place.Field.NAME);
                List<String> countries = new ArrayList<>();
                countries.add("LB");
                Intent i = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY,
                        fieldList).setCountries(countries).build(add_student.this);
                startActivityForResult(i, 100);
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        Bundle mapViewBundle = outState.getBundle(MAPVIEW_Bundle_KEY);
        if (mapViewBundle == null){
            mapViewBundle = new Bundle();
            outState.putBundle(MAPVIEW_Bundle_KEY, mapViewBundle);
        }
        mMapView.onSaveInstanceState(mapViewBundle);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {

            if (requestCode == 100 && resultCode == RESULT_OK) {
                // When Success get the place from Intent
                Place place = Autocomplete.getPlaceFromIntent(data);
                Status status = Autocomplete.getStatusFromIntent(data);
                Log.e("SomeTagToFilterTheLogcat", status.toString());
                // set Address on EditText
                schoolName.setText(place.getAddress());
                tv_schoolName.setText(place.getName());
                latLngs[0] = place.getLatLng();
                schoolData.put("Address", place.getAddress());
                schoolData.put("Name", place.getName());
                schoolData.put("Coordinates", place.getLatLng().toString());
                map.addMarker(new MarkerOptions()
                        .position(place.getLatLng())
                        .title(place.getName()));
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(place.getLatLng(), 18f));

            } else {
                Toast.makeText(add_student.this, "Cancelled", Toast.LENGTH_SHORT).show();
                Status status = Autocomplete.getStatusFromIntent(data);
                Log.e("SomeTagToFilterTheLogcat", status.toString());

            }
        }catch (Exception e){
            Log.e("Places Autocomplete", e.getMessage());
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;

    }

    public void submit() {

        for (EditText i: editTexts){
            if (TextUtils.isEmpty(i.getText())){
                Toast.makeText(add_student.this, "Please Fill All The fields!", Toast.LENGTH_LONG).show();
                return;
            }
        }
        if (TextUtils.isEmpty(openingTime.getSelectedItem().toString()) || TextUtils.isEmpty(closingTime.getSelectedItem().toString())){
            Toast.makeText(add_student.this, "Please Fill All The fields!", Toast.LENGTH_LONG).show();
            return;
        }
        schoolData.put("ModuleID", moduleID.getText().toString());
        schoolData.put("StudentName", studentName.getText().toString());
        schoolData.put("StudentAge", studentAge.getText().toString());
        schoolData.put("OpeningTime", openingTime.getSelectedItem().toString());
        schoolData.put("ClosingTime", closingTime.getSelectedItem().toString());



        Intent intent=new Intent();
        intent.putExtra("SchoolData", schoolData );
        setResult(1,intent);
        finish();//finishing activity
    }

    @Override
    protected void onStart() {
        super.onStart();
        mMapView.onStart();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
    }

}