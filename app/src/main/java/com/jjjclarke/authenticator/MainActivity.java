package com.jjjclarke.authenticator;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

        ImageButton button = findViewById(R.id.imageButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 1. Create an EditText for the input
                final EditText input = new EditText(MainActivity.this);
                input.setHint("Enter Secret Key (e.g. JBSW...)");

                // 2. Build the Dialog
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Update Secret Key")
                        .setMessage("Enter the new Base32 secret key:")
                        .setView(input)
                        .setPositiveButton("Save", (dialog, which) -> {
                            String newSecret = input.getText().toString().trim().toUpperCase();
                            if (!newSecret.isEmpty()) {
                                try {
                                    String secret = KeystoreManager.encryptSecret(newSecret);

                                    Account account = new Account();
                                    account.username = "Temporary";
                                    account.serviceProvider = "Temporary";
                                    account.blob = secret;

                                    new Thread(() -> {
                                        db.accountDao().insert(account);

                                        runOnUiThread(() -> {
                                            Toast.makeText(MainActivity.this, "Secret saved", Toast.LENGTH_SHORT).show();
                                        });
                                    }).start();
                                } catch (Exception e) {
                                    Toast.makeText(MainActivity.this, "Error processing key storage", Toast.LENGTH_SHORT).show();
                                    Log.e("a", Objects.requireNonNull(e.getMessage()));
                                }
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
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
}