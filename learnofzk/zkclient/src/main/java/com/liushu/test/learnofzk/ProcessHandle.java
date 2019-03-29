package com.liushu.test.learnofzk;

import org.apache.zookeeper.WatchedEvent;

/**
 * Created by liushu on 2019/3/26.
 */
public interface ProcessHandle {
    public void process(WatchedEvent event);
}
