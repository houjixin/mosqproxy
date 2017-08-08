package com.xyz.mosqproxy.datatype;

import com.alibaba.fastjson.JSONObject;
import com.xyz.mosqproxy.common.DefaultValues;

/**
 * Created by Jason on 2017/4/6.
 */

public class ClientStatus {
    public String getOnline() {
        return online;
    }

    public void setOnline(String online) {
        if (DefaultValues.CONN_STATUS_ONLINE.equals(online) ||
            DefaultValues.CONN_STATUS_OFFLINE.equals(online) ||
                DefaultValues.CONN_STATUS_ERROR.equals(online)||
                DefaultValues.CONN_STATUS_ACQUIRED.equals(online)) {
            this.online = online;
        }else{
            this.online = DefaultValues.CONN_STATUS_ERROR;
        }
    }

    public String toJsonString() {
        JSONObject joStatus = new JSONObject();
        joStatus.put("online", online);
        return joStatus.toJSONString();
    }

    private String online;//1：在线，2：不在线，3：刚刚获取mosq server，-1：发生内部错误
}
