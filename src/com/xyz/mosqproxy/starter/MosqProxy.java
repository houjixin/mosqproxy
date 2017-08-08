package com.xyz.mosqproxy.starter;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xyz.mosqproxy.common.MetricsTool;
import com.xyz.mosqproxy.common.ReturnCode;
import com.xyz.mosqproxy.config.Config;
import com.xyz.mosqproxy.datatype.ClientStatus;
import com.xyz.mosqproxy.datatype.ConnectionInfo;
import com.xyz.mosqproxy.datatype.RegResult;
import com.xyz.mosqproxy.exception.BasicException;
import com.xyz.mosqproxy.proxy.center.ProxyCenter;
import com.xyz.mosqproxy.thrift.datatype.ResBool;
import com.xyz.mosqproxy.thrift.datatype.ResSetStr;
import com.xyz.mosqproxy.thrift.datatype.ResStr;
import com.xyz.mosqproxy.thrift.stub.MosqProxyStub;

/**
 * Created by Jason on 2017/4/6.
 */
public class MosqProxy implements MosqProxyStub.Iface{
    private static Logger logger = LoggerFactory.getLogger(MosqProxy.class);
    private ProxyCenter proxyCenter = ProxyCenter.getInstance();
    private String getClassName(){return "MosqProxy";}
    public boolean isWorking = false;

    public void shutdown(long logIndex){
        proxyCenter.shutdown(logIndex);
    }

    public boolean init(long logIndex, Config cfg){
    	isWorking = proxyCenter.init(logIndex,cfg);
    	return isWorking;
    }


    @Override
    public ResStr getClientStatus(long logIndex, String caller, String appid, String userid, String devType, String ext) throws TException {
        String logFlag = getClassName()+ ".getClientStatus";
        MetricsTool.MetricsTimer mTimer = MetricsTool.getAndStartTimer(MosqProxy.class, logFlag);

        logger.info("[lid:{}][{}]>> caller:{}, appid:{}, userid:{}, devType:{}, ext:{}", logIndex, logFlag, caller, appid, userid, devType, ext);

        if (!isWorking)
        {
            logger.warn("[lid:{}][{}]<<  proxy is unworking!", logIndex, logFlag);
            return new ResStr(ReturnCode.SERVER_UNWORKING, null, null);
        }

        if (StringUtils.isBlank(caller) || StringUtils.isBlank(appid) || StringUtils.isBlank(userid) || StringUtils.isBlank(devType))
        {
            logger.warn("[lid:{}][{}]<<  parameter error!", logIndex, logFlag);
            return new ResStr(ReturnCode.PARAMETER_ERROR, null, null);
        }

        try
        {
            ClientStatus clientStatus = proxyCenter.getOnlineStatus(logIndex, appid, userid, devType);
            ResStr thriftRes = new ResStr(ReturnCode.SUCCESS, clientStatus.toJsonString(), null);
            logger.info("[lid:{}][{}]<< result: {}", logIndex, logFlag, thriftRes);
            return thriftRes;
        }
        catch (Exception e)
        {
            logger.warn("[lid:{}][{}]<< exception happened! detail:{}", logIndex, logFlag, e);
            return new ResStr(ReturnCode.INNER_ERROR, null, null);
        }
        finally
        {
            mTimer.stop();
        }
    }

