package com.xyz.mosqproxy.proxy.center.datacenter;

import java.beans.PropertyVetoException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xyz.mosqproxy.common.DefaultValues;
import com.xyz.mosqproxy.config.Config;
import com.xyz.mosqproxy.config.ConfigKeys;

public class Sharding 
{
	private static Logger logger = LoggerFactory.getLogger(Sharding.class);
	//本分配所操作的范围
	private int shardingRangeStart;
	private int shardingRangeEnd;
	private int isSaveDb = DefaultValues.SETTING_DISABLE;
	private int shardingIdex;
	//variable for cache 
	private String cache_Host;
	private int cache_Port;
	private String cache_UserName;
	private String cache_Pwd;
	private int cache_MaxActive;
	private int cache_MaxIdle;
	private int cache_Maxwait;	
	//variable for write db
	private String dbWrite_Host;
	private int dbWrite_Port;
	private String dbWrite_DbName;
	private String dbWrite_UserName;
	private String dbWrite_Pwd;
	private int dbWrite_MinPoolSize;
	private int dbWrite_MaxPoolSize;
	private int dbWrite_MaxIdleTime;
	private int dbWrite_MaxStatements;
	private int dbWrite_checkoutTimeout;
	// variable for read db
	private String dbRead_Host;
	private int dbRead_Port;
	private String dbRead_DbName;
	private String dbRead_UserName;
	private String dbRead_Pwd;
	private int dbRead_MinPoolSize;
	private int dbRead_MaxPoolSize;
	private int dbRead_MaxIdleTime;
	private int dbRead_MaxStatements;
	private int dbRead_checkoutTimeout;
	
	private RedisClient cache = new RedisClient();
	private DbHelper readDb = new DbHelper();
	private DbHelper writeDb = new DbHelper();
	
	public boolean init(long logIndex, int shardingIdex, int saveDb, Config cfg)
	{
		String logFlag = "Sharding.init(...)";
		boolean res = false;
		try
		{
			res = _init(logIndex, shardingIdex, saveDb, cfg);
		}
		catch (PropertyVetoException e)
		{
			res = false;
			logger.error("[lid:{}][{}] exception happened! detail:{}", logIndex, logFlag, e);
		}
		return res;
	}
	
