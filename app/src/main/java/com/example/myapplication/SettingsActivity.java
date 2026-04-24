package com.example.myapplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

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
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = findViewById(R.id.settings_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        findViewById(R.id.btn_change_username).setOnClickListener(v -> showChangeUsernameDialog());
        findViewById(R.id.btn_change_password).setOnClickListener(v -> showChangePasswordDialog());

        findViewById(R.id.about_app_click_area).setOnClickListener(v -> showAboutAppDialog());
        findViewById(R.id.btn_about_us).setOnClickListener(v -> showAboutUsDialog());
        findViewById(R.id.btn_contact_us).setOnClickListener(v -> showContactUsDialog());
    }

    private void showAboutAppDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.about_app)
                .setMessage(R.string.about_app_description)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void showAboutUsDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.about_us)
                .setMessage(R.string.about_us_description)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void showContactUsDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.contact_us)
                .setMessage(R.string.contact_info)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void showChangeUsernameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.change_username);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText currentPasswordInput = new EditText(this);
        currentPasswordInput.setHint(R.string.current_password_hint);
        currentPasswordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(currentPasswordInput);

        final EditText newUsernameInput = new EditText(this);
        newUsernameInput.setHint(R.string.new_username_hint);
        layout.addView(newUsernameInput);

        builder.setView(layout);

        builder.setPositiveButton(R.string.update, (dialog, which) -> {
            String currentPassword = currentPasswordInput.getText().toString();
            String newUsername = newUsernameInput.getText().toString().trim();

            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            String storedPassword = prefs.getString("password", "");

            if (currentPassword.equals(storedPassword)) {
                if (!newUsername.isEmpty()) {
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("username", newUsername);
                    editor.apply();
                    Toast.makeText(this, R.string.username_updated, Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, R.string.incorrect_current_password, Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }

    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.change_password);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText currentPasswordInput = new EditText(this);
        currentPasswordInput.setHint(R.string.current_password_hint);
        currentPasswordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(currentPasswordInput);

        final EditText newPasswordInput = new EditText(this);
        newPasswordInput.setHint(R.string.new_password_hint);
        newPasswordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(newPasswordInput);

        builder.setView(layout);

        builder.setPositiveButton(R.string.update, (dialog, which) -> {
            String currentPassword = currentPasswordInput.getText().toString();
            String newPassword = newPasswordInput.getText().toString();

            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            String storedPassword = prefs.getString("password", "");

            if (currentPassword.equals(storedPassword)) {
                if (!newPassword.isEmpty()) {
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("password", newPassword);
                    editor.apply();
                    Toast.makeText(this, R.string.password_updated, Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, R.string.incorrect_current_password, Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }
}
