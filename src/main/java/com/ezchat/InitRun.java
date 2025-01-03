package com.ezchat;

import com.ezchat.redis.RedisUtils;
import com.ezchat.webSocket.netty.NettyWebSocketStarter;
import io.lettuce.core.RedisConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.sql.SQLException;

@Component("initRun")
public class InitRun implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(InitRun.class);

    @Resource
    private DataSource dataSource;

    @Resource
    private RedisUtils redisUtils;

    @Resource
    private NettyWebSocketStarter nettyWebSocketStarter;

    @Override
    public void run(ApplicationArguments args) {
        try {
            dataSource.getConnection();
            redisUtils.get("test");
            new Thread(nettyWebSocketStarter).start();
            logger.info("初始化完成");
        } catch (SQLException e) {
            logger.error("数据库配置错误，请检查数据库配置");
        } catch (RedisConnectionException e) {
            logger.error("Redis配置错误，请检查Redis配置");
        } catch (Exception e) {
            logger.error("初始化失败", e);
        }
    }
}
