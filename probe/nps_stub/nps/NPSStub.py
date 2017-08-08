# -*- coding:utf-8 -*-
#
# Autogenerated by Thrift Compiler (1.0.0-dev)
#
# DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
#
#  options string: py
#

from thrift.Thrift import TType, TMessageType, TException, TApplicationException
from ttypes import *
from thrift.Thrift import TProcessor
from thrift.transport import TTransport
from thrift.protocol import TBinaryProtocol, TProtocol
try:
  from thrift.protocol import fastbinary
except:
  fastbinary = None


class Iface:
  """
  **************************************************************************************************
   服务接口
  ***************************************************************************************************
  """
  def checkOnlineStatus(self, logIndex, caller, user, ext):
    """
    查询指定用户的在线状态，需要指明待查询用户连接的是哪个mosquitto
    @param i64 logIndex 	 日志索引
    @param String caller	 调用方的标识
    @param String user	 待查询用户的标识
    @param string ext	 扩展参数，JSON字符串格式
    @return ResInt		result保持操作结果， value保持返回值，用户是否在线，1为在线，2为不在线，-1为未获取到在线情况
     

    Parameters:
     - logIndex
     - caller
     - user
     - ext
    """
    pass

  def mosqPub(self, logIndex, caller, topic, content, ext):
    """
    函数名：pubMosq
    函数功能：向指定mosquitto服务器的某个topic发送一条通知，需要指明待查询用户连接的是哪个mosquitto
    @param i64 logIndex 日志索引
    @param String caller	 调用方的标识
    @param String topic 发送的主题
    @param String content 消息的内容
    @param string ext	 扩展参数，JSON字符串格式
    @return ResBool result保持操作结果， value保持返回值，成功为true，失败为false
     

    Parameters:
     - logIndex
     - caller
     - topic
     - content
     - ext
    """
    pass

  def getMosquitto(self, logIndex, caller, user, ext):
    """
    函数名：getMosquitto
    函数功能：根据用户的标识为用户分配一个mosquito
    @param i64 logIndex 日志索引
    @param String caller	 调用方的标识
    @param string user  用户的标识
    @param string ext   扩展参数，JSON字符串格式
    @return ResStr result保持操作结果， value保持返回值，成功为JSON格式的mosquito信息，失败为null
     

    Parameters:
     - logIndex
     - caller
     - user
     - ext
    """
    pass

  def echo(self, logIndex, caller, srcStr, ext):
    """
    * 函数名称：echo
    * 函数功能：传递一个字符串给Thrift服务器，服务器把这个字符串原封不动的返回
    * @author houjixin
    * @param long(i64) logIndex 	日志索引
    * @param string caller 		调用方的标识，每个模块要调用本模块时都要提供调用方的标识；
    * @param string srcStr			传递给Thrift服务器的字符串
    * @param string ext   			扩展参数，JSON字符串格式
     * @return ResStr 				res中返回操作结果，value中返回字符串
    *

    Parameters:
     - logIndex
     - caller
     - srcStr
     - ext
    """
    pass