	private boolean _init(long logIndex, int shardingIdex, int saveDb, Config cfg) throws PropertyVetoException
	{
		String logFlag = "Sharding._init(...)";
		shardingRangeStart = cfg.getInt(ConfigKeys.PREFIX_SHARDING + shardingIdex + ConfigKeys.SUFFIX_SHARDING_RANGE_START);
		shardingRangeEnd = cfg.getInt(ConfigKeys.PREFIX_SHARDING + shardingIdex + ConfigKeys.SUFFIX_SHARDING_RANGE_END);
		if(shardingRangeEnd <= shardingRangeStart)
		{
			logger.error("[lid:{}][{}] sharding range error! sharding idex:{}, range start:{}, range end:{}", logIndex, logFlag, shardingIdex, shardingRangeStart, shardingRangeEnd);
			return false;
		}
		this.shardingIdex = shardingIdex;
		this.isSaveDb = saveDb;
		cache_Host = cfg.getString(ConfigKeys.PREFIX_SHARDING + shardingIdex + ConfigKeys.SUFFIX_CACHE_HOST);
		cache_Port = cfg.getInt(ConfigKeys.PREFIX_SHARDING + shardingIdex + ConfigKeys.SUFFIX_CACHE_PORT);
		cache_UserName = cfg.getString(ConfigKeys.PREFIX_SHARDING + shardingIdex + ConfigKeys.SUFFIX_CACHE_USER_NAME);
		cache_Pwd = cfg.getString(ConfigKeys.PREFIX_SHARDING + shardingIdex + ConfigKeys.SUFFIX_CACHE_PWD);
		cache_MaxActive = cfg.getInt(ConfigKeys.PREFIX_SHARDING + shardingIdex + ConfigKeys.SUFFIX_CACHE_POOL_MAX_ACTIVE);
		cache_MaxIdle = cfg.getInt(ConfigKeys.PREFIX_SHARDING + shardingIdex + ConfigKeys.SUFFIX_CACHE_POOL_MAX_IDLE);
		cache_Maxwait = cfg.getInt(ConfigKeys.PREFIX_SHARDING + shardingIdex + ConfigKeys.SUFFIX_CACHE_POOL_MAX_WAIT);
		if (!cache.init(logIndex, cfg, cache_Host, cache_Port, cache_UserName, cache_Pwd, cache_MaxActive, cache_MaxIdle, cache_Maxwait))
		{
			logger.error("[lid:{}][{}] cache initialized fail!, detail:cache_Host:{}, cache_Port:{}, cache_UserName:{}, cache_Pwd:{}, cache_MaxActive:{}, cache_MaxIdle:{}, cache_Maxwait:{}",
					logIndex, logFlag, cache_Host, cache_Port, cache_UserName, cache_Pwd, cache_MaxActive, cache_MaxIdle, cache_Maxwait);
			return false;
		}
		if (DefaultValues.SAVE_DB_VALUE == saveDb)
		{
			//variable for write db
			dbWrite_Host = cfg.getString(ConfigKeys.PREFIX_SHARDING + shardingIdex + ConfigKeys.SUFFIX_DB_WRITE_HOST);
			dbWrite_Port = cfg.getInt(ConfigKeys.PREFIX_SHARDING + shardingIdex + ConfigKeys.SUFFIX_DB_WRITE_PORT);
			dbWrite_DbName = cfg.getString(ConfigKeys.PREFIX_SHARDING + shardingIdex + ConfigKeys.SUFFIX_DB_WRITE_DB_NAME);
			dbWrite_UserName = cfg.getString(ConfigKeys.PREFIX_SHARDING + shardingIdex + ConfigKeys.SUFFIX_DB_WRITE_USERNAME);
			dbWrite_Pwd = cfg.getString(ConfigKeys.PREFIX_SHARDING + shardingIdex + ConfigKeys.SUFFIX_DB_WRITE_PWD);
			dbWrite_MinPoolSize = cfg.getInt(ConfigKeys.PREFIX_SHARDING + shardingIdex + ConfigKeys.SUFFIX_DB_WRITE_POOL_MIN_SIZE);
			dbWrite_MaxPoolSize = cfg.getInt(ConfigKeys.PREFIX_SHARDING + shardingIdex + ConfigKeys.SUFFIX_DB_WRITE_POOL_MAX_SIZE);
			dbWrite_MaxIdleTime = cfg.getInt(ConfigKeys.PREFIX_SHARDING + shardingIdex + ConfigKeys.SUFFIX_DB_WRITE_MAX_IDLETIME);
			dbWrite_MaxStatements = cfg.getInt(ConfigKeys.PREFIX_SHARDING + shardingIdex + ConfigKeys.SUFFIX_DB_WRITE_MAX_STATEMENTS);
			dbWrite_checkoutTimeout = cfg.getInt(ConfigKeys.PREFIX_SHARDING + shardingIdex + ConfigKeys.SUFFIX_DB_WRITE_CHECKOUT_TIMEOUT);
			String jdbcWriteUrl = "jdbc:mysql://" + dbWrite_Host + "/" + dbWrite_DbName + "?useUnicode=true&characterEncoding=UTF-8";
			if(!writeDb.intializeConnectionPool(jdbcWriteUrl, dbWrite_UserName, dbWrite_Pwd, dbWrite_MinPoolSize, dbWrite_MaxPoolSize, dbWrite_MaxIdleTime, dbWrite_MaxStatements, dbWrite_checkoutTimeout))
			{
				logger.error("[lid:{}][{}] Write database initialized fail!, detail: dbWrite_Host:{}, dbWrite_Port:{}, dbWrite_DbName:{}, dbWrite_UserName:{}, dbWrite_Pwd:{}, dbWrite_MinPoolSize:{}, dbWrite_MaxPoolSize:{}, dbWrite_MaxIdleTime:{}, dbWrite_MaxStatements:{}, dbWrite_checkoutTimeout:{}",
						logIndex, logFlag, dbWrite_Host, dbWrite_Port, dbWrite_DbName, dbWrite_UserName, dbWrite_Pwd, dbWrite_MinPoolSize, dbWrite_MaxPoolSize, dbWrite_MaxIdleTime, dbWrite_MaxStatements, dbWrite_checkoutTimeout);
				return false;
			}
			//variable for read db
			dbRead_Host = cfg.getString(ConfigKeys.PREFIX_SHARDING + shardingIdex + ConfigKeys.SUFFIX_DB_READ_HOST);
			dbRead_Port = cfg.getInt(ConfigKeys.PREFIX_SHARDING + shardingIdex + ConfigKeys.SUFFIX_DB_READ_PORT);
			dbRead_DbName = cfg.getString(ConfigKeys.PREFIX_SHARDING + shardingIdex + ConfigKeys.SUFFIX_DB_READ_DB_NAME);
			dbRead_UserName = cfg.getString(ConfigKeys.PREFIX_SHARDING + shardingIdex + ConfigKeys.SUFFIX_DB_READ_USERNAME);
			dbRead_Pwd = cfg.getString(ConfigKeys.PREFIX_SHARDING + shardingIdex + ConfigKeys.SUFFIX_DB_READ_PWD);
			dbRead_MinPoolSize = cfg.getInt(ConfigKeys.PREFIX_SHARDING + shardingIdex + ConfigKeys.SUFFIX_DB_READ_POOL_MIN_SIZE);
			dbRead_MaxPoolSize = cfg.getInt(ConfigKeys.PREFIX_SHARDING + shardingIdex + ConfigKeys.SUFFIX_DB_READ_POOL_MAX_SIZE);
			dbRead_MaxIdleTime = cfg.getInt(ConfigKeys.PREFIX_SHARDING + shardingIdex + ConfigKeys.SUFFIX_DB_READ_MAX_IDLETIME);
			dbRead_MaxStatements = cfg.getInt(ConfigKeys.PREFIX_SHARDING + shardingIdex + ConfigKeys.SUFFIX_DB_READ_MAX_STATEMENTS);
			dbRead_checkoutTimeout = cfg.getInt(ConfigKeys.PREFIX_SHARDING + shardingIdex + ConfigKeys.SUFFIX_DB_READ_CHECKOUT_TIMEOUT);
			String jdbcReadUrl = "jdbc:mysql://" + dbRead_Host + "/" + dbRead_DbName + "?useUnicode=true&characterEncoding=UTF-8";
			if(!readDb.intializeConnectionPool(jdbcReadUrl, dbRead_UserName, dbRead_Pwd, dbRead_MinPoolSize, dbRead_MaxPoolSize, dbRead_MaxIdleTime, dbRead_MaxStatements, dbRead_checkoutTimeout))
			{
				logger.error("[lid:{}][{}] Read database initialized fail!, detail: dbRead_Host:{}, dbRead_Port:{}, dbRead_DbName:{}, dbRead_UserName:{}, dbRead_Pwd:{}, dbRead_MinPoolSize:{}, dbRead_MaxPoolSize:{}, dbRead_MaxIdleTime:{}, dbRead_MaxStatements:{}, dbRead_checkoutTimeout:{}",
						logIndex, logFlag, dbRead_Host, dbRead_Port, dbRead_DbName, dbRead_UserName, dbRead_Pwd, dbRead_MinPoolSize, dbRead_MaxPoolSize, dbRead_MaxIdleTime, dbRead_MaxStatements, dbRead_checkoutTimeout);
				return false;
			}
		}
		displayShardingInfo(logIndex, shardingIdex, saveDb);
		return true;
	}
	
