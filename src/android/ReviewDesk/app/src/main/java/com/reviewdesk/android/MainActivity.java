package com.reviewdesk.android;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    
    private TextView serviceStatusText;
    private TextView connectionStatusText;
    private EditText serverUrlInput;
    private Button enableAccessibilityButton;
    private TextView logTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        serviceStatusText = findViewById(R.id.serviceStatusText);
        connectionStatusText = findViewById(R.id.connectionStatusText);
        serverUrlInput = findViewById(R.id.serverUrlInput);
        enableAccessibilityButton = findViewById(R.id.enableAccessibilityButton);
        logTextView = findViewById(R.id.logTextView);

        // Set default server URL
        serverUrlInput.setText(getString(R.string.default_server_url));

        // Enable Accessibility button
        enableAccessibilityButton.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
        });

        updateServiceStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateServiceStatus();
        
        // Update server URL if service is running
        String url = serverUrlInput.getText().toString().trim();
        if (!url.isEmpty() && MyAccessibilityService.getInstance() != null) {
            MyAccessibilityService.getInstance().setServerUrl(url);
        }
    }

    private void updateServiceStatus() {
        MyAccessibilityService service = MyAccessibilityService.getInstance();
        
        if (service != null) {
            serviceStatusText.setText(String.format(getString(R.string.service_status), "ENABLED"));
            connectionStatusText.setText(String.format(getString(R.string.connection_status), "CONNECTED"));
            addLog("✓ Accessibility Service is running");
            addLog("✓ SignalR connection established");
        } else {
            serviceStatusText.setText(String.format(getString(R.string.service_status), "DISABLED"));
            connectionStatusText.setText(String.format(getString(R.string.connection_status), "NOT CONNECTED"));
            addLog("✗ Accessibility Service not enabled");
            addLog("→ Please enable it in Settings");
        }
    }

    private void addLog(String message) {
        String currentLog = logTextView.getText().toString();
        if (currentLog.equals("Log messages will appear here...")) {
            currentLog = "";
        }
        logTextView.setText(message + "\n" + currentLog);
    }
}
