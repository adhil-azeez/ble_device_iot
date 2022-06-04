package com.example.bluetooth_device.views;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.bluetooth_device.R;
import com.example.bluetooth_device.ml.Model;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.util.Calendar;

public class CovidResultActivity extends AppCompatActivity {

    private Calendar myCalender = Calendar.getInstance();

    private TextView tvResult;
    private LinearLayout llResult;
    private TextView tvPrediction;
    private TextView tvHeartbeat, tvSpo2, tvTemperature;
    private  TextView tvName, tvDOB, tvGender, tvAge;

    private float temperature;
    private float heartbeat;
    private float spo;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_covid_result);

        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.colorPrimaryDark)));
        getSupportActionBar().setTitle("Result");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        initIds();

        heartbeat = getIntent().getFloatExtra("HEARTBEAT",0.0f);
        spo = getIntent().getFloatExtra("SPO",0.0f);
        temperature = getIntent().getFloatExtra("TEMPERATURE",0.0f);

        tvHeartbeat.setText(String.format(java.util.Locale.US,"%.0f",heartbeat));
        tvTemperature.setText(String.format(java.util.Locale.US,"%.1f",temperature));
        tvSpo2.setText(String.format(java.util.Locale.US,"%.1f",spo));

        predictCovidResult(spo, heartbeat, (float)((temperature*1.8)+32));

//        predictCovidResult(95,92,99);
//        predictCovidResult(97,56,96);
//        predictCovidResult(88,94,98);
//        predictCovidResult(94,100,103);
    }

    private void initIds() {
        tvResult = findViewById(R.id.tv_result);
        llResult = findViewById(R.id.ll_result);
        tvPrediction = findViewById(R.id.tv_prediction);
        tvHeartbeat = findViewById(R.id.tv_heartbeat);
        tvSpo2 = findViewById(R.id.tv_spo2);
        tvTemperature = findViewById(R.id.tv_temperature);

        tvName = findViewById(R.id.tvName);
        tvAge = findViewById(R.id.tvAge);
        tvGender = findViewById(R.id.tvGender);
        tvDOB = findViewById(R.id.tvDob);

        SharedPreferences sp = getApplicationContext().getSharedPreferences("SharedPreferences", Context.MODE_PRIVATE);

        if(sp.contains("NAME")){
            tvName.setText(sp.getString("NAME",""));
        }

        if(sp.contains("GENDER")){
            tvGender.setText(sp.getString("GENDER",""));
        }

        if(sp.contains("DOB")){
            String dob = sp.getString("DOB","");
            tvDOB.setText(dob);
           try{

               if(dob.contains("/")){
                   String[] splits  = dob.split("/");
                   if(splits.length == 3){
                       String age = getAge(Integer.parseInt(splits[2]), Integer.parseInt(splits[1]), Integer.parseInt(splits[0]));
                       tvAge.setText(age);
                   }
               }
           }catch (Exception e){
               e.printStackTrace();
           }


        }


    }

    private void predictCovidResult(float spo,float heartRate,  float temperature) {
        try {
            Model model = Model.newInstance(this);

            Log.d("#############","heartRate =  "+heartRate+", spo = "+spo+", temperature = "+temperature);

//            ByteBuffer byteBuffer = ByteBuffer.allocate(4*3);
//            byteBuffer.putFloat(spo);
//            byteBuffer.putFloat(heartRate);
//            byteBuffer.putFloat(temperature);

            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 3}, DataType.FLOAT32);
//            inputFeature0.loadBuffer(byteBuffer);
            inputFeature0.loadArray(new float[]{spo, heartRate, temperature});

            // Runs model inference and gets result.
            Model.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();
            for(int i=0; i< outputFeature0.getFloatArray().length; i++){
                Log.d("##########", "Array["+i+"]"+outputFeature0.getFloatArray()[i]);
            }

            final float positiveValue = outputFeature0.getFloatArray()[1];
            final float negativeValue = outputFeature0.getFloatArray()[0];

            boolean isPositive = positiveValue>negativeValue;
            Log.d("##########", "IsPositive: "+isPositive);

            if(isPositive){
                setPositiveView(positiveValue);
            }else{
                setNegativeValue(negativeValue);
            }

            // Releases model resources if no longer used.
            model.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setNegativeValue(float negativeValue) {
        llResult.setBackgroundColor(getResources().getColor(R.color.negative));
        tvResult.setText("NEGATIVE");
        setPredictionPercentage(negativeValue);
    }

    private void setPredictionPercentage(float value) {
        value = value*100;
        final  String percentage = String.format(java.util.Locale.US,"%.2f",value);
        tvPrediction.setText("The prediction is "+percentage+"%");
    }

    private void setPositiveView(float positiveValue) {
        llResult.setBackgroundColor(getResources().getColor(R.color.positive));
        tvResult.setText("POSITIVE");
        setPredictionPercentage(positiveValue);
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

    private String getAge(int year, int month, int day){
        Calendar dob = Calendar.getInstance();
        Calendar today = Calendar.getInstance();

        dob.set(year, month, day);

        int age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR);

        if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)){
            age--;
        }

        Integer ageInt = new Integer(age);
        String ageS = ageInt.toString();

        return ageS;
    }
}