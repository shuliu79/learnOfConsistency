package com.liushu.test.testmysql.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

/**
 * Created by liushu on 2019/3/17.
 */
@Entity
public class Counter {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Integer id;

    private int value;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }
}
