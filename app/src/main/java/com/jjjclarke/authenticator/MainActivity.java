package com.jjjclarke.authenticator;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private AccountDatabase db;
    private final List<Account> accountList = new ArrayList<>();

    private AccountAdapter adapter;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable updateTotpRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        SharedPreferences sp = getSharedPreferences("prefs", MODE_PRIVATE);
        boolean firstTime = sp.getBoolean("firstStartup", true);
        if (firstTime) {
            showWarningDialog();
        }

        db = AccountDatabase.getInstance(this);
        try {
            KeystoreManager.init();
        } catch (Exception e) {
            Toast.makeText(this, "KeystoreManager Fault", Toast.LENGTH_SHORT).show();
        }

        ListView listView = findViewById(R.id.ListView);
        adapter = new AccountAdapter(this, accountList);
        listView.setAdapter(adapter);

        new Thread(() -> {
            List<Account> accounts = db.accountDao().getAll();
            runOnUiThread(() -> {
                accountList.clear();
                accountList.addAll(accounts);
                adapter.notifyDataSetChanged();
            });
        }).start();

        Button button = findViewById(R.id.btnPlus);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ScanOptions options = new ScanOptions();
                options.setPrompt("Scan a OTPAuth QR code:");
                options.setBeepEnabled(true);
                options.setOrientationLocked(true);
                qrCodeLauncher.launch(options);
            }
        });

        updateTotpRunnable = new Runnable() {
            @Override
            public void run() {
                adapter.notifyDataSetChanged();

                handler.postDelayed(this, 1000);
            }
        };
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(updateTotpRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(updateTotpRunnable);
    }

    private final ActivityResultLauncher<ScanOptions> qrCodeLauncher = registerForActivityResult(
            new ScanContract(),
            result -> {
                if (result.getContents() != null) {
                    String rawUri = result.getContents();

                    try {
                        OtpAuthUri parsed = OtpAuthUri.parse(rawUri);
                        // create account
                        Account account = new Account();

                        account.type = parsed.getType();
                        account.path = parsed.getPath();
                        account.username = parsed.getUsername();
                        account.serviceProvider = parsed.getServiceProvider();
                        account.blob = KeystoreManager.encryptSecret(parsed.getSecret());
                        account.algorithm = parsed.getAlgorithm();
                        account.digits = parsed.getDigits();
                        account.period = parsed.getPeriod();

                        new Thread(() -> {
                            db.accountDao().insert(account);
                            runOnUiThread(() -> {
                                accountList.add(account);
                                adapter.notifyDataSetChanged();
                            });
                        }).start();
                    } catch (InvalidParameterException e) {
                        Toast.makeText(this, "Invalid QR Code", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(this, "Fault", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    private void showWarningDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.warning_title)
                .setMessage(R.string.warning_msg)
                .setPositiveButton(R.string.btn_ok, (dialog, which) -> {
                    SharedPreferences sp = getSharedPreferences("prefs", MODE_PRIVATE);
                    sp.edit().putBoolean("firstStartup", false).apply();
                    dialog.dismiss();
                })
                .setCancelable(false)
                .show();
    }
}