package com.liushu.test.learnofzk.curator

import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.CuratorFrameworkFactory
import org.apache.curator.framework.recipes.atomic.AtomicValue
import org.apache.curator.framework.recipes.atomic.DistributedAtomicInteger
import org.apache.curator.framework.recipes.cache.NodeCache
import org.apache.curator.framework.recipes.cache.NodeCacheListener
import org.apache.curator.framework.recipes.leader.LeaderSelector
import org.apache.curator.framework.recipes.leader.LeaderSelectorListenerAdapter
import org.apache.curator.framework.recipes.locks.InterProcessLock
import org.apache.curator.framework.recipes.locks.InterProcessMutex
import org.apache.curator.retry.ExponentialBackoffRetry
import org.apache.curator.test.TestingCluster
import org.apache.curator.test.TestingServer
import org.apache.curator.test.TestingZooKeeperServer
import org.apache.zookeeper.CreateMode
import org.apache.zookeeper.WatchedEvent
import org.apache.zookeeper.Watcher
import org.apache.zookeeper.data.Stat
import org.junit.Test

/**
 * Created by liushu on 2019/3/28.
 */
class CuratorTest {

    private CuratorFramework createSingleClient() {
        TestingServer server = CuratorUtils.getTestSingleServer()
        CuratorFramework client = CuratorFrameworkFactory.builder()
                .connectString(server.getConnectString())
                .sessionTimeoutMs(5000)
                .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                .build();
        client.start()
        return client;
    }

    @Test
    public void testSingleServer() {

        CuratorFramework client = createSingleClient();
        println client.getChildren().forPath("/")

        Thread.sleep(10000000);
    }

    @Test
    public void testNameSpace() {
        TestingServer server = CuratorUtils.getTestSingleServer()
        CuratorFramework client = CuratorFrameworkFactory.builder()
                .connectString(server.getConnectString())
                .sessionTimeoutMs(5000)
                .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                .namespace("testNameSpace")
                .build();
        client.start()
        //这里默认使用testNameSpace作为命名空间，看不到其他节点
        println client.getChildren().forPath("/")

        //能够看到 zookeeper和testNameSpace节点
        client = createSingleClient()
        println client.getChildren().forPath("/")
    }

    @Test
    public void testCreateAndGet() {

        String path = "/testCuratorCreate"
        CuratorFramework client = createSingleClient();

        client.delete().forPath(path)
        client.create().withMode(CreateMode.PERSISTENT)
                .forPath(path, "test".getBytes())

        println new String(client.getData().forPath(path))

        Stat stat = new Stat()
        client.getData().storingStatIn(stat).usingWatcher(new Watcher() {
            @Override
            void process(WatchedEvent event) {
                println "in watcher"
            }
        }).forPath(path)

        client.setData().withVersion(stat.getVersion()).forPath(path, "test1".getBytes())

        //watcher只监听一次
        client.setData().withVersion(stat.getVersion() + 1).forPath(path, "test2".getBytes())

    }

    @Test
    public void testNodeCache() {

        String path = "/testCuratorCreate"
        CuratorFramework client = createSingleClient();
        client.delete().forPath(path)
        client.create().withMode(CreateMode.PERSISTENT)
                .forPath(path, "test".getBytes())

        NodeCache nodeCache = new NodeCache(client, path)
        nodeCache.start()
        nodeCache.getListenable().addListener(new NodeCacheListener() {
            @Override
            void nodeChanged() throws Exception {
                println nodeCache.getCurrentData().getStat()
                println new String(nodeCache.getCurrentData().getData())
            }
        })

        //这里能够看到最早的创建事件

        Thread.sleep(1000)
        client.setData().forPath(path, "test1".getBytes())
        client.setData().forPath(path, "test2".getBytes())

        //连续的事件可能看不到中间过程，test1无法感知
        Thread.sleep(1000)

        //连续的事件可能看不到中间过程，test3可以被看到
        client.setData().forPath(path, "test3".getBytes())
        Thread.sleep(10000)

        //Notice：NodeCache底层并非使用watcher实现，而是使用了更底层的原理。需要确认是否会带来额外的线程开销。（可以和watcher混用）
        //相比之下，公司的lion对所有需要监听的key统一通过一个http长连通道来传递信息，性能的开销要少很多
    }

    @Test
    public void testCluster() {

        TestingCluster testingCluster = CuratorUtils.getTestCluster()
        println testingCluster.getConnectString()
        CuratorFramework client = CuratorFrameworkFactory.builder()
                .connectString(testingCluster.getConnectString())
                .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                .connectionTimeoutMs(1000)
                .build()

        client.start()

        println client.getChildren().forPath("/")

        println client.create().forPath("/testCluster")
        println client.getData().forPath("/testCluster")

        //找到leader
        TestingZooKeeperServer leader = null;
        for (TestingZooKeeperServer server : testingCluster.getServers()) {
            println server.getInstanceSpec().getServerId() + "——" + server.getQuorumPeer().getServerState()
            if (server.getQuorumPeer().getServerState().equals("leading")) {
                leader = server
            }
        }
        println leader.getInstanceSpec().getServerId()

        leader.kill()

        Thread.sleep(10)
        for (TestingZooKeeperServer server : testingCluster.getServers()) {
            println server.getInstanceSpec().getServerId() + "——" + server.getQuorumPeer().getServerState()
        }

        Thread.sleep(5000)
        for (TestingZooKeeperServer server : testingCluster.getServers()) {
            println server.getInstanceSpec().getServerId() + "——" + server.getQuorumPeer().getServerState()
            if (server.getQuorumPeer().getServerState().equals("leading")) {
                leader = server
            }
        }
        println leader.getInstanceSpec().getServerId()

    }

