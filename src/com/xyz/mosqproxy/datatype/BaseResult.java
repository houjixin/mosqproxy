package com.xyz.mosqproxy.datatype;

/**
 * Created by Jason on 2017/4/7.
 * 所有的返回值都由两部分构成：返回码和对应的值
 */
public class BaseResult {
    private int code;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    private String value;
}
