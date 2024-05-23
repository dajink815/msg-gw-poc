package com.uangel.ccaas.msggw.grpc;

import com.google.protobuf.InvalidProtocolBufferException;
import com.uangel.ccaas.aibotmsg.Message;
import com.uangel.ccaas.msggw.config.MsgGwConfig;
import com.uangel.ccaas.msggw.message.handler.IncomingHandler;
import com.uangel.ccaas.msggw.message.util.MessageParser;
import com.uangel.ccaas.msggw.service.AppInstance;
import com.uangel.ccaas.msggw.type.RcvMsgType;
import com.uangel.ccaas.msggw.util.PrintMsgModule;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class GwGrpcConsumer {
    private final ScheduledExecutorService executor;
    private final ArrayBlockingQueue<GrpcMsgInfo> queue;

    public GwGrpcConsumer(int consumerCount, int grpcBufferCount) {
        executor = Executors.newScheduledThreadPool(consumerCount, new BasicThreadFactory.Builder()
                .namingPattern("GRPC_CONSUMER-%d")
                .daemon(true)
                .priority(Thread.MAX_PRIORITY)
                .build());
        queue = new ArrayBlockingQueue<>(grpcBufferCount);

        for (int i = 0; i < consumerCount; i++) {
            executor.scheduleWithFixedDelay(() -> {
                try {
                    while (true) {
                        GrpcMsgInfo grpcMsgInfo = queue.poll();
                        if (grpcMsgInfo == null) break;
                        // handle
                        Message request = grpcMsgInfo.getRequest();
                        String msgType = MessageParser.getMsgType(request);
                        String tId = MessageParser.getTid(request);
                        log.debug("[MSG] consume: {} (tId:{})", msgType, tId);
                        IncomingHandler.getInstance().handle(request, RcvMsgType.GRPC, grpcMsgInfo.getAiwfCallBack());
                    }
                } catch (Exception e) {
                    log.warn("Err Occurs while handling gRPC Message", e);
                }
            }, 0, 10, TimeUnit.MILLISECONDS);
        }
    }

    public void consume(Message request, StreamObserver<Message> responseObserver) {
        try {
            GrpcMsgInfo grpcMsgInfo = new GrpcMsgInfo(request);
            grpcMsgInfo.setAiwfCallBack(response -> {
                try {
                    responseObserver.onNext(response);
                    responseObserver.onCompleted();
                    MsgGwConfig config = AppInstance.getInstance().getConfig();
                    String target = config.getGrpcTargetIp() + ":" + config.getGrpcTargetPort();
                    PrintMsgModule.printSendLog(target, response);
                } catch (InvalidProtocolBufferException e) {
                    throw new RuntimeException(e);
                }
            });

            if (!this.queue.offer(grpcMsgInfo)) {
                log.warn("gRPC RCV Queue full. Drop message.");
            }
        } catch (Exception e) {
            log.warn("Err Occurs", e);
        }
    }

    public void close() {
        if (this.executor != null && !this.executor.isShutdown()) {
            this.executor.shutdown();
        }
    }
}
