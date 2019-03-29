package com.liushu.test.learnofzk.curator;

import org.apache.curator.test.TestingCluster;
import org.apache.curator.test.TestingServer;

import java.io.File;

/**
 * Created by liushu on 2019/3/28.
 */
public class CuratorUtils {

    public static volatile TestingServer singleServer;
    public static volatile TestingCluster testCluster;

    public static synchronized TestingServer getTestSingleServer() throws Exception {

        if (singleServer == null){
            singleServer = new TestingServer(2181,new File("./zktest_singleserver"));
            singleServer.start();
        }

        return singleServer;
    }

    public static synchronized TestingCluster getTestCluster() throws Exception {

        return getTestCluster(3);
    }

    public static synchronized TestingCluster getTestCluster(int num) throws Exception {

        if (testCluster == null){

            testCluster = new TestingCluster(num);
            testCluster.start();
        }

        return testCluster;
    }
}
