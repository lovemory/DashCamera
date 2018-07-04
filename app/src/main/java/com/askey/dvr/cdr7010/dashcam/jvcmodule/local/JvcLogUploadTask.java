package com.askey.dvr.cdr7010.dashcam.jvcmodule.local;

import android.os.AsyncTask;

import com.askey.dvr.cdr7010.dashcam.application.DashCamApplication;
import com.askey.dvr.cdr7010.dashcam.jvcmodule.jvckenwood.MainApp;
import com.askey.dvr.cdr7010.dashcam.util.Logg;
import com.askey.dvr.cdr7010.dashcam.util.ZipManager;

import java.io.File;
import java.util.ArrayList;


/***
 * Company: Chengdu Skysoft Info&Tech Co., Ltd.
 * Copyright ©2014-2018 Chengdu Skysoft Info&Tech Co., Ltd.
 * Created by Mark on 2018/7/3.

 * @since:JDK1.6
 * @version:1.0
 * @see
 ***/
public class JvcLogUploadTask  extends AsyncTask<String, Integer, Boolean> {
    private static final String LOG_TAG = "JvcLogUploadTask";

    @Override
    protected Boolean doInBackground(String... strings) {
        String logAppRootPath = "/data/log/";
        File logAppRootFile = new File(logAppRootPath);
        if(logAppRootFile.exists()){
            //zip
            ArrayList<String> zipFilePathList = new ArrayList<>();
            for(File jvcAppFile : logAppRootFile.listFiles()){
                Logg.d(LOG_TAG, "doInBackground: zip jvc app > " + jvcAppFile.getName());
                for(File logFile : jvcAppFile.listFiles()){
                    Logg.d(LOG_TAG, "doInBackground: zip logFile > " + logFile.getPath());
                    zipFilePathList.add(logFile.getPath());
                }
            }
            String outputFilePath = DashCamApplication.getAppContext().getFilesDir().getAbsolutePath() + "/log.zip";
            ZipManager.zip(zipFilePathList, outputFilePath);
            //upload
            MainApp.getInstance().logUpload(outputFilePath);
        }else {
            Logg.w(LOG_TAG, "doInBackground: root app log file not exit.");
        }

        return true;
    }



}