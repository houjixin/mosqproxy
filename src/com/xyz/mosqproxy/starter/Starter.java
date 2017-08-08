package com.xyz.mosqproxy.starter;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.commons.lang3.StringUtils;
import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.server.TNonblockingServer;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.server.TThreadedSelectorServer;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TNonblockingServerTransport;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TTransportException;
import org.apache.thrift.transport.TTransportFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;

import com.xyz.mosqproxy.common.DefaultValues;
import com.xyz.mosqproxy.common.MetricsTool;
import com.xyz.mosqproxy.common.Tools;
import com.xyz.mosqproxy.config.Config;
import com.xyz.mosqproxy.config.ConfigKeys;
import com.xyz.mosqproxy.exception.BasicException;
import com.xyz.mosqproxy.telnet.TelnetDaemon;
import com.xyz.mosqproxy.thrift.stub.MosqProxyStub;

public class Starter
{
	private static String getClassName()
	{
		return "Starter";
	}
	private static Logger logger = LoggerFactory.getLogger(Starter.class);
	private static final String TELNETD_CONFIG_FILE = "/com/xyz/mosqproxy/telnet/telnetd.conf";/** Telnet 模块配置文件 */
	private static MosqProxy theInstance = new MosqProxy(); /**jmx模块使用*/
	
	private static Config cfg = null;
	
	public static String VERSION;
	private static int serverMode; 
	private static String strServerMode = null;
	//Thrift server的三种工作模式：线程池，非阻塞，THREADEDSELECTOR
	private static final int SERVER_MODE_THREAD_POOL = 1;
	private static final int SERVER_MODE_NONBLOCKING = 2;
	private static final int SERVER_MODE_THREADEDSELECTOR = 3;
	
	private static TServer server = null;
	private static int thriftListenPort;
	private static String serverInfo = null;
	public static String SERVICE_NAME;//本服务的名字,在telnet输出的时候也用，因此搞成public的
	
	private static int telnetPort;
	private static int jmxWebPort;
	private static int jmxRmiPort;
	
	//以多线程方式工作时的监听线程数和工作线程数
	private static int listerThreadNum;
	private static int workerThreadNum;
	
	public static void main(String[] args)
	{
		if (!init(DefaultValues.LOG_INDEX_INIT))
		{
			logger.error("[lid:{}][{}] initialization failure...", DefaultValues.LOG_INDEX_INIT, "main", SERVICE_NAME);
			System.exit(0);
		}

		if (!start(DefaultValues.LOG_INDEX_INIT))
		{
			logger.error("[lid:{}][{}] start service:{}fail...", DefaultValues.LOG_INDEX_INIT, "main", SERVICE_NAME);
			System.exit(0);
		}

	}// end of main()

	private static boolean start(long logIndex)
	{
		serverInfo = new StringBuilder("")
				         .append("\n\n 	**********************************************")
						 .append("\n 	   	---- ").append(SERVICE_NAME).append(" ----")
						 .append("\n 	   	Version: ").append(VERSION)
						 .append("\n 	   	listen Port: ").append(thriftListenPort)
						 .append("\n 	   	telnet Port: ").append(telnetPort)
						 .append("\n 	   	server mode: ").append(strServerMode)
						 .append("\n 	**********************************************\n")
						 .toString();

		logger.info(serverInfo);
		try
		{
			server.serve();
			return true;
		} catch (Exception e)
		{
			logger.error("[lid:{}][{}] Server start fail! exception happened! detail:{}", logIndex, "start", e);
			System.exit(0);
		}
		return false;
	}