class Client(Iface):
  """
  **************************************************************************************************
   服务接口
  ***************************************************************************************************
  """
  def __init__(self, iprot, oprot=None):
    self._iprot = self._oprot = iprot
    if oprot is not None:
      self._oprot = oprot
    self._seqid = 0

  def checkOnlineStatus(self, logIndex, caller, user, ext):
    """
    查询指定用户的在线状态，需要指明待查询用户连接的是哪个mosquitto
    @param i64 logIndex 	 日志索引
    @param String caller	 调用方的标识
    @param String user	 待查询用户的标识
    @param string ext	 扩展参数，JSON字符串格式
    @return ResInt		result保持操作结果， value保持返回值，用户是否在线，1为在线，2为不在线，-1为未获取到在线情况
     

    Parameters:
     - logIndex
     - caller
     - user
     - ext
    """
    self.send_checkOnlineStatus(logIndex, caller, user, ext)
    return self.recv_checkOnlineStatus()

  def send_checkOnlineStatus(self, logIndex, caller, user, ext):
    self._oprot.writeMessageBegin('checkOnlineStatus', TMessageType.CALL, self._seqid)
    args = checkOnlineStatus_args()
    args.logIndex = logIndex
    args.caller = caller
    args.user = user
    args.ext = ext
    args.write(self._oprot)
    self._oprot.writeMessageEnd()
    self._oprot.trans.flush()

  def recv_checkOnlineStatus(self):
    iprot = self._iprot
    (fname, mtype, rseqid) = iprot.readMessageBegin()
    if mtype == TMessageType.EXCEPTION:
      x = TApplicationException()
      x.read(iprot)
      iprot.readMessageEnd()
      raise x
    result = checkOnlineStatus_result()
    result.read(iprot)
    iprot.readMessageEnd()
    if result.success is not None:
      return result.success
    raise TApplicationException(TApplicationException.MISSING_RESULT, "checkOnlineStatus failed: unknown result")

  def mosqPub(self, logIndex, caller, topic, content, ext):
    """
    函数名：pubMosq
    函数功能：向指定mosquitto服务器的某个topic发送一条通知，需要指明待查询用户连接的是哪个mosquitto
    @param i64 logIndex 日志索引
    @param String caller	 调用方的标识
    @param String topic 发送的主题
    @param String content 消息的内容
    @param string ext	 扩展参数，JSON字符串格式
    @return ResBool result保持操作结果， value保持返回值，成功为true，失败为false
     

    Parameters:
     - logIndex
     - caller
     - topic
     - content
     - ext
    """
    self.send_mosqPub(logIndex, caller, topic, content, ext)
    return self.recv_mosqPub()

  def send_mosqPub(self, logIndex, caller, topic, content, ext):
    self._oprot.writeMessageBegin('mosqPub', TMessageType.CALL, self._seqid)
    args = mosqPub_args()
    args.logIndex = logIndex
    args.caller = caller
    args.topic = topic
    args.content = content
    args.ext = ext
    args.write(self._oprot)
    self._oprot.writeMessageEnd()
    self._oprot.trans.flush()

  def recv_mosqPub(self):
    iprot = self._iprot
    (fname, mtype, rseqid) = iprot.readMessageBegin()
    if mtype == TMessageType.EXCEPTION:
      x = TApplicationException()
      x.read(iprot)
      iprot.readMessageEnd()
      raise x
    result = mosqPub_result()
    result.read(iprot)
    iprot.readMessageEnd()
    if result.success is not None:
      return result.success
    raise TApplicationException(TApplicationException.MISSING_RESULT, "mosqPub failed: unknown result")

  def getMosquitto(self, logIndex, caller, user, ext):
    """
    函数名：getMosquitto
    函数功能：根据用户的标识为用户分配一个mosquito
    @param i64 logIndex 日志索引
    @param String caller	 调用方的标识
    @param string user  用户的标识
    @param string ext   扩展参数，JSON字符串格式
    @return ResStr result保持操作结果， value保持返回值，成功为JSON格式的mosquito信息，失败为null
     

    Parameters:
     - logIndex
     - caller
     - user
     - ext
    """
    self.send_getMosquitto(logIndex, caller, user, ext)
    return self.recv_getMosquitto()

  def send_getMosquitto(self, logIndex, caller, user, ext):
    self._oprot.writeMessageBegin('getMosquitto', TMessageType.CALL, self._seqid)
    args = getMosquitto_args()
    args.logIndex = logIndex
    args.caller = caller
    args.user = user
    args.ext = ext
    args.write(self._oprot)
    self._oprot.writeMessageEnd()
    self._oprot.trans.flush()

  def recv_getMosquitto(self):
    iprot = self._iprot
    (fname, mtype, rseqid) = iprot.readMessageBegin()
    if mtype == TMessageType.EXCEPTION:
      x = TApplicationException()
      x.read(iprot)
      iprot.readMessageEnd()
      raise x
    result = getMosquitto_result()
    result.read(iprot)
    iprot.readMessageEnd()
    if result.success is not None:
      return result.success
    raise TApplicationException(TApplicationException.MISSING_RESULT, "getMosquitto failed: unknown result")

  def echo(self, logIndex, caller, srcStr, ext):
    """
    * 函数名称：echo
    * 函数功能：传递一个字符串给Thrift服务器，服务器把这个字符串原封不动的返回
    * @author houjixin
    * @param long(i64) logIndex 	日志索引
    * @param string caller 		调用方的标识，每个模块要调用本模块时都要提供调用方的标识；
    * @param string srcStr			传递给Thrift服务器的字符串
    * @param string ext   			扩展参数，JSON字符串格式
     * @return ResStr 				res中返回操作结果，value中返回字符串
    *

    Parameters:
     - logIndex
     - caller
     - srcStr
     - ext
    """
    self.send_echo(logIndex, caller, srcStr, ext)
    return self.recv_echo()

  def send_echo(self, logIndex, caller, srcStr, ext):
    self._oprot.writeMessageBegin('echo', TMessageType.CALL, self._seqid)
    args = echo_args()
    args.logIndex = logIndex
    args.caller = caller
    args.srcStr = srcStr
    args.ext = ext
    args.write(self._oprot)
    self._oprot.writeMessageEnd()
    self._oprot.trans.flush()

  def recv_echo(self):
    iprot = self._iprot
    (fname, mtype, rseqid) = iprot.readMessageBegin()
    if mtype == TMessageType.EXCEPTION:
      x = TApplicationException()
      x.read(iprot)
      iprot.readMessageEnd()
      raise x
    result = echo_result()
    result.read(iprot)
    iprot.readMessageEnd()
    if result.success is not None:
      return result.success
    raise TApplicationException(TApplicationException.MISSING_RESULT, "echo failed: unknown result")


