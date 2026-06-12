package com.jjjclarke.authenticator;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class MainActivity extends AppCompatActivity {
    Handler handler = new Handler(Looper.getMainLooper());
    Runnable updateTotpRunnable;
    TextView textView;

    private AccountDatabase db;

    private String decryptedSecret;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        textView = findViewById(R.id.textView);

        db = AccountDatabase.getInstance(this);

        try {
            KeystoreManager.init();
        } catch (Exception e) {
            Toast.makeText(this, "KeystoreManager Fault", Toast.LENGTH_SHORT).show();
        }

        ImageButton button = (ImageButton) findViewById(R.id.imageButton);
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
                                    // Re-run through Keystore if you want it protected
                                    String protectedSecret = KeystoreManager.encryptSecret(newSecret);
                                    decryptedSecret = KeystoreManager.decryptSecret(protectedSecret);

                                    Toast.makeText(MainActivity.this, "Secret updated", Toast.LENGTH_SHORT).show();
                                } catch (Exception e) {
                                    Toast.makeText(MainActivity.this, "Error processing key", Toast.LENGTH_SHORT).show();
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
                try {
                    if (decryptedSecret != null) {
                        String code = TotpGenerator.generateTotp(decryptedSecret);
                        textView.setText(code);
                    }
                } catch (NoSuchAlgorithmException e) {
                    Toast.makeText(MainActivity.this, "NoSuchAlgorithmException", Toast.LENGTH_SHORT).show();
                } catch (InvalidKeyException e) {
                    Toast.makeText(MainActivity.this, "InvalidKeyException", Toast.LENGTH_SHORT).show();
                }

                handler.postDelayed(this, 2 * 1000);
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