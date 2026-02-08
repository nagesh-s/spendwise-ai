package com.spendwise.ai.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Helper class for managing expense photos.
 * Photos are stored in app's private storage for security.
 */
public class PhotoHelper {

    private static final String TAG = "PhotoHelper";
    private static final String PHOTOS_DIR = "expense_photos";
    private static final int MAX_IMAGE_SIZE = 1024; // Max dimension in pixels

    /**
     * Get or create the photos directory in app's private storage.
     */
    public static File getPhotosDir(Context context) {
        File photosDir = new File(context.getFilesDir(), PHOTOS_DIR);
        if (!photosDir.exists()) {
            photosDir.mkdirs();
        }
        return photosDir;
    }

    /**
     * Create a unique file for a new photo.
     */
    public static File createPhotoFile(Context context) throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String fileName = "EXPENSE_" + timeStamp + ".jpg";
        return new File(getPhotosDir(context), fileName);
    }

    /**
     * Get a FileProvider URI for a photo file (needed for camera intent).
     */
    public static Uri getPhotoUri(Context context, File photoFile) {
        return FileProvider.getUriForFile(
                context,
                context.getPackageName() + ".fileprovider",
                photoFile);
    }

    /**
     * Save an image from a content URI to app's private storage.
     * Compresses and resizes to save space.
     */
    public static String saveImageFromUri(Context context, Uri sourceUri) {
        try {
            File outputFile = createPhotoFile(context);

            InputStream inputStream = context.getContentResolver().openInputStream(sourceUri);
            if (inputStream == null) {
                return null;
            }

            // Decode with sampling to reduce memory usage
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();

            // Calculate sample size
            int sampleSize = 1;
            while (options.outWidth / sampleSize > MAX_IMAGE_SIZE ||
                    options.outHeight / sampleSize > MAX_IMAGE_SIZE) {
                sampleSize *= 2;
            }

            // Decode with sample size
            options.inJustDecodeBounds = false;
            options.inSampleSize = sampleSize;
            inputStream = context.getContentResolver().openInputStream(sourceUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();

            if (bitmap == null) {
                return null;
            }

            // Save compressed
            FileOutputStream fos = new FileOutputStream(outputFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos);
            fos.close();
            bitmap.recycle();

            Log.d(TAG, "Saved photo: " + outputFile.getName());
            return outputFile.getName();

        } catch (IOException e) {
            Log.e(TAG, "Error saving image", e);
            return null;
        }
    }

    /**
     * Get the full path to a photo given its filename.
     */
    public static File getPhotoFile(Context context, String fileName) {
        return new File(getPhotosDir(context), fileName);
    }

    /**
     * Delete a photo file.
     */
    public static boolean deletePhoto(Context context, String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return false;
        }
        File photoFile = getPhotoFile(context, fileName);
        return photoFile.exists() && photoFile.delete();
    }

    /**
     * Check if a photo exists.
     */
    public static boolean photoExists(Context context, String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return false;
        }
        return getPhotoFile(context, fileName).exists();
    }
}