class Processor(Iface, TProcessor):
  def __init__(self, handler):
    self._handler = handler
    self._processMap = {}
    self._processMap["checkOnlineStatus"] = Processor.process_checkOnlineStatus
    self._processMap["mosqPub"] = Processor.process_mosqPub
    self._processMap["getMosquitto"] = Processor.process_getMosquitto
    self._processMap["echo"] = Processor.process_echo

  def process(self, iprot, oprot):
    (name, type, seqid) = iprot.readMessageBegin()
    if name not in self._processMap:
      iprot.skip(TType.STRUCT)
      iprot.readMessageEnd()
      x = TApplicationException(TApplicationException.UNKNOWN_METHOD, 'Unknown function %s' % (name))
      oprot.writeMessageBegin(name, TMessageType.EXCEPTION, seqid)
      x.write(oprot)
      oprot.writeMessageEnd()
      oprot.trans.flush()
      return
    else:
      self._processMap[name](self, seqid, iprot, oprot)
    return True

  def process_checkOnlineStatus(self, seqid, iprot, oprot):
    args = checkOnlineStatus_args()
    args.read(iprot)
    iprot.readMessageEnd()
    result = checkOnlineStatus_result()
    result.success = self._handler.checkOnlineStatus(args.logIndex, args.caller, args.user, args.ext)
    oprot.writeMessageBegin("checkOnlineStatus", TMessageType.REPLY, seqid)
    result.write(oprot)
    oprot.writeMessageEnd()
    oprot.trans.flush()

  def process_mosqPub(self, seqid, iprot, oprot):
    args = mosqPub_args()
    args.read(iprot)
    iprot.readMessageEnd()
    result = mosqPub_result()
    result.success = self._handler.mosqPub(args.logIndex, args.caller, args.topic, args.content, args.ext)
    oprot.writeMessageBegin("mosqPub", TMessageType.REPLY, seqid)
    result.write(oprot)
    oprot.writeMessageEnd()
    oprot.trans.flush()

  def process_getMosquitto(self, seqid, iprot, oprot):
    args = getMosquitto_args()
    args.read(iprot)
    iprot.readMessageEnd()
    result = getMosquitto_result()
    result.success = self._handler.getMosquitto(args.logIndex, args.caller, args.user, args.ext)
    oprot.writeMessageBegin("getMosquitto", TMessageType.REPLY, seqid)
    result.write(oprot)
    oprot.writeMessageEnd()
    oprot.trans.flush()

  def process_echo(self, seqid, iprot, oprot):
    args = echo_args()
    args.read(iprot)
    iprot.readMessageEnd()
    result = echo_result()
    result.success = self._handler.echo(args.logIndex, args.caller, args.srcStr, args.ext)
    oprot.writeMessageBegin("echo", TMessageType.REPLY, seqid)
    result.write(oprot)
    oprot.writeMessageEnd()
    oprot.trans.flush()


# HELPER FUNCTIONS AND STRUCTURES

