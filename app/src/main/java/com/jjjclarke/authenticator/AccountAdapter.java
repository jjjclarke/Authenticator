package com.jjjclarke.authenticator;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public class AccountAdapter extends ArrayAdapter<Account> {
    public AccountAdapter(Context context, List<Account> accounts) {
        super(context, 0, accounts);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        Account account = getItem(position);

        if (convertView == null)
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_account, parent, false);

        ((TextView) convertView.findViewById(R.id.textServiceProvider)).setText(account.issuer);
        ((TextView) convertView.findViewById(R.id.textUsername)).setText(account.label);

        TextView totpView = convertView.findViewById(R.id.textTotp);
        TextView timerView = convertView.findViewById(R.id.textTimer);

        try {
            String secret = KeystoreManager.decryptSecret(account.secret);
            totpView.setText(TotpGenerator.generateTotp(secret));

            Runnable existingTimerRunnable = (Runnable) timerView.getTag();
            if (existingTimerRunnable != null) {
                timerView.removeCallbacks(existingTimerRunnable);
            }

            Runnable timerRunnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        int secondsRemaining = TotpGenerator.getSecondsUntilExpiry(account.period);
                        timerView.setText(String.valueOf(secondsRemaining));
                        if (secondsRemaining <= 0) {
                            totpView.setText(TotpGenerator.generateTotp(secret));
                        }
                    } catch (Exception e) {
                        totpView.setText(R.string.account_error);
                        timerView.setText("-");
                    }
                    timerView.postDelayed(this, 1000L);
                }
            };
            timerView.setTag(timerRunnable);
            timerView.post(timerRunnable);
        } catch (Exception e) {
            totpView.setText(R.string.account_error);
            timerView.setText("-");
        }

        return convertView;
    }
}
