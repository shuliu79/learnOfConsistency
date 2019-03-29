package com.liushu.test.learnofzk;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;

/**
 * Created by liushu on 2019/3/26.
 */
public class ZKFactory {

    private final static String CONNECT_STRING ="localhost:2181";
    private final static int SESSION_TIMEOUT =5000;

    private volatile static ZooKeeper instance;
    private static WatcherWrap watcher;

    public static void setProcessHandle(ProcessHandle processHandle){
        watcher.processHandle = processHandle;
    }

    static{
        watcher = new WatcherWrap();
        try {
            instance = new ZooKeeper(CONNECT_STRING, SESSION_TIMEOUT,watcher);
        } catch (IOException e) {
            throw new RuntimeException("启动失败");
        }
    }

    public static ZooKeeper getInstance(){

        return instance;
    }

    private static class WatcherWrap implements Watcher{

        private ProcessHandle processHandle;

        @Override
        public void process(WatchedEvent event) {
            if (processHandle!=null) {
                processHandle.process(event);
            }
        }
    }

    public static ZooKeeper createOne() throws IOException {
        return new ZooKeeper(CONNECT_STRING,SESSION_TIMEOUT,watcher);
    }

    public static ZooKeeper connectBySessionId(long sessionId,byte[] pwd) throws IOException {
        return new ZooKeeper(CONNECT_STRING,SESSION_TIMEOUT,watcher,sessionId,pwd);
    }
}