class checkOnlineStatus_args:
  """
  Attributes:
   - logIndex
   - caller
   - user
   - ext
  """

  thrift_spec = (
    None, # 0
    (1, TType.I64, 'logIndex', None, None, ), # 1
    (2, TType.STRING, 'caller', None, None, ), # 2
    (3, TType.STRING, 'user', None, None, ), # 3
    (4, TType.STRING, 'ext', None, None, ), # 4
  )

  def __init__(self, logIndex=None, caller=None, user=None, ext=None,):
    self.logIndex = logIndex
    self.caller = caller
    self.user = user
    self.ext = ext

  def read(self, iprot):
    if iprot.__class__ == TBinaryProtocol.TBinaryProtocolAccelerated and isinstance(iprot.trans, TTransport.CReadableTransport) and self.thrift_spec is not None and fastbinary is not None:
      fastbinary.decode_binary(self, iprot.trans, (self.__class__, self.thrift_spec))
      return
    iprot.readStructBegin()
    while True:
      (fname, ftype, fid) = iprot.readFieldBegin()
      if ftype == TType.STOP:
        break
      if fid == 1:
        if ftype == TType.I64:
          self.logIndex = iprot.readI64()
        else:
          iprot.skip(ftype)
      elif fid == 2:
        if ftype == TType.STRING:
          self.caller = iprot.readString()
        else:
          iprot.skip(ftype)
      elif fid == 3:
        if ftype == TType.STRING:
          self.user = iprot.readString()
        else:
          iprot.skip(ftype)
      elif fid == 4:
        if ftype == TType.STRING:
          self.ext = iprot.readString()
        else:
          iprot.skip(ftype)
      else:
        iprot.skip(ftype)
      iprot.readFieldEnd()
    iprot.readStructEnd()

  def write(self, oprot):
    if oprot.__class__ == TBinaryProtocol.TBinaryProtocolAccelerated and self.thrift_spec is not None and fastbinary is not None:
      oprot.trans.write(fastbinary.encode_binary(self, (self.__class__, self.thrift_spec)))
      return
    oprot.writeStructBegin('checkOnlineStatus_args')
    if self.logIndex is not None:
      oprot.writeFieldBegin('logIndex', TType.I64, 1)
      oprot.writeI64(self.logIndex)
      oprot.writeFieldEnd()
    if self.caller is not None:
      oprot.writeFieldBegin('caller', TType.STRING, 2)
      oprot.writeString(self.caller)
      oprot.writeFieldEnd()
    if self.user is not None:
      oprot.writeFieldBegin('user', TType.STRING, 3)
      oprot.writeString(self.user)
      oprot.writeFieldEnd()
    if self.ext is not None:
      oprot.writeFieldBegin('ext', TType.STRING, 4)
      oprot.writeString(self.ext)
      oprot.writeFieldEnd()
    oprot.writeFieldStop()
    oprot.writeStructEnd()

  def validate(self):
    return


  def __hash__(self):
    value = 17
    value = (value * 31) ^ hash(self.logIndex)
    value = (value * 31) ^ hash(self.caller)
    value = (value * 31) ^ hash(self.user)
    value = (value * 31) ^ hash(self.ext)
    return value

  def __repr__(self):
    L = ['%s=%r' % (key, value)
      for key, value in self.__dict__.iteritems()]
    return '%s(%s)' % (self.__class__.__name__, ', '.join(L))

  def __eq__(self, other):
    return isinstance(other, self.__class__) and self.__dict__ == other.__dict__

  def __ne__(self, other):
    return not (self == other)

class checkOnlineStatus_result:
  """
  Attributes:
   - success
  """

  thrift_spec = (
    (0, TType.STRUCT, 'success', (thrift_datatype.ttypes.ResInt, thrift_datatype.ttypes.ResInt.thrift_spec), None, ), # 0
  )

  def __init__(self, success=None,):
    self.success = success

  def read(self, iprot):
    if iprot.__class__ == TBinaryProtocol.TBinaryProtocolAccelerated and isinstance(iprot.trans, TTransport.CReadableTransport) and self.thrift_spec is not None and fastbinary is not None:
      fastbinary.decode_binary(self, iprot.trans, (self.__class__, self.thrift_spec))
      return
    iprot.readStructBegin()
    while True:
      (fname, ftype, fid) = iprot.readFieldBegin()
      if ftype == TType.STOP:
        break
      if fid == 0:
        if ftype == TType.STRUCT:
          self.success = thrift_datatype.ttypes.ResInt()
          self.success.read(iprot)
        else:
          iprot.skip(ftype)
      else:
        iprot.skip(ftype)
      iprot.readFieldEnd()
    iprot.readStructEnd()

  def write(self, oprot):
    if oprot.__class__ == TBinaryProtocol.TBinaryProtocolAccelerated and self.thrift_spec is not None and fastbinary is not None:
      oprot.trans.write(fastbinary.encode_binary(self, (self.__class__, self.thrift_spec)))
      return
    oprot.writeStructBegin('checkOnlineStatus_result')
    if self.success is not None:
      oprot.writeFieldBegin('success', TType.STRUCT, 0)
      self.success.write(oprot)
      oprot.writeFieldEnd()
    oprot.writeFieldStop()
    oprot.writeStructEnd()

  def validate(self):
    return


  def __hash__(self):
    value = 17
    value = (value * 31) ^ hash(self.success)
    return value

  def __repr__(self):
    L = ['%s=%r' % (key, value)
      for key, value in self.__dict__.iteritems()]
    return '%s(%s)' % (self.__class__.__name__, ', '.join(L))

  def __eq__(self, other):
    return isinstance(other, self.__class__) and self.__dict__ == other.__dict__

  def __ne__(self, other):
    return not (self == other)