	/**
	 * 判断一个数字是否落在本分片的范围内
	 * */
	public boolean isInRange(int param)
	{
		return shardingRangeStart <= param && param <shardingRangeEnd;
	}
	
	/**
	 * 打印本分片的所有信息
	 * */
	private void displayShardingInfo(long logIndex, int shardingIndex, int isSaveDb)
	{
		String logFlag = "Sharding.init(...)";
		StringBuffer logItemBuf = new StringBuffer();
		logItemBuf.append("\n  information of sharding: ").append(shardingIndex);
		logItemBuf.append("\n-------------------------------cache-----------------------------------------");
		logItemBuf.append("\n  cache_Host: ").append(cache_Host);
		logItemBuf.append("\n  cache_Port: ").append(cache_Port);
		logItemBuf.append("\n  cache_UserName: ").append(cache_UserName);
		logItemBuf.append("\n  cache_Pwd: ").append(cache_Pwd);
		logItemBuf.append("\n  cache_MaxActive: ").append(cache_MaxActive);
		logItemBuf.append("\n  cache_MaxIdle: ").append(cache_MaxIdle);
		logItemBuf.append("\n  cache_Maxwait: ").append(cache_Maxwait);
		if (DefaultValues.SAVE_DB_VALUE == isSaveDb)
		{
			logItemBuf.append("\n-------------------------------read db ---------------------------------------");
			logItemBuf.append("\n  dbRead_Host: ").append(dbRead_Host);
			logItemBuf.append("\n  dbRead_Port: ").append(dbRead_Port);
			logItemBuf.append("\n  dbRead_DbName: ").append(dbRead_DbName);
			logItemBuf.append("\n  dbRead_UserName: ").append(dbRead_UserName);
			logItemBuf.append("\n  dbRead_Pwd: ").append(dbRead_Pwd);
			logItemBuf.append("\n  dbRead_MinPoolSize: ").append(dbRead_MinPoolSize);
			logItemBuf.append("\n  dbRead_MaxPoolSize: ").append(dbRead_MaxPoolSize);
			logItemBuf.append("\n  dbRead_MaxIdleTime: ").append(dbRead_MaxIdleTime);
			logItemBuf.append("\n  dbRead_MaxStatements: ").append(dbRead_MaxStatements);
			logItemBuf.append("\n  dbRead_checkoutTimeout: ").append(dbRead_checkoutTimeout);
			logItemBuf.append("\n-------------------------------write db --------------------------------------");
			logItemBuf.append("\n  dbWrite_Host: ").append(dbWrite_Host);
			logItemBuf.append("\n  dbWrite_Port: ").append(dbWrite_Port);
			logItemBuf.append("\n  dbWrite_DbName: ").append(dbWrite_DbName);
			logItemBuf.append("\n  dbWrite_UserName: ").append(dbWrite_UserName);
			logItemBuf.append("\n  dbWrite_Pwd: ").append(dbWrite_Pwd);
			logItemBuf.append("\n  dbWrite_MinPoolSize: ").append(dbWrite_MinPoolSize);
			logItemBuf.append("\n  dbWrite_MaxPoolSize: ").append(dbWrite_MaxPoolSize);
			logItemBuf.append("\n  dbWrite_MaxIdleTime: ").append(dbWrite_MaxIdleTime);
			logItemBuf.append("\n  dbWrite_MaxStatements: ").append(dbWrite_MaxStatements);
			logItemBuf.append("\n  dbWrite_checkoutTimeout: ").append(dbWrite_checkoutTimeout);
		}
		logItemBuf.append("\n------------------------------------------------------------------------------\n");
		logger.info("[lid:{}][{}]\n\n{}", logIndex, logFlag, logItemBuf.toString());
	}
	
