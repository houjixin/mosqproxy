package com.xyz.mosqproxy.datatype;

import org.apache.commons.lang3.StringUtils;

import com.alibaba.fastjson.JSONObject;

/*
 * 系统消息，即mosqProxy内部向客户端发送的消息
 * */
public class SystemMsg
{
	public static int TYPE_UNBIND = 100;
	public static int TYPE_INVALID = 0;
	private String appId;
	private String userId;
	private String devId;
	private int type;
	private String content;
	
	public String toJsonString(){
		if(type == TYPE_INVALID)
			return null;
		JSONObject joCon = new JSONObject();
		if(!StringUtils.isBlank(appId))
			joCon.put("appId", appId);
		if(!StringUtils.isBlank(userId))
			joCon.put("userId", userId);
		if(!StringUtils.isBlank(devId))
			joCon.put("devId", devId);
		if(!StringUtils.isBlank(content))
			joCon.put("content", content);
		
		joCon.put("type", type);
		return joCon.toJSONString();
	}
	
	public boolean isValid(){
		return type != TYPE_INVALID;
	}
	public SystemMsg(){};
	public String getAppId()
	{
		return appId;
	}
	public void setAppId(String appId)
	{
		this.appId = appId;
	}
	public String getUserId()
	{
		return userId;
	}
	public void setUserId(String userId)
	{
		this.userId = userId;
	}
	public String getDevId()
	{
		return devId;
	}
	public void setDevId(String devId)
	{
		this.devId = devId;
	}
	public int getType()
	{
		return type;
	}
	public void setType(int type)
	{
		this.type = type;
	}
	public String getContent()
	{
		return content;
	}
	public void setContent(String content)
	{
		this.content = content;
	}

	public SystemMsg(String appId, String userId, String devId, int type, String content)
	{
		super();
		this.appId = appId;
		this.userId = userId;
		this.devId = devId;
		this.type = type;
		this.content = content;
	}

}