class mosqPub_args:
  """
  Attributes:
   - logIndex
   - caller
   - topic
   - content
   - ext
  """

  thrift_spec = (
    None, # 0
    (1, TType.I64, 'logIndex', None, None, ), # 1
    (2, TType.STRING, 'caller', None, None, ), # 2
    (3, TType.STRING, 'topic', None, None, ), # 3
    (4, TType.STRING, 'content', None, None, ), # 4
    (5, TType.STRING, 'ext', None, None, ), # 5
  )

  def __init__(self, logIndex=None, caller=None, topic=None, content=None, ext=None,):
    self.logIndex = logIndex
    self.caller = caller
    self.topic = topic
    self.content = content
    self.ext = ext

  def read(self, iprot):
    if iprot.__class__ == TBinaryProtocol.TBinaryProtocolAccelerated and isinstance(iprot.trans, TTransport.CReadableTransport) and self.thrift_spec is not None and fastbinary is not None:
      fastbinary.decode_binary(self, iprot.trans, (self.__class__, self.thrift_spec))
      return
    iprot.readStructBegin()
    while True:
      (fname, ftype, fid) = iprot.readFieldBegin()
      if ftype == TType.STOP:
        break
      if fid == 1:
        if ftype == TType.I64:
          self.logIndex = iprot.readI64()
        else:
          iprot.skip(ftype)
      elif fid == 2:
        if ftype == TType.STRING:
          self.caller = iprot.readString()
        else:
          iprot.skip(ftype)
      elif fid == 3:
        if ftype == TType.STRING:
          self.topic = iprot.readString()
        else:
          iprot.skip(ftype)
      elif fid == 4:
        if ftype == TType.STRING:
          self.content = iprot.readString()
        else:
          iprot.skip(ftype)
      elif fid == 5:
        if ftype == TType.STRING:
          self.ext = iprot.readString()
        else:
          iprot.skip(ftype)
      else:
        iprot.skip(ftype)
      iprot.readFieldEnd()
    iprot.readStructEnd()

  def write(self, oprot):
    if oprot.__class__ == TBinaryProtocol.TBinaryProtocolAccelerated and self.thrift_spec is not None and fastbinary is not None:
      oprot.trans.write(fastbinary.encode_binary(self, (self.__class__, self.thrift_spec)))
      return
    oprot.writeStructBegin('mosqPub_args')
    if self.logIndex is not None:
      oprot.writeFieldBegin('logIndex', TType.I64, 1)
      oprot.writeI64(self.logIndex)
      oprot.writeFieldEnd()
    if self.caller is not None:
      oprot.writeFieldBegin('caller', TType.STRING, 2)
      oprot.writeString(self.caller)
      oprot.writeFieldEnd()
    if self.topic is not None:
      oprot.writeFieldBegin('topic', TType.STRING, 3)
      oprot.writeString(self.topic)
      oprot.writeFieldEnd()
    if self.content is not None:
      oprot.writeFieldBegin('content', TType.STRING, 4)
      oprot.writeString(self.content)
      oprot.writeFieldEnd()
    if self.ext is not None:
      oprot.writeFieldBegin('ext', TType.STRING, 5)
      oprot.writeString(self.ext)
      oprot.writeFieldEnd()
    oprot.writeFieldStop()
    oprot.writeStructEnd()

  def validate(self):
    return


  def __hash__(self):
    value = 17
    value = (value * 31) ^ hash(self.logIndex)
    value = (value * 31) ^ hash(self.caller)
    value = (value * 31) ^ hash(self.topic)
    value = (value * 31) ^ hash(self.content)
    value = (value * 31) ^ hash(self.ext)
    return value

  def __repr__(self):
    L = ['%s=%r' % (key, value)
      for key, value in self.__dict__.iteritems()]
    return '%s(%s)' % (self.__class__.__name__, ', '.join(L))

  def __eq__(self, other):
    return isinstance(other, self.__class__) and self.__dict__ == other.__dict__

  def __ne__(self, other):
    return not (self == other)

