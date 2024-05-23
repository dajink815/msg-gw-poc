package com.uangel.ccaas.msggw.grpc;

import com.google.protobuf.InvalidProtocolBufferException;
import com.uangel.ccaas.aibotmsg.AiBotServiceGrpc;
import com.uangel.ccaas.aibotmsg.Message;
import com.uangel.ccaas.msggw.message.aiwf.BotStartReq;
import com.uangel.ccaas.msggw.message.aiwf.BotStopReq;
import com.uangel.ccaas.msggw.message.aiwf.BotTalkReq;
import com.uangel.ccaas.msggw.util.PrintMsgModule;
import io.grpc.stub.StreamObserver;

public class AiBotServiceImpl extends AiBotServiceGrpc.AiBotServiceImplBase {

    private final GwGrpcConsumer gwGrpcConsumer;

    public AiBotServiceImpl(GwGrpcConsumer gwGrpcConsumer) {
        this.gwGrpcConsumer = gwGrpcConsumer;
    }

    // todo StreamObserver 전달
    @Override
    public void botStart(Message request, StreamObserver<Message> responseObserver) {
        try {
            PrintMsgModule.printRcvLog(request);
            gwGrpcConsumer.consume(request);

            // 세션 생성 후 바로 BotStartRes 응답
            //new BotStartReq().handle(request, 1, responseObserver);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void botTalk(Message request, StreamObserver<Message> responseObserver) {
        try {
            PrintMsgModule.printRcvLog(request);
            gwGrpcConsumer.consume(request);

            //new BotTalkReq().handle(request);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void botStop(Message request, StreamObserver<Message> responseObserver) {
        try {
            PrintMsgModule.printRcvLog(request);
            gwGrpcConsumer.consume(request);

            // 세션 삭제 후 바로 BotStopRes 응답
            //new BotStopReq().handle(request);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
    }
}