	private static boolean init(long logIndex)
	{
		String logFlag = getClassName()+".init";
		VERSION = getVersion();
		
		if(!getConfig(logIndex))
		{
			logger.error("[lid:{}][{}] get configure data fail!", logIndex, logFlag);
			return false;
		}
		
		if (!theInstance.init(DefaultValues.LOG_INDEX_INIT, cfg))
		{
			logger.error("[lid:{}][{}] theInstance init fail!", logIndex, logFlag);
			return false;
		}
		
		setMetrics(logIndex);
		
		if(!startTelnet(logIndex))
		{
			logger.error("[lid:{}][{}] Start telnet fail!", logIndex, logFlag);
			return false;
		}
		
		try{
			createServer(logIndex);
		}catch(TTransportException tex)
		{
			logger.error("[lid:{}][{}] create thrift server fail!", logIndex, logFlag);
			return false;
		}

		return true;
	}
	
	private static void createServer(long logIndex) throws TTransportException
	{
		String logFlag = getClassName() + ".createServer";
		TProcessor tProcessor = new MosqProxyStub.Processor<MosqProxyStub.Iface>(theInstance);
		switch(serverMode)
		{
			case SERVER_MODE_NONBLOCKING 		:
			{
				strServerMode = "NONBLOCKING";
				TNonblockingServerSocket tnbSocketTransport = new TNonblockingServerSocket(thriftListenPort);
				TNonblockingServer.Args tnbArgs = new TNonblockingServer.Args(tnbSocketTransport);
				tnbArgs.maxReadBufferBytes = DefaultValues.THRIFT_MAX_READ_BUF;
				tnbArgs.processor(tProcessor);
				tnbArgs.transportFactory(new TFramedTransport.Factory());
				tnbArgs.protocolFactory(new TBinaryProtocol.Factory());
				// 使用非阻塞式IO，服务端和客户端需要指定TFramedTransport数据传输的方式
				server = new TNonblockingServer(tnbArgs);
				break;
			}				
			case SERVER_MODE_THREAD_POOL 		:
				{
					strServerMode = "THREAD_POOL";
					TServerSocket serverTransport = new TServerSocket(thriftListenPort);
					TThreadPoolServer.Args threadPoolServerArgs = new TThreadPoolServer.Args(serverTransport);
					threadPoolServerArgs.processor(tProcessor);
					threadPoolServerArgs.maxWorkerThreads(workerThreadNum);
					threadPoolServerArgs.protocolFactory(new TBinaryProtocol.Factory());
					// 线程池服务模型，使用标准的阻塞式IO，预先创建一组线程处理请求。
					server = new TThreadPoolServer(threadPoolServerArgs);
					break;
				}
			case SERVER_MODE_THREADEDSELECTOR 	:
				{
					strServerMode = "THREADED_SELECTOR";
					TNonblockingServerTransport serverTransport = new TNonblockingServerSocket(thriftListenPort);
					TTransportFactory transportFactory = new TFramedTransport.Factory(); 
					TThreadedSelectorServer.Args tArgs = new TThreadedSelectorServer.Args(serverTransport);
					tArgs.maxReadBufferBytes = DefaultValues.THRIFT_MAX_READ_BUF;
					tArgs.processor(tProcessor);
					tArgs.selectorThreads(listerThreadNum).workerThreads(workerThreadNum).transportFactory(transportFactory);
					server = new TThreadedSelectorServer(tArgs);
					break;
				}
			default:
				logger.info("[lid:{}][{}] parameter for server mode fail..", logIndex, logFlag);
		}
	}

	
	
	/**
	 * 设置metrics相关信息
	 * */
	private static void setMetrics(long logIndex)
	{
		//设置metrics的输出间隔
		int logGap = cfg.getInt("metrics.logGap");
		if(logGap <= 0)
			logGap = DefaultValues.METRICS_LOGGAP;
		MetricsTool.setLogGap(logGap);
		//根据配置文件确定metrics是否进行输出		
		if(cfg.getInt(ConfigKeys.METRICS_START) != MetricsTool.STOP)
			MetricsTool.start();
		else
			MetricsTool.stop();
	}
	
