package com.jjjclarke.authenticator;

import androidx.room.*;

@Entity
public class Account {
    @PrimaryKey(autoGenerate = true)
    public int uid;

    @ColumnInfo(name = "username")
    public String username;
    @ColumnInfo(name = "service_provider")
    public String serviceProvider;
    @ColumnInfo(name = "blob")
    public String blob;
}
