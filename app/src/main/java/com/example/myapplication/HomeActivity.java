package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.io.IOException;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class HomeActivity extends AppCompatActivity {

    private ImageView backgroundImageView;
    private ShapeableImageView profileImageView;
    private TextView usernameTextView;
    private ActivityResultLauncher<String> galleryLauncher;
    private ActivityResultLauncher<String> profileImageLauncher;
    private ActivityResultLauncher<Intent> addDeviceLauncher;
    private final OkHttpClient client = new OkHttpClient();
    private String esp32Ip = "192.168.4.1"; // Default IP for ESP32 in AP mode
    private String esp32Ssid = "ESP32-Access-Point"; // Default SSID
    private DrawerLayout drawerLayout;

    private MaterialCardView deleteConfirmationBar;
    private View pendingDeleteCard = null;

    @Override
    protected void attachBaseContext(Context newBase) {
        SharedPreferences prefs = newBase.getSharedPreferences("Settings", MODE_PRIVATE);
        String lang = prefs.getString("My_Lang", "en");
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        super.attachBaseContext(newBase.createConfigurationContext(config));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);

        toolbar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_theme) {
                toggleTheme();
            } else if (id == R.id.nav_wallpaper) {
                galleryLauncher.launch("image/*");
            } else if (id == R.id.nav_language) {
                showLanguageDialog();
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(HomeActivity.this, SettingsActivity.class));
            } else if (id == R.id.nav_logout) {
                logout();
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        // Initialize views from header
        View headerView = navigationView.getHeaderView(0);
        profileImageView = headerView.findViewById(R.id.nav_header_profile_image);
        usernameTextView = headerView.findViewById(R.id.nav_header_username);

        // Load username from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String username = prefs.getString("username", "Guest User");
        usernameTextView.setText(username);

        backgroundImageView = findViewById(R.id.backgroundImageView);
        MaterialSwitch lamp1 = findViewById(R.id.lamp1);
        MaterialSwitch lamp2 = findViewById(R.id.lamp2);
        MaterialSwitch lamp3 = findViewById(R.id.lamp3);
        MaterialSwitch switchRelay = findViewById(R.id.switch_relay);
        FloatingActionButton fabAddDevice = findViewById(R.id.fab_add_device);
        MaterialButton btnConnectEsp32 = findViewById(R.id.btnConnectEsp32);

        deleteConfirmationBar = findViewById(R.id.delete_confirmation_bar);
        MaterialButton btnUndoDelete = findViewById(R.id.btn_undo_delete);
        MaterialButton btnConfirmDelete = findViewById(R.id.btn_confirm_delete);

        ImageButton btnDeleteLamp1 = findViewById(R.id.btn_delete_lamp1);
        ImageButton btnDeleteLamp2 = findViewById(R.id.btn_delete_lamp2);
        ImageButton btnDeleteLamp3 = findViewById(R.id.btn_delete_lamp3);
        ImageButton btnDeleteRelay = findViewById(R.id.btn_delete_relay);

        MaterialCardView cardLamp1 = findViewById(R.id.card_lamp1);
        MaterialCardView cardLamp2 = findViewById(R.id.card_lamp2);
        MaterialCardView cardLamp3 = findViewById(R.id.card_lamp3);
        MaterialCardView cardRelay = findViewById(R.id.card_relay);

        btnDeleteLamp1.setOnClickListener(v -> showDeleteConfirmation(cardLamp1));
        btnDeleteLamp2.setOnClickListener(v -> showDeleteConfirmation(cardLamp2));
        btnDeleteLamp3.setOnClickListener(v -> showDeleteConfirmation(cardLamp3));
        btnDeleteRelay.setOnClickListener(v -> showDeleteConfirmation(cardRelay));

        checkDeletedDevices();

        btnUndoDelete.setOnClickListener(v -> hideDeleteConfirmation());
        btnConfirmDelete.setOnClickListener(v -> {
            if (pendingDeleteCard != null) {
                String deviceId = getResources().getResourceEntryName(pendingDeleteCard.getId());
                saveDeletedDevice(deviceId);
                pendingDeleteCard.setVisibility(View.GONE);
                Toast.makeText(this, R.string.device_deleted, Toast.LENGTH_SHORT).show();
                hideDeleteConfirmation();
            }
        });

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        try {
                            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        } catch (SecurityException e) {
                            // Fallback if not persistable
                        }
                        backgroundImageView.setImageURI(uri);
                        backgroundImageView.setVisibility(View.VISIBLE);
                        saveImageUri("wallpaper", uri.toString());
                        Toast.makeText(this, "Background updated", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        profileImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        try {
                            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        } catch (SecurityException e) {
                        }
                        profileImageView.setImageURI(uri);
                        saveImageUri("profile_image", uri.toString());
                        Toast.makeText(this, "Profile picture updated", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        profileImageView.setOnClickListener(v -> profileImageLauncher.launch("image/*"));

        addDeviceLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        String name = result.getData().getStringExtra("device_name");
                        Toast.makeText(this, "Device added: " + name, Toast.LENGTH_SHORT).show();
                    }
                }
        );

        fabAddDevice.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, AddDeviceActivity.class);
            addDeviceLauncher.launch(intent);
        });

        btnConnectEsp32.setOnClickListener(v -> showIpDialog());

        loadSavedPreferences();

        lamp1.setOnCheckedChangeListener((buttonView, isChecked) -> {
            showToast(getString(R.string.living_room_lamp), isChecked);
            sendCommandToEsp32("lamp1", isChecked);
        });
        lamp2.setOnCheckedChangeListener((buttonView, isChecked) -> {
            showToast(getString(R.string.bedroom_lamp), isChecked);
            sendCommandToEsp32("lamp2", isChecked);
        });
        lamp3.setOnCheckedChangeListener((buttonView, isChecked) -> {
            showToast(getString(R.string.kitchen_lamp), isChecked);
            sendCommandToEsp32("lamp3", isChecked);
        });
        switchRelay.setOnCheckedChangeListener((buttonView, isChecked) -> {
            showToast(getString(R.string.led_relay), isChecked);
            String command = isChecked ? "LED_ON" : "LED_OFF";
            sendSimpleCommand(command);
        });
    }

    private void sendSimpleCommand(String command) {
        String url = "http://" + esp32Ip + "/" + command;

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                response.close();
            }
        });
    }

    private void showDeleteConfirmation(View cardView) {
        pendingDeleteCard = cardView;
        deleteConfirmationBar.setVisibility(View.VISIBLE);
    }

    private void hideDeleteConfirmation() {
        pendingDeleteCard = null;
        deleteConfirmationBar.setVisibility(View.GONE);
    }

    private void showLanguageDialog() {
        final String[] languages = {"English", "العربية"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.select_language);
        builder.setSingleChoiceItems(languages, -1, (dialog, which) -> {
            if (which == 0) {
                setLocale("en");
            } else {
                setLocale("ar");
            }
            dialog.dismiss();
            recreate();
        });
        builder.show();
    }

    private void setLocale(String lang) {
        SharedPreferences.Editor editor = getSharedPreferences("Settings", MODE_PRIVATE).edit();
        editor.putString("My_Lang", lang);
        editor.apply();
    }

    private void showIpDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final android.widget.EditText ssidInput = new android.widget.EditText(this);
        ssidInput.setHint("ESP32 SSID (e.g. ESP32-AP)");
        ssidInput.setText(esp32Ssid);
        layout.addView(ssidInput);

        final android.widget.EditText ipInput = new android.widget.EditText(this);
        ipInput.setHint("ESP32 IP (Default 192.168.4.1)");
        ipInput.setText(esp32Ip);
        layout.addView(ipInput);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Connect to ESP32 WiFi")
                .setView(layout)
                .setPositiveButton("Connect", (dialog, which) -> {
                    esp32Ssid = ssidInput.getText().toString();
                    esp32Ip = ipInput.getText().toString();
                    connectToEsp32Wifi();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void connectToEsp32Wifi() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            WifiNetworkSpecifier specifier = new WifiNetworkSpecifier.Builder()
                    .setSsid(esp32Ssid)
                    .build();

            NetworkRequest request = new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .setNetworkSpecifier(specifier)
                    .build();

            ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            connectivityManager.requestNetwork(request, new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network network) {
                    super.onAvailable(network);
                    connectivityManager.bindProcessToNetwork(network);
                    runOnUiThread(() -> Toast.makeText(HomeActivity.this, "Connected to " + esp32Ssid, Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onUnavailable() {
                    super.onUnavailable();
                    runOnUiThread(() -> Toast.makeText(HomeActivity.this, "Failed to connect to " + esp32Ssid, Toast.LENGTH_SHORT).show());
                }
            });
        } else {
            Toast.makeText(this, "WiFi direct connection requires Android 10+", Toast.LENGTH_LONG).show();
        }
    }

    private void sendCommandToEsp32(String device, boolean isChecked) {
        String state = isChecked ? "on" : "off";
        String url = "http://" + esp32Ip + "/control?device=" + device + "&state=" + state;

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // Silently fail or log
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                response.close();
            }
        });
    }

    private void showToast(String lampName, boolean isChecked) {
        String state = isChecked ? "ON" : "OFF";
        Toast.makeText(this, lampName + " is " + state, Toast.LENGTH_SHORT).show();
    }

    private void saveDeletedDevice(String deviceId) {
        SharedPreferences prefs = getSharedPreferences("DeletedDevices", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(deviceId, true);
        editor.apply();
    }

    private void checkDeletedDevices() {
        SharedPreferences prefs = getSharedPreferences("DeletedDevices", MODE_PRIVATE);
        if (prefs.getBoolean("card_lamp1", false)) findViewById(R.id.card_lamp1).setVisibility(View.GONE);
        if (prefs.getBoolean("card_lamp2", false)) findViewById(R.id.card_lamp2).setVisibility(View.GONE);
        if (prefs.getBoolean("card_lamp3", false)) findViewById(R.id.card_lamp3).setVisibility(View.GONE);
        if (prefs.getBoolean("card_relay", false)) findViewById(R.id.card_relay).setVisibility(View.GONE);
    }

    private void toggleTheme() {
        SharedPreferences prefs = getSharedPreferences("Settings", MODE_PRIVATE);
        boolean isDark = prefs.getBoolean("IsDark", false);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("IsDark", !isDark);
        editor.apply();
        applyTheme(!isDark);
    }

    private void applyTheme(boolean isDark) {
        if (isDark) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    private void saveImageUri(String key, String uri) {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        prefs.edit().putString(key, uri).apply();
    }

    private void loadSavedPreferences() {
        SharedPreferences settings = getSharedPreferences("Settings", MODE_PRIVATE);
        boolean isDark = settings.getBoolean("IsDark", false);
        // If the system is already dark and we haven't set a preference, respect it
        if (!settings.contains("IsDark")) {
            int currentMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            isDark = (currentMode == Configuration.UI_MODE_NIGHT_YES);
        }
        applyTheme(isDark);

        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String bgUri = prefs.getString("wallpaper", null);
        if (bgUri != null) {
            backgroundImageView.setImageURI(android.net.Uri.parse(bgUri));
            backgroundImageView.setVisibility(View.VISIBLE);
        }
        String pUri = prefs.getString("profile_image", null);
        if (pUri != null) {
            profileImageView.setImageURI(android.net.Uri.parse(pUri));
        }
    }

    private void logout() {
        Intent intent = new Intent(HomeActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
