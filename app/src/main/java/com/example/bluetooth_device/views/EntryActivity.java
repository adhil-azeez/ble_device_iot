package com.example.bluetooth_device.views;

import androidx.appcompat.app.AppCompatActivity;

import android.app.DatePickerDialog;
import android.content.Intent;
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

public class EntryActivity extends AppCompatActivity {

    final Calendar myCalendar= Calendar.getInstance();
    private EditText etName;
    private EditText etDOB;
    private EditText etHeartbeat;
    private  EditText etSPO;
    private  EditText etTemperature;
    private RadioGroup rbGender;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entry);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.colorPrimaryDark)));
        getSupportActionBar().setTitle("Profile");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
     initViews();
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

    private void initViews() {
        etName = findViewById(R.id.etName);
        etDOB = findViewById(R.id.etDOB);
        rbGender = findViewById(R.id.rgGender);
        etHeartbeat = findViewById(R.id.etHeartBeat);
        etSPO = findViewById(R.id.etSPO);
        etTemperature = findViewById(R.id.etTemp);

        findViewById(R.id.btnSave).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onCheckResult(view);
            }
        });

        DatePickerDialog.OnDateSetListener date =new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int day) {
                etDOB.setText(day+"/"+month+"/"+year);
            }
        };

        etDOB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DatePickerDialog(EntryActivity.this,date, myCalendar.get(Calendar.YEAR), myCalendar.get(Calendar.MONTH), myCalendar.get(Calendar.DAY_OF_MONTH)).show();
            }
        });
    }

    public void onCheckResult(View view) {
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
            return;
        }

         float heartbeat;
         float temperature;
         float spo;


        try{
            heartbeat = Float.parseFloat(etHeartbeat.getText().toString());
        }catch (Exception e){
            Toast.makeText(this, "Please enter a valid heartbeat information", Toast.LENGTH_SHORT).show();
            return;
        }


        try{
            spo = Float.parseFloat(etSPO.getText().toString());
        }catch (Exception e){
            Toast.makeText(this, "Please enter a valid SPO information", Toast.LENGTH_SHORT).show();
            return;
        }


        try{
            temperature = Float.parseFloat(etTemperature.getText().toString());
        }catch (Exception e){
            Toast.makeText(this, "Please enter a valid temperature information", Toast.LENGTH_SHORT).show();
            return;
        }


        Intent intent = new Intent(EntryActivity.this, CovidResultActivity.class);
        intent.putExtra("HEARTBEAT", heartbeat);
        intent.putExtra("SPO", spo);
        intent.putExtra("TEMPERATURE", temperature);
        intent.putExtra("NAME", name);
        intent.putExtra("DOB", dob);
        intent.putExtra("GENDER",gender);
        intent.putExtra("HAS_USER_INFO", true);
        startActivity(intent);


    }
}