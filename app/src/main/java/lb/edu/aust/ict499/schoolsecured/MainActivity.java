package lb.edu.aust.ict499.schoolsecured;


import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.facebook.login.LoginManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


public class MainActivity extends AppCompatActivity implements RecyclerAdapter.onStudentNameClickListener {
    // Initializing global Variables


    private DrawerLayout drawerLayout;                                               // Side Navbar
    private ImageView imageView;                                                     // Image To click to open NavBar
    private NavigationView navView;                                                  // View For Items Inside NavBar
    private final int student_name_result_code = 1;                                  // Result code for onResult()
    private FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();                  // Firebase Authentication Instance
    private FirebaseFirestore db = FirebaseFirestore.getInstance();                  // Firestore Database instance
    private RecyclerView recyclerView;                                               // Recycler View Instance
    private RecyclerAdapter.onStudentNameClickListener listener;
    private SwipeRefreshLayout swipeRefreshLayout;                                   // SwipeRefresh Layout to refresh recyclerView
    private ArrayList<String> data = new ArrayList<>();
    private HashMap<String, String> studentNamesAndModuleIds = new HashMap<>();
    DatabaseReference realTimeDBref;
    // Verifying if a user is logged in
    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String Email = currentUser.getEmail();
        if (currentUser == null || TextUtils.isEmpty(Email)) {

            // If no user is logged in we sign out of all accounts and return to Login Screen
            FirebaseAuth.getInstance().signOut();
            LoginManager.getInstance().logOut();
            startActivity(new Intent(MainActivity.this, loginScreen.class));
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        // Assigning the Utilities to their Respective Layouts
        drawerLayout = findViewById(R.id.drawerLayout);
        imageView = findViewById(R.id.iv_toogleNavBar);
        navView = findViewById(R.id.navigation_view);
        recyclerView = findViewById(R.id.rv_modules);
        swipeRefreshLayout = findViewById(R.id.swipeRefresh);
        firebaseAuth = FirebaseAuth.getInstance();

        realTimeDBref = FirebaseDatabase.getInstance().getReference();



        // Setting Onclick for Image
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        // Setting OnClicks for Navigation bar Items
        navView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    // If the Add device button is pressed the Intent for Barcode Scanner is started
                    // After we scan the barcode the result is retrieved by OnResult() method
                    case R.id.item1:
                        IntentIntegrator intentIntegrator = new IntentIntegrator(MainActivity.this);
                        intentIntegrator.setPrompt("Scan a barcode or QR Code");
                        intentIntegrator.setOrientationLocked(false);
                        intentIntegrator.initiateScan();
                        break;
                    case R.id.item2:
                        Intent aboutIntent = new Intent(MainActivity.this, AboutPage.class);
                        startActivity(aboutIntent);
                        break;
                    case R.id.item3:
                        startActivity(new Intent(MainActivity.this, OpenWebApp.class));
                        break;
                    case R.id.item4:
                        logout();
                        break;
                }
                return true;
            }
        });

        // Getting the Student Data Assigned to currently logged in user
        db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        try {
            assert currentUser != null;
            if (currentUser.getDisplayName() != null) {
                realTimeDBref.child("Users").child(currentUser.getDisplayName()).get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DataSnapshot> task) {
                        if (!task.isSuccessful()){
                            Log.d("Get Student Data", "onComplete Task Not Successful: " + task.getException());
                        }
                        else{
                            HashMap<String, Object> resultFromdb = (HashMap<String, Object>) task.getResult().getValue();
                            if (resultFromdb != null){
                                System.out.println(resultFromdb);

                                Set<String> keySet = resultFromdb.keySet();
                                ArrayList<String> studentKeys = new ArrayList<>();

                                for (String i : keySet) {
                                    if (i.toLowerCase().contains("student".toLowerCase())) {
                                        studentKeys.add(i);
                                    }
                                }
                                HashMap<String, String> studentNamesAndModuleIds = new HashMap<>();
                                for (String key : studentKeys) {
                                    HashMap<String, String> studentData = (HashMap<String, String>) resultFromdb.get(key);
                                    String studentName = studentData.get("StudentName");
                                    String moduleID = studentData.get("ModuleID");
                                    studentNamesAndModuleIds.put(studentName, moduleID);
                                }
                             RecyclerAdapter adapter = new RecyclerAdapter(studentNamesAndModuleIds, new RecyclerAdapter.onStudentNameClickListener() {
                                 @Override
                                 public void onClick(int position) {
                                     Set<String> keyset = studentNamesAndModuleIds.keySet();
                                     ArrayList<String> listofkeys = new ArrayList<String>(keyset);
                                     Collection<String> values = studentNamesAndModuleIds.values();
                                     ArrayList<String> listofvalues = new ArrayList<String>(values);

                                     Intent i = new Intent(MainActivity.this, TrackStudentActivity.class);
                                     i.putExtra("Student Name", listofkeys.get(position));
                                     i.putExtra("Student Module ID", listofvalues.get(position));
                                     i.putExtra("Username",FirebaseAuth.getInstance().getCurrentUser().getDisplayName());
                                     startActivity(i);
                                 }
                             });
                                recyclerView.setAdapter(adapter);
                                recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
                            }
                        }
                    }
                });

            }
        } catch (Exception e) {
            e.printStackTrace();
        }


        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                swipeRefreshLayout.setRefreshing(false);
                refreshRecyclerView();
            }
        });

    }



    private void refreshRecyclerView() {
        db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        try {
            assert currentUser != null;
            if (currentUser.getDisplayName() != null) {
                realTimeDBref.child("Users").child(currentUser.getDisplayName()).get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DataSnapshot> task) {
                        if (!task.isSuccessful()){
                            Log.d("Get Student Data", "onComplete Task Not Successful: " + task.getException());
                        }
                        else{
                            HashMap<String, Object> resultFromdb = (HashMap<String, Object>) task.getResult().getValue();
                            if (resultFromdb != null){
                                System.out.println(resultFromdb);

                                Set<String> keySet = resultFromdb.keySet();
                                ArrayList<String> studentKeys = new ArrayList<>();

                                for (String i : keySet) {
                                    if (i.toLowerCase().contains("student".toLowerCase())) {
                                        studentKeys.add(i);
                                    }
                                }
                                HashMap<String, String> studentNamesAndModuleIds = new HashMap<>();
                                for (String key : studentKeys) {
                                    HashMap<String, String> studentData = (HashMap<String, String>) resultFromdb.get(key);
                                    String studentName = studentData.get("StudentName");
                                    String moduleID = studentData.get("ModuleID");
                                    studentNamesAndModuleIds.put(studentName, moduleID);
                                }
                                RecyclerAdapter adapter = new RecyclerAdapter(studentNamesAndModuleIds, new RecyclerAdapter.onStudentNameClickListener() {
                                    @Override
                                    public void onClick(int position) {
                                        onClick(position);
                                    }
                                });
                                recyclerView.setAdapter(adapter);
                                recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
                            }
                        }
                    }
                });

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void logout() {
        FirebaseAuth.getInstance().signOut();
        LoginManager.getInstance().logOut();
        Intent i = new Intent(MainActivity.this, loginScreen.class);
        startActivity(i);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == student_name_result_code) {
            assert data != null;
            try {
                HashMap<String, String> schoolData = (HashMap<String, String>)data.getSerializableExtra("SchoolData");
                String moduleID = schoolData.get("ModuleID");
                String openingTime = schoolData.get("OpeningTime");
                String closingTime = schoolData.get("ClosingTime");
                String studentName = schoolData.get("StudentName");
                String studentAge = schoolData.get("StudentAge");
                String schoolAddress = schoolData.get("Address");
                String schoolCoordinates = schoolData.get("Coordinates");
                String schoolName = schoolData.get("Name");

                HashMap<String, Object> studentNameData = new HashMap<>();
                HashMap<String, Object> schoolInfoData = new HashMap<>();

                studentNameData.put("StudentName", studentName);
                studentNameData.put("StudentAge", studentAge);
                studentNameData.put("ModuleID", moduleID);

                schoolInfoData.put("SchoolName", schoolName);
                schoolInfoData.put("SchoolAddress", schoolAddress);
                schoolInfoData.put("Coordinates", schoolCoordinates);
                schoolInfoData.put("OpeningTime", openingTime);
                schoolInfoData.put("ClosingTime", closingTime);

                studentNameData.put("SchoolData", schoolInfoData);


                String parentName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
                String parentEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();

                HashMap<String, Object> fullData = new HashMap<>();
                fullData.put("Parent Name", parentName);
                fullData.put("Parent Email", parentEmail);
                fullData.put("key", "0");
                fullData.put("Student: " + studentName, studentNameData);


                realTimeDBref.child("Users").child(parentName).updateChildren(fullData);



            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        IntentResult intentResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        // if the intentResult is null then
        // toast a message as "cancelled"
        if (intentResult != null) {
            if (intentResult.getContents() == null) {
                Toast.makeText(getBaseContext(), "Cancelled", Toast.LENGTH_SHORT).show();
            } else {
                // if the intentResult is not null we'll set
                // the content and format of scan message

                Intent i = new Intent(MainActivity.this, add_student.class);
                System.out.println(intentResult.getContents());
                i.putExtra("ModuleID", intentResult.getContents());
                startActivityForResult(i, student_name_result_code);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }


    @Override
    public void onClick(int position) {
        Set<String> keyset = studentNamesAndModuleIds.keySet();
        ArrayList<String> listofkeys = new ArrayList<String>(keyset);
        Collection<String> values = studentNamesAndModuleIds.values();
        ArrayList<String> listofvalues = new ArrayList<String>(values);

        Intent i = new Intent(MainActivity.this, TrackStudentActivity.class);
        i.putExtra("Student Name", listofkeys.get(position));
        i.putExtra("Student Module ID", listofvalues.get(position));
        i.putExtra("Username",FirebaseAuth.getInstance().getCurrentUser().getDisplayName());
        startActivity(i);
    }




}
