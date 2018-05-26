package com.yczhang.videoreplayer;

import android.net.Uri;

import java.io.FileInputStream;

public class FileItem {
    private Uri uri;
    private String title;

    public FileItem(Uri uri, String title) {
        this.uri = uri;
        this.title = title;
    }

    public Uri getUri() {
        return uri;
    }

    public String getTitle() {
        return title;
    }

    public static FileItem fromString(String s) throws Exception {
        int i = s.indexOf(',');
        if(i < 0) throw new Exception("Invalid uri item string");
        return new FileItem(Uri.parse(s.substring(0,i)),s.substring(i+1));
    }

    @Override
    public String toString() {
        return uri+","+title;
    }
}
