package com.liushu.test.learnofzk;

import com.google.common.collect.Lists;
import com.liushu.test.learnofzk.bean.Node;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;
import java.util.List;

/**
 * Created by liushu on 2019/3/26.
 */
public class ZKUtils {

    public static List<Node> findAllNode() throws KeeperException, InterruptedException {

        ZooKeeper zk = ZKFactory.getInstance();

        return findAllOfPath("/");

    }

    private static List<Node> findAllOfPath(String path) throws KeeperException, InterruptedException {

        List<Node> r = Lists.newArrayList();
        ZooKeeper zk = ZKFactory.getInstance();

        r.add(new Node(path,new String(zk.getData(path,null,null))));

        List<String> children = zk.getChildren(path,null);

        if (children!=null) {
            for (String child : children) {
                String nextPath = path.endsWith("/")?path+child:path+"/"+child;
                r.addAll(findAllOfPath(nextPath));
            }
        }

        return r;
    }

}
