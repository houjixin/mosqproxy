package com.xyz.mosqproxy.datatype;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.alibaba.fastjson.JSONObject;
import com.xyz.mosqproxy.common.DefaultValues;
import com.xyz.mosqproxy.common.Tools;

/**
 * Created by Jason on 2017/4/6.
 * json和redis中存储的字段名称和含义
 * k		对称加密的密钥
 * en_t    对称加密算法的类型，0:表示不加密，1：表示简单混淆加密
 * st       连接的状态：1：在线，2：不在线，0：刚刚获取mosq server
 * hi		该连接所在的mosquitto的内部地址（mosquitto要被内网和外网同时访问）
 * pi       该连接所在的mosquitto的内部端口号
 * ho		该连接所在的mosquitto的外部地址
 * po		该连接所在的mosquitto的外部端口号
 * ss       连接的会话，客户端连接mosquitto的时候使用
 * mf       mosquittoserver的标识，例如可以为mosquitto的内部地址:端口号
 */
public class ConnectionInfo {
    private String connId;
    private String status;
    private String key;
    private String encriptyType;
    private String session;
    private String innerHost;
    private int innerPort;
    private String outHost;
    private int outPort;

    public String getMosqFlag() {
        return mosqFlag;
    }

    public void setMosqFlag(String mosqFlag) {
        this.mosqFlag = mosqFlag;
    }

    private String mosqFlag;

    public String getSession() {
        return session;
    }

    public void setSession(String session) {
        this.session = session;
    }



    public ConnectionInfo(ConnectionInfo conInfo){
        this.connId         = conInfo.getConnId();
        this.status         = conInfo.getConnId();
        this.encriptyType   = conInfo.getEncriptyType();
        this.key            = conInfo.getKey();
        this.innerHost      = conInfo.getInnerHost();
        this.innerPort      = conInfo.getInnerPort();
        this.outHost        = conInfo.getOutHost();
        this.outPort        = conInfo.getOutPort();
        this.session        = conInfo.getSession();
        this.mosqFlag       = conInfo.getMosqFlag();
    }

    public String getInnerHost() {
        return innerHost;
    }

    public void setInnerHost(String innerHost) {
        this.innerHost = innerHost;
    }

    public int getInnerPort() {
        return innerPort;
    }

    public void setInnerPort(String innerPort) {

        this.innerPort = getPort(innerPort);
    }
    public void setInnerPort(int innerPort) {

        this.innerPort = innerPort;
    }

    public void setOutPort(int outPortPort) {

        this.outPort = outPortPort;
    }

    private int getPort(String port){
        int iPort;
        try{
            iPort = Integer.parseInt(port);
            if (!Tools.checkPort(iPort)){
                iPort = DefaultValues.INVALID_PORT;
            }
        }catch(NumberFormatException e){
            iPort = DefaultValues.INVALID_PORT;
        }
        return iPort;
    }

    public String getOutHost() {
        return outHost;
    }

    public void setOutHost(String outHost) {
        this.outHost = outHost;
    }

    public int getOutPort() {
        return outPort;
    }

    public void setOutPort(String outPort) {
        this.outPort = getPort(outPort);
    }

    public ConnectionInfo(){
        this.connId         = null;
        this.encriptyType   = null;
        this.key            = null;
        this.status         = null;
        this.innerPort      = DefaultValues.INVALID_PORT;
        this.outPort        = DefaultValues.INVALID_PORT;
        this.innerHost      = null;
        this.outHost        = null;
        this.mosqFlag       = null;
    }
    public String getConnId() {
        return connId;
    }