    @Override
    public ResBool sendMsgToUser(long logIndex, String caller, String appId, String userId, String topic, String msg, String ext) throws TException {
        String logFlag = getClassName()+ ".sendMsgToUser";
        MetricsTool.MetricsTimer mTimer = MetricsTool.getAndStartTimer(MosqProxy.class, logFlag);

        logger.info("[lid:{}][{}]>> caller:{}, appid:{}, userId:{}, topic:{}, msg:{}, ext:{}", logIndex, logFlag, caller, appId, userId, topic, msg, ext);

        if (!isWorking)
        {
            logger.warn("[lid:{}][{}]<<  proxy is unworking!", logIndex, logFlag);
            return new ResBool(ReturnCode.SERVER_UNWORKING, false, null);
        }

        if (StringUtils.isBlank(caller) || StringUtils.isBlank(appId) || StringUtils.isBlank(msg))
        {
            logger.warn("[lid:{}][{}]<<  parameter error!", logIndex, logFlag);
            return new ResBool(ReturnCode.PARAMETER_ERROR, false, null);
        }

        try
        {
            boolean res = proxyCenter.sendMsgToUser(logIndex, appId, userId, topic, msg);
            ResBool thriftRes = new ResBool(ReturnCode.SUCCESS, res, null);
            logger.info("[lid:{}][{}]<< result: {}", logIndex, logFlag, thriftRes);
            return thriftRes;
        }
        catch (Exception e)
        {
            logger.warn("[lid:{}][{}]<< exception happened! detail:{}", logIndex, logFlag, e);
            return new ResBool(ReturnCode.INNER_ERROR, false, null);
        }
        finally
        {
            mTimer.stop();
        }
    }

    @Override
    public ResSetStr sendMsgToUsers(long logIndex, String caller, String appId, Set<String> dstUserIds, String topic, String msg, String ext) throws TException {
    	String logFlag = getClassName()+ ".sendMsgToUsers";
        MetricsTool.MetricsTimer mTimer = MetricsTool.getAndStartTimer(MosqProxy.class, logFlag);

        logger.info("[lid:{}][{}]>> caller:{}, appId:{}, dstuserids:{}, topic:{}, msg:{}, ext:{}", logIndex, logFlag, caller, appId, dstUserIds, topic, msg, ext);

        if (!isWorking)
        {
            logger.warn("[lid:{}][{}]<<  proxy is unworking!", logIndex, logFlag);
            return new ResSetStr(ReturnCode.SERVER_UNWORKING, dstUserIds, null);
        }

        if (StringUtils.isBlank(caller) || dstUserIds == null || dstUserIds.isEmpty() || StringUtils.isBlank(msg))
        {
            logger.warn("[lid:{}][{}]<<  parameter error!", logIndex, logFlag);
            return new ResSetStr(ReturnCode.PARAMETER_ERROR, dstUserIds, null);
        }
        Set<String> failList = new HashSet<String>(); 
        failList.addAll(dstUserIds);
        try
        {
        	for(String dstUserId : dstUserIds){
        		if(proxyCenter.sendMsgToUser(logIndex,appId,dstUserId,topic,msg)){
        			failList.remove(dstUserId);
        		}
        	}
        	ResSetStr res;
        	if(failList.size() == 0){
        		res = new ResSetStr(ReturnCode.SUCCESS, null, null);
        	}else{
        		res = new ResSetStr(ReturnCode.SUC_PARTIAL, failList, null);
        	}
         	logger.info("[lid:{}][{}]<< result: {}", logIndex, logFlag, res);
         	return res;
        }
        catch (Exception e)
        {
            logger.warn("[lid:{}][{}]<< exception happened! detail:{}", logIndex, logFlag, e);
            return new ResSetStr(ReturnCode.INNER_ERROR, failList, null);
        }
        finally
        {
            mTimer.stop();
        }
    }


	@Override
	public ResStr registerConnection(long logIndex, String caller, String appId, String userId, String devType, String ext) throws TException
	{
		String logFlag = getClassName()+ ".registerConnection";
        MetricsTool.MetricsTimer mTimer = MetricsTool.getAndStartTimer(MosqProxy.class, logFlag);

        logger.info("[lid:{}][{}]>> caller:{}, appId:{}, userId:{}, devType:{}, ext:{}", logIndex, logFlag, caller, appId, userId, devType, ext);

        if (!isWorking)
        {
            logger.warn("[lid:{}][{}]<<  proxy is unworking!", logIndex, logFlag);
            return new ResStr(ReturnCode.SERVER_UNWORKING, null, null);
        }

        if (StringUtils.isBlank(caller) || StringUtils.isBlank(appId) || StringUtils.isBlank(userId) || StringUtils.isBlank(devType))
        {
            logger.warn("[lid:{}][{}]<<  parameter error!", logIndex, logFlag);
            return new ResStr(ReturnCode.PARAMETER_ERROR, null, null);
        }

        try
        {
        	RegResult res = proxyCenter.regConnection(logIndex, appId, userId, devType, false);
            ResStr thriftRes = new ResStr(res.getCode(), res.getValue(), null);
            logger.info("[lid:{}][{}]<< result: {}", logIndex, logFlag, thriftRes);
            return thriftRes;
        }
        catch (Exception e)
        {
            logger.warn("[lid:{}][{}]<< exception happened! detail:{}", logIndex, logFlag, e);
            return new ResStr(ReturnCode.INNER_ERROR, null, null);
        }
        finally
        {
            mTimer.stop();
        }
	}