class mosqPub_result:
  """
  Attributes:
   - success
  """

  thrift_spec = (
    (0, TType.STRUCT, 'success', (thrift_datatype.ttypes.ResBool, thrift_datatype.ttypes.ResBool.thrift_spec), None, ), # 0
  )

  def __init__(self, success=None,):
    self.success = success

  def read(self, iprot):
    if iprot.__class__ == TBinaryProtocol.TBinaryProtocolAccelerated and isinstance(iprot.trans, TTransport.CReadableTransport) and self.thrift_spec is not None and fastbinary is not None:
      fastbinary.decode_binary(self, iprot.trans, (self.__class__, self.thrift_spec))
      return
    iprot.readStructBegin()
    while True:
      (fname, ftype, fid) = iprot.readFieldBegin()
      if ftype == TType.STOP:
        break
      if fid == 0:
        if ftype == TType.STRUCT:
          self.success = thrift_datatype.ttypes.ResBool()
          self.success.read(iprot)
        else:
          iprot.skip(ftype)
      else:
        iprot.skip(ftype)
      iprot.readFieldEnd()
    iprot.readStructEnd()

  def write(self, oprot):
    if oprot.__class__ == TBinaryProtocol.TBinaryProtocolAccelerated and self.thrift_spec is not None and fastbinary is not None:
      oprot.trans.write(fastbinary.encode_binary(self, (self.__class__, self.thrift_spec)))
      return
    oprot.writeStructBegin('mosqPub_result')
    if self.success is not None:
      oprot.writeFieldBegin('success', TType.STRUCT, 0)
      self.success.write(oprot)
      oprot.writeFieldEnd()
    oprot.writeFieldStop()
    oprot.writeStructEnd()

  def validate(self):
    return


  def __hash__(self):
    value = 17
    value = (value * 31) ^ hash(self.success)
    return value

  def __repr__(self):
    L = ['%s=%r' % (key, value)
      for key, value in self.__dict__.iteritems()]
    return '%s(%s)' % (self.__class__.__name__, ', '.join(L))

  def __eq__(self, other):
    return isinstance(other, self.__class__) and self.__dict__ == other.__dict__

  def __ne__(self, other):
    return not (self == other)

class getMosquitto_args:
  """
  Attributes:
   - logIndex
   - caller
   - user
   - ext
  """

  thrift_spec = (
    None, # 0
    (1, TType.I64, 'logIndex', None, None, ), # 1
    (2, TType.STRING, 'caller', None, None, ), # 2
    (3, TType.STRING, 'user', None, None, ), # 3
    (4, TType.STRING, 'ext', None, None, ), # 4
  )

  def __init__(self, logIndex=None, caller=None, user=None, ext=None,):
    self.logIndex = logIndex
    self.caller = caller
    self.user = user
    self.ext = ext

  def read(self, iprot):
    if iprot.__class__ == TBinaryProtocol.TBinaryProtocolAccelerated and isinstance(iprot.trans, TTransport.CReadableTransport) and self.thrift_spec is not None and fastbinary is not None:
      fastbinary.decode_binary(self, iprot.trans, (self.__class__, self.thrift_spec))
      return
    iprot.readStructBegin()
    while True:
      (fname, ftype, fid) = iprot.readFieldBegin()
      if ftype == TType.STOP:
        break
      if fid == 1:
        if ftype == TType.I64:
          self.logIndex = iprot.readI64()
        else:
          iprot.skip(ftype)
      elif fid == 2:
        if ftype == TType.STRING:
          self.caller = iprot.readString()
        else:
          iprot.skip(ftype)
      elif fid == 3:
        if ftype == TType.STRING:
          self.user = iprot.readString()
        else:
          iprot.skip(ftype)
      elif fid == 4:
        if ftype == TType.STRING:
          self.ext = iprot.readString()
        else:
          iprot.skip(ftype)
      else:
        iprot.skip(ftype)
      iprot.readFieldEnd()
    iprot.readStructEnd()

  def write(self, oprot):
    if oprot.__class__ == TBinaryProtocol.TBinaryProtocolAccelerated and self.thrift_spec is not None and fastbinary is not None:
      oprot.trans.write(fastbinary.encode_binary(self, (self.__class__, self.thrift_spec)))
      return
    oprot.writeStructBegin('getMosquitto_args')
    if self.logIndex is not None:
      oprot.writeFieldBegin('logIndex', TType.I64, 1)
      oprot.writeI64(self.logIndex)
      oprot.writeFieldEnd()
    if self.caller is not None:
      oprot.writeFieldBegin('caller', TType.STRING, 2)
      oprot.writeString(self.caller)
      oprot.writeFieldEnd()
    if self.user is not None:
      oprot.writeFieldBegin('user', TType.STRING, 3)
      oprot.writeString(self.user)
      oprot.writeFieldEnd()
    if self.ext is not None:
      oprot.writeFieldBegin('ext', TType.STRING, 4)
      oprot.writeString(self.ext)
      oprot.writeFieldEnd()
    oprot.writeFieldStop()
    oprot.writeStructEnd()

  def validate(self):
    return


  def __hash__(self):
    value = 17
    value = (value * 31) ^ hash(self.logIndex)
    value = (value * 31) ^ hash(self.caller)
    value = (value * 31) ^ hash(self.user)
    value = (value * 31) ^ hash(self.ext)
    return value

  def __repr__(self):
    L = ['%s=%r' % (key, value)
      for key, value in self.__dict__.iteritems()]
    return '%s(%s)' % (self.__class__.__name__, ', '.join(L))

  def __eq__(self, other):
    return isinstance(other, self.__class__) and self.__dict__ == other.__dict__

  def __ne__(self, other):
    return not (self == other)

