package com.xyz.mosqproxy.proxy.center.mqttclient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xyz.mosqproxy.common.DefaultValues;
import com.xyz.mosqproxy.common.Tools;
import com.xyz.mosqproxy.config.Config;
import com.xyz.mosqproxy.config.ConfigKeys;

/**
 * MqttClientManager用于统一管理所有的mosquitto客户端，每个mosquitto客户端连接一个mosquitto服务器，
 * 每个mosquitto客户端有一个唯一的标识，可通过此标识指定要访问哪个mosquitto客户端
 * */
public class MqttClientManager
{
	private String getClassName(){return "MqttClientManager";}
	private static Logger logger = LoggerFactory.getLogger(MqttClientManager.class);
	public ConcurrentHashMap<String, ClientGroup> mqttClients = new ConcurrentHashMap<String, ClientGroup>();
	public ConcurrentHashMap<Integer, ClientGroup> allocMap = new ConcurrentHashMap<Integer, ClientGroup>();
	private int lastAllocIndex = DefaultValues.ALLOC_START_INDEX;
	public AtomicInteger mosquittoUpdateNum = new AtomicInteger(0);

	//当前每个mosquitto被分配出去的概率
	//是否使用负载均衡算法，如果不使用，则采用随机分配
	private int lb = DefaultValues.LB_BY_RANDOM; 
	//最大计算mosquitto分配概率的时间，即每经过这么长时间就必须进行一次概率计算，毫秒
	private long maxCalcProbTime = 0;
	private MosqLB mosqLB = new MosqLB();
	
	private MqttClientManager()	{};

	private static MqttClientManager mcm = new MqttClientManager();

	public static MqttClientManager getInstance()
	{
		return mcm;
	}
	
	/**
	 * 初始化每个mosquitto客户端，并将客户端与mosquitto连接起来， 这里的连接起来是指要在mqtt协议层完成连接和订阅功能
	 * */
	public boolean init(long logIndex, Config confData)
	{
		String logFlag = "MqttClientManager.init";
		
		// 读取各客户端的参数，并初始化所有的客户端
		if (!initMqttClients(logIndex, confData))
		{
			logger.error("[lid:{}][{}] get mosquitto server configure data fail... ", logIndex, logFlag);
			return false;
		}
		// 启动各客户端
		Set<String> clientFlags = mqttClients.keySet();
		for (String aClientflag : clientFlags)
		{
			logger.info("[lid:{}][{}] will start mosquitto client group: {} ", logIndex, logFlag, aClientflag);
			ClientGroup clientGroup = mqttClients.get(aClientflag);
			if (!clientGroup.startUp(logIndex))
			{
				logger.warn("[lid:{}][{}] start mosquitto client: {} fail!", logIndex, logFlag, aClientflag);
				return false;
			}
			logger.info("[lid:{}][{}] start mosquitto client group: {} success!", logIndex, logFlag, aClientflag);
		}
		if(!initLoadBalance(logIndex, confData)){
			logger.warn("[lid:{}][{}] init load balancefail!", logIndex, logFlag);
			return false;
		}
		return true;
	}
	
	private boolean initLoadBalance(long logIndex, Config confData){
		// 负载均衡相关配置
		String cfgKey = ConfigKeys.LB_MOSQ;
		this.lb = confData.getInt(cfgKey, DefaultValues.LB_BY_RANDOM);
		cfgKey = ConfigKeys.CALC_LB_INTERVAL;
		//转换成毫秒
		maxCalcProbTime = confData.getLong(cfgKey, DefaultValues.CALC_LB_INTERVAL) * 1000;

		mosqLB.init(logIndex);
		return true;
	}
	/**
	 * 函数名：initMqttClients 函数功能：从配置文件中读取各mosquito客户端的配置信息，并初始化mosquito客户端
	 * @author houjixin
	 * @param logIndex 日志索引
	 * @param  confData 配置信息
	 * */
	private boolean initMqttClients(long logIndex, Config confData)
	{
		String logFlag = getClassName() + ".initMqttClients";
		String cfgKey = ConfigKeys.MQTT_SERVER_NUM;
		int mosqNum = confData.getInt(cfgKey);
		if (mosqNum <= 0){
			logger.error("[lid:{}][{}] config data error! key:{}, value:{}", logIndex, logFlag, cfgKey, mosqNum);
			return false;
		}
		String flag;
		for (int mosqIndex = 1; mosqIndex <= mosqNum; mosqIndex++){
			
			cfgKey = ConfigKeys.PRIFIX_MQTT_SERVER + mosqIndex + ".flag";
			flag = confData.getString(cfgKey);
			if (StringUtils.isBlank(flag))
			{
				logger.error("[lid:{}][{}] config data error! key:{}, value:{}", logIndex, logFlag, cfgKey, flag);
				return false;
			}
			ClientGroup clientGroup = new ClientGroup();
			if (!clientGroup.init(logIndex, flag, mosqIndex, confData))
			{
				logger.error("[lid:{}][{}] initial mosquitto client group:{} fail!", logIndex, logFlag, flag);
				return false;
			}
			mqttClients.put(flag, clientGroup);
			allocMap.put(mosqIndex, clientGroup);
		}
		return true;
	}

