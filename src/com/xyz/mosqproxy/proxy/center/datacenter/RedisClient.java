package com.xyz.mosqproxy.proxy.center.datacenter;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Tuple;

import com.xyz.mosqproxy.common.DefaultValues;
import com.xyz.mosqproxy.common.MetricsTool;
import com.xyz.mosqproxy.common.ReturnCode;
import com.xyz.mosqproxy.config.Config;
import com.xyz.mosqproxy.exception.BasicException;
import com.xyz.mosqproxy.thrift.datatype.ResBool;
import com.xyz.mosqproxy.thrift.datatype.ResDouble;
import com.xyz.mosqproxy.thrift.datatype.ResLong;
import com.xyz.mosqproxy.thrift.datatype.ResSetStr;

public class RedisClient
{
	private String getClassName()
	{//仅用于内部获取类的最短名字
		return "RedisClient";
	}

	private static Logger logger = LoggerFactory.getLogger(RedisClient.class);

	private JedisPool jedisPool = null;
	private String host = null;
	private int port;
	private int maxActive;
	private int maxIdle;
	private int maxWait;
	@SuppressWarnings("unused") private String userName;
	@SuppressWarnings("unused") private String password;
//	private Map<String, String> luaScripts = new HashMap<String, String>();

	/**
	 * 函数名称：init
	 * 函数功能：获取一个指定类型的ID
	 *
	 * @param  logIndex 	日志索引
	 * @param 	host 		redis所在的主机名
	 * @param   port 			redis所使用的端口号
	 * @param  userName   登陆redis所使用的用户名
	 * @param  password   登陆redis所使用的密码
	 * @param     maxActive		jedis连接池所用，控制一个pool可分配多少个jedis实例
	 * @param     maxIdle		jedis连接池所用，控制一个pool最多有多少个状态为idle(空闲的)的jedis实例
	 * @param     maxWait		jedis连接池所用，表示当borrow(引入)一个jedis实例时，最大的等待时间，如果超过等待时间，则直接抛出JedisConnectionException；
	 * @return boolean 初始化成功返回true，否是返回false
	 * @author houjixin
	 */
	public boolean init(long logIndex, Config cfg, String host, int port, String userName, String password, int maxActive, int maxIdle, int maxWait)
	{/* String userName, String password暂时没用*/
//		String logFlag = getClassName() + ".init";
		this.userName = userName;
		this.password = password;
		this.host = host;
		this.port = port;
		this.maxActive = maxActive;
		this.maxIdle = maxIdle;
		this.maxWait = maxWait;

		JedisPoolConfig config = new JedisPoolConfig();
		//控制一个pool可分配多少个jedis实例，通过pool.getResource()来获取；
		//如果赋值为-1，则表示不限制；如果pool已经分配了maxActive个jedis实例，则此时pool的状态为exhausted(耗尽)。
		config.setMaxActive(this.maxActive);

		config.setMaxIdle(this.maxIdle);

		//表示当borrow(引入)一个jedis实例时，最大的等待时间，如果超过等待时间，则直接抛出JedisConnectionException；
		config.setMaxWait(this.maxWait);
		//在borrow一个jedis实例时，是否提前进行validate操作；如果为true，则得到的jedis实例均是可用的；
		config.setTestOnBorrow(false);

		jedisPool = new JedisPool(config, host, port);
		return test(logIndex);
	}



	public String getRedisInfo(long logIndex)
	{
		StringBuilder sb = new StringBuilder();
		return sb.append(host).append(":").append(port).toString();
	}

	public void shutDown(long logIndex)
	{
		String logFlag = getClassName() + ".shutDown";
		if (jedisPool == null)
			return;
		try
		{
			jedisPool.destroy();
			jedisPool = null;
		}
		catch (Exception ex)
		{
			logger.error("[lid:{}][{}] exception happened while shutting down jedis...; detail:{}", logIndex, logFlag, BasicException.getStackTrace(ex));
		}
	}
	
