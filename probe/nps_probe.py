# -*- coding:utf-8 -*-
# 导入库
import sys
sys.dont_write_bytecode = True  # 不产生*.pyc文件，必须放在尽可能最前面
sys.path.append(sys.path[0] + '/nps_stub')
import os
import StringIO
import threading
import ConfigParser
from nps_stub.nps import NPSStub  # 引入客户端类
from thrift import Thrift
from thrift.transport import TSocket
from thrift.transport import TTransport
from thrift.protocol import TBinaryProtocol
# import pydevd

# 全局变量
LOG_INDEX = -101  # 本监控程序特殊的logindex，区别其他程序
CONFIG_PATH = "/conf/nps.conf"  # 微服务配置文件地址
CONFIG_KEY = "thrift.listen.port"  # 服务端口，conf文件中的键值
CALLER = "probe"  # 传给echo的调用名
IP = "127.0.0.1"  # 服务地址
THRIFT_SUCCESS = 200  # 成功时的thrift返回值
EVENT_TIMEOUT = 15  # 线程超时，单位秒
TIMEOUT_MARK = -99  # 超时标志


# 主函数，实现探测功能，返回值：0成功，-1读取配置文件错误，-2 thrift exception，-3 exception，-4 starter异常，-99 超时
# 整个程序除最后会print结果给zabbix外，其他地方不允许调用print函数
def starter():
    # 从../conf/*.conf读取探测端口
    abspath = os.path.abspath(os.path.join(sys.path[0], os.pardir)) + CONFIG_PATH  # 获取conf绝对路径
    config = StringIO.StringIO()
    config.write('[root]\n')
    config.write(open(abspath, "r").read())
    config.seek(0, os.SEEK_SET)
    cp = ConfigParser.ConfigParser()
    cp.readfp(config)
    if not cp:
        return -1
    str_raw_port = cp.get("root", CONFIG_KEY)  # 端口配置行可能带#注释，要把注释去掉
    index = str_raw_port.find('#')
    if -1 == index:
        str_port = str_raw_port  # 无#注释
    else:
        str_port = str_raw_port[0:index]  # 有#注释
    port = int(str_port)

    # 运行探测任务
    res = [TIMEOUT_MARK]  # 初始化为超时状态
    event = threading.Event()
    probe_thread = threading.Thread(
            target=probe_job,
            args=(IP, port, event, res, ),
            name="probe" + IP + str(port))
    probe_thread.start()

    # 等待探测结果
    event.wait(EVENT_TIMEOUT)
    return res[0]


def probe_job(ip, port, event, res):
    try:
        thread_name = threading.currentThread().getName()
        transport = TSocket.TSocket(ip, port)
        transport = TTransport.TFramedTransport(transport)
        protocol = TBinaryProtocol.TBinaryProtocol(transport)
        client = NPSStub.Client(protocol)
        transport.open()
        result = client.echo(LOG_INDEX, CALLER, "hello", "")
        if THRIFT_SUCCESS == result.res and 'hello' == result.value:
            res[0] = 0
    except Thrift.TException, e:
        res[0] = -2
    except Exception, e:
        res[0] = -3
    finally:
        transport.close()
        event.set()


# 运行主函数
if __name__ == "__main__":
    res_code = -4  # starter未知异常
    try:
        res_code = starter()
    except Exception, e:
        res_code = -4
    finally:
        print(res_code)  # 打印int结果给zabbix
    if TIMEOUT_MARK == res_code:  # 超时，强退，必须放在最后
        os._exit(99)