	private static boolean getConfig(long logIndex)
	{
		String logFlag = getClassName()+".getConfig";
		//加载系统配置文件，系统配置文件中保存了各具体配置文件的路径
		cfg = new Config();
		if(!cfg.loadConfig(DefaultValues.FILE_PATH_SYSTEM))
		{
			logger.error("[lid:{}][{}] Loading configure data from file:{} fail!", DefaultValues.LOG_INDEX_INIT, logFlag, DefaultValues.FILE_PATH_SYSTEM);
			return false;
		}
		logger.info("[lid:{}][{}] Loading configure data from sytem config file:{} success!", DefaultValues.LOG_INDEX_INIT, logFlag, DefaultValues.FILE_PATH_SYSTEM);
		//加载logback的配置文件并设置logback
		String logbackCfgFile = cfg.getString(ConfigKeys.FILE_PAHT_LOGBACK);
		if(StringUtils.isBlank(logbackCfgFile) || !setLogbackCfg(logIndex, logbackCfgFile))
		{
			logger.error("[lid:{}][{}] Setting configure data for logback fail! configure file path: {} ", DefaultValues.LOG_INDEX_INIT, logFlag, logbackCfgFile);
			return false;
		}
		logger.info("[lid:{}][{}] Loading configure data from logback configure file:{} success!", DefaultValues.LOG_INDEX_INIT, logFlag, logbackCfgFile);
		//加载本服务所依赖的数据源的配置文件
		String dataSourceCfgFile = cfg.getString(ConfigKeys.FILE_PAHT_DATASOURCE);
		if(StringUtils.isBlank(dataSourceCfgFile) || !cfg.loadConfig(dataSourceCfgFile, true))
		{
			logger.error("[lid:{}][{}] Loading configure data from file:{} fail!", DefaultValues.LOG_INDEX_INIT, logFlag, dataSourceCfgFile);
			return false;
		}
		logger.info("[lid:{}][{}] Loading configure data from DataSource configure file:{} success!", DefaultValues.LOG_INDEX_INIT, logFlag, dataSourceCfgFile);
		//加载依赖服务的配置文件
		String dependenceFile = cfg.getString(ConfigKeys.FILE_PAHT_DEPENDENCE);
		if(StringUtils.isBlank(dependenceFile) || !cfg.loadConfig(dependenceFile, true))
		{
			logger.error("[lid:{}][{}] Loading configure data from file:{} fail!", DefaultValues.LOG_INDEX_INIT, logFlag, dependenceFile);
			return false;
		}
		logger.info("[lid:{}][{}] Loading configure data from dependence configure file:{} success!", DefaultValues.LOG_INDEX_INIT, logFlag, dependenceFile);
		//加载依赖服务的配置文件
		String selfCfgFile = cfg.getString(ConfigKeys.FILE_PAHT_SELF_CFG);
		if(StringUtils.isBlank(selfCfgFile) || !cfg.loadConfig(selfCfgFile, true))
		{
			logger.error("[lid:{}][{}] Loading configure data from file:{} fail!", DefaultValues.LOG_INDEX_INIT, logFlag, selfCfgFile);
			return false;
		}
		logger.info("[lid:{}][{}] Loading configure data from service configure file:{} success!", DefaultValues.LOG_INDEX_INIT, logFlag, selfCfgFile);		
		thriftListenPort = cfg.getInt(ConfigKeys.THRIFT_LISTEN_PORT);
		if (!Tools.checkPort(thriftListenPort))
		{
			logger.error("[lid:{}][{}] thrift listen port error! port:{}", logIndex, logFlag, thriftListenPort);
			return false;
		}
		
		SERVICE_NAME = cfg.getString(ConfigKeys.SERVICE_NAME);
		if (StringUtils.isBlank(SERVICE_NAME))
		{
			logger.error("[lid:{}][{}] service name error! key:{}", logIndex, logFlag, ConfigKeys.SERVICE_NAME);
			return false;
		}
		
		String hostName = getHostName(logIndex);
		if (StringUtils.isBlank(hostName))
		{
			logger.error("[lid:{}][{}] get hostname fail!", logIndex, logFlag);
			return false;
		}
		SERVICE_NAME = SERVICE_NAME + "-" + hostName;

		
		telnetPort= cfg.getInt(ConfigKeys.TELNET_PORT);
		if (!Tools.checkPort(telnetPort))
		{
			logger.error("[lid:{}][{}] telnet port error! port:{}", logIndex, logFlag, telnetPort);
			return false;
		}
		
		jmxWebPort= cfg.getInt(ConfigKeys.JMX_WEB_PORT);
		jmxRmiPort= cfg.getInt(ConfigKeys.JMX_RMI_PORT);
		if (!Tools.checkPort(jmxWebPort)||!Tools.checkPort(jmxRmiPort))
		{
			logger.error("[lid:{}][{}] jmx port error! web port:{} rmi port: {}", logIndex, logFlag, jmxWebPort, jmxRmiPort);
			return false;
		}
		
		listerThreadNum =  cfg.getInt(ConfigKeys.THRIFT_THREAD_NUM_LISTEN);
		if(listerThreadNum <= 0)
			listerThreadNum = DefaultValues.THRIFT_THREAD_NUM_LISTENER;
		workerThreadNum =  cfg.getInt(ConfigKeys.THRIFT_THREAD_NUM_WORKER);
		if(workerThreadNum <= 0)
			workerThreadNum = DefaultValues.THRIFT_THREAD_NUM_WORKER;
		
		serverMode =  cfg.getInt(ConfigKeys.THRIFT_SERVER_MODE);
		return true;
	}