	/**
	 * 函数名称：incr
	 * 函数功能：执行redis的incr命令，将key对应的值加1，并返回加之后的值
	 *
	 * @param    logIndex 日志索引
	 * @param  key id的类型
	 * @return long 操作成功返回获取到的long型值，否则返回DefaultValues.ID_ERROR
	 * @author houjixin
	 */
	public boolean test(long logIndex)
	{
		String logFlag = getClassName() + ".test";
		//metrics数据统计开始
		MetricsTool.MetricsTimer mTimer = MetricsTool.getAndStartTimer(RedisClient.class, "GetId");

		Jedis jds = null;
		String key = DefaultValues.REDIS_PREIFX_TEST + System.currentTimeMillis();
		String value = DefaultValues.REDIS_PREIFX_TEST + System.currentTimeMillis();
		boolean res = false;
		try
		{
			jds = jedisPool.getResource();
			jds.set(key, value);
			String getValue = jds.get(key);
			res = value.equals(getValue);
			jedisPool.returnResource(jds);
		}
		catch (Exception e)
		{
			logger.warn("[lid:{}][{}] Redis exeception！ parameters: key {}; detail:{}", logIndex, logFlag, key, BasicException.getStackTrace(e));
			jedisPool.returnBrokenResource(jds);
		}
		//metrics数据统计结束
		mTimer.stop();
		return res;
	}

	/**
	 * 函数名称：incr
	 * 函数功能：执行redis的incr命令，将key对应的值加1，并返回加之后的值
	 *
	 * @param    logIndex 日志索引
	 * @param  key id的类型
	 * @return long 操作成功返回获取到的long型值，否则返回DefaultValues.ID_ERROR
	 * @author houjixin
	 */
	public long incr(long logIndex, String key)
	{
		String logFlag = getClassName() + ".incr";
		//metrics数据统计开始
		MetricsTool.MetricsTimer mTimer = MetricsTool.getAndStartTimer(RedisClient.class, "GetId");

		Jedis jds = null;
		long id = DefaultValues.ID_ERROR;
		try
		{
			jds = jedisPool.getResource();
			id = jds.incr(key);
			jedisPool.returnResource(jds);
		}
		catch (Exception e)
		{
			logger.warn("[lid:{}][{}] Redis exeception！ parameters: key {}; detail:{}", logIndex, logFlag, key, BasicException.getStackTrace(e));
			jedisPool.returnBrokenResource(jds);
		}
		//metrics数据统计结束
		mTimer.stop();
		return id;
	}

	/**
	 * 函数名称：del
	 * 函数功能：执行redis的del命令，删除redis中的某个 key
	 *
	 * @param    logIndex 日志索引
	 * @param  key 操作redis的key值
	 * @return boolean 成功删除ID时，返回true，否则返回false
	 * @author houjixin
	 */
	public boolean del(long logIndex, String key)
	{
		String logFlag = getClassName() + ".del";
		//metrics数据统计开始
		MetricsTool.MetricsTimer mTimer = MetricsTool.getAndStartTimer(RedisClient.class, "del");

		Jedis jds = null;
		boolean res = false;
		try
		{
			jds = jedisPool.getResource();
			jds.del(key);
			res = true;
			jedisPool.returnResource(jds);
		}
		catch (Exception e)
		{
			logger.warn("[lid:{}][{}] Redis exeception！parameters: key {}; detail:{}", logIndex, logFlag, key, BasicException.getStackTrace(e));
			res = false;
			jedisPool.returnBrokenResource(jds);
		}
		//metrics数据统计结束
		mTimer.stop();
		return res;

	}

	public String hget(long logIndex, String key, String field)
	{//
		String logFlag = getClassName() + ".hget";
		//metrics数据统计开始
		MetricsTool.MetricsTimer mTimer = MetricsTool.getAndStartTimer(RedisClient.class, "hget");

		Jedis jds = null;
		String res = null;
		try
		{
			jds = jedisPool.getResource();
			res = jds.hget(key, field);
			jedisPool.returnResource(jds);
		}
		catch (Exception e)
		{
			logger.warn("[lid:{}][{}] Redis exeception！parameters: key {}; field {}; detail:{}", logIndex, logFlag, field, key, BasicException.getStackTrace(e));
			res = null;
			jedisPool.returnBrokenResource(jds);
		}
		//metrics数据统计结束
		mTimer.stop();
		return res;

	}

	public ResSetStr hVals(long logIndex, String key)
	{
		String logFlag = getClassName() + ".hVals";
		//metrics数据统计开始
		MetricsTool.MetricsTimer mTimer = MetricsTool.getAndStartTimer(RedisClient.class, "hVals");

		Jedis jds = null;
		ResSetStr res = new ResSetStr();
		try
		{
			jds = jedisPool.getResource();
			res.value = new HashSet<>(jds.hvals(key));
			res.res = ReturnCode.SUCCESS;
			jedisPool.returnResource(jds);
		}
		catch (Exception e)
		{
			logger.warn("[lid:{}][{}] Redis exeception！parameters: key {}; detail:{}", logIndex, logFlag, key, BasicException.getStackTrace(e));
			res.value = null;
			res.res = ReturnCode.INNER_ERROR;
			jedisPool.returnBrokenResource(jds);
		}
		//metrics数据统计结束
		mTimer.stop();
		return res;
	}