class getMosquitto_result:
  """
  Attributes:
   - success
  """

  thrift_spec = (
    (0, TType.STRUCT, 'success', (thrift_datatype.ttypes.ResStr, thrift_datatype.ttypes.ResStr.thrift_spec), None, ), # 0
  )

  def __init__(self, success=None,):
    self.success = success

  def read(self, iprot):
    if iprot.__class__ == TBinaryProtocol.TBinaryProtocolAccelerated and isinstance(iprot.trans, TTransport.CReadableTransport) and self.thrift_spec is not None and fastbinary is not None:
      fastbinary.decode_binary(self, iprot.trans, (self.__class__, self.thrift_spec))
      return
    iprot.readStructBegin()
    while True:
      (fname, ftype, fid) = iprot.readFieldBegin()
      if ftype == TType.STOP:
        break
      if fid == 0:
        if ftype == TType.STRUCT:
          self.success = thrift_datatype.ttypes.ResStr()
          self.success.read(iprot)
        else:
          iprot.skip(ftype)
      else:
        iprot.skip(ftype)
      iprot.readFieldEnd()
    iprot.readStructEnd()

  def write(self, oprot):
    if oprot.__class__ == TBinaryProtocol.TBinaryProtocolAccelerated and self.thrift_spec is not None and fastbinary is not None:
      oprot.trans.write(fastbinary.encode_binary(self, (self.__class__, self.thrift_spec)))
      return
    oprot.writeStructBegin('getMosquitto_result')
    if self.success is not None:
      oprot.writeFieldBegin('success', TType.STRUCT, 0)
      self.success.write(oprot)
      oprot.writeFieldEnd()
    oprot.writeFieldStop()
    oprot.writeStructEnd()

  def validate(self):
    return


  def __hash__(self):
    value = 17
    value = (value * 31) ^ hash(self.success)
    return value

  def __repr__(self):
    L = ['%s=%r' % (key, value)
      for key, value in self.__dict__.iteritems()]
    return '%s(%s)' % (self.__class__.__name__, ', '.join(L))

  def __eq__(self, other):
    return isinstance(other, self.__class__) and self.__dict__ == other.__dict__

  def __ne__(self, other):
    return not (self == other)

