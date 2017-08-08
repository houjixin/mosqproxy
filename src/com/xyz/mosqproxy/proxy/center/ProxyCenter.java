package com.xyz.mosqproxy.proxy.center;

import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;
import com.xyz.mosqproxy.common.DefaultValues;
import com.xyz.mosqproxy.common.ReturnCode;
import com.xyz.mosqproxy.common.Tools;
import com.xyz.mosqproxy.config.Config;
import com.xyz.mosqproxy.config.ConfigKeys;
import com.xyz.mosqproxy.datatype.ClientStatus;
import com.xyz.mosqproxy.datatype.ConnectionInfo;
import com.xyz.mosqproxy.datatype.RegResult;
import com.xyz.mosqproxy.datatype.UnbindMsg;
import com.xyz.mosqproxy.proxy.center.datacenter.DataCenter;
import com.xyz.mosqproxy.proxy.center.mqttclient.ClientGroup;
import com.xyz.mosqproxy.proxy.center.mqttclient.MqttClientManager;
import com.xyz.mosqproxy.starter.UpdateThread;

public class ProxyCenter
{
	private static Logger logger = LoggerFactory.getLogger(ProxyCenter.class);
	private static ProxyCenter proxyCenter = new ProxyCenter();
	private ProxyCenter(){};
	private MqttClientManager mqttClientManager = MqttClientManager.getInstance();
	private DataCenter datacenter = new DataCenter();
	private boolean encryptEnable = false;
	private int encryptKeyLen = 0;
	private int encryptType = 0;
	private int sessionLen = 0;
	private String systemTopic;
	private LinkedBlockingQueue<String> msgQueue = new LinkedBlockingQueue<String>();
	private UpdateThread updateThread = null;
	private boolean terminateUpThread = false;
	public void queMsg(String msg){
		if(!StringUtils.isBlank(msg))
			msgQueue.offer(msg);
		
	}
	public boolean isTerminateUpThread()
	{
		return terminateUpThread;
	}

	private String getClassName()
	{
		return "ProxyCenter";
	}

	public static ProxyCenter getInstance()
	{
		return proxyCenter;
	}

	public boolean init(long logIndex, Config cfg)
	{
		String logFlag = getClassName() + ".init";
		if (!mqttClientManager.init(logIndex, cfg))
		{
			logger.error("[lid:{}][{}] init MqttClientManager fail!", logIndex, logFlag);
			return false;
		}
		if (!datacenter.init(logIndex, cfg))
		{
			logger.error("[lid:{}][{}] init DataCenter fail!", logIndex, logFlag);
			return false;
		}

		systemTopic = cfg.getString(ConfigKeys.MOSQ_SYS_TOPIC);
		if(StringUtils.isBlank(systemTopic)){
			logger.error("[lid:{}][{}] configure data error! {} = null!", logIndex, logFlag, ConfigKeys.MOSQ_SYS_TOPIC);
			return false;
		}
		encryptEnable = false;
		if(cfg.getInt(ConfigKeys.ENCRYPT_ENABLE) == DefaultValues.ENCRY_ENABLE){
			encryptEnable = true;
		}

		if(encryptEnable){
			encryptKeyLen = cfg.getInt(ConfigKeys.ENCRYPT_KEY_LEN);
			if (encryptKeyLen <= 0)
			{
				logger.error("[lid:{}][{}] configure data error! {} <=0", logIndex, logFlag, ConfigKeys.ENCRYPT_KEY_LEN);
				return false;
			}
			encryptKeyLen = encryptKeyLen/ 8 + 1;// 转换位数到Byte,便于随机生成Byte
			encryptType = cfg.getInt(ConfigKeys.ENCRYPT_TYPE);
			if(encryptType != DefaultValues.ENCRY_TYPE_NOMAL){
				logger.error("[lid:{}][{}] Unsupported encrypt type! we only support type:1, your parameter:{} = {}", logIndex, logFlag, ConfigKeys.ENCRYPT_KEY_LEN, encryptType);
				return false;
			}
		}
		sessionLen = cfg.getInt(ConfigKeys.SESSION_LEN);
		if(sessionLen <= 0){
			logger.warn("[lid:{}][{}] Cann't get session len from configure file! we will use default value:{}, please check your configure key{}", logIndex, logFlag, DefaultValues.SESSION_LEN, ConfigKeys.SESSION_LEN);
			sessionLen = DefaultValues.SESSION_LEN;
		}


		updateThread = new UpdateThread(logIndex);
		Thread t = new Thread(updateThread);
		t.start();
		return true;
	}