	public boolean hset(long logIndex, String key, String field, String value)
	{
		String logFlag = getClassName() + ".hset";
		//metrics数据统计开始
		MetricsTool.MetricsTimer mTimer = MetricsTool.getAndStartTimer(RedisClient.class, "hset");

		Jedis jds = null;
		boolean res = false;

		try
		{
			jds = jedisPool.getResource();
			jds.hset(key, field, value);
			res = true;
			jedisPool.returnResource(jds);
		}
		catch (Exception e)
		{
			logger.warn("[lid:{}][{}] Redis exeception！ parameters:  key {}; field {},value {}; detail:{}", logIndex, logFlag, key, field, value, BasicException.getStackTrace(e));
			jedisPool.returnBrokenResource(jds);
		}
		//metrics数据统计结束
		mTimer.stop();
		return res;
	}

	/**
	 * 查询hash中某个域是否存在
	 *
	 * @param key 待查询的key
	 * @param field 待查询的field
	 * @return 查询成功 result值为：ReturnCode.SUCCESS，
	 * 此时如果field存在，则value为true，field不存在value为false
	 * 如果查询中出现了异常result值为ThriftRes.EXCEPTION, value值为false
	 */
	public ResBool hexists(long logIndex, String key, String field)
	{
		String logFlag = getClassName() + ".hexists";
		//metrics数据统计开始
		MetricsTool.MetricsTimer mTimer = MetricsTool.getAndStartTimer(RedisClient.class, "hexists");

		Jedis jds = null;
		ResBool res = null;
		try
		{
			jds = jedisPool.getResource();
			boolean resv = jds.hexists(key, field);
			res = new ResBool(ReturnCode.SUCCESS, resv, null);
			jedisPool.returnResource(jds);
		}
		catch (Exception e)
		{
			jedisPool.returnBrokenResource(jds);
			logger.warn("[lid:{}][{}] Redis exeception！ parameters: key {}; field {}, detail:{}", logIndex, logFlag, key, field, BasicException.getStackTrace(e));
			res = new ResBool(ReturnCode.INNER_ERROR, false, null);
		}
		//metrics数据统计结束
		mTimer.stop();
		return res;
	}

	public boolean hmset(long logIndex, String key, Map<String, String> hash)
	{
		String logFlag = getClassName() + ".hmset";
		Jedis jds = null;
		boolean res = false;
		//metrics数据统计开始
		MetricsTool.MetricsTimer mTimer = MetricsTool.getAndStartTimer(RedisClient.class, "hmset");

		try
		{
			jds = jedisPool.getResource();
			jds.hmset(key, hash);
			res = true;
			jedisPool.returnResource(jds);
		}
		catch (Exception e)
		{
			logger.warn("[lid:{}][{}] Redis exeception！ parameters: key {}; hash {}, detail:{}", logIndex, logFlag, key, hash.toString(), BasicException.getStackTrace(e));
			jedisPool.returnBrokenResource(jds);
		}
		//metrics数据统计结束
		mTimer.stop();
		return res;
	}

	public Map<String, String> hgetAll(long logIndex, String key)
	{
		String logFlag = getClassName() + ".hgetAll";
		//metrics数据统计开始
		MetricsTool.MetricsTimer mTimer = MetricsTool.getAndStartTimer(RedisClient.class, "hgetAll");

		Jedis jds = null;
		Map<String, String> res = null;
		try
		{
			jds = jedisPool.getResource();
			res = jds.hgetAll(key);
			jedisPool.returnResource(jds);
		}
		catch (Exception e)
		{
			logger.warn("[lid:{}][{}] Redis exeception！ parameters: key {}; detail:{}", logIndex, logFlag, key, BasicException.getStackTrace(e));
			res = null;
			jedisPool.returnBrokenResource(jds);
		}

		//metrics数据统计结束
		mTimer.stop();
		return ((res == null || res.isEmpty()) ? null : res);
	}

	public List<String> hmget(long logIndex, String key, String[] fields)
	{
		String logFlag = getClassName() + ".hmget";
		//metrics数据统计开始
		MetricsTool.MetricsTimer mTimer = MetricsTool.getAndStartTimer(RedisClient.class, "hmget");

		Jedis jds = null;
		List<String> res = null;
		try
		{
			jds = jedisPool.getResource();
			res = jds.hmget(key, fields);
			jedisPool.returnResource(jds);
		}
		catch (Exception e)
		{
			logger.warn("[lid:{}][{}] Redis exeception！ parameters: key {}, fields {}; detail:{}", logIndex, logFlag, key, java.util.Arrays.toString(fields),
					BasicException.getStackTrace(e));
			res = null;
			jedisPool.returnBrokenResource(jds);
		}
		//metrics数据统计结束
		mTimer.stop();
		return ((res == null || res.isEmpty()) ? null : res);
	}

