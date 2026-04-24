package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import androidx.activity.result.ActivityResultLauncher;

import java.util.Locale;

public class AddDeviceActivity extends AppCompatActivity {

    private TextInputEditText deviceName;
    private TextInputEditText deviceDescription;

    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(new ScanContract(),
            result -> {
                if(result.getContents() == null) {
                    Toast.makeText(AddDeviceActivity.this, R.string.scan_cancelled, Toast.LENGTH_LONG).show();
                } else {
                    String name = deviceName.getText().toString();
                    String description = deviceDescription.getText().toString();
                    
                    Intent data = new Intent();
                    data.putExtra("device_name", name);
                    data.putExtra("device_description", description);
                    data.putExtra("qr_content", result.getContents());
                    setResult(RESULT_OK, data);
                    
                    Toast.makeText(AddDeviceActivity.this, R.string.device_added, Toast.LENGTH_LONG).show();
                    finish();
                }
            });

    @Override
    protected void attachBaseContext(Context newBase) {
        SharedPreferences prefs = newBase.getSharedPreferences("Settings", MODE_PRIVATE);
        String lang = prefs.getString("My_Lang", "");
        if (lang.isEmpty()) {
            super.attachBaseContext(newBase);
        } else {
            Locale locale = new Locale(lang);
            Locale.setDefault(locale);
            Configuration config = newBase.getResources().getConfiguration();
            config.setLocale(locale);
            Context context = newBase.createConfigurationContext(config);
            super.attachBaseContext(context);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_device);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        deviceName = findViewById(R.id.deviceName);
        deviceDescription = findViewById(R.id.deviceDescription);
        MaterialButton btnAddAndScan = findViewById(R.id.btnAddAndScan);

        btnAddAndScan.setOnClickListener(v -> {
            if (deviceName.getText().toString().trim().isEmpty()) {
                deviceName.setError(getString(R.string.name_required));
                return;
            }
            
            ScanOptions options = new ScanOptions();
            options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
            options.setPrompt(getString(R.string.scan_qr_prompt));
            options.setCameraId(0);
            options.setBeepEnabled(false);
            options.setBarcodeImageEnabled(true);
            barcodeLauncher.launch(options);
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
