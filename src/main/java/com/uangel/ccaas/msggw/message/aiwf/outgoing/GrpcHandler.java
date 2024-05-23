package com.uangel.ccaas.msggw.message.aiwf.outgoing;

import com.google.protobuf.InvalidProtocolBufferException;
import com.uangel.ccaas.aibotmsg.Message;
import com.uangel.ccaas.msggw.util.PrintMsgModule;
import io.grpc.stub.StreamObserver;

public class GrpcHandler extends OutgoingHandler {

    @Override
    public void reply(Message response, StreamObserver<Message> responseObserver) {
        try {
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            String target = config.getGrpcTargetIp() + ":" + config.getGrpcTargetPort();
            PrintMsgModule.printSendLog(target, response);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
    }
}
