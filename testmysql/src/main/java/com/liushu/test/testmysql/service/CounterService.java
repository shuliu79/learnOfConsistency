package com.liushu.test.testmysql.service;

import com.liushu.test.testmysql.model.Counter;
import com.liushu.test.testmysql.repository.CounterRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.util.Optional;

/**
 * Created by liushu on 2019/3/17.
 */
@Service
public class CounterService {

    private final static int TARGET_ID = 3;

    @Resource
    private CounterRepository counterRepository;

    @Resource
    private PlatformTransactionManager transactionManager;

    public Counter init(){

        Counter c = counterRepository.findById(TARGET_ID).orElseGet(Counter::new);
        c.setValue(0);
        counterRepository.save(c);
        return c;
    }

    public int testNotRollback(){
        Counter c = counterRepository.findById(TARGET_ID).orElseThrow(()->new RuntimeException("need init"));
        c.setValue(c.getValue()+1);
        counterRepository.save(c);
        throw new RuntimeException("test");
    }

    @Transactional
    public int testRollback(){
        Counter c = counterRepository.findById(TARGET_ID).orElseThrow(()->new RuntimeException("need init"));
        c.setValue(c.getValue()+1);
        counterRepository.save(c);
        throw new RuntimeException("test");
    }

    public int incr(){
        Counter c = counterRepository.findById(TARGET_ID).orElseThrow(()->new RuntimeException("need init"));
        c.setValue(c.getValue()+1);
        counterRepository.save(c);
        return c.getValue();
    }

    @Transactional
    public int incrWithTransaction(){
        return incr();
    }

    public int incrSerializable(){
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        //设置事务隔离级别
        transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
        //设置为required传播级别
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) { //事务块
                Counter c = counterRepository.findById(TARGET_ID).orElseThrow(()->new RuntimeException("need init"));
                c.setValue(c.getValue()+1);
                counterRepository.save(c);
            }
        });
        return 0;
    }

    @Transactional
    public int incrWithLock(){
        Counter c = counterRepository.findByIdWithLock(TARGET_ID);
        c.setValue(c.getValue()+1);
        counterRepository.save(c);
        return c.getValue();
    }


    public int getValue(){
        return counterRepository.findById(TARGET_ID).orElseThrow(()->new RuntimeException("need init")).getValue();
    }

}