class echo_args:
  """
  Attributes:
   - logIndex
   - caller
   - srcStr
   - ext
  """

  thrift_spec = (
    None, # 0
    (1, TType.I64, 'logIndex', None, None, ), # 1
    (2, TType.STRING, 'caller', None, None, ), # 2
    (3, TType.STRING, 'srcStr', None, None, ), # 3
    (4, TType.STRING, 'ext', None, None, ), # 4
  )

  def __init__(self, logIndex=None, caller=None, srcStr=None, ext=None,):
    self.logIndex = logIndex
    self.caller = caller
    self.srcStr = srcStr
    self.ext = ext

  def read(self, iprot):
    if iprot.__class__ == TBinaryProtocol.TBinaryProtocolAccelerated and isinstance(iprot.trans, TTransport.CReadableTransport) and self.thrift_spec is not None and fastbinary is not None:
      fastbinary.decode_binary(self, iprot.trans, (self.__class__, self.thrift_spec))
      return
    iprot.readStructBegin()
    while True:
      (fname, ftype, fid) = iprot.readFieldBegin()
      if ftype == TType.STOP:
        break
      if fid == 1:
        if ftype == TType.I64:
          self.logIndex = iprot.readI64()
        else:
          iprot.skip(ftype)
      elif fid == 2:
        if ftype == TType.STRING:
          self.caller = iprot.readString()
        else:
          iprot.skip(ftype)
      elif fid == 3:
        if ftype == TType.STRING:
          self.srcStr = iprot.readString()
        else:
          iprot.skip(ftype)
      elif fid == 4:
        if ftype == TType.STRING:
          self.ext = iprot.readString()
        else:
          iprot.skip(ftype)
      else:
        iprot.skip(ftype)
      iprot.readFieldEnd()
    iprot.readStructEnd()

  def write(self, oprot):
    if oprot.__class__ == TBinaryProtocol.TBinaryProtocolAccelerated and self.thrift_spec is not None and fastbinary is not None:
      oprot.trans.write(fastbinary.encode_binary(self, (self.__class__, self.thrift_spec)))
      return
    oprot.writeStructBegin('echo_args')
    if self.logIndex is not None:
      oprot.writeFieldBegin('logIndex', TType.I64, 1)
      oprot.writeI64(self.logIndex)
      oprot.writeFieldEnd()
    if self.caller is not None:
      oprot.writeFieldBegin('caller', TType.STRING, 2)
      oprot.writeString(self.caller)
      oprot.writeFieldEnd()
    if self.srcStr is not None:
      oprot.writeFieldBegin('srcStr', TType.STRING, 3)
      oprot.writeString(self.srcStr)
      oprot.writeFieldEnd()
    if self.ext is not None:
      oprot.writeFieldBegin('ext', TType.STRING, 4)
      oprot.writeString(self.ext)
      oprot.writeFieldEnd()
    oprot.writeFieldStop()
    oprot.writeStructEnd()

  def validate(self):
    return


  def __hash__(self):
    value = 17
    value = (value * 31) ^ hash(self.logIndex)
    value = (value * 31) ^ hash(self.caller)
    value = (value * 31) ^ hash(self.srcStr)
    value = (value * 31) ^ hash(self.ext)
    return value

  def __repr__(self):
    L = ['%s=%r' % (key, value)
      for key, value in self.__dict__.iteritems()]
    return '%s(%s)' % (self.__class__.__name__, ', '.join(L))

  def __eq__(self, other):
    return isinstance(other, self.__class__) and self.__dict__ == other.__dict__

  def __ne__(self, other):
    return not (self == other)

class echo_result:
  """
  Attributes:
   - success
  """

  thrift_spec = (
    (0, TType.STRUCT, 'success', (thrift_datatype.ttypes.ResStr, thrift_datatype.ttypes.ResStr.thrift_spec), None, ), # 0
  )

  def __init__(self, success=None,):
    self.success = success

  def read(self, iprot):
    if iprot.__class__ == TBinaryProtocol.TBinaryProtocolAccelerated and isinstance(iprot.trans, TTransport.CReadableTransport) and self.thrift_spec is not None and fastbinary is not None:
      fastbinary.decode_binary(self, iprot.trans, (self.__class__, self.thrift_spec))
      return
    iprot.readStructBegin()
    while True:
      (fname, ftype, fid) = iprot.readFieldBegin()
      if ftype == TType.STOP:
        break
      if fid == 0:
        if ftype == TType.STRUCT:
          self.success = thrift_datatype.ttypes.ResStr()
          self.success.read(iprot)
        else:
          iprot.skip(ftype)
      else:
        iprot.skip(ftype)
      iprot.readFieldEnd()
    iprot.readStructEnd()

  def write(self, oprot):
    if oprot.__class__ == TBinaryProtocol.TBinaryProtocolAccelerated and self.thrift_spec is not None and fastbinary is not None:
      oprot.trans.write(fastbinary.encode_binary(self, (self.__class__, self.thrift_spec)))
      return
    oprot.writeStructBegin('echo_result')
    if self.success is not None:
      oprot.writeFieldBegin('success', TType.STRUCT, 0)
      self.success.write(oprot)
      oprot.writeFieldEnd()
    oprot.writeFieldStop()
    oprot.writeStructEnd()

  def validate(self):
    return


  def __hash__(self):
    value = 17
    value = (value * 31) ^ hash(self.success)
    return value

  def __repr__(self):
    L = ['%s=%r' % (key, value)
      for key, value in self.__dict__.iteritems()]
    return '%s(%s)' % (self.__class__.__name__, ', '.join(L))

  def __eq__(self, other):
    return isinstance(other, self.__class__) and self.__dict__ == other.__dict__

  def __ne__(self, other):
    return not (self == other)