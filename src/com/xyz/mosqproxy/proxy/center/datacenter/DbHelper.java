package com.xyz.mosqproxy.proxy.center.datacenter;

import java.beans.PropertyVetoException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.xyz.mosqproxy.common.DefaultValues;
import com.xyz.mosqproxy.common.MetricsTool;

/**
 * DB 访问辅助类
 * 
 * @author 
 */
public class DbHelper
{
	private static final Logger m_logger = LoggerFactory.getLogger(DbHelper.class);
	//metrics数据统计工具
//	private MetricsTool m_metricsTool = MetricsTool.getInstance();
	
	/**
	 * DB 连接池。使用 getConnection() 方法获取一条新的数据库连接
	 */
	private ComboPooledDataSource m_connectionPool = null;
	
	private String getClassName()
	{
		return "DbHelper";
	}

	public void chineseCharTest()
	{
		String sql1 = "INSERT INTO server_config_table(category,`key`,value) VALUES(2, \"char_test.from_java.case1\", \"来自Java的第1个Case\")";

		String sql2 = "INSERT INTO server_config_table(category,`key`,value) VALUES(?, ?, ?)";

		String sql3 = "select * from server_config_table";

		Connection conn = null;

		try
		{
			conn = m_connectionPool.getConnection();
			PreparedStatement ps1 = conn.prepareStatement(sql1);
			ps1.execute();

			PreparedStatement ps2 = conn.prepareStatement(sql2);
			ps2.setInt(1, 2);
			ps2.setString(2, "char_test.from_java.case2");
			ps2.setString(3, "来自Java的第2个Case");
			ps2.execute();

			// output
			PreparedStatement ps3 = conn.prepareStatement(sql3);
			ResultSet rs = ps3.executeQuery();
			while (rs.next())
			{
				int cat = rs.getInt("category");
				String key = rs.getString("key");
				String value = rs.getString("value");
				System.out.println(cat + "\t" + key + "\t" + value);
			}
			safeClose(DefaultValues.LOG_INDEX_TEST, ps1);
			safeClose(DefaultValues.LOG_INDEX_TEST, ps2);
			safeClose(DefaultValues.LOG_INDEX_TEST, ps3);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		finally
		{
			safeClose(DefaultValues.LOG_INDEX_TEST, conn);
		}
	}

	/**
	 * 初始化数据库连接池
	 * @param jdbcUrl String
	 * @param userName String
	 * @param password String
	 * @param minPoolSize int
	 * @param maxPoolSize int
	 * @param maxIdleTime int
	 * @param maxStateMents int
	 * @param checkoutTimeout int
	 * @throws PropertyVetoException
	 */
	public boolean intializeConnectionPool(String jdbcUrl, String userName, String password, int minPoolSize, int maxPoolSize, int maxIdleTime, int maxStateMents, int checkoutTimeout)
			throws PropertyVetoException
	{
		m_connectionPool = new ComboPooledDataSource();
		m_connectionPool.setDriverClass("com.mysql.jdbc.Driver");
		m_connectionPool.setJdbcUrl(jdbcUrl);
		m_connectionPool.setUser(userName);
		m_connectionPool.setPassword(password);

		m_connectionPool.setMinPoolSize(minPoolSize);
		m_connectionPool.setMaxPoolSize(maxPoolSize);
		m_connectionPool.setMaxIdleTime(maxIdleTime);
		m_connectionPool.setMaxStatements(maxStateMents);
		m_connectionPool.setCheckoutTimeout(checkoutTimeout);
		return true;
	}
	
	public JSONObject getPoolInfo(long logIndex)
	{
		String logFlag = getClassName() + ".getPoolInfo";
		JSONObject joPoolInfo = new JSONObject();
		try
		{
			joPoolInfo.put("NumBusyConnections", m_connectionPool.getNumBusyConnections());
			joPoolInfo.put("NumIdleConnections", m_connectionPool.getNumIdleConnections());
			joPoolInfo.put("NumConnections", m_connectionPool.getNumConnections());
			joPoolInfo.put("MinPoolSize", m_connectionPool.getMinPoolSize());
			joPoolInfo.put("MaxPoolSize", m_connectionPool.getMaxPoolSize());
		}
		catch (SQLException ex)
		{
			m_logger.warn("[lid:{}][{}] get getPoolInfo failed! detail:{}", logIndex, logFlag, ex);
			return null;
		}
		return joPoolInfo;
	}

	/**
	 * 关闭数据库连接池
	 */
	public void shutdownConnectionPool(long logIndex)
	{
		String logFlag = getClassName() + ".shutdownConnectionPool";
		try
		{
			if (m_connectionPool != null)
				m_connectionPool.close();
		}
		catch (Exception ex)
		{
			m_logger.warn("[lid:{}][{}] Close Connection Pool failed! detail:{}", logIndex, logFlag, ex);
		}
	}

	/**
	 * 获取数据库连接，记得用完后要 close
	 * 
	 * @return
	 * @throws SQLException
	 */
	public Connection getConnection(long logIndex) 
	{
		String logFlag = getClassName() + ".getConnection";
		try
		{
			return m_connectionPool.getConnection();
		}
		catch (SQLException e)
		{
			m_logger.error("[lid:{}][{}] exception happened! get connection from friendscircle db fail, detail:{}", logIndex, logFlag, e);
			return null;
		}
	}
	
	/*
	 * 下列函数用于事务相关操作
	 * */
	
	public boolean commit(long logIndex, Connection conn)
	{
		String logFlag = getClassName() + ".commit";
		if(conn == null)
			return false;
		try
		{
			conn.commit();
			return true;
		}
		catch (SQLException e)
		{
			m_logger.warn("[lid:{}][{}] Close Connection Pool failed! detail:{}", logIndex, logFlag, e);
			return false;
		}
	}
	
	
	public boolean rollback(long logIndex, Connection conn)
	{
		String logFlag = getClassName() + ".rollback";
		if(conn == null)
			return false;
		try
		{
			conn.rollback();
			return true;
		}
		catch (SQLException e)
		{
			m_logger.warn("[lid:{}][{}] Close Connection Pool failed! detail:{}", logIndex, logFlag, e);
			return false;
		}
	}
	
	/**实现JDBC的事务操作
	 * @author songbin
	 * @param logIndex long 日至索引
	 * @param sql String sql语句模版
	 * @param params List<JSONArray> 参数
	 * @return true or false
	 * */
	public boolean transcationSql(long logIndex, String sql, List<JSONArray> params)
	{
		String logFlag = this.getClassName() + ".insertSql_transcation_ext";
		PreparedStatement ps = null;
		Connection conn = null;

		if (params.isEmpty())
		{
			return true;
		}

		try
		{
			conn = this.getConnection(logIndex);
			conn.setAutoCommit(false);
			ps = (PreparedStatement) conn.prepareStatement(sql);

			for (JSONArray jsonItem : params)
			{
				// for(Iterator it = jsonItem.iterator(); it.hasNext() ;){}
				for (int index = 0; index < jsonItem.size(); index++)
				{
					if (jsonItem.get(index) instanceof String)
						ps.setString(index + 1, jsonItem.getString(index));
					else if (jsonItem.get(index) instanceof Integer)
						ps.setInt(index + 1, jsonItem.getIntValue(index));
					else if (jsonItem.get(index) instanceof Double)
						ps.setDouble(index + 1, (Double) jsonItem.getDouble(index));
					else if (jsonItem.get(index) instanceof Long)
						ps.setDouble(index + 1, (Long) jsonItem.getLong(index));
					else if (jsonItem.get(index) instanceof Timestamp)
						ps.setTimestamp(index + 1, (Timestamp) jsonItem.get(index));
					else
					{
						m_logger.warn("[lid:{}][{}] Unsupported SQL query param:{}", logIndex, logFlag, jsonItem.get(index).getClass().getSimpleName());
						break;
					}

					// ps.setString(index+1, jsonItem.getString(index));
				}

				ps.addBatch();
			}

			ps.executeBatch();
			conn.commit();
			return true;
		}
		catch (SQLException e)
		{
			m_logger.warn("[lid:{}][{}] exception happened! detail:{}", logIndex, logFlag, e);
			if( !rollback(logIndex, conn) )
			{
				m_logger.warn("[lid:{}][{}] rollback failed!", logIndex, logFlag);
			}
			
			return false;
		}
		finally
		{
			try
			{
				if (conn != null)
				{
					conn.setAutoCommit(true);
				}
				this.safeClose(logIndex, ps, conn);
			}
			catch (SQLException e)
			{
				m_logger.warn("[lid:{}][{}] free resource exception happened! detail:{}", logIndex, logFlag, e);
			}
		}

	}
	
	
	public boolean updateSql_transcation(long logIndex, Connection conn,String sql,String[] params)
	{
		String logFlag = getClassName() + ".updateSql_transcation";
		try
		{
			conn.setAutoCommit(false);
			PreparedStatement ps = (PreparedStatement) conn.prepareStatement(sql);

			for (int i = 1; i <= params.length; i++)
			{
				ps.setString(i, params[i - 1]);
			}
			ps.addBatch();
			ps.execute();
			ps.close();
			return true;
		}
		catch (SQLException e)
		{
			m_logger.warn("[lid:{}][{}] exception happened, sql:{}, detail:{}", logIndex, logFlag, getPreparedSQL(sql, params), e);
			return false;
		}
		finally
		{
			try
			{
				conn.setAutoCommit(true);
			}
			catch (SQLException e)
			{
				m_logger.warn("[lid:{}][{}] exception happened, sql:{}, detail:{}", logIndex, logFlag, getPreparedSQL(sql, params), e);
				return false;
			}
		}
		
	 
	}
	
//
//	public int insertSql(String sql,String[] params)
//	{
//		Connection conn = null;
//		PreparedStatement ps = null;
//		int insertedNum = 0;
//		try {
//			conn = m_connectionPool.getConnection();
//			ps=(PreparedStatement) conn.prepareStatement(sql);
//
//			for (int i = 1; i <= params.length; i++){
//				ps.setString(i, params[i - 1]);
//			}	
//			insertedNum = ps.executeUpdate(sql);
//		}catch (SQLException e)
//		{
//			m_logger.warn(String.format("[insertSql]exception happened, sql: %s \n; detail: \n",getPreparedSQL(sql,params)),e);
//		}
//		finally
//		{
//			safeClose(ps,conn);
//
//		}
//		return insertedNum;
//	}
	
	/**
	 * 执行一个SQL语句。
	 * 
	 * 举例：
	 * 
	 * querySql("{ call p_nwc_exit_meetingroom(?, ?, ?) }", new
	 * Object[]{meetingNumber, callee});
	 * 
	 * @param sql
	 *            SQL 语句
	 * @param params
	 *            输入参数。注意参数和表的 field 的类型、个数必须对应
	 * @return
	 * @throws SQLException
	 */
	@SuppressWarnings("resource")
	public ArrayList<HashMap<String, String>> querySql(long logIndex, String sql, Object[] params) 
	{
		String logFlag = getClassName() + "querySql";
		//metrics数据统计开始
		MetricsTool.MetricsTimer mTimer = MetricsTool.getAndStartTimer(DbHelper.class, "querySql");
		
		if (m_logger.isDebugEnabled())
		{
			m_logger.debug(String.format("[lid:{}][{}] sql:{}", logIndex, logFlag, getPreparedSQL(sql, params)));
		}
		
		ResultSet 			rs 	 = null;
		Connection 			conn = null;
		PreparedStatement 	ps 	 = null;
		
		ArrayList<HashMap<String, String>> rows = new ArrayList<HashMap<String, String>>();
		try
		{
			conn = m_connectionPool.getConnection();
			ps = (PreparedStatement) conn.prepareStatement(sql);
			if (params != null)
			{
				for (int i = 1; i <= params.length; i++)
				{
					if (params[i - 1] instanceof String)
						ps.setString(i, params[i - 1].toString());
					else if (params[i - 1] instanceof Integer)
						ps.setInt(i, (Integer) params[i - 1]);
					else
					{
						m_logger.warn("[lid:{}][{}] Unsupported SQL query param:{}", logIndex, logFlag, params[i - 1].getClass().getSimpleName());
						safeClose(logIndex, conn);
						safeClose(logIndex, ps);
						
						//metrics数据统计结束
						mTimer.stop();
						return null;
					}
				}
			}
			// 获取结果集
			rs = ps.executeQuery();
			// 循环读数据，数据库里面的每一行数据就是list的一个节点，
			ResultSetMetaData rsmd = (ResultSetMetaData) rs.getMetaData();
			while (rs.next())
			{// 数据库中具体每行的数据，都用该行的“名字-值”方式的存储在HashMap中
				HashMap<String, String> row = new HashMap<String, String>();
				for (int i = 1; i <= rsmd.getColumnCount(); i++)
				{
					String colName = rsmd.getColumnLabel(i);
					String colValue = rs.getString(i);
					row.put(colName, colValue);
				}
				rows.add(row);
			}
		}catch(SQLException ex)
		{
			m_logger.warn("[lid:{}][{}] exception happened, sql:{} detail:{}", logIndex, logFlag, getPreparedSQL(sql,params),ex);
			//metrics数据统计结束
			mTimer.stop();
			return null;
		}
		finally
		{
			safeClose(logIndex, rs,ps,conn);
		}
		//metrics数据统计结束
		mTimer.stop();
		return rows;
	}

	/**
	 * 执行一个 UPDATE 的 SQL语句，返回值是影响到的行数。
	 * 也支持replace to操作
	 * @param sql
	 * @param params
	 * @return UPDATE 影响到的行数，如果 <=0，说明 Update 失败
	 * @throws SQLException
	 */
	@SuppressWarnings("resource")
	public int executeUpdate(long logIndex, String sql, Object[] params) 
	{
		String logFlag = getClassName() + "executeUpdate";
		//metrics数据统计开始
		MetricsTool.MetricsTimer mTimer = MetricsTool.getAndStartTimer(DbHelper.class, "executeUpdate");
		
		if (m_logger.isDebugEnabled())
		{
			m_logger.debug("[lid:{}][{}] sql:{}", logIndex, getPreparedSQL(sql, params));
		}
		Connection conn = null;
		PreparedStatement ps = null;
		int updateCount = -1;
		try
		{
			conn = m_connectionPool.getConnection();
			ps = (PreparedStatement) conn.prepareStatement(sql);
			
			if (params != null)
			{
				for (int i = 1; i <= params.length; i++)
				{
					if (params[i - 1] instanceof String)
						ps.setString(i, params[i - 1].toString());
					else if (params[i - 1] instanceof Integer)
						ps.setInt(i, (Integer) params[i - 1]);
					else if (params[i - 1] instanceof Double)
						ps.setDouble(i, (Double) params[i - 1]);
					else if (params[i - 1] instanceof Long)
						ps.setDouble(i, (Long) params[i - 1]);
					else if (params[i - 1] instanceof Timestamp)
					ps.setTimestamp(i, (Timestamp)params[i - 1]);
				else
					{
						m_logger.warn("[lid:{}][{}] Unsupported SQL query param:{}", logIndex, logFlag, params[i - 1].getClass().getSimpleName());
						//metrics数据统计结束
						mTimer.stop();
						return updateCount;
					}
				}
			}
			// 获取结果集
			boolean result = ps.execute();
			if (!result)
				updateCount = ps.getUpdateCount();
			else
				m_logger.warn("[lid:{}][{}] Result is a ResultSet, this function is for UPDAT. SQL={}", logIndex, logFlag, getPreparedSQL(sql, params));
			//metrics数据统计结束
			mTimer.stop();
			return updateCount;
			
		}
		catch (Exception e)
		{
			m_logger.warn("[lid:{}][{}] exception happened.. detail:{}", logIndex, logFlag, e);
		}
		finally
		{
			safeClose(logIndex, ps);
			safeClose(logIndex, conn);
		}
		//metrics数据统计结束
		mTimer.stop();
		return updateCount;
	}
	
	
	@SuppressWarnings("resource")
	public int insertSql(long logIndex, String sql, Object[] params) 
	{
		String logFlag = getClassName() + "insertSql";
		//metrics数据统计开始
		MetricsTool.MetricsTimer mTimer = MetricsTool.getAndStartTimer(DbHelper.class, "insertSql");
		
		if (m_logger.isDebugEnabled())
		{
			m_logger.debug("[lid:{}][{}] sql:{}", logIndex, getPreparedSQL(sql, params));
		}
		
		Connection conn = null;
		PreparedStatement ps = null;
		int updateCount = -1;
		try
		{
			conn = m_connectionPool.getConnection();
			ps = (PreparedStatement) conn.prepareStatement(sql);
			if (params != null)
			{
				for (int i = 1; i <= params.length; i++)
				{
					if (params[i - 1] instanceof String)
						ps.setString(i, params[i - 1].toString());
					else if (params[i - 1] instanceof Integer)
						ps.setInt(i, (Integer) params[i - 1]);
					else if (params[i - 1] instanceof Double)
						ps.setDouble(i, (Double) params[i - 1]);
					else if (params[i - 1] instanceof Long)
						ps.setDouble(i, (Long) params[i - 1]);
					else if (params[i - 1] instanceof Timestamp)
						ps.setTimestamp(i, (Timestamp)params[i - 1]);
					else
					{
						m_logger.warn("[lid:{}][{}] Unsupported SQL query param:{}", logIndex, logFlag, params[i - 1].getClass().getSimpleName());
						// metrics数据统计结束
						mTimer.stop();
						return -1;
					}
				}
			}
			// 获取结果集
			updateCount = ps.executeUpdate();
		}
		catch (SQLException e)
		{
			m_logger.warn("[lid:{}][{}] exception happened..", logIndex, logFlag, e);
		}
		finally
		{
			safeClose(logIndex, ps);
			safeClose(logIndex, conn);
		}
		//metrics数据统计结束
		mTimer.stop();
		return updateCount;
	}

	/**
	 * 执行一个存储过程。目前参数仅支持字符串、整型；返回值也只支持整型。
	 * 
	 * 举例：
	 * 
	 * int ret =
	 * executeStoreProcedure("{ call p_nwc_exit_meetingroom(?, ?, ?) }", new
	 * Object[]{meetingNumber, callee});
	 * 
	 * @param conn
	 *            db 连接
	 * @param sql
	 *            存储过程的 SQL，如："{ call p_nwc_exit_meetingroom(?, ?, ?) }"
	 * @param params
	 *            输入参数。
	 * @return 返回值，整数
	 * @throws SQLException
	 */
	public int executeStoreProcedure(long logIndex, Connection conn, String sql, Object[] params) throws SQLException
	{
		String logFlag = getClassName() + "executeStoreProcedure";
		//metrics数据统计开始
		MetricsTool.MetricsTimer mTimer = MetricsTool.getAndStartTimer(DbHelper.class, "executeStoreProcedure");
		
		if (m_logger.isDebugEnabled())
		{
			StringBuffer sb = new StringBuffer();
			sb.append("Execute Stored Precedure: ").append(sql).append(", Params: ");
			for (int i = 0; i < params.length; i++)
			{
				sb.append(params[i].toString());
				if (i < (params.length - 1))
					sb.append(",");
			}
			m_logger.debug("[lid:{}][{}] {}", logIndex, logFlag, sb.toString());
		}

		int ret = 0;

		CallableStatement cs = conn.prepareCall(sql);

		// 设置 SP 的输入参数，暂时只支持字符串和整型
		for (int i = 0; i < params.length; i++)
		{
			if (params[i] instanceof String)
			{
				cs.setString(i + 1, params[i].toString());
			}
			else if (params[i] instanceof Integer)
			{
				cs.setInt(i + 1, new Integer(params[i].toString()));
			}
			else
			{
				m_logger.warn("[lid:{}][{}] Unexpected param type{}", logIndex, logFlag, params[i]);
				//metrics数据统计结束
				mTimer.stop();
				return ret;
			}
		}

		// 设置 SP 的输出，暂时只支持整型
		cs.registerOutParameter(params.length + 1, java.sql.Types.INTEGER);

		if (!cs.execute())
		{
			ret = cs.getInt(params.length + 1); // 返回值
		}

		safeClose(logIndex, cs);

		if (m_logger.isDebugEnabled())
		{
			m_logger.debug("[lid:{}][{}] Stored Precedure: {}, returns:{}", logIndex, logFlag, sql, ret);
		}
		
		//metrics数据统计结束
		mTimer.stop();
		return ret;
	}

	/**
	 * 安全关闭结果集
	 * 
	 * @param rs
	 */
	public void safeClose(long logIndex, ResultSet rs)
	{
		String logFlag = getClassName() + "safeClose";
		try
		{
			if (rs != null)
				rs.close();
		}
		catch (Exception ex)
		{
			m_logger.warn("[lid:{}][{}] Close ResultSet failed, detail:{}", logIndex, logFlag, ex);
		}
	}

	/**
	 * 安全关闭 PreparedStatement
	 * 
	 * @param stat
	 */
	public void safeClose(long logIndex, PreparedStatement stat)
	{
		String logFlag = getClassName() + "safeClose";
		try
		{
			if (stat != null)
				stat.close();
		}
		catch (Exception ex)
		{
			m_logger.warn("[lid:{}][{}] Close PreparedStatement failed, detail:{}", logIndex, logFlag, ex);
		}
	}

	/**
	 * 安全关闭 Connection
	 * 
	 * @param conn
	 */
	public void safeClose(long logIndex, Connection conn)
	{
		String logFlag = getClassName() + "safeClose";
		try
		{
			if (conn != null)
				conn.close();
		}
		catch (Exception ex)
		{
			m_logger.warn("[lid:{}][{}] Close db connection failed, detail:{}", logIndex, logFlag, ex);
		}
	}

	/**
	 * 安全关闭多个 DB 对象（ResultSet, PreparedStatement, Connection)
	 * 
	 * @param objs
	 *            要关闭的 DB 对象（ResultSet, PreparedStatement,
	 *            Connection)，注意参数顺序一般应该是：ResultSet, PreparedStatement,
	 *            Connection
	 */
	public void safeClose(long logIndex, Object... objs)
	{
		String logFlag = getClassName() + "safeClose";
		try
		{
			for (Object obj : objs)
			{
				if (obj instanceof ResultSet)
					safeClose(logIndex, (ResultSet) obj);
				else if (obj instanceof PreparedStatement)
					safeClose(logIndex, (PreparedStatement) obj);
				else if (obj instanceof Connection)
					safeClose(logIndex, (Connection) obj);
				else if (obj != null)
					m_logger.warn("I don't known how to close: " + obj.getClass().getName());
			}
		}
		catch (Exception ex)
		{
			m_logger.warn("[lid:{}][{}] Close failed, detail:{}", logIndex, logFlag, ex);
		}
	}

	/**
	 * 获得PreparedStatement向数据库提交的SQL语句
	 */
	public String getPreparedSQL(String sql, Object[] params)
	{
		if (params == null || params.length == 0)
			return sql;

		StringBuffer returnSQL = new StringBuffer();
		String[] subSQL = sql.split("\\?");
		for (int i = 0; i < params.length; i++)
		{
			if (params[i] instanceof String)
				returnSQL.append(subSQL[i]).append(" '").append(params[i]).append("' ");
			else
				returnSQL.append(subSQL[i]).append(" ").append(params[i]).append(" ");
		}
		if (subSQL.length > params.length)
		{
			returnSQL.append(subSQL[subSQL.length - 1]);
		}
		return returnSQL.toString();
	}
	
	/**单纯的执行一个sql语句*/
	public boolean executeSql(long logIndex, String sql)
	{
		String logFlag = getClassName() + "executeSql";
		//metrics数据统计开始
		MetricsTool.MetricsTimer mTimer = MetricsTool.getAndStartTimer(DbHelper.class, "insertSql");
		
		if (m_logger.isDebugEnabled())
		{
			m_logger.debug("[lid:{}][{}] sql:{}", logIndex, logFlag, sql);
		}
		
		Connection conn = null;
		PreparedStatement ps = null;
		boolean hasResultSet = false;
		try
		{
			conn = m_connectionPool.getConnection();
			ps = (PreparedStatement) conn.prepareStatement(sql);
			// 获取结果集
			hasResultSet = ps.execute();
		}
		catch (SQLException e)
		{
			m_logger.warn("[lid:{}][{}] exception happened.. detail:{}", logIndex,logFlag, e);
		}
		finally
		{
			safeClose(logIndex, ps);
			safeClose(logIndex, conn);
		}
		//metrics数据统计结束
		mTimer.stop();
		return hasResultSet;
	}
	
}
