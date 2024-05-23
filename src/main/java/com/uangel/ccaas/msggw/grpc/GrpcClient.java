package com.uangel.ccaas.msggw.grpc;

import com.uangel.ccaas.aibotmsg.AiBotServiceGrpc;
import com.uangel.ccaas.msggw.config.MsgGwConfig;
import com.uangel.ccaas.msggw.service.AppInstance;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
public class GrpcClient {
    private static GrpcClient grpcClient;
    private final MsgGwConfig config = AppInstance.getInstance().getConfig();
    private final int port = config.getGrpcTargetPort();
    private final String host = config.getGrpcTargetIp();

    private final ManagedChannel channel; // gRPC 서버와의 연결
    private final AiBotServiceGrpc.AiBotServiceBlockingStub stub;
    private final AiBotServiceGrpc.AiBotServiceStub serviceStub;

    private GrpcClient() {
        channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        stub = AiBotServiceGrpc.newBlockingStub(channel);
        serviceStub = AiBotServiceGrpc.newStub(channel);
        log.info("gRPC Client started [{}:{}]", host, port);
    }

    public static GrpcClient getInstance() {
        if (grpcClient == null) grpcClient = new GrpcClient();
        return grpcClient;
    }

    public void shutdown() {
        try {
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Failed to handle GrpcClient.shutdown", e);
        }
    }

    public boolean isShutdown() {
        return channel.isShutdown();
    }



}
