package com.sveder.cardboardpassthrough;

import android.app.Activity;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.File;

public class Translate {
    public static final String DOWNLOAD_BASE = "http://tesseract-ocr.googlecode.com/files/";
    private static final String TAG = "TRANSLATE";
    public static String translatedText;

    public static void translateImage(byte[] data, final Activity activity){
        translatedText = "";
        File dir = getStorageDirectory(activity);

        MainActivity main = (MainActivity) activity;
        main.toggleProgessBar(true);

        OcrAsyncTask ocrAsyncTask = new OcrAsyncTask(activity, data, dir.toString(), new OcrAsyncTask.Callback() {
            @Override
            public void onComplete(Object o, Error error) {
                if (error != null) {
                    Toast.makeText(activity, "OCR Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("OcrAsyncTask", error.getMessage());
                    return;
                }
                String excerpt = (String) o;
                String stripped = excerpt.replaceAll("[^ A-Za-zùûüÿàâæçéèêëïîôœ]", "");
                float gibberishRatio = ((float) stripped.length()) / excerpt.length();
                if(gibberishRatio < 0.75){
                    MainActivity main = (MainActivity) activity;
                    main.toggleProgessBar(false);
                    main.showText("Text not recognized. Try again.");
                }else {
                    TranslateAsyncTask translateAsyncTask = new TranslateAsyncTask(excerpt, "FR", "EN", new TranslateAsyncTask.Callback() {
                        @Override
                        public void onComplete(Object o, Error error) {
                            if (error != null) {
                                Toast.makeText(activity, "Translate Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                                Log.e("TranslateAsyncTask", error.getMessage());
                                return;
                            }
                            DataWrapper translation = (DataWrapper) o;
                            translatedText = translation.data.translations.get(0).translatedText;
//                        Toast.makeText(activity, translatedText, Toast.LENGTH_SHORT).show();
                            MainActivity main = (MainActivity) activity;
                            main.toggleProgessBar(false);
                            main.showText(translatedText);
                            Log.e("TranslateAsyncTask", translatedText);
                        }
                    });
                    translateAsyncTask.execute();
                }
            }
        });
        ocrAsyncTask.execute();
    }



    public static File getStorageDirectory(Activity activity) {
        //Log.d(TAG, "getStorageDirectory(): API level is " + Integer.valueOf(android.os.Build.VERSION.SDK_INT));

        String state = null;
        try {
            state = Environment.getExternalStorageState();
        } catch (RuntimeException e) {
            Log.e(TAG, "Is the SD card visible?", e);
        }

        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {

            // We can read and write the media
            //    	if (Integer.valueOf(android.os.Build.VERSION.SDK_INT) > 7) {
            // For Android 2.2 and above

            try {
                return activity.getExternalFilesDir(Environment.MEDIA_MOUNTED);
            } catch (NullPointerException e) {
                // We get an error here if the SD card is visible, but full
                Log.e(TAG, "External storage is unavailable");
            }


        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            // We can only read the media
            Log.e(TAG, "External storage is read-only");
        } else {
            // Something else is wrong. It may be one of many other states, but all we need
            // to know is we can neither read nor write
            Log.e(TAG, "External storage is unavailable");
        }
        return null;
    }

    public static void initOcrIfNecessary(Activity activity, String langCode){

        boolean doNewInit = false;
        File storageDirectory = getStorageDirectory(activity);
        if(storageDirectory != null){
            File data = new File(storageDirectory.toString()
                    + File.separator + "tessdata"
                    + File.separator + langCode + ".traineddata");
            doNewInit = !data.exists() || data.isDirectory();
        }
        if (doNewInit) {
            new OcrInitAsyncTask(activity, langCode).execute(storageDirectory.toString());
        }
    }
}
