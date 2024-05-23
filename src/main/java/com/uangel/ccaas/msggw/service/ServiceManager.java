package com.uangel.ccaas.msggw.service;

import com.uangel.ccaas.msggw.config.MsgGwConfig;
import com.uangel.ccaas.msggw.grpc.GrpcClient;
import com.uangel.ccaas.msggw.grpc.GrpcServer;
import com.uangel.ccaas.msggw.rmq.GwRmqManager;
import com.uangel.ccaas.msggw.service.schedule.IntervalTaskManager;
import lombok.extern.slf4j.Slf4j;
import umedia.library.common.TimeUtil;

import java.io.IOException;

@Slf4j
public class ServiceManager {
    private static final AppInstance appInstance = AppInstance.getInstance();
    private static final String AIWF_CONF = "msggw_user.config";
    private static final MsgGwConfig config = appInstance.getConfig();
    private static ServiceManager serviceManager = null;
    private GwRmqManager gwRmqManager = null;
    private GrpcServer grpcServer = null;
    private IntervalTaskManager taskManager = null;
    private boolean isQuit;

    private ServiceManager() throws IOException, NoSuchFieldException {
        config.init(appInstance.getConfigPath().resolve(AIWF_CONF));
    }

    public static ServiceManager getInstance() throws IOException, NoSuchFieldException {
        if (serviceManager == null) serviceManager = new ServiceManager();
        return serviceManager;
    }

    public void loop() {
        this.startService();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.error("Process is about to quit (Ctrl+C)");
            this.isQuit = true;
            this.stopService();
        }));

        while (!isQuit) {
            try {
                TimeUtil.trySleep(1000);
            } catch (Exception e) {
                log.error("Failed to AIWF Service loop", e);
            }
        }

        log.error("AIWF Process Down");
    }

    private void startService() {
        try {
            gwRmqManager = GwRmqManager.getInstance();
            gwRmqManager.startRmq();

            grpcServer = GrpcServer.getInstance();
            grpcServer.start();
        } catch (Exception e) {
            log.warn("Failed to startService", e);
        }

        taskManager = IntervalTaskManager.getInstance();
        taskManager.init();
    }

    private void stopService() {
        try {
            if (gwRmqManager != null) gwRmqManager.stopRmq();
            if (grpcServer != null) grpcServer.stop();
            if (taskManager != null) taskManager.stop();

            GrpcClient grpcClient = GrpcClient.getInstance();
            if (!grpcClient.isShutdown()) grpcClient.shutdown();
        } catch (Exception e) {
            log.warn("Failed to AIWF stopService", e);
        }
    }
}
