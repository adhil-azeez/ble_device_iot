package com.example.bluetooth_device.views;

import androidx.appcompat.app.AppCompatActivity;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.example.bluetooth_device.R;

import java.util.Calendar;

public class ProfileActivity extends AppCompatActivity {
    final Calendar myCalendar= Calendar.getInstance();
    private EditText etName;
    private EditText etDOB;
    private RadioGroup rbGender;

    private  boolean isFromSplash = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.colorPrimaryDark)));
        getSupportActionBar().setTitle("Profile");
        isFromSplash = getIntent().getBooleanExtra("isFromSplash", false);

        if(!isFromSplash) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        initViews();
    }

    private void initViews() {
        etName = findViewById(R.id.etName);
        etDOB = findViewById(R.id.etDOB);
        rbGender = findViewById(R.id.rgGender);

        DatePickerDialog.OnDateSetListener date =new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int day) {
                etDOB.setText(day+"/"+month+"/"+year);
            }
        };

        etDOB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DatePickerDialog(ProfileActivity.this,date, myCalendar.get(Calendar.YEAR), myCalendar.get(Calendar.MONTH), myCalendar.get(Calendar.DAY_OF_MONTH)).show();
            }
        });

        SharedPreferences sp = getApplicationContext().getSharedPreferences("SharedPreferences", Context.MODE_PRIVATE);
        if(sp.contains("NAME")){
            etName.setText(sp.getString("NAME", ""));
        }

        if(sp.contains("DOB")){
            etDOB.setText(sp.getString("DOB", ""));
        }

        if(sp.contains("GENDER")){
            switch(sp.getString("GENDER", "")){
                case "Male":
                    rbGender.check(R.id.rbMale);
                    break;

                case "Female":
                    rbGender.check(R.id.rbFemale);
                    break;
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    public void onSaveClick(View view) {
         String name = etName.getText().toString().trim();
         String dob = etDOB.getText().toString().trim();
         int selectedId = rbGender.getCheckedRadioButtonId();
        RadioButton selectedGenderButton = findViewById(selectedId);
        String gender = selectedGenderButton.getText().toString();

        if(name.isEmpty()){
            Toast.makeText(this, "Please enter the name", Toast.LENGTH_SHORT).show();
            return;
        }

        if(dob.isEmpty()){
            Toast.makeText(this, "Please select the Date of Birth", Toast.LENGTH_SHORT).show();
        }

        SharedPreferences sp =  getApplicationContext().getSharedPreferences("SharedPreferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("NAME", name);
        editor.putString("DOB", dob);
        editor.putString("GENDER", gender);
       boolean isSaved =  editor.commit();
       if(isSaved){
           Toast.makeText(this, "Successfully saved your details", Toast.LENGTH_SHORT).show();

           if(isFromSplash){
               Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
               intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
               startActivity(intent);
           }
       }else{
           Toast.makeText(this, "Unable to save the details. Please try again", Toast.LENGTH_SHORT).show();
       }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }
}