	/**
	 * 向指定topic发送一条消息，需要指明待查询用户连接的是哪个mosquitto
	 * 
	 * @param serverFlag 标识待查询mosquitto的表示，计划用 "ip:端口号" 表示
	 * @param topic
	 * @param content 消息的内容
	 * @return int 0为成功，-1为失败
	 * */
	public int pub(long logIndex, String serverFlag, String topic, String content)
	{
		String logFlag = "MqttClientManager.pub";
		ClientGroup cliGroup = mqttClients.get(serverFlag);
		if (cliGroup == null)
		{
			logger.warn("[lid:{}][{}]get ClientGroup information fail! serverFlag:{}", logIndex, logFlag, serverFlag);
			return -1;
		}
		PahoMqttClient c = cliGroup.getClient(logIndex);
		if (c == null)
		{
			logger.warn("[lid:{}][{}]get nps Mosquitto information fail!", logIndex, logFlag);
			return -1;
		}
		return c.pub(logIndex, topic, content);

	}

	
	/**
	 * 向指定topic发送一条消息，需要指明待查询用户连接的是哪个mosquitto
	 * 
	 * @param serverFlag 标识待查询mosquitto的表示，计划用 "ip:端口号" 表示
	 * @param topic
	 * @param content 消息的内容
	 * @return boolean
	 * */
	public boolean sendBroadcastMsg(long logIndex, String topic, String msg)
	{
		String logFlag = getClassName() + ".sendBroadcastMsg";
		for(ClientGroup cliGroup: mqttClients.values()){
			if (cliGroup == null)
			{
				logger.warn("[lid:{}][{}]get ClientGroup information fail!", logIndex, logFlag);
				continue;
			}
			PahoMqttClient c = cliGroup.getClient(logIndex);
			if (c == null)
			{
				logger.warn("[lid:{}][{}]get Mosquitto information fail!mosquitto flag:{}", logIndex, logFlag, cliGroup.getFlag());
				continue;
			}
			if(c.pub(logIndex, topic, msg) != DefaultValues.SEND_MSG_SUC){
				logger.warn("[lid:{}][{}]get Mosquitto information fail!, mosquitto flag:{}", logIndex, logFlag, cliGroup.getFlag());
			}
		}
		return true;
	}

	public void shutdown(long logIndex)
	{
		String logFlag = "MqttClientManager.shutdown";
		Set<String> clientFlags = mqttClients.keySet();
		for (String aClientflag : clientFlags)
		{
			logger.info("[lid:{}][{}] shutdown mosquitto client: {} ", logIndex, logFlag, aClientflag);
			ClientGroup cliGroup = mqttClients.get(aClientflag);
			cliGroup.shutDown(logIndex);
		}
	}

	/**
	 * 函数名称：getMqttServerInfo 函数功能：根据用户的ID分配一个mosquitto服务器实例
	 * 
	 * @param logIndex 调用索引
	 * @return JSONObject mosquitto服务器实例的信息，JSON格式，详细见设计文档
	 * */
	public ClientGroup getMqttServerInfo(long logIndex)
	{
		String logFlag = "MqttClientManager.getMqttServer";
		int tryCounter = 1;
		ClientGroup allocatedCli = null;
		while (tryCounter < DefaultValues.MAX_ALLOC_TRY)
		{
			allocatedCli = innerGetMqttServer(logIndex);
			if (allocatedCli != null && allocatedCli.isWorking())
				break;
			logger.debug("[lid:{}][{}] cann't alloc valid client for user:{}! tryCounter={}; flag:{}; stat:{}", logIndex, logFlag, tryCounter, allocatedCli.getFlag(), allocatedCli.isWorking());
			tryCounter++;
		}
		
		if (allocatedCli == null || !allocatedCli.isWorking())
		{
			logger.debug("[lid:{}][{}] cann't alloc valid client！", logIndex, logFlag);
			return null;
		}
		else
			return allocatedCli;
	}
	
	private ClientGroup innerGetMqttServer(long logIndex)
	{
		String logFlag = getClassName() + ".innerGetMqttServer";
		ClientGroup allocatedCli = null; 
		 
		if(lb == DefaultValues.LB_BY_CON_NUM)
		{
			allocatedCli = mosqLB.getMosq(logIndex);
		}
		else if(lb == DefaultValues.LB_BY_ORDER)
		{
			int newIndex = lastAllocIndex++;
			if(newIndex > allocMap.size()){
				lastAllocIndex = DefaultValues.ALLOC_START_INDEX;
				newIndex = DefaultValues.ALLOC_START_INDEX;
			}
			allocatedCli = allocMap.get(newIndex);
		}
		
		if(allocatedCli == null)
		{//默认使用随机分配方式,索引是从1开始的，所以要+1
//			logger.debug("[lid:{}][{}] will use random alloc ! randomValue:{}", logIndex, logFlag);
			allocatedCli = allocMap.get(1 + Tools.getRandomInt(logIndex, allocMap.size()));
			logger.debug("[lid:{}][{}] random alloc ! flag:{}; stat:{}", logIndex, logFlag, allocatedCli.getFlag(), allocatedCli.isWorking());
		}
		return allocatedCli;
	}

	

