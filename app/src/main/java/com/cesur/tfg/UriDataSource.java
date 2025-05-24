package com.cesur.tfg;

import android.content.ContentResolver;
import android.net.Uri;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.activation.DataSource;


public class UriDataSource implements DataSource {
    private static final String TAG = "UriDataSource";
    private final Uri uri;
    private final String mimeType;
    private final ContentResolver contentResolver;

    public UriDataSource(Uri uri, String mimeType, ContentResolver contentResolver) {
        this.uri = uri;
        this.mimeType = mimeType != null ? mimeType : "application/octet-stream"; // Default MIME type
        this.contentResolver = contentResolver;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        Log.d(TAG, "Obteniendo InputStream para URI: " + uri.toString());
        InputStream inputStream = contentResolver.openInputStream(uri);
        if (inputStream == null) {
            Log.e(TAG, "No se pudo abrir InputStream para URI: " + uri.toString());
            throw new IOException("No se pudo obtener InputStream para el URI: " + uri);
        }
        return inputStream;
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        Log.e(TAG, "getOutputStream() no está soportado para UriDataSource");
        throw new IOException("getOutputStream() no está soportado para UriDataSource");
    }

    @Override
    public String getContentType() {
        Log.d(TAG, "Obteniendo ContentType: " + mimeType);
        return mimeType;
    }

    @Override
    public String getName() {
        String path = uri.getLastPathSegment();
        Log.d(TAG, "Obteniendo Name (path): " + (path != null ? path : uri.toString()));
        return path != null ? path : uri.toString();
    }
}
