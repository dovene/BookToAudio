package com.gold.booktoaudio.helper;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;

public class TessOCR {
    private final TessBaseAPI mTess;
    private static String TESS_DIR="/tesseract/";
    private static String TESS_DATA_DIR="tessdata/";
// ressources https://solidgeargroup.com/ocr-on-android
    //https://github.com/tesseract-ocr/tessdata. here are data files to download in asset folder for supported for languages

    public TessOCR(Context context, String language) {
        mTess = new TessBaseAPI();
        //String datapath = context.getFilesDir() + "/tesseract/"; mTess.init(datapath, language);

        String datapath = context.getFilesDir() + TESS_DIR;
        //String language = "eng";
        File dir = new File(getTessDir(context));
        if (!dir.exists())
            dir.mkdirs();

        FileUtils.copyAssets("",getTessDir(context),context,"traineddata");
        mTess.init(datapath, language);
    }

    public static String getTessDir(Context context){
        return context.getFilesDir() + TESS_DIR +TESS_DATA_DIR;
    }

    public String getOCRResult(Bitmap bitmap) {
        mTess.setImage(bitmap);
        return mTess.getUTF8Text();
    }

    public void onDestroy() {
        if (mTess != null) mTess.end();
    }
}
