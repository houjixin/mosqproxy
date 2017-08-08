package com.xyz.mosqproxy.common;

public class DefaultValues
{
	//系统配置文件路径
	public static final String FILE_PATH_SYSTEM = "conf/system.properties";
	public static final int METRICS_LOGGAP = 10000;//metrics的输出间隔
	//默认的两个日志索引
	public static final long LOG_INDEX_INIT = 0;//该日志索引保留，仅作为初始时使用
	public static final long LOG_INDEX_TEST = -1;//自测时使用

	public static final int THRIFT_THREAD_NUM_LISTENER = 10;//thrift监听线程数
	public static final int THRIFT_THREAD_NUM_WORKER = 50;//thrift工作线程数
	
	public static final long RES_ERROR = -1;//错误的返回结果
	public static final long ID_ERROR = -1;//错误的ID
	public static final int SOCKET_TIMEOUT = 1000;//默认的socket超时时间，单位：毫秒
	public static final int MQTT_KEEPALIVE = 60;//mosquitt默认的keepalive超时时间,单位：秒
	public static final int WORK_TIMEOUT = 3000;//业务请求的最大延时，单位：毫秒
	public static final int MQTT_CLIENT_INIT_TIMEOUT = 5000;//mosquitto客户端的连接初始化超时时间，单位：毫秒
	
	//用户状态
	public static final String CONN_STATUS_ONLINE = "1";//连接在线
	public static final String CONN_STATUS_OFFLINE = "2";//连接不在线
	public static final String CONN_STATUS_ACQUIRED = "0";//已分配，刚刚获得mosquitto server，还没建立tcpip连接
	public static final String CONN_STATUS_ERROR = "-1";//查询用户在线状态错误
	
	public static final int MQTT_LOGOUT_NOTI = 0;//下线通知
	public static final int MQTT_LOGIN_NOTI = 1;//上线通知

	public static final int MQTT_DEFAULT_QOS = 0;//默认qos等级
	public static final int SETTING_DISABLE = 0;

	public static final int ETEMNUMPERID = 3;// ID类型的配置信息中项数
	public static final int BASE_CONFIG_SOURCE_File = 0;// 从配置文件中读取配置信息

	public static final int MQTT_RETRY_DELAY = 1000;  // 1秒
	
	public static final long THRIFT_MAX_READ_BUF = 16384000L;//thrift 内部申请处理socket buffer数据缓冲区大小
	
	public static final int CALC_LB_INTERVAL = 60;//默认的计算负载均衡的间隔时间，单位秒
	//连接mosquitto时需要提供clientid，下面这两个值用于随机产生这个clientid时使用的默认值
	public static final int CLIENT_ID_MAX_RANDOM = 100000000;//产生clientid时的随机值
	public static final String CLIENT_ID_PREFIX = "mosqProxy-";
	public static final int BYTE_LEN_1= 1;//字节长度1
	public static final String MOSQ_USR= "xyz";//默认mosquitto的用户名
	public static final String MOSQ_PWD= "xyz";//默认mosquitto的密码
	//mosquitto分配算法
	public static final int LB_BY_RANDOM = 1;//随机分配
	public static final int LB_BY_ORDER = 2;//顺序分配
	public static final int LB_BY_CON_NUM = 3;//按照mosquitto的连接数
	
	public static final int ALLOC_START_INDEX = 0;//顺序分配mosquitto时的起始分配索引
	
	public static final int RECONNECT_INTERVAL = 1000;//mosquitto重连间隔，默认1秒
	public static final int INVALID_PORT = -1;//无效的端口

	public static final int RANDOM_START_SERIAL_ID = 888888;
	public static final String KEY_BASE = "abcdefghijklmnopqrstuvwxyz0123456789?!@#$%^&*()_+-=";//用于生成随机字符串用于加密
	public static final int SAVE_DB_VALUE = 1;//1 表示需要写入数据库
	public static final int SETTING_ENABLE = 1;//1 表示开关打开
	public static final int MAX_ALLOC_TRY = 10;// 在为用户分配mosquitto的时最多尝试10次
	public static final int MAX_RANDOM = 100000;
	
	public static final String NOT_USE_KEY = "-1";
	public static final String REDIS_PRIFIX = "mp.";
	public static final String CON_ID_KEY_PRIFIX = "mp.cid";
	public static final String CON_INFO_STATUS = "st";
	public static final String CON_INFO_KEY = "k";
	public static final String CON_INFO_ENCRY_TYPE = "en_t";
	public static final String CON_INFO_INNER_HOST = "hi";
	public static final String CON_INFO_INNER_PORT = "pi";
	public static final String CON_INFO_OUT_HOST = "ho";
	public static final String CON_INFO_OUT_PORT = "po";
	public static final String CON_INFO_SESSION = "ss";
	public static final String CON_INFO_MOSQ_FLAG = "mf";
	public static final String RETURN_CODE = "code";
	public static final String RETURN_VALUE = "value";

	public static final int ENCRY_TYPE_NOMAL = 1;//默认的混淆加密算法
	public static final int ENCRY_TYPE_NO = 0;//不加密
	public static final int ENCRY_ENABLE = 1;//默认的混淆加密算法
	public static final int SESSION_LEN = 64;//默认session的长度
	
	public static final String REDIS_PREIFX_TEST = "test-";
	public static final int SEND_MSG_SUC = 0;
}