	public boolean hdel(long logIndex, String key, String field)
	{
		String logFlag = getClassName() + ".hdel";
		Jedis jds = null;
		boolean res = false;
		//metrics数据统计开始
		MetricsTool.MetricsTimer mTimer = MetricsTool.getAndStartTimer(RedisClient.class, "hdel");

		try
		{
			jds = jedisPool.getResource();
			jds.hdel(key, field);
			res = true;
			jedisPool.returnResource(jds);
		}
		catch (Exception e)
		{
			logger.warn("[lid:{}][{}] Redis exeception！ parameters: key {}, field {}; detail:{}", logIndex, logFlag, key, field, BasicException.getStackTrace(e));
			jedisPool.returnBrokenResource(jds);
		}
		//metrics数据统计结束
		mTimer.stop();
		return res;
	}

	// ////////////////////////////////////////////////////////
	// sorted set
	// ////////////////////////////////////////////////////////
	public boolean zadd(long logIndex, String key, Map<Double, String> scoreMembers)
	{
		String logFlag = getClassName() + ".zadd(map)";
		//metrics数据统计开始
		MetricsTool.MetricsTimer mTimer = MetricsTool.getAndStartTimer(RedisClient.class, "zadd");

		Jedis jds = null;
		boolean res = false;
		try
		{
			jds = jedisPool.getResource();
			jds.zadd(key, scoreMembers);
			res = true;
			jedisPool.returnResource(jds);
		}
		catch (Exception e)
		{
			logger.warn("[lid:{}][{}] Redis exeception！ parameters: key {}, scoreMembers {}; detail:{}", logIndex, logFlag, key, scoreMembers.toString(), BasicException.getStackTrace(e));
			jedisPool.returnBrokenResource(jds);
		}
		//metrics数据统计结束
		mTimer.stop();
		return res;
	}

	public ResBool zadd(long logIndex, String key, double score, String member)
	{
		String logFlag = getClassName() + ".zadd";
		//metrics数据统计开始
		MetricsTool.MetricsTimer mTimer = MetricsTool.getAndStartTimer(RedisClient.class, "zadd");

		Jedis jds = null;
		ResBool res = new ResBool(ReturnCode.NO_CONTENT, false, null);
		try
		{
			jds = jedisPool.getResource();
			jds.zadd(key, score, member);
			res.res = ReturnCode.SUCCESS;
			res.value = true;
			jedisPool.returnResource(jds);
		}
		catch (Exception e)
		{
			logger.warn("[lid:{}][{}] Redis exeception！ parameters: key {}, score {}, member {}; detail:{}", logIndex, logFlag, key, score, member, BasicException.getStackTrace(e));
			res.res = ReturnCode.INNER_ERROR;
			res.value = false;
			jedisPool.returnBrokenResource(jds);
		}
		//metrics数据统计结束
		mTimer.stop();
		return res;
	}

	public ResBool zrem(long logIndex, String key, String[] members)
	{
		String logFlag = getClassName() + ".zrem";
		//metrics数据统计开始
		MetricsTool.MetricsTimer mTimer = MetricsTool.getAndStartTimer(RedisClient.class, "zrem");

		Jedis jds = null;
		ResBool res = new ResBool();
		try
		{
			jds = jedisPool.getResource();
			jds.zrem(key, members);
			res.res = ReturnCode.SUCCESS;
			res.value = true;
			jedisPool.returnResource(jds);
		}
		catch (Exception e)
		{
			logger.warn("[lid:{}][{}] Redis exeception！ parameters: key {}, members {}; detail:{}", logIndex, logFlag, key,
					java.util.Arrays.toString(members), BasicException.getStackTrace(e));
			res.res = ReturnCode.INNER_ERROR;
			res.value = false;
			jedisPool.returnBrokenResource(jds);
		}
		//metrics数据统计结束
		mTimer.stop();
		return res;
	}

