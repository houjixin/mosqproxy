package com.xyz.mosqproxy.config;

public class ConfigKeys
{
	//各配置文件路径在系统配置文件中的key
	public static final String FILE_PAHT_DATASOURCE = "datasource.properties.path";
	public static final String FILE_PAHT_DEPENDENCE = "dependence.properties.path";
	public static final String FILE_PAHT_SELF_CFG = "service.properties.path";
	public static final String FILE_PAHT_LOGBACK = "logback.path";
	//配置文件里面用到的配置参数的key值
	public static String THRIFT_LISTEN_PORT = "thrift.listen.port";
	public static String THRIFT_SERVER_MODE = "thrift.server.mode";	
	public static String THRIFT_THREAD_NUM_LISTEN = "thrift.threadNum.listen";
	public static String THRIFT_THREAD_NUM_WORKER = "thrift.threadNum.worker";
	
	//metrics相关配置参数的key
	public static String METRICS_START = "metrics.start";
	public static String METRICS_LOGGAP = "metrics.logGap";
	
	public static String MOSQ_SYS_TOPIC = "mosq.topic.system";
	//JMX相关配置的参数的key
	public static String JMX_WEB_PORT = "jmx.web.port";
	public static String JMX_RMI_PORT = "jmx.rmi.port";
	public static String JMX_RMI_URL_PREX = "jmx.rmi.url.prefix";
		
	//telnet相关配置的参数的key
	public static String TELNET_PORT = "telnet.port";
	public static final String SERVICE_NAME = "service.name";
	//配置信息的加载方式，例如从zookeeper还是从配置文件中读取
	public static final String BASE_CONFIG_SOURCE = "baseconfig.source";
	public static final String MQTT_SERVER_NUM = "mqtt.server.number";
	public static final String MQTT_SERVER_KEEPALIVE = "mqtt.server.keepalive";
	public static final String MQTT_SERVER_WORKTIMEOUT = "mqtt.server.worktimeout";
	public static final String MQTT_SERVER_INITTIMEOUT = "mqtt.server.inittimeout";
	public static final String PRIFIX_MQTT_SERVER = "mqtt.server";
	public static final String MQTT_SERVER_FOREIGN_INFO = "mqtt.server";

	public static final String MOSQ_TOPIC_ONLINE = "mosq.topic.online";
	public static final String MOSQ_TOPIC_OFFLINE = "mosq.topic.offline";
	public static final String MOSQ_TOPIC_ACTIVE_NUM = "mosq.topic.active.num";

	public static final String CONN_TO_MOSQUITTO_NUM = "connection.to.mosquito.Num";
	public static final String LB_MOSQ = "mqtt.server.lb";
	public static final String CALC_LB_INTERVAL = "mqtt.server.lb.interval";
	
	public static final String PREFIX_SHARDING = "sharding";
	public static final String SHARDING_NUM = "sharding.num";
	public static final String SHARDING_MAX_SHARDING_VALUE = "sharding.maxShardingValue";
	
	public static final String SHARDING_CACHE_EXPIRETIME_ENALBE = "sharding.cache.expiretime.enable";
	public static final String SHARDING_CACHE_EXPIRETIME = "sharding.cache.expiretime";
	public static final String SHARDING_ENABLEDB = "sharding.enableDb";
	
	public static final String SUFFIX_SHARDING_RANGE_START = ".shardingRangeStart";
	public static final String SUFFIX_SHARDING_RANGE_END = ".shardingRangeEnd";
	//cache的相关配置项的前后缀
	public static final String SUFFIX_CACHE_HOST = ".cache.host";
	public static final String SUFFIX_CACHE_PORT = ".cache.port";
	public static final String SUFFIX_CACHE_USER_NAME = ".cache.host.userName";
	public static final String SUFFIX_CACHE_PWD = ".cache.host.password";
	public static final String SUFFIX_CACHE_POOL_MAX_ACTIVE = ".cache.pool.maxActive";
	public static final String SUFFIX_CACHE_POOL_MAX_IDLE = ".cache.pool.maxIdle";
	public static final String SUFFIX_CACHE_POOL_MAX_WAIT = ".cache.pool.maxwait";
	//读数据库相关配置项的前后缀
	public static final String SUFFIX_DB_READ_HOST = ".db.read.host";
	public static final String SUFFIX_DB_READ_PORT = ".db.read.port";
	public static final String SUFFIX_DB_READ_DB_NAME = ".db.read.host.dbName";
	public static final String SUFFIX_DB_READ_USERNAME = ".db.read.host.userName";
	public static final String SUFFIX_DB_READ_PWD = ".db.read.host.password";
	public static final String SUFFIX_DB_READ_POOL_MIN_SIZE = ".db.read.pool.minPoolSize";
	public static final String SUFFIX_DB_READ_POOL_MAX_SIZE = ".db.read.pool.maxPoolSize";
	public static final String SUFFIX_DB_READ_MAX_IDLETIME = ".db.read.pool.maxIdleTime";
	public static final String SUFFIX_DB_READ_MAX_STATEMENTS = ".db.read.pool.maxStatements";
	public static final String SUFFIX_DB_READ_CHECKOUT_TIMEOUT = ".db.read.pool.checkoutTimeout";
	//写数据库相关配置项的前后缀
	public static final String SUFFIX_DB_WRITE_HOST = ".db.write.host";
	public static final String SUFFIX_DB_WRITE_PORT = ".db.write.port";
	public static final String SUFFIX_DB_WRITE_DB_NAME = ".db.write.host.dbName";
	public static final String SUFFIX_DB_WRITE_USERNAME = ".db.write.host.userName";
	public static final String SUFFIX_DB_WRITE_PWD = ".db.write.host.password";
	public static final String SUFFIX_DB_WRITE_POOL_MIN_SIZE = ".db.write.pool.minPoolSize";
	public static final String SUFFIX_DB_WRITE_POOL_MAX_SIZE = ".db.write.pool.maxPoolSize";
	public static final String SUFFIX_DB_WRITE_MAX_IDLETIME = ".db.write.pool.maxIdleTime";
	public static final String SUFFIX_DB_WRITE_MAX_STATEMENTS = ".db.write.pool.maxStatements";
	public static final String SUFFIX_DB_WRITE_CHECKOUT_TIMEOUT = ".db.write.pool.checkoutTimeout";
	
	//加密相关配置的key
	public static final String ENCRYPT_ENABLE = "encrypt.enable";
	public static final String ENCRYPT_KEY_LEN = "encrypt.kenLen";
	public static final String ENCRYPT_TYPE = "encrypt.type";
	public static final String SESSION_LEN = "session.len";
	
}
