package com.uangel.ccaas.msggw.config;

import com.uangel.ccaas.msggw.type.SendMsgType;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import umedia.library.common.ValidationUtil;
import umedia.library.config.ConfigValue;
import umedia.library.config.ini.IniConfig;

import javax.validation.constraints.NotBlank;

@Slf4j
@Getter
public class MsgGwConfig extends IniConfig {
    // COMMON
    @ConfigValue("COMMON.LONG_SESSION")
    private int longSession;
    @ConfigValue("COMMON.TEST_MODE")
    private String testModeStr = "false";
    @Setter
    private boolean testMode;

    // RMQ
    @ConfigValue("RMQ.HOST")
    private String rmqHost;
    @NotBlank
    @ConfigValue("RMQ.USER")
    private String rmqUser;
    @ConfigValue("RMQ.PORT")
    private int rmqPort;
    @NotBlank
    @ConfigValue("RMQ.PASS")
    private String rmqPass;
    @ConfigValue("RMQ.THREAD_SIZE")
    private int rmqThreadSize;
    @ConfigValue("RMQ.QUEUE_CONSUMER_SIZE")
    private int rmqConsumerQueueSize;
    @ConfigValue("RMQ.QUEUE_SENDER_SIZE")
    private int rmqSenderQueueSize;

    @ConfigValue("RMQ.LOCAL_QUEUE")
    private String msgGwQueue;
    @ConfigValue("RMQ.AIWF_QUEUE")
    private String aiwfQueue;

    // GRPC
    @ConfigValue("GRPC.SERVER_IP")
    private String grpcServerIp;
    @ConfigValue("GRPC.SERVER_PORT")
    private int grpcServerPort;
    @ConfigValue("GRPC.TARGET_IP")
    private String grpcTargetIp;
    @ConfigValue("GRPC.TARGET_PORT")
    private int grpcTargetPort;
    @ConfigValue("GRPC.THREAD_SIZE")
    private int grpcThreadSize;
    @ConfigValue("GRPC.QUEUE_CONSUMER_SIZE")
    private int grpcConsumerQueueSize;
    @ConfigValue("GRPC.QUEUE_SENDER_SIZE")
    private int grpcSenderQueueSize;

    // TIMEOUT
    @ConfigValue("TIMEOUT.HTTP_TIMEOUT")
    private int httpTimeout;

    // AI
    @ConfigValue("AI.CHATBOT_URL")
    private String chatBotUrl;
    @ConfigValue("AI.SEND_TYPE")
    private int sendType; // Send Protocol Type - 0: HTTP, 1: gRPC
    private SendMsgType sendMsgType;


    @Override
    public void afterFieldSetting() {
        ValidationUtil.validCheck(this);
        this.testMode = checkTrue(testModeStr);
        this.sendMsgType = SendMsgType.getType(sendType);
    }

    public boolean checkTrue(String str) {
        return "TRUE".equalsIgnoreCase(str);
    }
}
