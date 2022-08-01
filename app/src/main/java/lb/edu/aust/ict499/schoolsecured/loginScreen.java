package lb.edu.aust.ict499.schoolsecured;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AdditionalUserInfo;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

public class loginScreen extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private final static int RC_SIGN_IN = 1;
    private final static int FB_SIGN_IN = 2;
    private final static int EMAIL_PASSWORD_LOGIN = 3;
    private SignInButton googleLoginInButton;
    private LoginButton facebookLogInButton;
    private Button registerButton, normalLoginButton;
    private EditText username, password;
    private CallbackManager mCallBackManager;
    private List<String> permissions = new ArrayList<String>();
    private Executor executor;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;


    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Intent intent = new Intent(loginScreen.this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_screen);

        /////////////////////////////

        executor = ContextCompat.getMainExecutor(this);
        biometricPrompt = new BiometricPrompt(loginScreen.this,
                executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode,
                                              @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Toast.makeText(getApplicationContext(),
                        "Authentication error: " + errString, Toast.LENGTH_SHORT)
                        .show();
                Log.d("Authentication Error", "onAuthenticationError: " + errString);
                FirebaseAuth.getInstance().signOut();
                LoginManager.getInstance().logOut();
            }

            @Override
            public void onAuthenticationSucceeded(
                    @NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);

                startActivity(new Intent(loginScreen.this, MainActivity.class));
                finish();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(getApplicationContext(), "Authentication failed",
                        Toast.LENGTH_SHORT)
                        .show();
                FirebaseAuth.getInstance().signOut();
                LoginManager.getInstance().logOut();
            }
        });

        try {
            promptInfo = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Biometric login for my app")
                    .setSubtitle("Log in using your biometric credential")
                    .setAllowedAuthenticators(BiometricManager.Authenticators.DEVICE_CREDENTIAL | BiometricManager.Authenticators.BIOMETRIC_STRONG)
                    //.setNegativeButtonText("Cancel")
                    .build();
        } catch (Exception e) {
            promptInfo = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Biometric login for my app")
                    .setSubtitle("Log in using your biometric credential")
                    //.setAllowedAuthenticators(BiometricManager.Authenticators.DEVICE_CREDENTIAL | BiometricManager.Authenticators.BIOMETRIC_STRONG)
                    .setNegativeButtonText("Cancel")
                    .build();
        }
        /////////////////////////////////////

        mAuth = FirebaseAuth.getInstance();
        facebookLogInButton = findViewById(R.id.btn_facebookLogin);
        mCallBackManager = CallbackManager.Factory.create();
        permissions.add("email");
        facebookLogInButton.setReadPermissions(Arrays.asList("email"));

        facebookLogInButton.registerCallback(mCallBackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                handleFacebookAccessToken(loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {
                Toast.makeText(loginScreen.this, "Cancelled", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(@NonNull FacebookException e) {
                Toast.makeText(loginScreen.this, e.toString(), Toast.LENGTH_LONG).show();
            }
        });

        createRequest();

        googleLoginInButton = findViewById(R.id.btn_googleLogin);
        registerButton = findViewById(R.id.btn_register);
        normalLoginButton = findViewById(R.id.btn_login);
        username = findViewById(R.id.et_username);
        password = findViewById(R.id.et_password);


        googleLoginInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signIn();
            }
        });

        normalLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email_string = username.getText().toString();
                String pass = password.getText().toString();
                loginUser(email_string, pass);
            }
        });

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(loginScreen.this, RegisterActivity.class);
                startActivityForResult(i, EMAIL_PASSWORD_LOGIN);
            }
        });

    }

    private void createRequest() {
        try {
            // Configure Google Sign In
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id_1))
                    .requestEmail()
                    .build();

            // Build a GoogleSignInClient with the options specified by gso.
            mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Calling google SignIn Intent
    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                // ...
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        } else if (requestCode == FB_SIGN_IN) {
            mCallBackManager.onActivityResult(requestCode, resultCode, data);
        } else if (requestCode == EMAIL_PASSWORD_LOGIN) {
            try {
                String email = data.getStringExtra("Email");
                String password = data.getStringExtra("Password");
                mAuth.createUserWithEmailAndPassword(email, password);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        try {
            AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
            mAuth.signInWithCredential(credential)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {

                                biometricPrompt.authenticate(promptInfo);

                            } else {
                                Toast.makeText(loginScreen.this, "Sorry auth failed.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(loginScreen.this, "Google Login Failed", Toast.LENGTH_LONG).show();
        }
    }

    private void handleFacebookAccessToken(AccessToken token) {
        try {
            AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
            mAuth.signInWithCredential(credential)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {

                                biometricPrompt.authenticate(promptInfo);

                            } else {
                                // If sign in fails, display a message to the User.
                                Toast.makeText(loginScreen.this, task.getResult().toString(),
                                        Toast.LENGTH_SHORT).show();

                            }
                        }
                    });
        } catch (Exception e) {
            Toast.makeText(loginScreen.this, "Facebook Login Failed", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void loginUser(String email_string, String pass) {
        try {
            if (TextUtils.isEmpty(email_string) || TextUtils.isEmpty(pass)) {
                Toast.makeText(loginScreen.this, "Please Enter All fields to Continue", Toast.LENGTH_LONG).show();
                username.setText("");
                password.setText("");
            } else {
                mAuth.signInWithEmailAndPassword(email_string, pass).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            biometricPrompt.authenticate(promptInfo);
                        } else {
                            Toast.makeText(loginScreen.this, "Authentication Failed", Toast.LENGTH_LONG).show();
                            username.setText("");
                            password.setText("");
                        }
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(loginScreen.this, "Error Login In", Toast.LENGTH_SHORT).show();
        }
    }

}