	// 获取指定区间的结果，使用start == 0、end== -1时获取全部结果，结果递增排列
	public Set<String> zrange(long logIndex, String key, long start, long end)
	{
		String logFlag = getClassName() + ".zrange";
		//metrics数据统计开始
		MetricsTool.MetricsTimer mTimer = MetricsTool.getAndStartTimer(RedisClient.class, "zrange");

		Jedis jds = null;
		Set<String> res = null;
		try
		{
			jds = jedisPool.getResource();
			res = jds.zrange(key, start, end);
			jedisPool.returnResource(jds);
		}
		catch (Exception e)
		{
			logger.warn("[lid:{}][{}] Redis exeception！ parameters: key {}, start {}, end {}; detail:{}", logIndex, logFlag, key, start, end, BasicException.getStackTrace(e));
			res = null;
			jedisPool.returnBrokenResource(jds);
		}
		//metrics数据统计结束
		mTimer.stop();
		return ((res == null || res.isEmpty()) ? null : res);
	}

	// 获取指定区间的结果，使用start == 0、end== -1时获取全部结果，结果递减排列
	public Set<String> zrevrange(long logIndex, String key, long start, long end)
	{
		String logFlag = getClassName() + ".zrevrange";
		//metrics数据统计开始
		MetricsTool.MetricsTimer mTimer = MetricsTool.getAndStartTimer(RedisClient.class, "zrevrange");

		Jedis jds = null;
		Set<String> res = null;
		try
		{
			jds = jedisPool.getResource();
			res = jds.zrevrange(key, start, end);
			jedisPool.returnResource(jds);
		}
		catch (Exception e)
		{
			logger.warn("[lid:{}][{}] Redis exeception！ parameters: key {}, start {}, end {}; detail:{}", logIndex, logFlag, key, start, end, BasicException.getStackTrace(e));
			res = null;
			jedisPool.returnBrokenResource(jds);
		}
		//metrics数据统计结束
		mTimer.stop();
		return ((res == null || res.isEmpty()) ? null : res);
	}

	// 获取指定区间的结果，使用start == 0、end== -1时获取全部结果，结果递减排列
	public Set<Tuple> zrevrangewithscores(long logIndex, String key, long start, long end)
	{
		String logFlag = getClassName() + ".zrevrangewithscores";
		//metrics数据统计开始
		MetricsTool.MetricsTimer mTimer = MetricsTool.getAndStartTimer(RedisClient.class, "zrevrangewithscores");

		Jedis jds = null;
		Set<Tuple> res = null;
		try
		{
			jds = jedisPool.getResource();
			res = jds.zrevrangeWithScores(key, start, end);
			jedisPool.returnResource(jds);
		}
		catch (Exception e)
		{
			logger.warn("[lid:{}][{}] Redis exeception！ parameters: key {}, start {}, end {}; detail:{}", logIndex, logFlag, key, start, end, BasicException.getStackTrace(e));
			res = null;
			jedisPool.returnBrokenResource(jds);
		}
		//metrics数据统计结束
		mTimer.stop();
		return ((res == null || res.isEmpty()) ? null : res);
	}

	// 获取指定区间的结果，使用start == 0、end== -1时获取全部结果，结果递减排列
	public Set<String> zrevrangeByScore(long logIndex, String key, double start, double end)
	{
		String logFlag = getClassName() + ".zrevrangeByScore";
		//metrics数据统计开始
		MetricsTool.MetricsTimer mTimer = MetricsTool.getAndStartTimer(RedisClient.class, "zrevrangeByScore");

		Jedis jds = null;
		Set<String> res = null;
		try
		{
			jds = jedisPool.getResource();
			res = jds.zrevrangeByScore(key, start, end);
			jedisPool.returnResource(jds);
		}
		catch (Exception e)
		{
			logger.warn("[lid:{}][{}] Redis exeception！ parameters: key {}, start {}, end {}; detail:{}", logIndex, logFlag, key, start, end, BasicException.getStackTrace(e));
			res = null;
			jedisPool.returnBrokenResource(jds);
		}
		//metrics数据统计结束
		mTimer.stop();
		return ((res == null || res.isEmpty()) ? null : res);
	}

	// score的值不能为-1
	public ResDouble zscore(long logIndex, String key, String member)
	{
		String logFlag = getClassName() + ".zscore";
		//metrics数据统计开始
		MetricsTool.MetricsTimer mTimer = MetricsTool.getAndStartTimer(RedisClient.class, "zscore");

		Jedis jds = null;
		ResDouble res = new ResDouble();
		try
		{
			jds = jedisPool.getResource();
			res.value = jds.zscore(key, member);
			res.res = ReturnCode.SUCCESS;
			jedisPool.returnResource(jds);
		}
		catch (Exception e)
		{
			logger.warn("[lid:{}][{}] Redis exeception！ parameters: key {}, member {}; detail:{}", logIndex, logFlag, key, member, BasicException.getStackTrace(e));
			res.res = ReturnCode.INNER_ERROR;
			jedisPool.returnBrokenResource(jds);
		}
		//metrics数据统计结束
		mTimer.stop();
		return res;
	}

