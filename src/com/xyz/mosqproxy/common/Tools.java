package com.xyz.mosqproxy.common;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;

/*
 * 通用工具类，主要提供通用的工具操作，例如检查字符串，检查端口等
 * */
public class Tools {
	private static Logger logger = LoggerFactory.getLogger(Tools.class);
	private static final int MIN_PORT = 1;
	private static final int MAX_PORT = 65535;
	// 用于产生消息序列号
	private static AtomicLong localIdGen = new AtomicLong(System.currentTimeMillis());
	private static AtomicLong negativeIdGen = new AtomicLong(System.currentTimeMillis() * (-1));

	/**
	 * 以递减方式产生一个负的序列号，从-1000开始递减
	 * */
	public static long getSerial()
	{
		return negativeIdGen.getAndDecrement();
	}
	public static long getId(){return localIdGen.getAndIncrement();}
	
	/**
	 * 函数名：checkPort
	 * 函数功能：检查端口是否有效
	 * @author Jason.hou
	 * @param port 待检查的端口号码
	 * @return boolean 如果端口号在有效范围内，即[MIN_PORT,MAX_PORT]则返回成功，否则返回失败
	 * */
	public static boolean checkPort(int port)
	{
		return port>=MIN_PORT && port<=MAX_PORT;
	}
	
	public static JSONObject str2Json(long logIndex, String str)
	{
		String logFlag = "tools.str2Json";
		try
		{
			return JSONObject.parseObject(str);
		}
		catch (Exception ex)
		{
			logger.warn("[lid:{}][{}] exception happend! String:{} detail:{}", logIndex, logFlag, str, ex);
			return null;
		}
	}
	
	public static int getMin(int firstVal, int secVal)
	{
		return firstVal > secVal ? secVal : firstVal;
	}
	
	/**
	 * 函数名：str2Int
	 * @author joniers.jia
	 * 函数功能：
	 * @param  logIndex log id
	 * @param  param 待转换的字符串
	 * @return int 转换成功返回true，转换失败返回false
	 * */
	public static int str2Int(long logIndex, String param)
	{
		String logFlag = "tools.str2Int";
		int serverNum = -1;
		try
		{
			serverNum = Integer.valueOf(param).intValue();		
		}
		catch (Exception e)
		{
			logger.warn("[lid:{}][{}] exception happend! param:{} detail:{}", logIndex, logFlag, param, e);
			return -1;
		}
		return serverNum;
	}
	
	/**
	 * 函数名：encrypt
	 * @author joniers.jia
	 * 函数功能：
	 * @param  strSrc 需要加密的源字符串
	 * @param  strKey 用于加密的随机字符串
	 * @return String 返回加密后的字符串
	 * */
	public static String encrypt(String strSrc, String strKey)
	{
		char[] data = strSrc.toCharArray();
		char[] keyData = strKey.toCharArray();
		int keyIndex = 0;
		for (int i = 0; i < data.length; i++)
		{
			data[i] = (char)(data[i]^keyData[keyIndex]);
			if (++keyIndex == keyData.length)
			{
				keyIndex = 0;
			}
		}
		return new String(data);
	}

	public static String getRandomString(int length)
	{ // length表示生成字符串的长度
		String base = DefaultValues.KEY_BASE;
		Random random = new Random();
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < length; i++)
		{
			int number = random.nextInt(base.length());
			sb.append(base.charAt(number));
		}
		return sb.toString();
	} 
	public static int getRandomInt(long logIndex, int maxRandomRange)
	{
		Random rand =new Random();
		return rand.nextInt(maxRandomRange);	
	}
	
	public static void sleep(long logIndex, long t)
	{
		String logFlag = "tools.sleep";
		try
		{
			Thread.sleep(t);
		}
		catch (InterruptedException e)
		{
			logger.warn("[lid:{}][{}] exception happend! time:{} detail:{}", logIndex, logFlag, t, e);
		}
	}

}
