package com.liushu.test.learnofzk

import org.apache.zookeeper.AsyncCallback
import org.apache.zookeeper.CreateMode
import org.apache.zookeeper.KeeperException
import org.apache.zookeeper.WatchedEvent
import org.apache.zookeeper.Watcher
import org.apache.zookeeper.ZooDefs
import org.apache.zookeeper.ZooKeeper
import org.apache.zookeeper.data.Stat
import org.junit.Test

import java.util.concurrent.atomic.AtomicBoolean

/**
 * Created by liushu on 2019/3/26.
 */
class ZKTest {

    @Test
    public void testGet(){

        List<String> children = ZKFactory.getInstance().getChildren("/",null);
        println children;

        List<Node> all = ZKUtils.findAllNode();
        all.each {
            println it
        }
    }

    @Test
    public void testGetStat(){

        Stat stat = new Stat();
        byte[] data = ZKFactory.getInstance().getData("/testzk",null,stat);
        //自增id，创建时间，版本等信息
        println stat
        println new String(data)
    }

    /**
     * 创建永久节点
     *  1.创建
     *  2.重复创建
     *  3.访问
     *  4.断开连接
     *  5.重新创建
     *  6.访问
     */
    @Test
    public void testCreate(){

        String path = "/testzk/testcreate"
        ZooKeeper zk = ZKFactory.createOne();
        if (zk.exists(path,null)){
            Stat stat = new Stat()
            zk.getData(path,null,stat)
            println stat.getVersion()
            zk.delete(path,stat.getVersion())
        }

        assert !zk.exists(path,null)

        println zk.create(path,"testCreate".bytes,ZooDefs.Ids.OPEN_ACL_UNSAFE,CreateMode.PERSISTENT)
        try {
            println zk.create(path, "testCreate".bytes, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT)
        }catch (Exception e){
            //NodeExistsException
            e.printStackTrace()
        }

        assert zk.exists(path,null)

//        zk.close();

        Thread.sleep(1000)

        zk = ZKFactory.createOne()
        assert zk.exists(path,null)

    }

    /**
     * 创建临时节点
     *  1.创建
     *  2.访问
     *  3.断开连接
     *  4.重新创建
     *  5.无法访问
     *  6.重新连接
     *  7.访问
     */
    @Test
    public void testCreateEphemeral(){

        String path = "/testzk/testcreateEphemeral"
        ZooKeeper zk = ZKFactory.createOne();
        if (zk.exists(path,null)){
            Stat stat = new Stat()
            zk.getData(path,null,stat)
            println stat.getVersion()
            zk.delete(path,stat.getVersion())
        }

        assert !zk.exists(path,null)

        println zk.create(path,"testCreateEphemeral".bytes,ZooDefs.Ids.OPEN_ACL_UNSAFE,CreateMode.EPHEMERAL)

        assert zk.exists(path,null)

        long sessionId = zk.getSessionId()
        byte[] pwd = zk.getSessionPasswd()

//        zk.close(); 如果close了，再连也是close

        Thread.sleep(1000)

        ZooKeeper zk2 = ZKFactory.createOne()
        assert zk2.exists(path,null)

        zk.close()
        assert !zk2.exists(path,null)

        println zk2.create(path,"testCreateEphemeral".bytes,ZooDefs.Ids.OPEN_ACL_UNSAFE,CreateMode.EPHEMERAL)
    }

    //更新数据
    @Test
    public void testSetAndVersion(){

        String path = "/testcreate"
        ZooKeeper zk = ZKFactory.getInstance();
        if (zk.exists(path,null)){
            Stat stat = new Stat()
            zk.getData(path,null,stat)
            println stat.getVersion()
            zk.delete(path,stat.getVersion())
        }

        Stat stat = new Stat()
        println zk.create(path,"test".bytes,ZooDefs.Ids.OPEN_ACL_UNSAFE,CreateMode.PERSISTENT)
        println zk.getData(path,null,stat)

        assert stat.getVersion() == 0

        try {
            zk.setData(path, "test1".bytes, 3)
        }catch (KeeperException.BadVersionException e){
                //版本号是zk用来做并发控制的，必须严格按照拿到的的去传
        }

        zk.setData(path, "test1".bytes, stat.getVersion())
        //这里的stat已经是过期状态了
        assert stat.getVersion() == 0

        zk.getData(path,null,stat)
        assert stat.getVersion() == 1

        //版本传 -1 表示不受限制
        zk.setData(path, "test2".bytes, -1)

        zk.getData(path,null,stat)
        assert stat.getVersion() == 2

    }

    //回调
    @Test
    public void testWatcher(){

        String path = "/testzk/testcreate"
        ZooKeeper zk = ZKFactory.getInstance();
        if (zk.exists(path,null)){
            Stat stat = new Stat()
            zk.getData(path,null,stat)
            println stat.getVersion()
            zk.delete(path,stat.getVersion())
        }

        println zk.create(path,"test".bytes,ZooDefs.Ids.OPEN_ACL_UNSAFE,CreateMode.PERSISTENT)

        final int defaultConut = 0;
        final int newCount = 0;

        ZKFactory.setProcessHandle(new ProcessHandle() {
            @Override
            void process(WatchedEvent event) {
                defaultConut++;
                println "in default "+event.getPath()
            }
        })
        Stat stat = new Stat()
        //表示使用默认的
        zk.getData(path,true,stat);

        //这两种情况都不会使用watcher
        zk.getData(path,false,stat);
        zk.getData(path,null,stat);

        zk.getData(path,new Watcher() {
            @Override
            void process(WatchedEvent event) {
                newCount++;
                println "in new "+event.getPath();
            }
        },stat)

        zk.setData(path,"test1".bytes,stat.getVersion())
        Thread.sleep(100)
        //如果不指定，则使用默认的
        assert defaultConut == 1
        //如果指定，使用指定的
        assert newCount == 1;

        zk.setData(path,"test2".bytes,stat.getVersion()+1)
        //watcher的设置的只会起一次作用
        //个人理解watcher只是提醒值发送了变化，需要客户端去重新获取最新值，但是不能保证客户端可以看到所有中间过程
        //意味着并发环境下可能会错过一些变化，所以不能用来做消息通知
        assert defaultConut == 1
        assert newCount == 1;
    }

    @Test
    public void testCallBack(){

        String path = "/testzk/testcreate"
        ZooKeeper zk = ZKFactory.getInstance();
        if (zk.exists(path,null)){
            Stat stat = new Stat()
            zk.getData(path,null,stat)
            println stat.getVersion()
            zk.delete(path,stat.getVersion())
        }

        final AtomicBoolean success = new AtomicBoolean(false);

        //异步创建，通过callback来回掉
        zk.create(path,"test".bytes,ZooDefs.Ids.OPEN_ACL_UNSAFE,CreateMode.PERSISTENT,new AsyncCallback.StringCallback() {
            @Override
            void processResult(int rc, String path1, Object ctx, String name) {

                println rc  //错误码
                println path1
                println ctx
                println name
                success.set(true);
            }
        },"cxt")

        Thread.sleep(1000)
        assert success.get()
    }

    //安全
    @Test
    public void testACL(){

    }


}
