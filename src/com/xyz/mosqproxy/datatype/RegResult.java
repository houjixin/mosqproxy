package com.xyz.mosqproxy.datatype;

import com.alibaba.fastjson.JSONObject;
import com.xyz.mosqproxy.common.DefaultValues;

/**
 * Created by Jason on 2017/4/7.
 */
public class RegResult extends BaseResult{
    private ConnectionInfo conInfo;
    public RegResult(int retCode, ConnectionInfo conInfo){
        setCode(retCode);
        this.conInfo = conInfo;
    }
    public ConnectionInfo getConnectionInfo(){
    	return conInfo;
    }
    public String toJsonString(){
        JSONObject joRes = new JSONObject();
        joRes.put(DefaultValues.RETURN_CODE, getCode());
        joRes.put(DefaultValues.RETURN_VALUE, conInfo);
        return joRes.toJSONString();
    }
    
    public String getValue() {
    	if(conInfo != null)
    		return conInfo.toOutSimpleString();
        return null;
    }

}