	/**
	 * 函数名称：getOnlineStatus
	 * 函数功能：查询指定用户是否在线
	 * @param logIndex 日志索引
	 * @param appid 应用标识
	 *
	 * @return ClientStatus
	 * */
	public ClientStatus getOnlineStatus(long logIndex, String appid, String userid, String devid) throws Exception{
		//String logFlag = getClassName() + "getOnlineStatus";
		ClientStatus clientStatus = new ClientStatus();
		clientStatus.setOnline(datacenter.getOnlineStatus(logIndex, appid, userid, devid));
		return clientStatus;
	}
	/**
	 * 绑定appId、userId、devId到一个已经存在且有效的connId上，
	 * 如果所提供的连接时无效的，则返回码为：CONN_INVALID，
	 * */
	public RegResult forceBindConnId(long logIndex, String appId, String userId, String devId, String connId) throws Exception {
		String logFlag = getClassName() + ".forceBindConnId";
		if (StringUtils.isBlank(connId)){
			return new RegResult(ReturnCode.PARAMETER_ERROR, null);
		}
		//传入的connId非空，说明这是绑定操作，即将appid.userid的devid绑定到已经存在的connid
		ConnectionInfo conInfo = datacenter.getConnectionInfo(logIndex,connId);
		if(conInfo.isValid()){
			//告知原来那个连接它被踢下来了，后续它将收不到mosquitto的通知了；
			UnbindMsg unbingMsg = UnbindMsg.Create(appId, userId, devId, null);
			if(unbingMsg != null && unbingMsg.isValid()){
				if(!sendMsgToConn(logIndex, conInfo, systemTopic, unbingMsg.toJsonString())){
					logger.warn("[lid:{}][{}] Sending unbing message fail!, appId:{}, userId:{}, devId:{}, Conn:{}", logIndex, logFlag, appId, userId, devId, conInfo);	
				}
			}
			if(!datacenter.bindConnId(logIndex, appId, userId, devId, connId)){
				logger.warn("[lid:{}][{}] save bind information into redis fail!, appId:{}, userId:{}, devId:{}, newConn:{}", logIndex, logFlag, appId, userId, devId, conInfo);
				return new RegResult(ReturnCode.INNER_ERROR, null);
			}
			return new RegResult(ReturnCode.SUCCESS, conInfo);
		}else{
			logger.warn("[lid:{}][{}] Cann't bing client to an invalid connection:{}", logIndex, logFlag, conInfo);
			return new RegResult(ReturnCode.CONN_INVALID, null);
		}
	}
	/**
	 * 绑定是需要确定当前设备是不是已经绑定到了某个连接上，包括当前这个连接上，
	 * 如果绑定到了别的连接上，需要给用户提示:BIND_EXIST，不可私自强制绑定；
	 * 如果绑定到了当前的连接上，直接返回成功；
	 * */
	public RegResult bindConnId(long logIndex, String appId, String userId, String devId, String connId) throws Exception {
		String logFlag = getClassName() + ".bindConnId";
		if (StringUtils.isBlank(connId)){
			return new RegResult(ReturnCode.PARAMETER_ERROR, null);
		}

		ConnectionInfo conInfo = datacenter.getConnectionInfo(logIndex,connId);
		if(conInfo.isValid()&& mqttClientManager.mosquittoIsWorking(logIndex, conInfo.getMosqFlag())){
			logger.warn("[lid:{}][{}] client has been band to connection:{}", logIndex, logFlag, conInfo);
			return new RegResult(ReturnCode.BIND_EXIST, null);
		}else{
			if(!datacenter.bindConnId(logIndex, appId, userId, devId, connId)){
				logger.warn("[lid:{}][{}] save bing informationinto redis fail!, appId:{}, userId:{}, devId:{}, newConn:{}", logIndex, logFlag, appId, userId, devId, conInfo);
				return new RegResult(ReturnCode.INNER_ERROR, null);
			}
			return new RegResult(ReturnCode.SUCCESS, conInfo);
		}
	}
	/**
	 * 为某个appId的某个用户userId的某个设备devId创建一个新连接，
	 * 需要先判断该appid的userId的devId是不是已经绑定到了一个已存在并且有效的连接，
	 * 如果是，则返回：BIND_EXIST，
	 * 否则，为之创建一个新连接，并完成绑定；
	 * */
	public RegResult regConnection(long logIndex, String appId, String userId, String devId, boolean forceReg) throws Exception{
		String logFlag = getClassName() + ".regConnection";
		//1.传入的连接Id为空，先检查这个appId.userId.devId是否已绑定到了某个连接上？
		ConnectionInfo conInfo = datacenter.getConnectionInfo(logIndex,appId, userId, devId);
		if (conInfo.isValid() && mqttClientManager.mosquittoIsWorking(logIndex, conInfo.getMosqFlag())){
			if(forceReg){
				/*强制踢另一个已经在线的用户下线时，给对方发送一个通知，
				 * 告知原来那个连接它被踢下来了，后续它将收不到mosquitto的通知了；
				 * */
				UnbindMsg unbingMsg = UnbindMsg.Create(appId, userId, devId, null);
				if(unbingMsg != null && unbingMsg.isValid()){
					if(!sendMsgToConn(logIndex, conInfo, systemTopic, unbingMsg.toJsonString())){
						logger.warn("[lid:{}][{}] Sending unbing message fail!, appId:{}, userId:{}, devId:{}, Conn:{}", logIndex, logFlag, appId, userId, devId, conInfo);	
					}
				}
			}else{
				/*
				*该 应用下 该用户 的 当前设备 已被绑定到了其他连接上，可以理解为该用户已在其他同类型设备上登录了，
				* 这种情况交由用户来决定是否要踢掉原来的登录，如果要踢掉则直接执行forceBindConnId接口即可。
				*/
				logger.warn("[lid:{}][{}] client has been band to connection:{}", logIndex, logFlag, conInfo);
				return new RegResult(ReturnCode.BIND_EXIST, null);

			}
		}
		//2.创建新的连接ID，并将当前appId的用户userId的devId绑定到这个连接ID上
		ConnectionInfo newConn = createNewConnection(logIndex);
		if (newConn == null){
			logger.warn("[lid:{}][{}] create new connection information fail! appId:{}, userId:{}, devId:{}", logIndex, logFlag, appId, userId, devId);
			return new RegResult(ReturnCode.INNER_ERROR, null);
		}

		if(!datacenter.bindConnId(logIndex, appId, userId, devId, newConn.getConnId())){
			logger.warn("[lid:{}][{}] save bing informationinto redis fail!, appId:{}, userId:{}, devId:{}, newConn:{}", logIndex, logFlag, appId, userId, devId, newConn);
			return new RegResult(ReturnCode.INNER_ERROR, null);
		}
		return new RegResult(ReturnCode.SUCCESS, newConn);
	}