	@Override
	public ResStr bindConnId(long logIndex, String caller, String appId, String userId, String devType, String connId, String ext) throws TException
	{
		String logFlag = getClassName()+ ".bindConnId";
        MetricsTool.MetricsTimer mTimer = MetricsTool.getAndStartTimer(MosqProxy.class, logFlag);

        logger.info("[lid:{}][{}]>> caller:{}, appId:{}, userId:{}, devType:{}, connId:{}, ext:{}", logIndex, logFlag, caller, appId, userId, devType, connId, ext);

        if (!isWorking)
        {
            logger.warn("[lid:{}][{}]<<  proxy is unworking!", logIndex, logFlag);
            return new ResStr(ReturnCode.SERVER_UNWORKING, null, null);
        }

        if (StringUtils.isBlank(caller) || StringUtils.isBlank(appId) || StringUtils.isBlank(userId) || StringUtils.isBlank(devType) || StringUtils.isBlank(connId))
        {
            logger.warn("[lid:{}][{}]<<  parameter error!", logIndex, logFlag);
            return new ResStr(ReturnCode.PARAMETER_ERROR, null, null);
        }

        try
        {
        	RegResult res = proxyCenter.bindConnId(logIndex, appId, userId, devType, connId);
            ResStr thriftRes = new ResStr(res.getCode(), res.getValue(), null);
            logger.info("[lid:{}][{}]<< result: {}", logIndex, logFlag, thriftRes);
            return thriftRes;
        }
        catch (Exception e)
        {
            logger.warn("[lid:{}][{}]<< exception happened! detail:{}", logIndex, logFlag, e);
            return new ResStr(ReturnCode.INNER_ERROR, null, null);
        }
        finally
        {
            mTimer.stop();
        }
	}

	@Override
	public ResStr forceBindConnId(long logIndex, String caller, String appId, String userId, String devType, String connId, String ext) throws TException
	{
		String logFlag = getClassName()+ ".forceBindConnId";
        MetricsTool.MetricsTimer mTimer = MetricsTool.getAndStartTimer(MosqProxy.class, logFlag);

        logger.info("[lid:{}][{}]>> caller:{}, appId:{}, userId:{}, devType:{}, connId:{}, ext:{}", logIndex, logFlag, caller, appId, userId, devType, connId, ext);

        if (!isWorking)
        {
            logger.warn("[lid:{}][{}]<<  proxy is unworking!", logIndex, logFlag);
            return new ResStr(ReturnCode.SERVER_UNWORKING, null, null);
        }

        if (StringUtils.isBlank(caller) || StringUtils.isBlank(appId) || StringUtils.isBlank(userId) || StringUtils.isBlank(devType) || StringUtils.isBlank(connId))
        {
            logger.warn("[lid:{}][{}]<<  parameter error!", logIndex, logFlag);
            return new ResStr(ReturnCode.PARAMETER_ERROR, null, null);
        }

        try
        {
        	RegResult res = proxyCenter.forceBindConnId(logIndex, appId, userId, devType, connId);
            ResStr thriftRes = new ResStr(res.getCode(), res.getValue(), null);
            logger.info("[lid:{}][{}]<< result: {}", logIndex, logFlag, thriftRes);
            return thriftRes;
        }
        catch (Exception e)
        {
            logger.warn("[lid:{}][{}]<< exception happened! detail:{}", logIndex, logFlag, e);
            return new ResStr(ReturnCode.INNER_ERROR, null, null);
        }
        finally
        {
            mTimer.stop();
        }
	}

