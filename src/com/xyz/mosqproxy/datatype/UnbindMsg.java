package com.xyz.mosqproxy.datatype;

import org.apache.commons.lang3.StringUtils;

public class UnbindMsg extends SystemMsg
{
	private UnbindMsg(String appId, String userId, String devId, int type, String content)	{
		super(appId, userId, devId, type, content);
	}
	public boolean isValid(){
		return (getType() == SystemMsg.TYPE_INVALID) &&
				!StringUtils.isBlank(getAppId()) &&
				!StringUtils.isBlank(getUserId()) &&
				!StringUtils.isBlank(getDevId());
	}
	
	public static UnbindMsg Create(String appId, String userId, String devId, String content){
		if(StringUtils.isBlank(appId) || StringUtils.isBlank(userId))
			return null;
		return new UnbindMsg(appId, userId, devId, SystemMsg.TYPE_UNBIND, content);
	}
}
