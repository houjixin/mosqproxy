package com.xyz.mosqproxy.proxy.center.datacenter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.CRC32;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xyz.mosqproxy.common.DefaultValues;
import com.xyz.mosqproxy.common.ReturnCode;
import com.xyz.mosqproxy.config.Config;
import com.xyz.mosqproxy.config.ConfigKeys;
import com.xyz.mosqproxy.datatype.ConnectionInfo;
import com.xyz.mosqproxy.exception.BasicException;
/*
*redis中采用两个hash表存储：
* 第一个hash表，用于存储第三方应用的某个账户下所有设备的连接ID，其key为：mp.[appid].[userid]
* field为设备ID，value为connectionId；
* 第二个hash表，用于存储连接的信息，其key为：mp.cid.[连接ID],连接id由ID生成器生成；
* field包括：
* 	字段名     描述
*   key		对称加密的密钥
*	en_t    对称加密算法的类型，1：表示简单混淆加密
*   s       连接的状态：1：在线，2：不在线，3：刚刚获取mosq server
*   hi		该连接所在的mosquitto的内部地址（mosquitto要被内网和外网同时访问）
*   pi      该连接所在的mosquitto的内部端口号
*   ho		该连接所在的mosquitto的外部地址
*   po		该连接所在的mosquitto的外部端口号
* */
public class DataCenter
{
	private static Logger logger = LoggerFactory.getLogger(DataCenter.class);
	private List<Sharding> shardingList = new ArrayList<Sharding>();
	private int maxShardingValue = 0;
	private int isSaveDb = 0;
	private int expireTime = 0;
	private int etEnable = DefaultValues.SETTING_DISABLE;
	private String getClassName()
	{
		return "DataCenter";
	}
	public boolean init(long logIndex, Config cfg)
	{
		String logFlag = "DataCenter.init";
		this.etEnable = cfg.getInt(ConfigKeys.SHARDING_CACHE_EXPIRETIME_ENALBE);
		if (this.etEnable < 0)
		{
			logger.error("[lid:{}][{}] configure data error! etEnable:{}", logIndex, logFlag, this.etEnable);
			return false;
		}
		this.expireTime = cfg.getInt(ConfigKeys.SHARDING_CACHE_EXPIRETIME);
		if (this.expireTime < 0)
		{
			logger.error("[lid:{}][{}] configure data error! expiretime:{}", logIndex, logFlag, this.expireTime);
			return false;
		}
		this.isSaveDb = cfg.getInt(ConfigKeys.SHARDING_ENABLEDB, DefaultValues.SETTING_DISABLE);
		if (this.isSaveDb < 0)
		{
			logger.error("[lid:{}][{}] configure data error! isSaveDb:{}", logIndex, logFlag, this.isSaveDb);
			return false;
		}
		// 获取各分片的配置信息
		maxShardingValue = cfg.getInt(ConfigKeys.SHARDING_MAX_SHARDING_VALUE);
		if (maxShardingValue <= 0)
		{
			logger.error("[lid:{}][{}] configure data error! maxShardingValue:{}", logIndex, logFlag, maxShardingValue);
			return false;
		}
		
		int shardingNum = cfg.getInt(ConfigKeys.SHARDING_NUM);
		for (int shardingIdex = 1; shardingIdex <= shardingNum; shardingIdex++)
		{
			Sharding newSharding = new Sharding();
			if (!newSharding.init(logIndex, shardingIdex, this.isSaveDb, cfg))
			{
				logger.error("[lid:{}][{}] a new Sharding initialized fail! shardingIdex:{}", logIndex, logFlag, shardingIdex);
				return false;
			}
			shardingList.add(newSharding);
		}
		return true;
	}


	// 通过CRC32的值获取所在的分片
	private Sharding getSharding(long logIndex, String key)
	{
		String logFlag = getClassName() + ".getSharding";
		if (StringUtils.isBlank(key))
		{
			logger.warn("[lid:{}][{}] parameter is null", logIndex, logFlag);
			return null;
		}
		CRC32 crc32 = new CRC32();
		crc32.update(key.getBytes());
		long vCRC32 = crc32.getValue();
		int hashRes = (int) (Math.abs(vCRC32) % maxShardingValue);
		for (int i = 0; i < shardingList.size(); i++)
		{
			if (shardingList.get(i).isInRange(hashRes))
				return shardingList.get(i);
		}
		return null;
	}

	public void shutdown(long logIndex)
	{
		for (int i = 0; i < shardingList.size(); i++)
		{
			shardingList.get(i).shutdown(logIndex);
		}
	}

	public String getOnlineStatus(long logIndex, String appid, String userid, String devid) throws Exception{
		String logFlag = getClassName() + "getOnlineStatus";
		String redisKey = new StringBuilder(DefaultValues.REDIS_PRIFIX).append(appid).append(userid).toString();
		RedisClient rc = getRedisClient(logIndex,redisKey);
		if (rc == null)
		{
			logger.debug("[lid:{}][{}] Cann't get Redis from current sharding!", logIndex, logFlag);
			throw new BasicException(logIndex, ReturnCode.INNER_ERROR, "Cann't get Redis from current sharding!", null);
		}
		String conId = rc.hget(logIndex, redisKey,devid);
		if(StringUtils.isBlank(conId)){
			return null;
		}else{
			String conIdKey = new StringBuilder(DefaultValues.CON_ID_KEY_PRIFIX).append(conId).toString();
			return rc.hget(logIndex, conIdKey, DefaultValues.CON_INFO_STATUS);
		}
	}