	public String getInfo(long logIndex)
	{
		StringBuffer logItemBuf = new StringBuffer();
		logItemBuf.append("\n  information of sharding: ").append(shardingIdex);
		logItemBuf.append("\n-------------------------------cache-----------------------------------------");
		logItemBuf.append("\n  cache_Host: ").append(cache_Host);
		logItemBuf.append("\n  cache_Port: ").append(cache_Port);
		logItemBuf.append("\n  cache_UserName: ").append(cache_UserName);
		logItemBuf.append("\n  cache_Pwd: ").append(cache_Pwd);
		logItemBuf.append("\n  cache_MaxActive: ").append(cache_MaxActive);
		logItemBuf.append("\n  cache_MaxIdle: ").append(cache_MaxIdle);
		logItemBuf.append("\n  cache_Maxwait: ").append(cache_Maxwait);
		if (1 == isSaveDb)
		{
			logItemBuf.append("\n-------------------------------read db ---------------------------------------");
			logItemBuf.append("\n  dbRead_Host: ").append(dbRead_Host);
			logItemBuf.append("\n  dbRead_Port: ").append(dbRead_Port);
			logItemBuf.append("\n  dbRead_DbName: ").append(dbRead_DbName);
			logItemBuf.append("\n  dbRead_UserName: ").append(dbRead_UserName);
			logItemBuf.append("\n  dbRead_Pwd: ").append(dbRead_Pwd);
			logItemBuf.append("\n  dbRead_MinPoolSize: ").append(dbRead_MinPoolSize);
			logItemBuf.append("\n  dbRead_MaxPoolSize: ").append(dbRead_MaxPoolSize);
			logItemBuf.append("\n  dbRead_MaxIdleTime: ").append(dbRead_MaxIdleTime);
			logItemBuf.append("\n  dbRead_MaxStatements: ").append(dbRead_MaxStatements);
			logItemBuf.append("\n  dbRead_checkoutTimeout: ").append(dbRead_checkoutTimeout);
			logItemBuf.append("\n-------------------------------write db --------------------------------------");
			logItemBuf.append("\n  dbWrite_Host: ").append(dbWrite_Host);
			logItemBuf.append("\n  dbWrite_Port: ").append(dbWrite_Port);
			logItemBuf.append("\n  dbWrite_DbName: ").append(dbWrite_DbName);
			logItemBuf.append("\n  dbWrite_UserName: ").append(dbWrite_UserName);
			logItemBuf.append("\n  dbWrite_Pwd: ").append(dbWrite_Pwd);
			logItemBuf.append("\n  dbWrite_MinPoolSize: ").append(dbWrite_MinPoolSize);
			logItemBuf.append("\n  dbWrite_MaxPoolSize: ").append(dbWrite_MaxPoolSize);
			logItemBuf.append("\n  dbWrite_MaxIdleTime: ").append(dbWrite_MaxIdleTime);
			logItemBuf.append("\n  dbWrite_MaxStatements: ").append(dbWrite_MaxStatements);
			logItemBuf.append("\n  dbWrite_checkoutTimeout: ").append(dbWrite_checkoutTimeout);
		}
		logItemBuf.append("\n------------------------------------------------------------------------------\n");
		return logItemBuf.toString();
	}
	
	public RedisClient getRedisClient(long logIndex)
	{
		return cache;
	}
	
	public DbHelper GetReadDb(long logIndex)
	{
		return readDb;
	}
	
	public DbHelper GetWriteDb(long logIndex)
	{
		return writeDb;
	}
	
	public void shutdown(long logIndex)
	{
		cache.shutDown(logIndex);
		readDb.shutdownConnectionPool(logIndex);
		writeDb.shutdownConnectionPool(logIndex);
	}
}
