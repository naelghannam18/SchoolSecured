package lb.edu.aust.ict499.schoolsecured;

import static java.lang.Thread.sleep;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;
import android.view.WindowManager;

public class SplashScreen extends AppCompatActivity {

    private final static int SPLASH_SCREEN_TIMEOUT = 2000;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent i = new Intent(SplashScreen.this, loginScreen.class);
                startActivity(i);
                finish();
            }
        }, SPLASH_SCREEN_TIMEOUT);

        setContentView(R.layout.activity_main);

    }
}