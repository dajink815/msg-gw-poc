package com.uangel.ccaas.msggw.grpc;

import com.google.protobuf.InvalidProtocolBufferException;
import com.uangel.ccaas.aibotmsg.AiBotServiceGrpc;
import com.uangel.ccaas.aibotmsg.Message;
import com.uangel.ccaas.msggw.util.PrintMsgModule;
import io.grpc.stub.StreamObserver;

public class AiBotServiceImpl extends AiBotServiceGrpc.AiBotServiceImplBase {

    private final GwGrpcConsumer gwGrpcConsumer;

    public AiBotServiceImpl(GwGrpcConsumer gwGrpcConsumer) {
        this.gwGrpcConsumer = gwGrpcConsumer;
    }

    @Override
    public void botStart(Message request, StreamObserver<Message> responseObserver) {
        try {
            PrintMsgModule.printRcvLog(request);
            gwGrpcConsumer.consume(request, responseObserver);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void botTalk(Message request, StreamObserver<Message> responseObserver) {
        try {
            PrintMsgModule.printRcvLog(request);
            gwGrpcConsumer.consume(request, responseObserver);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void botStop(Message request, StreamObserver<Message> responseObserver) {
        try {
            PrintMsgModule.printRcvLog(request);
            gwGrpcConsumer.consume(request, responseObserver);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
    }
}
