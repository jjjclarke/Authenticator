package com.jjjclarke.authenticator;

import androidx.room.*;

@Entity
public class Account {
    //
    // https://docs.yubico.com/yesdk/users-manual/application-oath/uri-string-format.html
    //

    @PrimaryKey(autoGenerate = true)
    public int uid;

    @ColumnInfo(name = "type")
    public String type;
    @ColumnInfo(name = "label")
    public String label;
    @ColumnInfo(name = "secret")
    public String secret;
    @ColumnInfo(name = "issuer")
    public String issuer;
    @ColumnInfo(name = "algorithm")
    public String algorithm;
    @ColumnInfo(name = "digits")
    public int digits;
    @ColumnInfo(name = "counter")
    public int counter;
    @ColumnInfo(name = "period")
    public int period;
}
