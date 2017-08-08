@echo on
SETLOCAL ENABLEDELAYEDEXPANSION 
set PATH=%PATH%;C:\Program Files\Java\jdk1.7.0_75\bin
set PARAMS=-Djava.rmi.server.hostname=localhost -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.ssl=false 
set curdir=%~dp0
cd /d %curdir%
cd ..

:: for /R "./lib" %%s in (*) do ( 

for /r  %%s in (lib\*.*) do ( 
	@set CP=!CP! %%s
) 
  
java -server -Xms128m -Xmx1024m %PARAMS% -classpath %CP% com.xyz.service.starter.Starter

pause