	/**
	 * 创建一个新的连接，并将连接信息存入缓存
	 * */
	public ConnectionInfo createNewConnection(long logIndex)
	{
		String logFlag = getClassName() + ".createNewConnection";
		ConnectionInfo connInfo = new ConnectionInfo();
		connInfo.setConnId(String.valueOf(Tools.getId()));
		connInfo.setStatus(DefaultValues.CONN_STATUS_ACQUIRED);
		if (encryptEnable){
			connInfo.setEncriptyType(String.valueOf(encryptType));
			connInfo.setKey(Tools.getRandomString(encryptKeyLen));
		}else{
			connInfo.setEncriptyType(String.valueOf(DefaultValues.ENCRY_TYPE_NO));
		}
		connInfo.setSession(Tools.getRandomString(sessionLen));
		ClientGroup newCG = mqttClientManager.getMqttServerInfo(logIndex);
		connInfo.setInnerHost(newCG.getInnerHost());
		connInfo.setInnerPort(newCG.getInnerPort());
		connInfo.setOutHost(newCG.getForeignHost());
		connInfo.setOutPort(newCG.getForeignPort());
		connInfo.setMosqFlag(newCG.getFlag());
		if (connInfo.isValid()){
			if(!datacenter.saveConnInfo(logIndex, connInfo)){
				logger.warn("[lid:{}][{}] save connection information into redis fail!, connection infomaiton:{}", logIndex, logFlag, connInfo);
				return null;
			}
			return connInfo;
		}
		return null;
	}


	/**
	 * 函数名称：sendMsgToUser 
	 * 函数功能：给指定用户下的全部在线设备发送消息
	 * @param  logIndex 日志索引
	 * @param  topic 目标topic,这里的topic就是用户订阅的主体，一般都是用户的标识或用户标识相关的东西
	 * @param  msg 被发送的通知内容
	 * @return boolean 发送成功返回true，发送失败返回false
	 * */
	public boolean sendMsgToUser(long logIndex, String appId, String userId, String topic, String msg) throws Exception
	{
//		String logFlag = getClassName() + ".sendMsgToUser";
//		String userKey = new StringBuilder(DefaultValues.REDIS_PRIFIX).append(appId).append(userId).toString();
		String realTopic = new StringBuilder(appId).append("/")
		.append(userId).append("/")
		.append(topic)
		.toString();
		Map<String, ConnectionInfo> allConnInfo = datacenter.getAllConnectionInfo(logIndex, appId, userId);
		for (ConnectionInfo conInfo: allConnInfo.values()) {
			if (conInfo.isValid() && mqttClientManager.mosquittoIsWorking(logIndex, conInfo.getMosqFlag())) {
				if (DefaultValues.ENCRY_TYPE_NOMAL == Integer.parseInt(conInfo.getEncriptyType())) {
					return (mqttClientManager.pub(logIndex, conInfo.getMosqFlag(), realTopic, Tools.encrypt(msg, conInfo.getKey())) != DefaultValues.RES_ERROR);
				} else {
					return (mqttClientManager.pub(logIndex, conInfo.getMosqFlag(), realTopic, msg) != DefaultValues.RES_ERROR);
				}
			}
		}
		return true;
	}
	