	public boolean mosquittoIsWorking(long logIndex, String flag)
	{
		String logFlag = "MqttClientManager.mosquittoIsWorking";
		ClientGroup cli = mqttClients.get(flag);
		if (cli == null)
		{
			logger.warn("[lid:{}][{}] get client group failed! flag:{}", logIndex, logFlag,flag);
			return false;
		}
		return cli.isWorking();
	}
	
	public ClientGroup getClientGroup(long logIndex, String flag)
	{
		String logFlag = "MqttClientManager.getClientGroup";
		ClientGroup cli = mqttClients.get(flag);
		if (cli == null)
		{
			logger.warn("[lid:{}][{}] get client group failed! flag:{}", logIndex, logFlag,flag);
		}
		return cli;
	}
	
	//mosquitto的负载均衡类
	class MosqLB{
		class GroupRange{
			public ClientGroup cg = null;
			public int rangeLower = 0;//自己范围的下限
			public int rangeUpper = 0;//自己范围的上限
		}
		private List<GroupRange> groupRanges = null;
		private AtomicBoolean isCalcating = new AtomicBoolean(false);
		private int maxRandomNum = 0;
		//上次计算mosquitto分配概率的时间，毫秒
		private long lastMosqProbTime = 0;
		public void init(long logIndex)
		{
			calcProb(logIndex);
			maxRandomNum = groupRanges.size();
		}
		public ClientGroup getMosq(long logIndex)
		{
			//检查是否需要重新计算mosquitto的分配概率
			if(System.currentTimeMillis() - lastMosqProbTime > maxCalcProbTime)
			{
				calcProb(logIndex);
			}
			if(maxRandomNum <= 0){
//				logger.debug("<lid:{}>maxRandomNum <= 0", logIndex);
				return null;
			}
			int rand = Tools.getRandomInt(logIndex, maxRandomNum);
			//注意：有效区间范围为左闭右开:[lower,upper)
			for(GroupRange aGr : groupRanges)
			{
				if(aGr.rangeLower <= rand && rand < aGr.rangeUpper)
					return aGr.cg;
			}
			return null;
		}
		
		/**
		 * 计算各mosquitto被分配的概率
		 * */
		private void calcProb(long logIndex)
		{
			String logFlag = "MosqLB.calcProb";
			if(isCalcating.compareAndSet(false, true))
			{
				try{
					ArrayList<MqttClientManager.MosqLB.GroupRange> newGroupRanges = new ArrayList<MqttClientManager.MosqLB.GroupRange>();
					int newMaxRandomNum = 0;
					int totalUserNum = 0;
					//由于用户量越大概率越小，因此需要按照总量-每个的连接数，产生一个值，这个值对于每个mosquitto来说，连接数越大此值越大，连接数越小此值越小
					/**
					 * 例如有三个mosquitto，其连接数分布为：
					 * 	m1: 3000, m2:2000, m3:1000
					 * 计算方法为：
					 * (1)计算出来总和：m1+m2+m3 = 3000 + 2000 +1000 = 6000
					 * (2)计算每个mosquitto的反向值：总和-当前mosquitto的连接数
					 * 		m1:6000-3000=3000
					 * 		m2:6000-2000=4000
					 * 		m3:6000-1000=5000
					 * (3)计算每个mosquitto的范围和随机数的最大值：
					 * 	    m1: [0, 3000)
					 * 		m2:[3000,7000) 
					 * 		m3:[7000, 12000)
					 * 		maxRandomNum = 12000
					 * */
					for(Map.Entry<String, ClientGroup> aGroup: mqttClients.entrySet())
					{
						totalUserNum += aGroup.getValue().getMosqUserNum();
					}
					int lastUpperRanger = 0;//上一个阶段的上限
					for(Map.Entry<String, ClientGroup> aGroup: mqttClients.entrySet())
					{
						GroupRange aGr = new GroupRange();
						aGr.cg = aGroup.getValue();
						aGr.rangeLower = lastUpperRanger;
						aGr.rangeUpper = aGr.rangeLower + (totalUserNum-aGroup.getValue().getMosqUserNum());
						newMaxRandomNum = aGr.rangeUpper;
						lastUpperRanger = newMaxRandomNum;
						newGroupRanges.add(aGr);
					}
					groupRanges = newGroupRanges;
					maxRandomNum = newMaxRandomNum;
					lastMosqProbTime = System.currentTimeMillis();
				}catch(Exception ex)
				{
					logger.warn("[lid:{}][{}] exception happened! detail:{}", logIndex, logFlag, ex);	
				}finally{
					isCalcating.set(false);
				}
			}
			
		}
	}
}
