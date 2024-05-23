package com.uangel.ccaas.msggw.rmq;

import com.uangel.ccaas.msggw.config.MsgGwConfig;
import com.uangel.ccaas.msggw.rmq.receiver.GwRmqConsumer;
import com.uangel.ccaas.msggw.service.AppInstance;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import umedia.library.common.PasswdUtil;
import umedia.rmq.RmqManager;
import umedia.rmq.RmqModule;

@Slf4j
public class GwRmqManager {
    private static final AppInstance appInstance = AppInstance.getInstance();
    private static final MsgGwConfig config = appInstance.getConfig();
    private static GwRmqManager gwRmqManager;
    private final RmqManager rmqManager = RmqManager.getInstance();
    @Getter
    private RmqModule rmqModule;

    private GwRmqManager() {
        // Nothing
    }

    public static GwRmqManager getInstance() {
        if (gwRmqManager == null) gwRmqManager = new GwRmqManager();
        return gwRmqManager;
    }

    public void startRmq() {
        String host = config.getRmqHost();
        String user = config.getRmqUser();
        int port = config.getRmqPort();
        this.rmqModule = addModule(host, user, port, config.getMsgGwQueue());
        startServer();
    }

    private RmqModule addModule(String host, String user, int port, String queue) {
        String decPass = PasswdUtil.decriptString(config.getRmqPass());
        try {
            RmqModule newModule = new RmqModule(host, user, decPass, port, config.getRmqSenderQueueSize());
            rmqManager.addRmqModule(queue, newModule, queue, () -> {
                log.info("RabbitMQ Server [{}] Connect Success. [{}@{}:{}]", queue, user, host, port);
                appInstance.setRmqConnect(true);
            }, () -> {
                log.warn("RabbitMQ Server [{}] DisConnect. [{}@{}:{}]", queue, user, host, port);
                appInstance.setRmqConnect(false);
            });
            return newModule;
        } catch (Exception e) {
            log.warn("Failed to addModule", e);
            return null;
        }
    }

    private void startServer() {
        try {
            GwRmqConsumer aiwfConsumer = new GwRmqConsumer(config.getRmqThreadSize(), config.getRmqConsumerQueueSize());
            log.debug("RMQ ConsumerCount {}, bufferCount {}", config.getRmqThreadSize(), config.getRmqConsumerQueueSize());
            rmqManager.start(aiwfConsumer);
        } catch (Exception e) {
            log.warn("Failed to startRmqServer", e);
        }
    }


    // 서버, 클라이언트 연결 해제
    public void stopRmq() {
        if (rmqManager != null) rmqManager.stop();
    }


}