	public boolean sendMsgToDevType(long logIndex, String appId, String userId, String devType, String topic, String msg) throws Exception
	{
		String realTopic = new StringBuilder(appId).append("/")
	        	.append(userId).append("/")
	        	.append(devType).append("/")
	        	.append(topic)
	        	.toString();
		
		ConnectionInfo conInfo = datacenter.getConnectionInfo(logIndex, appId, userId, devType);
		return sendMsgToConn(logIndex, conInfo, realTopic, msg);
	}
	
		
	/*
	 * 向指定的某个mosquitto(对应了这里的一个连接)的某个主题上发送消息
	 * */
	private boolean sendMsgToConn(long logIndex, ConnectionInfo conInfo, String topic, String msg) throws Exception
	{
//		String logFlag = getClassName() + ".sendMsgToConn";
		if(conInfo == null){
			return false;
		}
		if (conInfo.isValid() && mqttClientManager.mosquittoIsWorking(logIndex, conInfo.getMosqFlag())) {
			if (DefaultValues.ENCRY_TYPE_NOMAL == Integer.parseInt(conInfo.getEncriptyType())) {
				return (mqttClientManager.pub(logIndex, conInfo.getMosqFlag(), topic, Tools.encrypt(msg, conInfo.getKey())) != DefaultValues.RES_ERROR);
			} else {
				return (mqttClientManager.pub(logIndex, conInfo.getMosqFlag(), topic, msg) != DefaultValues.RES_ERROR);
			}
		}
		return true;
	}
	
	/**
	 * 函数名称：sendBroadcastMsg 
	 * 函数功能：向指定的appId下的所有订阅了主题topic的客户端广播消息
	 * 
	 * @param  logIndex 日志索引
	 * @param  appId
	 * @param  topic 目标topic,这里的topic就是用户订阅的主体
	 * @param  msg 被发送的通知内容
	 * */
	public boolean sendBroadcastMsg(long logIndex, String appId, String topic, String msg) throws Exception
	{
		String realTopic = new StringBuilder(appId)
		.append("/")
		.append(topic)
		.toString();
		return mqttClientManager.sendBroadcastMsg(logIndex, realTopic, msg);
	}

	public void shutdown(long logIndex)
	{
		mqttClientManager.shutdown(logIndex);
		datacenter.shutdown(logIndex);
		terminateUpThread = true;
	}


	/***
	 * 函数名称：updateUserOnlineStatus
	 * 函数功能：ClientGroup的成员变量listenerClient的回调函数，根据mosq的回馈信息来设置redis里的用户在线状态
	 * 
	 * @param logIndex 日志索引
	 * @return 执行是否成功
	 */
	public void updateUserOnlineStatus(long logIndex) throws Exception
	{
		String logFlag = getClassName() + ".updateUserOnlineStatus";
		String msg = msgQueue.poll(200, TimeUnit.MILLISECONDS);
		if(StringUtils.isBlank(msg))
			return;
		JSONObject joMsg = Tools.str2Json(logIndex, msg);
		if (null == joMsg)
		{
			logger.warn("[lid:{}][{}] msg={} from listenerClient is not a json object", logIndex, logFlag, msg);
			return ;
		}
		int nType = joMsg.getIntValue("type");
		String connId = joMsg.getString("client");
		//long lTimeStamp = joMsg.getLongValue("timestamp");
		String connStatus;
		if (DefaultValues.MQTT_LOGIN_NOTI == nType)
		{
			connStatus = DefaultValues.CONN_STATUS_ONLINE;
		}
		else if (DefaultValues.MQTT_LOGOUT_NOTI == nType)
		{
			connStatus = DefaultValues.CONN_STATUS_OFFLINE;
		}
		else
		{
			logger.warn("[lid:{}][{}] nType={} on/off line notification is illegal", logIndex, logFlag, nType);
			return ;
		}
		// 根据user从redis读取user对应的joMosqFlag
		ConnectionInfo connInfo = datacenter.getConnectionInfo(logIndex, connId);
		if (StringUtils.isBlank(connInfo.getConnId()))
		{
			logger.warn("[lid:{}][{}] Cann't get connection information with id:", logIndex, logFlag, connId);
			return ;
		}
		connInfo.setStatus(connStatus);
		datacenter.saveConnInfo(logIndex, connInfo);
	}

}