	public ResLong zrevrank(long logIndex, String key, String member)
	{
		String logFlag = getClassName() + ".zrevrank";
		//metrics数据统计开始
		MetricsTool.MetricsTimer mTimer = MetricsTool.getAndStartTimer(RedisClient.class, "zrevrank");

		Jedis jds = null;
		ResLong res = new ResLong();
		try
		{
			jds = jedisPool.getResource();
			res.value = jds.zrevrank(key, member);
			res.res = ReturnCode.SUCCESS;
			jedisPool.returnResource(jds);
		}
		catch (Exception e)
		{
			logger.warn("[lid:{}][{}] Redis exeception！ parameters: key {}, member {}; detail:{}", logIndex, logFlag, key, member, BasicException.getStackTrace(e));
			res.value = 0;
			res.res = ReturnCode.INNER_ERROR;
			jedisPool.returnBrokenResource(jds);
		}
		//metrics数据统计结束
		mTimer.stop();
		return res;
	}

	public ResLong zrank(long logIndex, String key, String member)
	{
		String logFlag = getClassName() + ".zrank";
		//metrics数据统计开始
		MetricsTool.MetricsTimer mTimer = MetricsTool.getAndStartTimer(RedisClient.class, "zrank");

		ResLong res = new ResLong();
		Jedis jds = null;
		try
		{
			jds = jedisPool.getResource();
			res.value = jds.zrank(key, member);
			res.res = ReturnCode.SUCCESS;
			jedisPool.returnResource(jds);
		}
		catch (Exception e)
		{
			logger.warn("[lid:{}][{}] Redis exeception！ parameters: key {}, member {}; detail:{}", logIndex, logFlag, key, member, BasicException.getStackTrace(e));
			res.res = ReturnCode.INNER_ERROR;
			jedisPool.returnBrokenResource(jds);
		}
		//metrics数据统计结束
		mTimer.stop();
		return res;
	}

	// ////////////////////////////////////////////////////////
	// string
	// ////////////////////////////////////////////////////////
	public boolean set(long logIndex, String key, String value)
	{
		String logFlag = getClassName() + ".set";
		//metrics数据统计开始
		MetricsTool.MetricsTimer mTimer = MetricsTool.getAndStartTimer(RedisClient.class, "set");

		Jedis jds = null;
		try
		{
			jds = jedisPool.getResource();
			jds.set(key, value);
			jedisPool.returnResource(jds);
			mTimer.stop();
			return true;
		}
		catch (Exception e)
		{
			logger.warn("[lid:{}][{}] Redis exeception！ parameters: key {}, value {}; detail:{}", logIndex, logFlag, key, value, BasicException.getStackTrace(e));
			jedisPool.returnBrokenResource(jds);
			mTimer.stop();
			return false;
		}
	}

	public String get(long logIndex, String key)
	{
		String logFlag = getClassName() + ".get";
		//metrics数据统计开始
		MetricsTool.MetricsTimer mTimer = MetricsTool.getAndStartTimer(RedisClient.class, "get");

		Jedis jds = null;
		String res = null;
		try
		{
			jds = jedisPool.getResource();
			res = jds.get(key);
			jedisPool.returnResource(jds);
		}
		catch (Exception e)
		{
			logger.warn("[lid:{}][{}] Redis exeception！ parameters: key {}; detail:{}", logIndex, logFlag, key, BasicException.getStackTrace(e));
			res = null;
			jedisPool.returnBrokenResource(jds);
		}
		//metrics数据统计结束
		mTimer.stop();
		return res;
	}

	/**
	 * 判断指定key是否在redis中存在，
	 * 如果查询正常，则result为ThriftRes.SUCCESS，结果放在valuez中，即如果存在为true，不存在为false
	 * 如果查询过程出现异常，则result为ThriftRes.EXCEPTION，value为false
	 * 简而言之：只有key在redis中时，value才为true，其他情况都是false，而result则返回操作解雇的原因
	 */
	public ResBool exists(long logIndex, String key)
	{
		String logFlag = getClassName() + ".exists";
		//metrics数据统计开始
		MetricsTool.MetricsTimer mTimer = MetricsTool.getAndStartTimer(RedisClient.class, logFlag);

		Jedis jds = null;
		boolean value = false;
		try
		{
			jds = jedisPool.getResource();
			value = jds.exists(key);

			jedisPool.returnResource(jds);
			mTimer.stop();
			return new ResBool(ReturnCode.SUCCESS, value, null);
		}
		catch (Exception e)
		{
			jedisPool.returnBrokenResource(jds);
			logger.warn("[lid:{}][{}] Redis exeception！ parameters: key {}; detail:{}", logIndex, logFlag, key, BasicException.getStackTrace(e));
			mTimer.stop();
			return new ResBool(ReturnCode.INNER_ERROR, false, null);
		}
	}

