package com.jjjclarke.authenticator;

import android.annotation.SuppressLint;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;


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

    public static OtpAuthUri parse(String authuri) throws Exception {
        OtpAuthUri output = new OtpAuthUri();

        if (authuri == null || !authuri.startsWith("otpauth://"))
            throw new IllegalArgumentException("Invalid otpauth:// URI");

        String rest = authuri.substring("otpauth://".length());

        int slashAfterType = rest.indexOf('/');
        if (slashAfterType == -1)
            throw new IllegalArgumentException("Missing path in URI");
        String sType = rest.substring(0, slashAfterType).toLowerCase();
        if (sType.equals("totp"))
            output.type = "TOTP";
        else if (sType.equals("hotp"))
            output.type = "HOTP";
        else
            throw new IllegalArgumentException("Unknown OTP type");

        String afterType = rest.substring(slashAfterType + 1);
        int queryStart = afterType.indexOf('?');
        String rawPath = queryStart != -1 ? afterType.substring(0, queryStart) : afterType;
        String query = queryStart != -1 ? afterType.substring(queryStart + 1) : "";

        String path = URLDecoder.decode(rawPath, "UTF-8");
        output.path = path;

        String service = "";
        String username;

        int colinIdx = path.indexOf(':');
        if (colinIdx != -1) {
            service = path.substring(0, colinIdx).trim();
            username = path.substring(colinIdx + 1).trim();
        } else
            username = path.trim();

        String sk = "";
        String algo = "SHA1"; // default
        int digits = 6; // default
        int period = 30; // default

        if (!query.isEmpty()) {
            for (String param : query.split("&")) {
                int eq = param.indexOf('=');
                if (eq == -1)
                    continue;
                String key = param.substring(0, eq).toLowerCase();
                String value = java.net.URLDecoder.decode(
                        param.substring(eq + 1), "UTF-8");
                        switch (key) {
                            case "secret":
                                sk = value;
                                break;
                            case "algorithm":
                                algo = value;
                                break;
                            case "issuer":
                                if (service.isEmpty())
                                    service = value;
                                break;
                            case "digits":
                                digits = Integer.parseInt(value);
                                break;
                            case "period":
                                period = Integer.parseInt(value);
                                break;
                        }
            }
        }

        if (sk.isEmpty())
            throw new IllegalArgumentException("Missing required parameter");

        output.serviceProvider = service;
        output.username = username;
        output.secret = sk;
        output.algorithm = algo;
        output.digits = digits;
        output.period = period;

        return output;
    }

    public String getType() {
        return type;
    }

    public String getPath() {
        return path;
    }

    public String getUsername() {
        return username;
    }

    public String getSecret() {
        return secret;
    }

    public String getServiceProvider() {
        return serviceProvider;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public int getDigits() {
        return digits;
    }

    public int getPeriod() {
        return period;
    }
}
