###一.Zooker原生客户端测试
1. 启动服务 
 - 路径: .../server/zookeeper-3.4.10/
 - 配置: .../server/zookeeper-3.4.10/conf/zoo.cfg
 - 启动脚本: .../server/zookeeper-3.4.10/bin/zkServer.sh start

2. 状态检查
 - 检查端口是否起来 lsof -i tcp:2181
 - 获取stat：
   1. telnet 127.0.0.1 2181  
   2. stat  

3.停止
 - .../server/zookeeper-3.4.10/bin/zkServer.sh stop
 
 
 ###二.curator测试
 1. 启动服务
 - 使用TestingServer可以启动测试的ZK服务
 - 使用TestingCluster可以启动测试的ZK集群
 - 以上两种方式起来的服务均可以使用原生方式访问