	/**
	 * 获取内部版本号
	 * */
	private static String getVersion()
	{
		return Version.VERSION;
	}
	/**
	 * 启动 telnet 服务
	 * */ 
	private static boolean startTelnet(long logIndex)
	{

		String logFlag = getClassName()+".startTelnet";
		try
		{
			TelnetDaemon.getInstance().startTelnetDaemon(telnetPort, Starter.class.getResourceAsStream(TELNETD_CONFIG_FILE));
		}
		catch (Exception e)
		{
			logger.warn("[lid:{}][{}] Start telnet fail,telnetPort:{},detail:{}", logIndex, logFlag, telnetPort,e);
			return false;
		}
		return true;
	}
	
	
	public static void shutdown(long logIndex)
	{
		String logFlag = getClassName() + "shutdown";
		logger.info("[lid:{}][{}] Will Shutdown", logIndex, logFlag);
		try{
			theInstance.shutdown(logIndex);
			TelnetDaemon.getInstance().stopTelnetDaemon();
		}catch(Exception ex)
		{
			logger.warn("[lid:{}][{}] exception happened! detail {}", logIndex, logFlag, ex);
		}
		finally{
			System.exit(0);
		}
	}

	public static String getInfo(long logIndex)
	{
		return serverInfo;
	}
	/**
	 * 获取本机hostname
	 * @param logIndex
	 * @return 发生异常返回null
	 */
	public static String getHostName(long logIndex)
	{
		String logFlag = getClassName() + "getHostName";
		try
		{
			return InetAddress.getLocalHost().getHostName();
		}
		catch (UnknownHostException e)
		{
			logger.warn("[lid:{}][{}] exception happened! detail {}", logIndex, logFlag, e);
			return null;
		}
	}
	/**
	* 设置读取logback.xml文件的路径
	* @param logIndex  日志索引
	* @param path      文件路径
	* @return 是否成功
	*/
	private static boolean setLogbackCfg(long logIndex, String path)
	{
	   String logFlag = getClassName() + ".setLogbackCfg";
	   File file = new File(path);
	   if (!file.exists())
	   {
	      return false;
	   }

	   LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
	   JoranConfigurator joranConfigurator = new JoranConfigurator();
	   joranConfigurator.setContext(lc);
	   lc.reset();
	   try
	   {
	      joranConfigurator.doConfigure(file);
	   }
	   catch (JoranException e)
	   {
	      logger.error("[lid:{}][{}]<< exception happened! detail:\n{}", logIndex, logFlag, BasicException.getStackTrace(e));
	      return false;
	   }
	   return true;
	}
}