	@Override
	public ResStr forceRegisterConnection(long logIndex, String caller, String appId, String userId, String devType, String ext) throws TException
	{
		String logFlag = getClassName()+ ".forceRegisterConnection";
        MetricsTool.MetricsTimer mTimer = MetricsTool.getAndStartTimer(MosqProxy.class, logFlag);

        logger.info("[lid:{}][{}]>> caller:{}, appId:{}, userId:{}, devType:{}, ext:{}", logIndex, logFlag, caller, appId, userId, devType, ext);

        if (!isWorking)
        {
            logger.warn("[lid:{}][{}]<<  proxy is unworking!", logIndex, logFlag);
            return new ResStr(ReturnCode.SERVER_UNWORKING, null, null);
        }

        if (StringUtils.isBlank(caller) || StringUtils.isBlank(appId) || StringUtils.isBlank(userId) || StringUtils.isBlank(devType))
        {
            logger.warn("[lid:{}][{}]<<  parameter error!", logIndex, logFlag);
            return new ResStr(ReturnCode.PARAMETER_ERROR, null, null);
        }

        try
        {
        	RegResult res = proxyCenter.regConnection(logIndex, appId, userId, devType, true);
            ResStr thriftRes = new ResStr(res.getCode(), res.getValue(), null);
            logger.info("[lid:{}][{}]<< result: {}", logIndex, logFlag, thriftRes);
            return thriftRes;
        }
        catch (Exception e)
        {
            logger.warn("[lid:{}][{}]<< exception happened! detail:{}", logIndex, logFlag, e);
            return new ResStr(ReturnCode.INNER_ERROR, null, null);
        }
        finally
        {
            mTimer.stop();
        }
	}

	/**
	 * 向每个mosquitto的主题appId/topic上pub一条内容为msg的消息
	 * */
	@Override
	public ResBool sendBroadcastMsg(long logIndex, String caller, String appId, String topic, String msg, String ext) throws TException
	{
		String logFlag = getClassName()+ ".sendBroadcastMsg";
        MetricsTool.MetricsTimer mTimer = MetricsTool.getAndStartTimer(MosqProxy.class, logFlag);

        logger.info("[lid:{}][{}]>> caller:{}, appId:{}, topic:{}, msg:{}, ext:{}", logIndex, logFlag, caller, appId, topic, msg, ext);

        if (!isWorking)
        {
            logger.warn("[lid:{}][{}]<<  proxy is unworking!", logIndex, logFlag);
            return new ResBool(ReturnCode.SERVER_UNWORKING, false, null);
        }

        if (StringUtils.isBlank(caller) || StringUtils.isBlank(appId) || StringUtils.isBlank(msg))
        {
            logger.warn("[lid:{}][{}]<<  parameter error!", logIndex, logFlag);
            return new ResBool(ReturnCode.PARAMETER_ERROR, false, null);
        }

        try
        {
            boolean res = proxyCenter.sendBroadcastMsg(logIndex, appId, topic, msg);
            ResBool thriftRes = new ResBool(ReturnCode.SUCCESS, res, null);
            logger.info("[lid:{}][{}]<< result: {}", logIndex, logFlag, thriftRes);
            return thriftRes;
        }
        catch (Exception e)
        {
            logger.warn("[lid:{}][{}]<< exception happened! detail:{}", logIndex, logFlag, e);
            return new ResBool(ReturnCode.INNER_ERROR, false, null);
        }
        finally
        {
            mTimer.stop();
        }
	}

