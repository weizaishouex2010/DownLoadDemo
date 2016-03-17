package com.huangzhiwei.downloaddemo.service;

import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;

import com.huangzhiwei.downloaddemo.entity.FileInfo;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by huangzhiwei on 16/3/14.
 */
public class DownloadService extends Service {

    public static final String DOWNLOAD_PATH =
            Environment.getExternalStorageDirectory().getAbsolutePath() +"/downloads/";
    public static final String ACTION_START = "ACTION_START";
    public static final String ACTION_STOP = "ACTION_STOP";
    public static final String ACTION_UPDATE = "ACTION_UPDATE";
    private DownloadTask downloadTask;
    public static final int MSG_INIT = 0;
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if(ACTION_START.equals(intent.getAction()))
        {
            FileInfo fileInfo = (FileInfo) intent.getSerializableExtra("fileinfo");
            Log.i("test","start"+fileInfo.toString());
            //启动初始化县城
            new InitThread(fileInfo).start();
        }else if(ACTION_STOP.equals(intent.getAction()))
        {
            FileInfo fileInfo = (FileInfo) intent.getSerializableExtra("fileinfo");
            Log.i("test","stop"+fileInfo.toString());
            if(downloadTask!=null)
            {
                downloadTask.isPause = true;
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what)
            {
                case MSG_INIT:
                    FileInfo fileInfo = (FileInfo) msg.obj;
                    Log.i("test","Init:"+fileInfo);
                    //启动下载任务
                    downloadTask = new DownloadTask(DownloadService.this,fileInfo);
                    downloadTask.download();
                    break;
            }
        }
    };

    //初始化子线程
    class InitThread extends Thread
    {
        private FileInfo mFileInfo = null;

        public InitThread(FileInfo mFileInfo) {
            this.mFileInfo = mFileInfo;
        }

        public void run()
        {
            HttpURLConnection conn = null;
            RandomAccessFile raf = null;
            //连接网络文件
            try {
                URL url = new URL(mFileInfo.getUrl());
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(3000);
                conn.setRequestMethod("GET");//下载用get
                int length = -1;
                if(conn.getResponseCode() == 200)
                {
                    //获取文件长度
                    length = conn.getContentLength();
                }
                if(length<0)
                {
                    return;
                }
                File dir = new File(DOWNLOAD_PATH);
                if(!dir.exists())
                {
                    dir.mkdir();
                }
                //在本地创建文件
                File file = new File(dir,mFileInfo.getFileName());
                raf = new RandomAccessFile(file,"rwd");
                raf.setLength(length);
                mFileInfo.setLength(length);
                mHandler.obtainMessage(MSG_INIT,mFileInfo).sendToTarget();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                conn.disconnect();
                try {
                    raf.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
