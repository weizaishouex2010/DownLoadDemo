package com.huangzhiwei.downloaddemo.db;

/**
 * Created by huangzhiwei on 16/3/16.
 */

import com.huangzhiwei.downloaddemo.entity.ThreadInfo;

import java.util.List;

/**
 * 数据访问接口
 */
public interface ThreadDAO {
    /**
     * 插入线程信息
     * @param threadInfo
     */
     void insertThread(ThreadInfo threadInfo);

    /**
     * 删除线程
     * @param url
     * @param thread_id
     */
     void deleteThread(String url, int thread_id);

    /**
     * 更新线程
     * @param url
     * @param thread_id
     * @param finished
     */
     void updateThread(String url, int thread_id,int finished);

    /**
     * 查询文件的线程信息
     * @param url
     * @return
     */
     List<ThreadInfo> getThread(String url);

    /**
     * 判断线程是否存在
     * @param url
     * @param thread_id
     * @return
     */
    boolean isExists(String url, int thread_id);


}
