package com.uangel.ccaas.msggw.grpc;

import com.uangel.ccaas.aibotmsg.Message;
import com.uangel.ccaas.msggw.message.handler.IncomingHandler;
import com.uangel.ccaas.msggw.message.util.MessageParser;
import com.uangel.ccaas.msggw.observer.ObserverInfo;
import com.uangel.ccaas.msggw.observer.ObserverManager;
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
    private final ArrayBlockingQueue<Message> queue;


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
                        Message request = queue.poll();
                        if (request == null) break;
                        // handle
                        String msgType = MessageParser.getMsgType(request);
                        String tId = MessageParser.getTid(request);
                        log.debug("[MSG] consume: {} (tId:{})", msgType, tId);

                        ObserverInfo observerInfo = ObserverManager.getInstance().deleteObserverInfo(tId);
                        if (observerInfo == null) {

                            return;
                        }
                        IncomingHandler.getInstance().handle(request, RcvMsgType.GRPC, observerInfo.getResponseObserver());
                    }
                } catch (Exception e) {
                    log.warn("Err Occurs while handling gRPC Message", e);
                }
            }, 0, 10, TimeUnit.MILLISECONDS);
        }
    }

    public void consume(Message msg, StreamObserver<Message> responseObserver) {
        try {
            if (!this.queue.offer(msg)) {
                log.warn("gRPC RCV Queue full. Drop message.");
            } else {
                String tId = MessageParser.getTid(msg);
                ObserverManager.getInstance().putObserverInfo(tId, responseObserver);
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
