package com.jjjclarke.authenticator;

import android.net.Uri;

public class OtpAuthUri {
    private String type;
    private String path;
    private String username;
    private String secret; // not a blob, it's unencrypted
    private String serviceProvider;
    private String algorithm = "SHA1";
    private int digits = 6;
    private int period = 30;

    public OtpAuthUri() {}

    public static OtpAuthUri parse(String authuri) throws IllegalArgumentException {
        Uri uri = Uri.parse(authuri);
        if (!"otpauth".equals(uri.getScheme())) {
            throw new IllegalArgumentException("Not an otpauth:// URI");
        }

        OtpAuthUri result = new OtpAuthUri();
        result.type = uri.getHost();

        String path = uri.getPath();
        // getPath() returns "/Label" so this is some magic
        result.path = path != null && path.length() > 1 ? Uri.decode(path.substring(1)) : "";

        result.secret = uri.getQueryParameter("secret");
        if (result.secret == null || result.secret.isEmpty()) {
            throw new IllegalArgumentException("Missing secret parameter");
        }

        result.serviceProvider = uri.getQueryParameter("issuer");

        String algParam = uri.getQueryParameter("algorithm");
        if (algParam != null)
            result.algorithm = algParam.toUpperCase();
        String digitsParam = uri.getQueryParameter("digits");
        if (digitsParam != null)
            result.digits = Integer.parseInt(digitsParam);
        String periodParam = uri.getQueryParameter("period");
        if (periodParam != null)
            result.period = Integer.parseInt(periodParam);

        return result;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getServiceProvider() {
        return serviceProvider;
    }

    public void setServiceProvider(String serviceProvider) {
        this.serviceProvider = serviceProvider;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public int getDigits() {
        return digits;
    }

    public void setDigits(int digits) {
        this.digits = digits;
    }

    public int getPeriod() {
        return period;
    }

    public void setPeriod(int period) {
        this.period = period;
    }
}