	/**
	 * 加载lua脚本
	 */
	public String scriptLoad(long logIndex, String script)
	{
		String logFlag = getClassName() + ".scriptLoad";
		//metrics数据统计开始
		MetricsTool.MetricsTimer mTimer = MetricsTool.getAndStartTimer(RedisClient.class, logFlag);

		Jedis jds = null;
		String sha;
		try
		{
			jds = jedisPool.getResource();
			sha = jds.scriptLoad(script);

			jedisPool.returnResource(jds);
			mTimer.stop();
			return sha;
		}
		catch (Exception e)
		{
			jedisPool.returnBrokenResource(jds);
			logger.warn("[lid:{}][{}] Redis exeception！ parameters: key {}; detail:{}", logIndex, logFlag, script, BasicException.getStackTrace(e));
			mTimer.stop();
			return null;
		}
	}

	public Object eval(long logIndex, String script, List<String> keys, List<String> args)
	{
		String logFlag = getClassName() + ".eval";
		//metrics数据统计开始
		MetricsTool.MetricsTimer mTimer = MetricsTool.getAndStartTimer(RedisClient.class, logFlag);
		Object obj = null;
		Jedis jds = null;
		try
		{
			jds = jedisPool.getResource();
			obj = jds.eval(script, keys, args);

			jedisPool.returnResource(jds);
			mTimer.stop();
			return obj;
		}
		catch (Exception e)
		{
			jedisPool.returnBrokenResource(jds);
			logger.warn("[lid:{}][{}] Redis exeception！ parameters: key {}; detail:{}", logIndex, logFlag, script, BasicException.getStackTrace(e));
			mTimer.stop();
			return null;
		}
	}

	public Object eval(long logIndex, String script, int keyCount, String[] params)
	{
		String logFlag = getClassName() + ".eval(script-keyCount-params)";
		//metrics数据统计开始
		MetricsTool.MetricsTimer mTimer = MetricsTool.getAndStartTimer(RedisClient.class, logFlag);
		Object obj = null;
		Jedis jds = null;
		try
		{
			jds = jedisPool.getResource();
			obj = jds.eval(script, keyCount, params);

			jedisPool.returnResource(jds);
			mTimer.stop();
			return obj;
		}
		catch (Exception e)
		{
			jedisPool.returnBrokenResource(jds);
			logger.warn("[lid:{}][{}] Redis exeception！ parameters: script {}, keyCount:{}, params:{}, detail:{}", logIndex, logFlag, script, keyCount,
					java.util.Arrays.toString(params), BasicException.getStackTrace(e));
			mTimer.stop();
			return null;
		}
	}

	public List<String> lRange(long logIndex, String key, long start, long end)
	{
		String logFlag = getClassName() + ".lRange";
		//metrics数据统计开始
		MetricsTool.MetricsTimer mTimer = MetricsTool.getAndStartTimer(RedisClient.class, logFlag);
		List<String> res = null;
		Jedis jds = null;
		try
		{
			jds = jedisPool.getResource();
			res = jds.lrange(key, start, end);

			jedisPool.returnResource(jds);
			mTimer.stop();
			return res;
		}
		catch (Exception e)
		{
			jedisPool.returnBrokenResource(jds);
			logger.warn("[lid:{}][{}] Redis exeception！ parameters: key {}, start:{}, end:{}, detail:{}", logIndex, logFlag, key, start, end, BasicException.getStackTrace(e));
			mTimer.stop();
			return null;
		}
	}

	public Long setnx(long logIndex, String key, String value)
	{
		String logFlag = getClassName() + ".setnx";
		//metrics数据统计开始
		MetricsTool.MetricsTimer mTimer = MetricsTool.getAndStartTimer(RedisClient.class, logFlag);
		Long res = null;
		Jedis jds = null;
		try
		{
			jds = jedisPool.getResource();
			res = jds.setnx(key, value);

			jedisPool.returnResource(jds);
			mTimer.stop();
			return res;
		}
		catch (Exception e)
		{
			jedisPool.returnBrokenResource(jds);
			logger.warn("[lid:{}][{}] Redis exeception！ parameters: key {}, value:{}, detail:{}", logIndex, logFlag, key, value, BasicException.getStackTrace(e));
			mTimer.stop();
			return null;
		}
	}

