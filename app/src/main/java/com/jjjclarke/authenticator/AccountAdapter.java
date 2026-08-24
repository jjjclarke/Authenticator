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
        try {
            String secret = KeystoreManager.decryptSecret(account.secret);
            totpView.setText(TotpGenerator.generateTotp(secret));
        } catch (Exception e) {
            totpView.setText(R.string.account_error);
        }

        return convertView;
    }
}
