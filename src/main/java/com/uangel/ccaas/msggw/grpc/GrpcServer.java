package com.uangel.ccaas.msggw.grpc;

import com.uangel.ccaas.msggw.config.MsgGwConfig;
import com.uangel.ccaas.msggw.service.AppInstance;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
public class GrpcServer {
    private static GrpcServer grpcServer;
    private final MsgGwConfig config = AppInstance.getInstance().getConfig();
    private final int port = config.getGrpcServerPort();
    private final Server server;
    private final GwGrpcConsumer gwGrpcConsumer;

    private GrpcServer() {
        gwGrpcConsumer = new GwGrpcConsumer(config.getGrpcThreadSize(), config.getGrpcConsumerQueueSize());
        AiBotServiceImpl aiBotService = new AiBotServiceImpl(gwGrpcConsumer);
        server = ServerBuilder.forPort(port)
                // add Service
                .addService(aiBotService)
                .build();
        log.info("gRPC Server created");
    }

    public static GrpcServer getInstance() {
        if (grpcServer == null) grpcServer = new GrpcServer();
        return grpcServer;
    }

    public void start() throws IOException {
        server.start();
        log.info("gRPC Server started, listening on {}", port);
    }

    public void stop() throws InterruptedException {
        server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        if (gwGrpcConsumer != null) gwGrpcConsumer.close();
    }
}