    @Test
    public void testMasterSelect() {

        TestingCluster testingCluster = CuratorUtils.getTestCluster()
        CuratorFramework client = CuratorFrameworkFactory.builder()
                .connectString(testingCluster.getConnectString())
                .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                .connectionTimeoutMs(1000)
                .build()

        client.start()

        CuratorFramework client2 = CuratorFrameworkFactory.builder()
                .connectString(testingCluster.getConnectString())
                .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                .connectionTimeoutMs(1000)
                .build()

        client2.start()


        LeaderSelector leaderSelector0 = new LeaderSelector(client, "/leaderSelect", new LeaderSelectorListenerAdapter() {
            @Override
            void takeLeadership(CuratorFramework client0) throws Exception {

                println "client1 被选举成为Leader"
                Thread.sleep(3000)
            }
        })

        LeaderSelector leaderSelector1 = new LeaderSelector(client, "/leaderSelect", new LeaderSelectorListenerAdapter() {
            @Override
            void takeLeadership(CuratorFramework client0) throws Exception {

                println "client2 被选举成为Leader"
                Thread.sleep(3000)
            }
        })

        leaderSelector0.start()
        leaderSelector1.start()

        Thread.sleep(10000)

        try {
            leaderSelector0.start()
            leaderSelector1.start()
        } catch (Exception e) {
            //内部实现是竞争锁
            //只能竞选一次
            //有一个问题，如果某个leader竞选成功过后如何通知其他人放弃竞选
        }

    }

    @Test
    public void testLock() {

        TestingCluster testingCluster = CuratorUtils.getTestCluster(5)
        CuratorFramework client = CuratorFrameworkFactory.builder()
                .connectString(testingCluster.getConnectString())
                .retryPolicy(new ExponentialBackoffRetry(1000, 10))
                .connectionTimeoutMs(1000)
                .build()

        client.start()

        CuratorFramework client2 = CuratorFrameworkFactory.builder()
                .connectString(testingCluster.getConnectString())
                .retryPolicy(new ExponentialBackoffRetry(1000, 10))
                .connectionTimeoutMs(1000)
                .build()

        client2.start()

        InterProcessLock lock = new InterProcessMutex(client, "/lockPath")
        InterProcessLock lock2 = new InterProcessMutex(client2, "/lockPath")

        new Thread(new Runnable() {
            @Override
            void run() {
                while (true) {
                    lock.acquire()
                    println System.currentTimeMillis()
                    println "lock1"
                    Thread.sleep(1000)
                    lock.release()
                }
            }
        }).start()

        new Thread(new Runnable() {
            @Override
            void run() {
                while (true) {
                    lock2.acquire()
                    println System.currentTimeMillis()
                    println "lock2"
                    Thread.sleep(1000)
                    lock2.release()
                }
            }
        }).start()

        Thread.sleep(6000)

        //kill掉3台机器
        println "kill some"
        testingCluster.getServers().get(0).kill()
        testingCluster.getServers().get(1).kill()
//        testingCluster.getServers().get(2).kill()
        //如果kill超过一半，则集群会变得不可用


        //分布式锁使用了临时节点，这样client如果crash过后锁会自动被释放
        lock.acquire()
        println "lock111"
        client.close()

        while (true){
            Thread.sleep(1000);
        }

    }

    @Test
    public void testAtomicInt(){

        TestingCluster testingCluster = CuratorUtils.getTestCluster(5)
        CuratorFramework client = CuratorFrameworkFactory.builder()
                .connectString(testingCluster.getConnectString())
                .retryPolicy(new ExponentialBackoffRetry(1000, 10))
                .connectionTimeoutMs(1000)
                .build()

        client.start()

        CuratorFramework client2 = CuratorFrameworkFactory.builder()
                .connectString(testingCluster.getConnectString())
                .retryPolicy(new ExponentialBackoffRetry(1000, 10))
                .connectionTimeoutMs(1000)
                .build()

        client2.start()

        DistributedAtomicInteger atomicInteger = new DistributedAtomicInteger(client, "/AutomicInteger",new ExponentialBackoffRetry(1000,3))
        DistributedAtomicInteger atomicInteger2 = new DistributedAtomicInteger(client2, "/AutomicInteger",new ExponentialBackoffRetry(1000,3))

        new Thread(new Runnable() {
            @Override
            void run() {
                while (true) {
                    //注意，可能会失败，重试次数由RetryPolicy决定
                    AtomicValue<Integer> r = atomicInteger.add(1)
                    if (r.succeeded()){
                        println r.postValue();
                    }
                    Thread.sleep(1)
                }
            }
        }).start()

        new Thread(new Runnable() {
            @Override
            void run() {
                while (true) {
                    AtomicValue<Integer> r = atomicInteger2.add(1)
                    if (r.succeeded()){
                        println r.postValue();
                    }
                    Thread.sleep(1)
                }
            }
        }).start()

        Thread.sleep(3000)

        //只创建了一个节点，不是使用的String，而是使用的的字节码
        println client.getChildren().forPath("/")
        println new String(client.getData().forPath("/AutomicInteger"))
    }

}
