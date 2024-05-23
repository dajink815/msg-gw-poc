package com.uangel.ccaas.msggw.message.ai;

import com.google.protobuf.InvalidProtocolBufferException;
import com.uangel.ccaas.aibotmsg.*;
import com.uangel.ccaas.msggw.config.MsgGwConfig;
import com.uangel.ccaas.msggw.message.AiwfCallBack;
import com.uangel.ccaas.msggw.message.AiwfOutMessage;
import com.uangel.ccaas.msggw.message.type.AiBotMsgType;
import com.uangel.ccaas.msggw.service.AppInstance;
import com.uangel.ccaas.msggw.session.SessionInfo;
import com.uangel.ccaas.msggw.util.PrintMsgModule;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
public class AiGrpcSender {
    private static final MsgGwConfig config = AppInstance.getInstance().getConfig();
    private static final String HOST = config.getGrpcTargetIp();
    private static final int PORT = config.getGrpcTargetPort();
    private final ManagedChannel channel; // gRPC 서버와의 연결
    private final AiBotServiceGrpc.AiBotServiceBlockingStub stub;
    private final AiBotServiceGrpc.AiBotServiceStub serviceStub;

    public AiGrpcSender() {
        channel = ManagedChannelBuilder.forAddress(HOST, PORT)
                .usePlaintext().build();
        stub = AiBotServiceGrpc.newBlockingStub(channel);
        serviceStub = AiBotServiceGrpc.newStub(channel);
    }

    public void shutdown() {
        try {
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Failed to handle TestGrpcSender.shutdown", e);
        }
    }

    public boolean isShutdown() {
        return channel.isShutdown();
    }

    public boolean sendAiRequest(SessionInfo sessionInfo, String question, AiwfCallBack callBack) {
        try {
            String sessionId = sessionInfo.getSessionId();
            String botId = sessionInfo.getBotId();

            // build & print request message
            AiMessage aiMessage = AiMessage.newBuilder()
                    .setSessionId(sessionId)
                    .setMsg(question)
                    .build();
            Message request = new AiwfOutMessage(AiBotMsgType.AI_MESSAGE, botId, aiMessage).build();
            PrintMsgModule.printSendLog(HOST + ":" + PORT, request);

            //Consumer<Message> resHandler = response -> printLog(response, false);

            // send request
            serviceStub.aiRequest(request, new StreamObserver<>() {
                @Override
                public void onNext(Message response) {
                    try {
                        // rcv AiMessage Response
                        PrintMsgModule.printRcvLog(response);
                    } catch (InvalidProtocolBufferException e) {
                        throw new RuntimeException(e);
                    }

                    // parse Msg
                    AiMessage aiMsgRes = response.getAiMessage();
                    String answer = aiMsgRes.getMsg();

                    // build & send BotTalkRes
                    BotTalkRes botTalkRes = BotTalkRes.newBuilder()
                            .setSessionId(sessionId)
                            .setType(1)
                            .addAnswer(buildTtsMessage(answer))
                            .build();
                    Message aiwfResponse = new AiwfOutMessage(AiBotMsgType.BOT_TALK_RES, sessionInfo.getTalkTid(), botId,botTalkRes).build();
                    callBack.reply(aiwfResponse);
                }

                @Override
                public void onError(Throwable throwable) {
                    log.error("[AI] AiRequest Error occurred: {}", throwable.getMessage());
                }

                @Override
                public void onCompleted() {
                    log.debug("[AI] AiRequest Completed");
                }
            });

            log.debug("[AI] End of AiRequest");
            return true;
        } catch (Exception e) {
            log.warn("[AI] Failed to handle AiGrpcSender.AiRequest", e);
        }

        return false;
    }

    private TtsMessage buildTtsMessage(String content) {
        return TtsMessage.newBuilder()
                .setText(content)
                .setBargeIn(false)
                .build();
    }


}
