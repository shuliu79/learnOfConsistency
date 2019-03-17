package com.liushu.test.testmysql;

import com.liushu.test.testmysql.service.CounterService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.PlatformTransactionManager;

import javax.annotation.Resource;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by liushu on 2019/3/17.
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class TransactionTest {

    @Resource
    private CounterService counterService;

    @Resource
    private PlatformTransactionManager transactionManager;

    @Test
    public void init(){
        System.out.println(counterService.init());
    }

    /**
     * 测试事务是否有效
     */
    @Test
    public void testRollback(){

        counterService.init();
        System.out.println(counterService.getValue());
        try {
            counterService.testRollback();
        }catch (Exception e){
        }
        System.out.println(counterService.getValue());

        try {
            counterService.testNotRollback();
        }catch (Exception e){
        }
        System.out.println(counterService.getValue());

    }

    /**
     * 无事务
     * @throws InterruptedException
     */
    @Test
    public void incrWithoutTransaction() throws InterruptedException {

        int threadNum = 16;
        counterService.init();
        ExecutorService executorService = Executors.newFixedThreadPool(threadNum);
        for (int i=0;i<threadNum;i++){

            executorService.submit(() -> {
                for (int i1 = 0; i1 <100; i1++) {
                    counterService.incr();
                }
            });
        }

        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.MINUTES);
        //期望值为1600，实际值为140，因为并发操作产生了冲突，中间某些提交被掩盖了。
        System.out.println(counterService.getValue());
    }

    /**
     * 默认事务
     * @throws InterruptedException
     */
    @Test
    public void incrWithTransaction() throws InterruptedException {

        int threadNum = 16;
        counterService.init();
        ExecutorService executorService = Executors.newFixedThreadPool(threadNum);
        for (int i=0;i<threadNum;i++){

            executorService.submit(() -> {
                for (int i1 = 0; i1 <100; i1++) {
                    counterService.incrWithTransaction();
                }
            });
        }

        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.MINUTES);
        //期望值为1600，实际值为200+，Mysql默认的事务是repeatable-read，不能保证完全隔离
        System.out.println(counterService.getValue());
    }

    /**
     * 最高隔离级别的事务
     * @throws InterruptedException
     */
    @Test
    public void incrSerializable() throws InterruptedException {

        int threadNum = 16;
        counterService.init();
        ExecutorService executorService = Executors.newFixedThreadPool(threadNum);
        for (int i=0;i<threadNum;i++){

            executorService.submit(() -> {
                for (int i1 = 0; i1 <100; i1++) {
                    counterService.incrSerializable();
                }
            });
        }

        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.MINUTES);
        //如果串行化，值应该为1600，实际值为106。在serialiable级别下，数据库select操作会加读锁，但是不会加写锁，实际上
        //(innoDB存储引擎(姜承尧，第二版) P332)
        System.out.println(counterService.getValue());
    }

    /**
     * 事务+一致性锁定读
     * @throws InterruptedException
     */
    @Test
    public void incrWithLock() throws InterruptedException {

        int threadNum = 16;
        counterService.init();
        ExecutorService executorService = Executors.newFixedThreadPool(threadNum);
        for (int i=0;i<threadNum;i++){

            executorService.submit(() -> {
                for (int i1 = 0; i1 <100; i1++) {
                    counterService.incrWithLock();
                }
            });
        }

        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.MINUTES);
        //如果串行化，值应该为1600，实际值为1600。通过加锁的方式，才能实现完全的隔离性。
        System.out.println(counterService.getValue());
    }




}
