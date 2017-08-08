namespace java com.xyz.mosqproxy.thrift.stub

include "thrift_datatype.thrift"

/****************************************************************************************************
* 服务接口
*****************************************************************************************************/
service MosqProxyStub
{
	thrift_datatype.ResStr getConnection(1:i64 logIndex, 2:string caller, 3:string ext),
    thrift_datatype.ResStr bindConnId(1:i64 logIndex, 2:string caller, 3:string appId, 4:string userId, 5:string devId, 6:string connId, 7:string ext),
    thrift_datatype.ResStr forceBindConnId(1:i64 logIndex, 2:string caller, 3:string appId, 4:string userId, 5:string devId, 6:string connId, 7:string ext),
	/**
	 * 查询指定用户的在线状态，需要指明待查询用户连接的是哪个mosquitto
	 * @param logIndex 	 日志索引
	 * @param caller	 调用方的标识
	 * @param appid   应用标识
	 * @param userid  用户标识
	 * @param devid   设备标识
	 * @param ext	 扩展参数，JSON字符串格式
	 * @return ResStr		 result保持操作结果， 成功时由value字段返回结果，value为JSON格式字符串，它包含online字段，字段值含义为：用户是否在线，1为在线，2为不在线，-1为未获取到在线情况
	 * */
	thrift_datatype.ResStr getClientStatus(1:i64 logIndex, 2:string caller, 3:string appId, 4:string userId, 5:string devId, 6:string ext),
	
	
	thrift_datatype.ResBool sendMsgToUser(1:i64 logIndex, 2:string caller, 3:string appId, 4:string userId, 5:string topic, 6:string msg, 7:string ext),
	
	thrift_datatype.ResBool sendMsgToDevType(1:i64 logIndex, 2:string caller, 3:string appId, 4:string userId, 5:string devType, 6:string topic, 7:string msg, 8:string ext),
	
	thrift_datatype.ResBool sendBroadcastMsg(1:i64 logIndex, 2:string caller, 3:string appId, 4:string topic, 5:string msg, 6:string ext),
	
	thrift_datatype.ResSetStr sendMsgToUsers(1:i64 logIndex, 2:string caller, 3:string appId, 4:set<string> dstUserIds, 5:string topic, 6:string msg, 7:string ext),
	
	/**
	 * 函数名称：echo
	 * 函数功能：传递一个字符串给Thrift服务器，服务器把这个字符串原封不动的返回
	 * @author houjixin
	 * @param  logIndex 	日志索引
	 * @param  caller 		调用方的标识，每个模块要调用本模块时都要提供调用方的标识；
	 * @param  srcStr			传递给Thrift服务器的字符串
	 * @param  ext   			扩展参数，JSON字符串格式
 	 * @return ResStr 				res中返回操作结果，value中返回字符串
	 * */
	thrift_datatype.ResStr echo(1:i64 logIndex, 2:string caller, 3:string srcStr, 4:string ext)
}