	private RedisClient getRedisClient(long logIndex, String shardingKey){
		String logFlag = getClassName() + "getRedisClient";
		Sharding dstSharding = getSharding(logIndex, shardingKey);
		if (dstSharding == null){
			logger.debug("[lid:{}][{}] Cann't get sharding with key:{}", logIndex, logFlag, shardingKey);
			return null;
		}
		return dstSharding.getRedisClient(logIndex);
	}
	//获取appId的UserId下的指定某个devId的连接信息
	public ConnectionInfo getConnectionInfo(long logIndex, String appId, String userId, String devId) throws Exception{
		return getConnectionInfo(logIndex, getConnectionId(logIndex, appId, userId, devId));
	}

	//获取某个connId的所有信息
	public ConnectionInfo getConnectionInfo(long logIndex, String connId) {
		String logFlag = getClassName() + "getConnectionInfo";
		String redisKey = new StringBuilder(DefaultValues.CON_ID_KEY_PRIFIX).append(connId).toString();
		ConnectionInfo conInfo = new ConnectionInfo();
		if(StringUtils.isBlank(connId)){
			return conInfo;
		}
		RedisClient rc = getRedisClient(logIndex, redisKey);
		if (rc == null)
		{
			logger.debug("[lid:{}][{}] Cann't get Redis from current sharding! sharding key:{}", logIndex, logFlag, redisKey);
			return conInfo;
		}
		Map<String, String > con = rc.hgetAll(logIndex, new StringBuilder(DefaultValues.CON_ID_KEY_PRIFIX).append(connId).toString());
		if(con != null && !con.isEmpty())
		{
			conInfo.setConnId(connId);
			conInfo.initFromMap(con);
		}
		return conInfo;
	}

	public boolean saveConnInfo(long logIndex, ConnectionInfo conInfo){
		String logFlag = getClassName() + "saveConnInfo";
		if (conInfo == null || !conInfo.isValid()){
			logger.warn("[lid:{}][{}] Save connection informatioin into redis fail, connection information is null or invalid! connection information:{}", logIndex, logFlag, conInfo);
			return false;
		}
		String redisKey = new StringBuilder(DefaultValues.CON_ID_KEY_PRIFIX).append(conInfo.getConnId()).toString();
		RedisClient rc = getRedisClient(logIndex, redisKey);
		if (rc == null)
		{
			logger.debug("[lid:{}][{}] Cann't get Redis from current sharding! sharding key:{}", logIndex, logFlag, redisKey);
			return false;
		}
		return rc.hmset(logIndex, redisKey, conInfo.getCachedConnDetal());
	}

	public boolean bindConnId(long logIndex, String appId, String userId, String devId, String connId){
		String logFlag = getClassName() + "bindConnId";
		if(StringUtils.isBlank(connId)){
			logger.warn("[lid:{}][{}] connection id is null! appId:{}, userId:{}, devId:{}", logIndex, logFlag, appId, userId, devId);
			return false;
		}
		String redisKey = new StringBuilder(DefaultValues.REDIS_PRIFIX).append(appId).append(userId).toString();
		RedisClient rc = getRedisClient(logIndex, redisKey);
		if (rc == null)
		{
			logger.debug("[lid:{}][{}] Cann't get Redis from current sharding! sharding key:{}", logIndex, logFlag, redisKey);
			return false;
		}
		return rc.hset(logIndex, redisKey, devId, connId);
	}

	public String getConnectionId(long logIndex, String appId, String userId, String devId) throws Exception{
		String logFlag = getClassName() + "getConnectionId";
		String redisKey = new StringBuilder(DefaultValues.REDIS_PRIFIX).append(appId).append(userId).toString();
		RedisClient rc = getRedisClient(logIndex, redisKey);
		if (rc == null)
		{
			logger.debug("[lid:{}][{}] Cann't get Redis from current sharding! sharding key:{}", logIndex, logFlag, redisKey);
			throw new BasicException(logIndex, ReturnCode.INNER_ERROR, "Cann't get Redis from current sharding!", null);
		}
		return rc.hget(logIndex, redisKey,devId);
	}

	public Map<String, String> getAllConnectionIds(long logIndex, String appId, String userId) throws Exception{
		String logFlag = getClassName() + "getConnectionId";
		String redisKey = new StringBuilder(DefaultValues.REDIS_PRIFIX).append(appId).append(userId).toString();
		RedisClient rc = getRedisClient(logIndex, redisKey);
		if (rc == null)
		{
			logger.debug("[lid:{}][{}] Cann't get Redis from current sharding! sharding key:{}", logIndex, logFlag, redisKey);
			throw new BasicException(logIndex, ReturnCode.INNER_ERROR, "Cann't get Redis from current sharding!", null);
		}

		return rc.hgetAll(logIndex, redisKey);
	}

	public Map<String, ConnectionInfo> getAllConnectionInfo(long logIndex, String appId, String userId) throws Exception{
//		String logFlag = getClassName() + "getAllConnectionInfo";
		Map<String, ConnectionInfo> res = new HashMap<>();
		Map<String, String> ids = getAllConnectionIds(logIndex, appId, userId);
		if(ids == null || ids.isEmpty()){
			return res;
		}

		for (String connId: ids.values()){
			if(StringUtils.isBlank(connId)){
				continue;
			}
			ConnectionInfo connInfo = getConnectionInfo(logIndex,connId);
			res.put(connId, connInfo);
		}
		return res;
	}

	public String getInfo(long logIndex)
	{
		StringBuffer infoBuf = new StringBuffer();
		infoBuf.append("infomation of data center:\n");
		for(Sharding s : shardingList)
		{
			infoBuf.append(s.getInfo(logIndex));
		}
		return infoBuf.toString();
	}
}
