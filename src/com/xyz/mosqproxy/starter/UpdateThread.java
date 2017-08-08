package com.xyz.mosqproxy.starter;

import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xyz.mosqproxy.exception.BasicException;
import com.xyz.mosqproxy.proxy.center.ProxyCenter;

/***
 * 用来处理用户状态更新信息的线程，初始化时启动
 */
public class UpdateThread implements Runnable
{
	private static Logger logger = LoggerFactory.getLogger(UpdateThread.class);
	private long logIndex;
	private static AtomicLong jobIndex = new AtomicLong(System.currentTimeMillis());
	public UpdateThread(long logIndex)
	{
		super();
		this.logIndex = logIndex;
	}

	private String getClassName()
	{
		return "UpdateThread";
	}

	@Override
	public void run()
	{
		String logFlag = this.getClassName() + ".run";
		while (!ProxyCenter.getInstance().isTerminateUpThread())
		{
			try
			{
				ProxyCenter.getInstance().updateUserOnlineStatus(jobIndex.getAndIncrement());

			}catch (Exception e)
			{
				logger.warn("[lid:{}][{}] Exception happended! detail:{}", logIndex, logFlag, BasicException.getStackTrace(e));
			}
		}
	}

}
