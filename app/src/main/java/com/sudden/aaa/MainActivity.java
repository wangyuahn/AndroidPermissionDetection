package com.sudden.aaa;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int SERVER_CHECK_INTERVAL = 10000; // 10 seconds
    private static final int TIMEOUT_DURATION = 10000; // 10 seconds
    private TextView logTextView;
    private Handler handler;
    private Runnable serverCheckRunnable;
    private Runnable timeoutRunnable;
    private ExecutorService executorService;
    private ActivityResultLauncher<String[]> requestPermissionLauncher;
    private List<String> permissionsToRequest;
    private boolean permissionsRequested = false; // 添加一个标志位

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        logTextView = findViewById(R.id.logTextView);
        Button agreeButton = findViewById(R.id.agreeButton);
        Button disagreeButton = findViewById(R.id.disagreeButton);
        handler = new Handler(Looper.getMainLooper());
        executorService = Executors.newSingleThreadExecutor();

        // 初始化权限请求
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    Log.d(TAG, "Permission request result: " + result.toString()); // 输出所有权限的结果
                    boolean allGranted = true;
                    List<String> deniedPermissions = new ArrayList<>();
                    for (Map.Entry<String, Boolean> entry : result.entrySet()) {
                        String permission = entry.getKey();
                        Boolean granted = entry.getValue();
                        Log.d(TAG, "Permission: " + permission + ", Granted: " + granted); // 输出每个权限的授予状态
                        if (!granted) {
                            allGranted = false;
                            deniedPermissions.add(permission);
                        }
                    }
                    if (allGranted) {
                        logMessage("所有权限已授予");
                        startServerCheck();
                    } else {
                        StringBuilder messageBuilder = new StringBuilder("部分权限未授予，无法继续：");
                        for (String permission : deniedPermissions) {
                            messageBuilder.append("\n- ").append(permission);
                        }
                        showPermissionDeniedDialog(messageBuilder.toString());
                    }
                }
        );

        permissionsToRequest = new ArrayList<>();
        permissionsToRequest.add(Manifest.permission.CAMERA);
        permissionsToRequest.add(Manifest.permission.RECORD_AUDIO);
        permissionsToRequest.add(Manifest.permission.READ_PHONE_STATE);
        permissionsToRequest.add(Manifest.permission.CALL_PHONE);
        permissionsToRequest.add(Manifest.permission.READ_CALL_LOG);
        permissionsToRequest.add(Manifest.permission.WRITE_CALL_LOG);
        permissionsToRequest.add(Manifest.permission.READ_CONTACTS);
        permissionsToRequest.add(Manifest.permission.WRITE_CONTACTS);
        permissionsToRequest.add(Manifest.permission.INTERNET);
        permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissionsToRequest.add(Manifest.permission.SEND_SMS);
        permissionsToRequest.add(Manifest.permission.READ_SMS);
        permissionsToRequest.add(Manifest.permission.RECEIVE_SMS);
        permissionsToRequest.add(Manifest.permission.BODY_SENSORS);
        permissionsToRequest.add(Manifest.permission.ACCESS_WIFI_STATE);
        permissionsToRequest.add(Manifest.permission.ACCESS_NETWORK_STATE);
        permissionsToRequest.add(Manifest.permission.RECEIVE_BOOT_COMPLETED);
        permissionsToRequest.add(Manifest.permission.VIBRATE);
        permissionsToRequest.add(Manifest.permission.FOREGROUND_SERVICE);

        //permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        //permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES);
        permissionsToRequest.add(Manifest.permission.READ_MEDIA_VIDEO);
        permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO);
        // 添加其他需要的权限

        agreeButton.setOnClickListener(v -> {
            logMessage("用户点击了同意");
            if (!permissionsRequested) {
                requestAllPermissions();
                permissionsRequested = true; // 设置标志位为 true
            }
            handler.removeCallbacks(timeoutRunnable);
        });

        disagreeButton.setOnClickListener(v -> {
            logMessage("用户点击了不同意");
            finish();
        });

        // 超时退出
        timeoutRunnable = () -> {
            logMessage("超时未选择，应用退出");
            finish();
        };
        handler.postDelayed(timeoutRunnable, TIMEOUT_DURATION);
    }

    private void requestAllPermissions() {
        List<String> permissionsToRequestNow = new ArrayList<>();
        for (String permission : permissionsToRequest) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequestNow.add(permission);
            }
        }

        if (!permissionsToRequestNow.isEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequestNow.toArray(new String[0]));
        } else {
            logMessage("所有权限已授予");
            startServerCheck();
        }
    }

    private void startServerCheck() {
        serverCheckRunnable = new Runnable() {
            @Override
            public void run() {
                checkServerConnection();
                handler.postDelayed(this, SERVER_CHECK_INTERVAL);
            }
        };
        handler.post(serverCheckRunnable);
    }

    private void checkServerConnection() {
        executorService.execute(() -> {
            boolean isConnected = isServerReachable("www.baidu.com", 80, 2000); // Replace with your server address and port
            String message = isConnected ? "服务器已连接" : "服务器未连接";
            logMessage(message);
        });
    }

    private boolean isServerReachable(String host, int port, int timeout) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeout);
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Error checking server connection", e);
            return false;
        }
    }

    private void logMessage(final String message) {
        runOnUiThread(() -> {
            logTextView.append(message + "\n");
            Log.d(TAG, message);
        });
    }

    private void showPermissionDeniedDialog(String message) {
        runOnUiThread(() -> {
            new AlertDialog.Builder(this)
                    .setTitle("权限请求")
                    .setMessage(message)
                    .setPositiveButton("退出", (dialog, which) -> {
                        finish();
                    })
                    .setCancelable(false)
                    .show();
        });
    }
}