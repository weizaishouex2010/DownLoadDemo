package com.huangzhiwei.downloaddemo.service;

/**
 * Created by huangzhiwei on 16/3/16.
 */

import android.content.Context;
import android.content.Intent;

import com.huangzhiwei.downloaddemo.db.ThreadDAO;
import com.huangzhiwei.downloaddemo.db.ThreadDAOImpl;
import com.huangzhiwei.downloaddemo.entity.FileInfo;
import com.huangzhiwei.downloaddemo.entity.ThreadInfo;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 * 下载任务类
 */
public class DownloadTask {
    private Context mContext = null;
    private FileInfo mFileInfo = null;
    private ThreadDAO mDao = null;
    private int mFinished = 0;
    public boolean isPause = false;
    public DownloadTask(Context mContext, FileInfo mFileInfo) {
        this.mContext = mContext;
        this.mFileInfo = mFileInfo;
        this.mDao = new ThreadDAOImpl(mContext);
    }


    public void  download()
    {
        //读取数据库线程信息
        List<ThreadInfo> threadInfos = mDao.getThread(mFileInfo.getUrl());

        ThreadInfo threadInfo = null;
        if(threadInfos.size()==0)
        {
            //初始化线程信息
            threadInfo = new ThreadInfo(0,mFileInfo.getUrl(),0,mFileInfo.getLength(),0);

        }
        else
        {
            threadInfo = threadInfos.get(0);

        }
        //创建子线程  下载
        new DownloadThread(threadInfo).start();
    }
    class DownloadThread extends Thread
    {
        private ThreadInfo mThreadInfo = null;

        public DownloadThread(ThreadInfo mThreadInfo) {
            this.mThreadInfo = mThreadInfo;
        }

        public void run()
        {
            //项数据库插入信息
            if(!mDao.isExists(mThreadInfo.getUrl(),mThreadInfo.getId()))
            {
                mDao.insertThread(mThreadInfo);

            }
            //设置线程的下载位置
            HttpURLConnection conn = null;
            RandomAccessFile raf=null;
            InputStream input=null;
            try {
                URL url = new URL(mThreadInfo.getUrl());
                try {
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(5000);
                    conn.setRequestMethod("GET");
                    int start = mThreadInfo.getStart() + mThreadInfo.getFinished();
                    conn.setRequestProperty("Range","bytes="+start+"-"+mThreadInfo.getEnd());
                    //设置文件写入位置
                    File file = new File(DownloadService.DOWNLOAD_PATH,mFileInfo.getFileName());
                    raf = new RandomAccessFile(file,"rwd");
                    raf.seek(start);

                    Intent intent = new Intent(DownloadService.ACTION_UPDATE);
                    //开始下载
                    mFinished += mThreadInfo.getFinished();
                    if(conn.getResponseCode() == 206) //部分下载
                    {
                        //读取数据
                        input = conn.getInputStream();
                        byte[] buffer = new byte[1024*4];
                        int len = -1;
                        long time = System.currentTimeMillis();
                        while ((len = input.read(buffer))!=-1)
                        {
                            //写入文件
                            raf.write(buffer,0,len);
                            //下载暂停时进入保存下载进度
                            mFinished += len;
                            if(System.currentTimeMillis()-time>500)
                            {
                                time = System.currentTimeMillis();
                                intent.putExtra("finished",mFinished*100/mFileInfo.getLength());
                                mContext.sendBroadcast(intent);
                            }
                            //下载进度发送广播给activity
                            if(isPause)
                            {
                                mDao.updateThread(mThreadInfo.getUrl(),mThreadInfo.getId(),mFinished);
                                return;
                            }

                        }
                        //删除线程
                        mDao.deleteThread(mThreadInfo.getUrl(),mThreadInfo.getId());

                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }

            } catch (MalformedURLException e) {
                e.printStackTrace();
            }finally {

                try {
                    conn.disconnect();
                    raf.close();
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }



        }
    }



}
