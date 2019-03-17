package com.liushu.test.testmysql.repository;

import com.liushu.test.testmysql.model.Counter;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

/**
 * Created by liushu on 2019/3/17.
 */
public interface CounterRepository extends CrudRepository<Counter,Integer> {

    @Query(value = "SELECT * FROM counter c WHERE c.id = ?1 for UPDATE",
        nativeQuery = true)
    Counter findByIdWithLock(int id);


}
