package com.xyz.mosqproxy.telnet;

import java.io.IOException;
import java.net.SocketException;

import net.wimpi.telnetd.io.BasicTerminalIO;
import net.wimpi.telnetd.io.toolkit.Editfield;
import net.wimpi.telnetd.net.Connection;
import net.wimpi.telnetd.net.ConnectionEvent;
import net.wimpi.telnetd.shell.Shell;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xyz.mosqproxy.common.DefaultValues;
import com.xyz.mosqproxy.common.MetricsTool;
import com.xyz.mosqproxy.starter.Starter;

public class TelnetShell implements Shell
{
	private static final Logger logger = LoggerFactory.getLogger(TelnetShell.class);

	// private static final long m_defaultLifetime = 86400 * 1000 * 7; // 7天

	private static final int LF = 10;
	private static final int CR = 13;

	private Connection m_Connection;
	private BasicTerminalIO m_IO;
	private Editfield m_EF;
	
//	//metrics数据统计工具
//	private MetricsTool m_metricsTool = MetricsTool.getInstance();

	@Override
	public void run(Connection con)
	{
		m_Connection = con;
		m_IO = m_Connection.getTerminalIO();
		m_Connection.addConnectionListener(this);
		String command = null;
		try
		{
			m_IO.write("\n" + Starter.SERVICE_NAME +" v" + Starter.VERSION + ", " + getUsage());

			boolean done = false;
			while (!done)
			{
				m_IO.write(BasicTerminalIO.CRLF + ">>");
				m_IO.flush();

				int i = m_IO.read();
				if (i == -1 || i == -2)
				{
					done = true;
				} else
				{
					m_EF = new Editfield(m_IO, "edit", 50);
					if (LF != i && CR != i)
					{
						m_EF.append((char) i);
					}
					m_EF.run();
					command = m_EF.getValue();

					done = processCommand(m_IO, command);

				}
			}
		} catch (SocketException se)
		{
			logger.debug("SocketException", se);
		} catch (IOException e)
		{
			logger.info("IOException", e);
		} catch (Exception e)
		{
			try
			{
				m_IO.write("Process command failed: " + e.getMessage() + "\r\n");
			} catch (IOException e1)
			{
				logger.warn("Write failed message failed: " + command, e1);
			}
			logger.warn("Process command failed: " + command, e);
		}
	}

	@Override
	public void connectionIdle(ConnectionEvent arg0)
	{
		try
		{
			m_IO.write("CONNECTION_IDLE");
			m_IO.flush();
		} catch (IOException e)
		{
			logger.warn("CONNECTION_IDLE", e);
		}
	}

	@Override
	public void connectionLogoutRequest(ConnectionEvent arg0)
	{
		try
		{
			m_IO.write("CONNECTION_LOGOUTREQUEST");
			m_IO.flush();
		} catch (IOException e)
		{
			logger.warn("CONNECTION_LOGOUTREQUEST", e);
		}
	}

	@Override
	public void connectionSentBreak(ConnectionEvent arg0)
	{
		try
		{
			m_IO.write("CONNECTION_BREAK");
			m_IO.flush();
		} catch (IOException e)
		{
			logger.warn("CONNECTION_BREAK", e);
		}
	}

	@Override
	public void connectionTimedOut(ConnectionEvent arg0)
	{
		try
		{
			m_IO.write("CONNECTION_TIMEDOUT");
			m_IO.flush();
		} catch (IOException e)
		{
			logger.warn("CONNECTION_TIMEDOUT", e);
		} finally
		{
			// close connection
			m_Connection.close();
		}
	}


	
	public static Shell createShell()
	{
		return new TelnetShell();
	}

	private void shutdownMainServer(BasicTerminalIO btio) throws IOException
	{
		String logItem = "Shutdown " + Starter.SERVICE_NAME + " ... bye.\r\n";
		writeBack(btio, logItem);
		logger.warn(logItem);

		new Thread()
		{
			public void run()
			{
				Starter.shutdown(DefaultValues.LOG_INDEX_INIT);
			}
		}.start();
	}

	private void getServerInfo(BasicTerminalIO btio) throws IOException 
	{
		writeBack(btio, Starter.getInfo(DefaultValues.LOG_INDEX_INIT));
	}
	
	/**
	 * 处理消息
	 * 
	 * @param btio
	 *            使用BasicTerminalIO向终端发送信息
	 * @param evt
	 * @return true：done，立即断开连接； false：NOT done，连接不断开
	 * @throws Exception
	 */
	private boolean processCommand(BasicTerminalIO btio, String evt)
			throws Exception
	{
		if (evt.length() == 0)
			return false;
		btio.write(BasicTerminalIO.CRLF);
		String[] strs = evt.split(" +");
		if (strs.length == 0)
			return false;
		for (int i = 0; i < strs.length; i++)
			strs[i].trim(); // 去掉头尾空格
		String cmd = strs[0]; // 第一个字符串是命令名
		if (cmd.equalsIgnoreCase("quit") || cmd.equalsIgnoreCase("exit")) // 退出本次登录
		{
			return true;
		} else if (cmd.endsWith("Shutdown")) // 退出 Main Server
		{
			shutdownMainServer(btio);
			return true;
		} else if(cmd.equalsIgnoreCase("service"))
		{
			getServerInfo(btio);
		}else if(cmd.equalsIgnoreCase("openmetrics") || cmd.equalsIgnoreCase("om"))
		{
			MetricsTool.start();
		}else if(cmd.equalsIgnoreCase("closemetrics") || cmd.equalsIgnoreCase("cm"))
		{
			MetricsTool.stop();
		}else
		{
			writeBack(btio, "Unknown command: " + evt + "\r\n");
		}
		return false;
	}

	private void writeBack(BasicTerminalIO btio, String msg) throws IOException
	{
		btio.write(msg);
		btio.flush();
	}

//	private void writeAndLog(BasicTerminalIO btio, String msg) throws IOException
//	{
//		logger.info(msg);
//		btio.write(msg);
//	}

	private String getUsage()
	{
		// SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		// long aliveTime =
		// Instlink.getConfig().getInt(ConfigKey.KEY_MES_EVICT_THRESHOLD) *
		// 1000; // 存活时间（毫秒）
		// long recvTimeThreshold = System.currentTimeMillis() - aliveTime; //
		// 换算为一个绝对时间

		StringBuffer sb = new StringBuffer();
		sb.append("Usage:")
				.append("\r\n=======================================================================")
				.append("\r\n-------------------telnet commands for ").append(Starter.SERVICE_NAME).append("--------------\r\n\n")
				.append("(1) quit(exit)\t\t(!!!)exit from telnet")
				.append("\r\n-----------------------------------------------------------------------\r\n\n")
				.append("(1) Shutdown\t\t(!!!)Shutdown ").append(Starter.SERVICE_NAME)
				.append("\r\n-----------------------------------------------------------------------\r\n\n")
				.append("(2) service\t\tGet information of ").append(Starter.SERVICE_NAME)
				.append("\r\n-----------------------------------------------------------------------\r\n\n")
				.append("(4) openmetrics (om)\t\tOpen metrics\n \t\t\t eg: openmetrics \n \t\t\t or: om")
				.append("\r\n-----------------------------------------------------------------------\r\n\n")
				.append("(5) closemetrics (cm)\t\tClose metrics\n \t\t\t eg: closemetrics \n \t\t\t or: cm")
				.append("\r\n-----------------------------------------------------------------------\r\n\n")
				.append("=======================================================================\r\n");
		return sb.toString();
	}
	
}
