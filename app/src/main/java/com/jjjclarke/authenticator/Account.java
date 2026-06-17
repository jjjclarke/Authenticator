package com.jjjclarke.authenticator;

import androidx.room.*;

@Entity
public class Account {
    @PrimaryKey(autoGenerate = true)
    public int uid;

    @ColumnInfo(name = "type")
    public String type;
    @ColumnInfo(name = "path")
    public String path;
    @ColumnInfo(name = "username")
    public String username;
    @ColumnInfo(name = "service_provider")
    public String serviceProvider;
    @ColumnInfo(name = "blob")
    public String blob;
    @ColumnInfo(name = "algorithm")
    public String algorithm;
    @ColumnInfo(name = "digits")
    public int digits;
    @ColumnInfo(name = "period")
    public int period;
}
