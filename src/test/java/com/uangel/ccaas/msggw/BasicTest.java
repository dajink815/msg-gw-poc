package com.uangel.ccaas.msggw;

import com.uangel.ccaas.msggw.config.MsgGwConfig;
import com.uangel.ccaas.msggw.grpc.GrpcServer;
import com.uangel.ccaas.msggw.rmq.GwRmqManager;
import com.uangel.ccaas.msggw.rmq.receiver.GwRmqConsumer;
import com.uangel.ccaas.msggw.scenario.TestDefine;
import com.uangel.ccaas.msggw.service.AppInstance;
import com.uangel.ccaas.msggw.service.schedule.IntervalTaskManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import umedia.library.common.TimeUtil;

import java.io.IOException;
import java.nio.file.Path;

public class BasicTest {
    protected final AppInstance instance = AppInstance.getInstance();
    protected final MsgGwConfig config = new MsgGwConfig();


    @Before
    public void init() throws IOException, NoSuchFieldException {
        config.init(Path.of("src/test/resources/config").resolve("msggw_user.config"));
        instance.setConfig(config);

        // Connect to RMQ Server
        GwRmqConsumer gwRmqConsumer = null;
        if (TestDefine.CONNECT_RMQ_SERVER) {
            GwRmqManager.getInstance().startRmq();
        } else {
            gwRmqConsumer = new GwRmqConsumer(config.getRmqThreadSize(), config.getRmqConsumerQueueSize());
        }

        GrpcServer.getInstance().start();
        IntervalTaskManager.getInstance().init();

        // Sender

        TimeUtil.trySleep(1000);
    }

    @After
    public void stop() throws InterruptedException {
        GwRmqManager.getInstance().stopRmq();
        GrpcServer.getInstance().stop();
    }

    @Test
    public void test() {
        System.out.println(">>>>   Test Started");

        TimeUtil.trySleep(20000);
    }
}
