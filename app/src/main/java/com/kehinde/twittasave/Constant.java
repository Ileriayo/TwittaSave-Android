package com.kehinde.twittasave;

import android.Manifest;

/**
 * Created by kehinde on 3/31/17.
 */

public class Constant {

    // Note: Your consumer key and secret should be obfuscated in your source code before shipping.
    public static final String TWITTER_KEY = "[KEY]";
    public static final String TWITTER_SECRET = "[SECRET]";


    // Storage Permissions
    public static final int REQUEST_EXTERNAL_STORAGE = 1;
    public static final String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };


    public static final String FIRSTRUN = "firstrun";
}
