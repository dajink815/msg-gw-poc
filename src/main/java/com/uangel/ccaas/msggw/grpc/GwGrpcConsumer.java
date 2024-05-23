package com.uangel.ccaas.msggw.grpc;

import com.uangel.ccaas.aibotmsg.Message;
import com.uangel.ccaas.msggw.util.PrintMsgModule;
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
                        Message data = queue.poll();
                        if (data == null) break;
                        // todo handle
                        // handleMessage(data);
                        log.debug("[MSG] consume: {}", data);
                    }
                } catch (Exception e) {
                    log.warn("Err Occurs while handling gRPC Message", e);
                }
            }, 0, 10, TimeUnit.MILLISECONDS);
        }
    }

    public void consume(Message msg) {
        try {
            if (!this.queue.offer(msg)) {
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