	public Long lpush(long logIndex, String key, String[] strings)
	{
		String logFlag = getClassName() + ".lpush";
		//metrics数据统计开始
		MetricsTool.MetricsTimer mTimer = MetricsTool.getAndStartTimer(RedisClient.class, logFlag);
		Long res = null;
		Jedis jds = null;
		try
		{
			jds = jedisPool.getResource();
			res = jds.lpush(key, strings);

			jedisPool.returnResource(jds);
			mTimer.stop();
			return res;
		}
		catch (Exception e)
		{
			jedisPool.returnBrokenResource(jds);
			logger.warn("[lid:{}][{}] Redis exeception！ parameters: key {}, strings:{}, detail:{}", logIndex, logFlag, key,
					java.util.Arrays.toString(strings), BasicException.getStackTrace(e));
			mTimer.stop();
			res = (long) 0;
			return null;
		}
	}

	public Long rpush(long logIndex, String key, String[] strings)
	{
		String logFlag = getClassName() + ".rpush";
		//metrics数据统计开始
		MetricsTool.MetricsTimer mTimer = MetricsTool.getAndStartTimer(RedisClient.class, logFlag);
		Long res = null;
		Jedis jds = null;
		try
		{
			jds = jedisPool.getResource();
			res = jds.rpush(key, strings);

			jedisPool.returnResource(jds);
			mTimer.stop();
			return res;
		}
		catch (Exception e)
		{
			jedisPool.returnBrokenResource(jds);
			logger.warn("[lid:{}][{}] Redis exeception！ parameters: key {}, strings:{}, detail:{}", logIndex, logFlag, key,
					java.util.Arrays.toString(strings), BasicException.getStackTrace(e));
			mTimer.stop();
			res = (long) 0;
			return null;
		}
	}
	
	public long expired(long logIndex, String key, int seconds)
	{
		String logFlag = getClassName() + ".expired";
		//metrics数据统计开始
		MetricsTool.MetricsTimer mTimer = MetricsTool.getAndStartTimer(RedisClient.class, logFlag);
		Long res = null;
		Jedis jds = null;
		try
		{
			jds = jedisPool.getResource();
			res = jds.expire(key, seconds);
			jedisPool.returnResource(jds);
			mTimer.stop();
			return res;
		}
		catch (Exception e)
		{
			jedisPool.returnBrokenResource(jds);
			logger.warn("[lid:{}][{}] Redis set expired exeception！ parameters: key {}, detail:{}", logIndex, logFlag, key, BasicException.getStackTrace(e));
			mTimer.stop();
			res = (long) 0;
			return res;
		}
	}
	
	public String setex(long logIndex, String key, String value, int expiretime)
	{
		String logFlag = getClassName() + ".setex";
		//metrics数据统计开始
		MetricsTool.MetricsTimer mTimer = MetricsTool.getAndStartTimer(RedisClient.class, logFlag);
		String res;
		Jedis jds = null;
		try
		{
			jds = jedisPool.getResource();
			res = jds.setex(key, expiretime, value);

			jedisPool.returnResource(jds);
			mTimer.stop();
			return res;
		}
		catch (Exception e)
		{
			jedisPool.returnBrokenResource(jds);
			logger.warn("[lid:{}][{}] Redis exeception！ parameters: key {}, value:{}, expiretime:{} detail:{}", logIndex, logFlag, key, value,expiretime, BasicException.getStackTrace(e));
			mTimer.stop();
			return null;
		}
	}
	/*
	 * 判断key的类型
	 * 返回值类型：
	 * "string"表示的key:value类型
	 * "list"表示List
	 * "set"表示Set
	 * "zset"表示Sorted Set
	 * "hash"表示hash
	 * "none"表示key不存在
	 */
	public String type(long logIndex, String key)
	{
		String logFlag = getClassName() + ".type";
		//metrics数据统计开始
		MetricsTool.MetricsTimer mTimer = MetricsTool.getAndStartTimer(RedisClient.class, logFlag);
		String res = null;
		Jedis jds = null;
		try
		{
			jds = jedisPool.getResource();
			res = jds.type(key);

			jedisPool.returnResource(jds);
			mTimer.stop();
			return res;
		}
		catch (Exception e)
		{
			jedisPool.returnBrokenResource(jds);
			logger.warn("[lid:{}][{}] Redis exeception！ parameters: key {}, detail:{}", logIndex, logFlag, key, BasicException.getStackTrace(e));
			mTimer.stop();
			return null;
		}
	}

}
