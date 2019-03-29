package com.liushu.test.learnofzk.bean;

/**
 * Created by liushu on 2019/3/26.
 */
public class Node {

    private String path;

    private String value;

    public Node(String path, String value) {
        this.path = path;
        this.value = value;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return path+":"+value;
    }
}