	@Override
	public ResStr getConnection(long logIndex, String caller, String ext) throws TException
	{
		String logFlag = getClassName()+ ".getConnection";
        MetricsTool.MetricsTimer mTimer = MetricsTool.getAndStartTimer(MosqProxy.class, logFlag);

        logger.info("[lid:{}][{}]>> caller:{}, ext:{}", logIndex, logFlag, caller, ext);

        if (!isWorking)
        {
            logger.warn("[lid:{}][{}]<<  proxy is unworking!", logIndex, logFlag);
            return new ResStr(ReturnCode.SERVER_UNWORKING, null, null);
        }

        if (StringUtils.isBlank(caller))
        {
            logger.warn("[lid:{}][{}]<<  parameter error!", logIndex, logFlag);
            return new ResStr(ReturnCode.PARAMETER_ERROR, null, null);
        }

        try
        {
        	ResStr thriftRes;
        	ConnectionInfo connInfo = proxyCenter.createNewConnection(logIndex);
        	if(connInfo == null || !connInfo.isValid()){
        		thriftRes = new ResStr(ReturnCode.INNER_ERROR, null, null);
        	}else{
        		thriftRes = new ResStr(ReturnCode.SUCCESS, connInfo.toOutSimpleString(), null);
        	}
            logger.info("[lid:{}][{}]<< result: {}", logIndex, logFlag, thriftRes);
            return thriftRes;
        }
        catch (Exception e)
        {
            logger.warn("[lid:{}][{}]<< exception happened! detail:{}", logIndex, logFlag, e);
            return new ResStr(ReturnCode.INNER_ERROR, null, null);
        }
        finally
        {
            mTimer.stop();
        }
	}

	@Override
	public ResBool sendMsgToDevType(long logIndex, String caller, String appId, String userId, String devType, String topic, String msg, String ext)
			throws TException
	{
		String logFlag = getClassName()+ ".sendMsgToDevType";
        MetricsTool.MetricsTimer mTimer = MetricsTool.getAndStartTimer(MosqProxy.class, logFlag);

        logger.info("[lid:{}][{}]>> caller:{}, appid:{}, userId:{}, devType:{}, topic:{}, msg:{}, ext:{}", logIndex, logFlag, caller, appId, userId, devType, topic, msg, ext);

        if (!isWorking)
        {
            logger.warn("[lid:{}][{}]<<  proxy is unworking!", logIndex, logFlag);
            return new ResBool(ReturnCode.SERVER_UNWORKING, false, null);
        }

        if (StringUtils.isBlank(caller)
        		|| StringUtils.isBlank(appId)
        		|| StringUtils.isBlank(userId)
        		|| StringUtils.isBlank(devType)
        		|| StringUtils.isBlank(msg))
        {
            logger.warn("[lid:{}][{}]<<  parameter error!", logIndex, logFlag);
            return new ResBool(ReturnCode.PARAMETER_ERROR, false, null);
        }

        try
        {
            boolean res = proxyCenter.sendMsgToDevType(logIndex, appId, userId, devType, topic, msg);
            ResBool thriftRes = new ResBool(ReturnCode.SUCCESS, res, null);
            logger.info("[lid:{}][{}]<< result: {}", logIndex, logFlag, thriftRes);
            return thriftRes;
        }
        catch (Exception e)
        {
            logger.warn("[lid:{}][{}]<< exception happened! detail:{}", logIndex, logFlag, e);
            return new ResBool(ReturnCode.INNER_ERROR, false, null);
        }
        finally
        {
            mTimer.stop();
        }
	}
	

    @Override
    public ResStr echo(long logIndex, String caller, String srcStr, String ext) throws TException {
        String logFlag = getClassName() + ".echo";
        // metrics数据统计开始
        MetricsTool.MetricsTimer mTimer = MetricsTool.getAndStartTimer(MosqProxy.class, logFlag);

        try
        {
            if (!isWorking)
            {
                logger.warn("[lid:{}][{}] service is unworking!", logIndex, logFlag);
                return new ResStr(ReturnCode.SERVER_UNWORKING, null, null);
            }

            if (StringUtils.isBlank(caller))
            {
                logger.warn("[lid:{}][{}] parameter error!", logIndex, logFlag);
                return new ResStr(ReturnCode.PARAMETER_ERROR, null, null);
            }

            return new ResStr(ReturnCode.SUCCESS, srcStr, null);
        }
        catch (Exception e)
        {
            logger.warn("[lid:{}][{}]<< exception happened! detail:{}", logIndex, logFlag, BasicException.getStackTrace(e));
            return new ResStr(ReturnCode.INNER_ERROR, null, null);
        }
        finally
        {
            mTimer.stop();
        }
    }
}
