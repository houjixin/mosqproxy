package com.xyz.mosqproxy.proxy.center.mqttclient;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xyz.mosqproxy.common.DefaultValues;
import com.xyz.mosqproxy.common.Tools;
import com.xyz.mosqproxy.config.Config;
import com.xyz.mosqproxy.config.ConfigKeys;
import com.xyz.mosqproxy.exception.BasicException;
import com.xyz.mosqproxy.starter.Starter;

public class ClientGroup
{
	private static Logger logger = LoggerFactory.getLogger(ClientGroup.class);
	private List<PahoMqttClient> clientList = new ArrayList<PahoMqttClient>();
	private AtomicInteger curIndex = new AtomicInteger(0);
	private PahoMqttClient moniterClient;//每个group里有一个listenerClient专门负责监听mosq的系统消息
	private int clientNum;
	private String flag;
	private String innerHost;
	private int innerPort;
	private String foreignHost;
	private int foreignPort;
	private int keepalive;
	private int initTimeout;
	private int worktimeout;
	private String getClassName(){return "ClientGroup";}
	public String getInnerHost()
	{
		return innerHost;
	}
	public int getInnerPort()
	{
		return innerPort;
	}
	public String getForeignHost()
	{
		return foreignHost;
	}
	public int getForeignPort()
	{
		return foreignPort;
	}

	public boolean isWorking()
	{
		boolean workingStat = false;
		for(PahoMqttClient mc: clientList)
		{
			if(mc != null && mc.isWorking()){
				workingStat = true;
				break;
			}
				
		}
		return workingStat;
	}
	
	private AtomicBoolean isNotConn = new AtomicBoolean(true);
	
	public boolean isNotConnecting(long logIndex)
	{
		return isNotConn.get();
	}
	/**
	 * compAndSetConnStat
	 * 判断并且设置连接装是否为正在连接，如果当前为*未连接*则将其设置为*正在连接状态*，并且返回成功
	 * */
	public boolean compAndSetNoConnStat(long logIndex)
	{
		return isNotConn.compareAndSet(true, false);
	}
	
	public void setNotConnStat(long logIndex, boolean stat)
	{
		isNotConn.set(stat);
	}
	
//	public void setWorking(boolean isWorking)
//	{
//		this.isWorking = isWorking;
//	}

	//当前ClientGroup中所有MqttClient所连接的mosquitto中的连接数
	private int mosqUserNum = 0;
	public int getMosqUserNum() {
		return mosqUserNum;
	}

	public void setMosqUserNum(int mosqUserNum) {
		if(mosqUserNum >= 0)
			this.mosqUserNum = mosqUserNum;
	}

	private boolean isUpdate = false;

	public void setUpdate(boolean isUpdate)
	{
		this.isUpdate = isUpdate;
	}

	public boolean getUpdate()
	{
		return this.isUpdate;
	}
	
	public String getFlag()
	{
		return flag;
	}
	
