package lb.edu.aust.ict499.schoolsecured;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


// This activity is called via startActivityOnResult()
// The User Enters His credentials that get validated
// After Validation is done, the data is sent back to the Login Activity via an Intent.


public class RegisterActivity extends AppCompatActivity {

    private EditText email, password, confirmPassword, firstName, lastName;
    private Button registerButton;
    private TextView passwordError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        email = findViewById(R.id.et_registerUsername);
        password = findViewById(R.id.et_registerPassword);
        confirmPassword = findViewById(R.id.et_registerConfirmPassword);
        firstName = findViewById(R.id.et_registerFirstName);
        lastName = findViewById(R.id.et_registerLastName);
        registerButton = findViewById(R.id.btn_registerSubmit);

        passwordError = findViewById(R.id.tv_passwordError);

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                registerUser(email.getText().toString(), password.getText().toString(),
                        confirmPassword.getText().toString(), firstName.getText().toString()
                        , lastName.getText().toString());
            }
        });
    }

    // Emptying the Fields
    public void emptyFields(){
        email.setText("");
        password.setText("");
        confirmPassword.setText("");
        firstName.setText("");
        lastName.setText("");
    }

    // Data Validation
    private void registerUser(String email_string, String pass, String confirmPass, String firstName, String lastName) {

        // Setting Password Error Message to Invisible
        passwordError.setVisibility(View.INVISIBLE);

        // The Email Regex Pattern
        // Java email validation permitted by RFC 5322
        String email_regex = "^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$";
        //Creating Pattern Object
        Pattern email_pattern = Pattern.compile(email_regex);
        // Creating Matcher Object
        Matcher email_matcher = email_pattern.matcher(email_string);

        // Password Pattern Creation
        // Password Regex
        String password_regex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#&()–[{}]:;',?/*~$^+=<>]).{8,20}$";
        // Password Pattern Object
        Pattern password_pattern = Pattern.compile(password_regex);
        // Password Matcher Object
        Matcher password_matcher = password_pattern.matcher(pass);

        // Checking if any of the fields is empty
        if (TextUtils.isEmpty(email_string) || TextUtils.isEmpty(pass) || TextUtils.isEmpty(confirmPass) ||
                TextUtils.isEmpty(firstName) ||TextUtils.isEmpty(lastName) ){
            Toast.makeText(RegisterActivity.this, "Please Fill all fields", Toast.LENGTH_LONG).show();
            emptyFields();
        // Checking if the Password and password confirmation Matche
        } else if (!pass.matches(confirmPass)){
            Toast.makeText(RegisterActivity.this, "Passwords Do not match", Toast.LENGTH_SHORT).show();
            password.setText("");
            confirmPassword.setText("");
        // Checking if Email matches the pattern
        } else if (!email_matcher.matches()){
            Toast.makeText(RegisterActivity.this, "Invalid Email", Toast.LENGTH_SHORT).show();
            email.setText("");
        }
        // Checking if Password matches the Pattern
        else if (!password_matcher.matches()){
            Toast.makeText(RegisterActivity.this, "Weak Password", Toast.LENGTH_SHORT).show();
            passwordError.setVisibility(View.VISIBLE);
        }
        // if all Validations pass, the data is passed to the Intent and sent back to the login screen
        else  {
            // Send data back to login screen via putExtras.
            Intent i = new Intent();
            i.putExtra("Email", email_string);
            i.putExtra("Password", pass);
            i.putExtra("Firstname", firstName);
            i.putExtra("Lastname", lastName);
            setResult(3, i);
            finish();
        }
    }
}