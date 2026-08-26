package com.jjjclarke.authenticator;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private AccountDatabase db;
    private List<Account> accountList = new ArrayList<>();
    private AccountAdapter adapter;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable updateTotpRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        setSupportActionBar(findViewById(R.id.toolbar));
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = AccountDatabase.getInstance(this);

        try {
            KeystoreManager.init();
        } catch (Exception e) {
            Toast.makeText(this, "KeystoreManager Fault", Toast.LENGTH_SHORT).show();
        }

        ListView listView = findViewById(R.id.ListView);
        adapter = new AccountAdapter(this, accountList);
        listView.setAdapter(adapter);
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            Account accountToDelete = adapter.getItem(position);
            if (accountToDelete == null) {
                return true;
            }

            new AlertDialog.Builder(this)
                    .setTitle(R.string.delete_account_title)
                    .setMessage(R.string.delete_account_message)
                    .setPositiveButton(R.string.btn_ok, (dialog, which) ->
                            new Thread(() -> {
                                db.accountDao().delete(accountToDelete);
                                runOnUiThread(() -> {
                                    accountList.remove(accountToDelete);
                                    adapter.notifyDataSetChanged();
                                    Toast.makeText(this, R.string.account_deleted, Toast.LENGTH_SHORT).show();
                                });
                            }).start()
                    )
                    .setNegativeButton(R.string.btn_cancel, null)
                    .show();
            return true;
        });

        new Thread(() -> {
            List<Account> accounts = db.accountDao().getAll();
            runOnUiThread(() -> {
                accountList.clear();
                accountList.addAll(accounts);
                adapter.notifyDataSetChanged();
            });
        }).start();

        FloatingActionButton button = findViewById(R.id.floatingActionButton);
        button.setOnClickListener(v -> {
            ScanOptions options = new ScanOptions();
            options.setPrompt("Scan a OTPAuth QR code:");
            options.setBeepEnabled(true);
            options.setOrientationLocked(true);
            qrCodeLauncher.launch(options);
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
                        account.label = parsed.getLabel();
                        account.secret = KeystoreManager.encryptSecret(parsed.getSecret());
                        account.issuer = parsed.getIssuer();
                        account.algorithm = parsed.getAlgorithm();
                        account.digits = parsed.getDigits();
                        account.counter = parsed.getCounter();
                        account.period = parsed.getPeriod();

                        new Thread(() -> {
                            long accountId = db.accountDao().insert(account);
                            account.uid = (int) accountId;
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
}