	public boolean init(long logIndex, String flag, int mosqIndex, Config confData){

		/**
		 * 初始化时需要创建普通客户端和监控客户端
		 * */
		String logFlag = getClassName() + ".init";
		String cfgKey = ConfigKeys.PRIFIX_MQTT_SERVER + mosqIndex + ".flag";
		this.flag = flag;

		cfgKey = ConfigKeys.PRIFIX_MQTT_SERVER + mosqIndex + ".inner.host";
		this.innerHost = confData.getString(cfgKey);
		if (StringUtils.isBlank(innerHost))
		{
			logger.error("[lid:{}][{}] config data error! key:{}, value:{}", logIndex, logFlag, cfgKey, innerHost);
			return false;
		}

		cfgKey = ConfigKeys.PRIFIX_MQTT_SERVER + mosqIndex + ".inner.port";
		this.innerPort = confData.getInt(cfgKey);
		if (!Tools.checkPort(innerPort))
		{
			logger.error("[lid:{}][{}] config data error! key:{}, value:{}", logIndex, logFlag, cfgKey, innerPort);
			return false;
		}

		cfgKey = ConfigKeys.PRIFIX_MQTT_SERVER + mosqIndex + ".foreign.host";
		this.foreignHost = confData.getString(cfgKey);
		if (StringUtils.isBlank(foreignHost))
		{
			logger.error("[lid:{}][{}] config data error! key:{}, value:{}", logIndex, logFlag, cfgKey, foreignHost);
			return false;
		}
		cfgKey = ConfigKeys.PRIFIX_MQTT_SERVER + mosqIndex + ".foreign.port";
		this.foreignPort = confData.getInt(cfgKey);
		if (!Tools.checkPort(foreignPort))
		{
			logger.error("[lid:{}][{}] config data error! key:{}, value:{}", logIndex, logFlag, cfgKey, foreignPort);
			return false;
		}

		cfgKey = ConfigKeys.CONN_TO_MOSQUITTO_NUM;
		clientNum = confData.getInt(cfgKey);
		if (clientNum <= 0)
		{
			logger.error("[lid:{}][{}] config data error! key:{}, value:{}", logIndex, logFlag, cfgKey, clientNum);
			return false;
		}
		String clientIdPrefix = Starter.SERVICE_NAME;
		if(StringUtils.isBlank(clientIdPrefix))
		{
			Random rand =new Random(DefaultValues.CLIENT_ID_MAX_RANDOM);
			clientIdPrefix = DefaultValues.CLIENT_ID_PREFIX + rand.nextInt(DefaultValues.CLIENT_ID_MAX_RANDOM);
		}
		cfgKey = ConfigKeys.MQTT_SERVER_KEEPALIVE;
		this.keepalive = confData.getInt(cfgKey);
		if (keepalive <= 0)
		{
			keepalive = DefaultValues.MQTT_KEEPALIVE;
			logger.warn("[lid:{}][{}] config data error! key:{}, value:{}, will use default value:{}", logIndex, logFlag, cfgKey, keepalive,
					DefaultValues.MQTT_KEEPALIVE);
		}
		
		cfgKey = ConfigKeys.MQTT_SERVER_WORKTIMEOUT;
		worktimeout = confData.getInt(cfgKey);
		if (worktimeout <= 0)
		{
			worktimeout = DefaultValues.WORK_TIMEOUT;
			logger.warn("[lid:{}][{}] config data error! key:{}, value:{}, will use default value:{}", logIndex, logFlag, cfgKey, worktimeout,
					DefaultValues.WORK_TIMEOUT);
		}
		cfgKey = ConfigKeys.MQTT_SERVER_INITTIMEOUT;
		initTimeout = confData.getInt(cfgKey);
		if (initTimeout <= 0)
		{
			initTimeout = DefaultValues.MQTT_CLIENT_INIT_TIMEOUT;
			logger.warn("[lid:{}][{}] config data error! key:{}, value:{}, will use default value:{}", logIndex, logFlag, cfgKey, initTimeout,
					DefaultValues.MQTT_CLIENT_INIT_TIMEOUT);
		}
		//创建普通的mosquitto客户端，用于后续向mosquitto发送消息
		for (int j = 0; j < clientNum; j++)
		{
			String clientid = String.format("%s-%d", clientIdPrefix, j);
			PahoMqttClient mqttClient = new PahoMqttClient();
			if (!mqttClient.init(logIndex, clientid, innerHost, innerPort, clientid, keepalive, worktimeout, initTimeout, confData))
			{
				logger.error("[lid:{}][{}] initial mosquitto client:{} fail!", logIndex, logFlag, flag);
				return false;
			}
			mqttClient.setOwner(this);
			logger.debug("[lid:{}][{}] will init mosquitto client! client id:{}", logIndex, logFlag, clientid);
			clientList.add(mqttClient);
		}
		// 创建监控mosquitto，用于监控mosquitto的上下线通知，在线用户数等
		
		
		//初始化listenerClient，设置回调函数
		moniterClient = new PahoMqttClient();
		String lcId = String.format("%s-%d", clientIdPrefix, Tools.getRandomInt(logIndex, DefaultValues.CLIENT_ID_MAX_RANDOM));
		
		if (!moniterClient.init(logIndex, lcId, innerHost, innerPort, lcId, keepalive, worktimeout, initTimeout, confData))
		{
			logger.error("[lid:{}][{}] initial moniterClient:{} fail!", logIndex, logFlag, flag);
			return false;
		}
		moniterClient.setOwner(this);
		logger.info("[lid:{}][{}] moniterClient id is:{}", logIndex, logFlag, lcId);
		return true;
	}

	/**
	 * 函数名：getClient 函数功能：获取mosquito客户端
	 * @param  logIndex 日志索引
	 * */
	public PahoMqttClient getClient(long logIndex)
	{
		String logFlag = "ClientGroup.getClient";
		int i = 0;
		PahoMqttClient cli = null;
		while(i < clientList.size())
		{
			cli = clientList.get(curIndex.getAndIncrement() % this.clientNum);
			
			if (cli != null && cli.isWorking())
				break;
			logger.warn("[lid:{}][{}] chosed client is unworking! will choose another one!", logIndex, logFlag);
			i++;
		}
		
		return cli;
	}


	public boolean startUp(long logIndex)
	{
		String logFlag = "ClientGroup.startUp";
		try
		{
			for (PahoMqttClient mqttClient : clientList)
			{
				if (!mqttClient.startUp(logIndex))
				{
					logger.error("[lid:{}][{}] start mosquitto client {} fail! ", logIndex, logFlag, mqttClient.getClientId());
					return false;
				}
			}
			//启动监控的mosquitto客户端
			if (!moniterClient.startUp(logIndex))
			{
				logger.error("[lid:{}][{}] start moniterClient {} failed! ", logIndex, logFlag, moniterClient.getClientId());
				return false;
			}
			return true;
		}
		catch (Exception e)
		{
			logger.error("[lid:{}][{}] Exception:{}", logIndex, logFlag, BasicException.getStackTrace(e));
			return false;
		}
		
	}

	public void shutDown(long logIndex)
	{
		for (PahoMqttClient mqttClient : clientList)
		{
			mqttClient.shutDown(logIndex);
		}
		//关闭listenerClient	
		moniterClient.shutDown(logIndex);
	}
	public int getKeepalive()
	{
		return keepalive;
	}


}
