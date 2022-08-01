package lb.edu.aust.ict499.schoolsecured;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.format.Time;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class OpenWebApp extends AppCompatActivity {

    String accessKey = "";
    TextView keyTextView;
    TextView timerTextView;
    private final FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();                  // Firebase Authentication Instance
    private DatabaseReference realTimeDBref;
    FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_web_app);

        accessKey = createRandomKey();
        keyTextView = findViewById(R.id.tv_key);
        timerTextView = findViewById(R.id.tv_timer);
        keyTextView.setText(accessKey.toString());
        realTimeDBref = FirebaseDatabase.getInstance().getReference();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        keyTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                copyToClipboard(accessKey);
            }
        });

        addKey(accessKey);
        startCount();

    }

    private String createRandomKey(){
        int n =6;
        // chose a Character random from this String
        String AlphaNumericString = "0123456789";


        // create StringBuffer size of AlphaNumericString
        StringBuilder sb = new StringBuilder(n);

        for (int i = 0; i < n; i++) {

            // generate a random number between
            // 0 to AlphaNumericString variable length
            int index
                    = (int)(AlphaNumericString.length()
                    * Math.random());

            // add Character one by one in end of sb
            sb.append(AlphaNumericString
                    .charAt(index));
        }

        return sb.toString();
    }

    private void addKey(String key){
        String username = currentUser.getDisplayName();
        HashMap<String,String> keymap = new HashMap<>();
        keymap.put("key",key);
        realTimeDBref.child("Users").child(username).child("key").setValue(keymap);
    }

    private void removeKey(){
        String username = currentUser.getDisplayName();
        HashMap<String,String> keymap = new HashMap<>();
        keymap.put("key","0");
        realTimeDBref.child("Users").child(username).child("key").setValue(keymap);
    }

    private void startCount(){
        Toast.makeText(OpenWebApp.this, "Created Session Key.", Toast.LENGTH_LONG).show();
        long time = TimeUnit.SECONDS.toMillis(30);
        final int[] timerDuration = new int[1];
        timerDuration[0] = 30;
        new CountDownTimer(time, 1000) {
            @Override
            public void onTick(long l) {
                timerDuration[0] -=1;
                timerTextView.setText(String.valueOf(timerDuration[0]));
            }

            @Override
            public void onFinish() {

                removeKey();
                Toast.makeText(OpenWebApp.this, "Closing Session...", Toast.LENGTH_LONG).show();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                finish();
            }
        }.start();
    }

    private void copyToClipboard(String data){
        ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clipData = ClipData.newPlainText("key", data);
        clipboardManager.setPrimaryClip(clipData);
        Toast.makeText(OpenWebApp.this, "Key Copied To Clipboard", Toast.LENGTH_LONG).show();
    }
}