    public void setConnId(String connId) {
        this.connId = connId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getEncriptyType() {
        return encriptyType;
    }

    public void setEncriptyType(String encriptyType) {
        this.encriptyType = encriptyType;
    }

    public boolean isValid()
    {
        if(StringUtils.isBlank(connId))
            return false;
        //check connection status
        if (!DefaultValues.CONN_STATUS_ONLINE.equals(status) && !DefaultValues.CONN_STATUS_ACQUIRED.equals(status)){
            return false;
        }
        //check mosquitto host and port
        if(StringUtils.isBlank(innerHost) || StringUtils.isBlank(outHost) || !Tools.checkPort(innerPort) || !Tools.checkPort(outPort)){
            return false;
        }
        return true;
    }

    /**
     * 从一个map中来初始化这个连接对象，map的key也必须遵循本类的定义，否则无法解析
     * */
    public void initFromMap(Map<String, String> conMap){
        if(conMap == null || conMap.isEmpty())
            return;
        status          = conMap.get(DefaultValues.CON_INFO_STATUS);
        key             = conMap.get(DefaultValues.CON_INFO_KEY);
        encriptyType    = conMap.get(DefaultValues.CON_INFO_KEY);
        innerHost       = conMap.get(DefaultValues.CON_INFO_INNER_HOST);
        innerPort       = getPort(conMap.get(DefaultValues.CON_INFO_INNER_PORT));
        outHost         = conMap.get(DefaultValues.CON_INFO_OUT_HOST);
        outPort         = getPort(conMap.get(DefaultValues.CON_INFO_OUT_PORT));
        session         = conMap.get(DefaultValues.CON_INFO_SESSION);
        mosqFlag        = conMap.get(DefaultValues.CON_INFO_MOSQ_FLAG);
    }

    /**
     * 获取一个connid的详细信息，不包括connid，这个map可以直接用于redis存储
     * */
    public Map<String, String> getCachedConnDetal(){

        Map<String, String> conInfo = new HashMap<>();

        if(!StringUtils.isBlank(status)){
            conInfo.put(DefaultValues.CON_INFO_STATUS, status);
        }
        if(!StringUtils.isBlank(key)){
            conInfo.put(DefaultValues.CON_INFO_KEY, key);
        }
        if(!StringUtils.isBlank(encriptyType)){
            conInfo.put(DefaultValues.CON_INFO_ENCRY_TYPE, encriptyType);
        }
        if(!StringUtils.isBlank(innerHost)){
            conInfo.put(DefaultValues.CON_INFO_INNER_HOST, innerHost);
        }
        if(Tools.checkPort(innerPort)){
            conInfo.put(DefaultValues.CON_INFO_INNER_PORT, String.valueOf(innerPort));
        }
        if(!StringUtils.isBlank(outHost)){
            conInfo.put(DefaultValues.CON_INFO_OUT_HOST, outHost);
        }
        if(Tools.checkPort(outPort)){
            conInfo.put(DefaultValues.CON_INFO_OUT_PORT, String.valueOf(outPort));
        }
        if(!StringUtils.isBlank(session)){
            conInfo.put(DefaultValues.CON_INFO_SESSION, session);
        }
        if(!StringUtils.isBlank(mosqFlag)){
            conInfo.put(DefaultValues.CON_INFO_MOSQ_FLAG, mosqFlag);
        }
        return  conInfo;
    }
    /*
    *外部简化版的信息，不包括hi、pi这些数据，主要用于返回给客户端使用
    * */
    public JSONObject toOutSimpleInfo(){
        JSONObject conInfo = new JSONObject();
        if(!StringUtils.isBlank(connId)){
            conInfo.put(DefaultValues.CON_INFO_STATUS, connId);
        }
        if(!StringUtils.isBlank(status)){
            conInfo.put(DefaultValues.CON_INFO_STATUS, status);
        }
        if(!StringUtils.isBlank(key)){
            conInfo.put(DefaultValues.CON_INFO_KEY, key);
        }
        if(!StringUtils.isBlank(encriptyType)){
            conInfo.put(DefaultValues.CON_INFO_ENCRY_TYPE, encriptyType);
        }
        if(!StringUtils.isBlank(outHost)){
            conInfo.put(DefaultValues.CON_INFO_OUT_HOST, outHost);
        }
        if(Tools.checkPort(outPort)){
            conInfo.put(DefaultValues.CON_INFO_OUT_PORT, String.valueOf(outPort));
        }
        if(!StringUtils.isBlank(session)){
            conInfo.put(DefaultValues.CON_INFO_SESSION, session);
        }
        if(!StringUtils.isBlank(mosqFlag)){
            conInfo.put(DefaultValues.CON_INFO_MOSQ_FLAG, mosqFlag);
        }
        return conInfo;
    }
    
    /*
     *外部简化版的信息，不包括hi、pi这些数据，主要用于返回给客户端使用
     * */
     public String toOutSimpleString(){
         return toOutSimpleInfo().toJSONString();
     }
}
