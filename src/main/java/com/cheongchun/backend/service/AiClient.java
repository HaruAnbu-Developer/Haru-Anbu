package com.cheongchun.backend.service;

import com.haruanbu.ai.grpc.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.stereotype.Service;
import javax.annotation.PreDestroy;

@Service
public class AiClient {
    private final ManagedChannel channel;
    private final VoiceConversationGrpc.VoiceConversationBlockingStub blockingStub;

    public AiClient() {
        // Python gRPC Server (localhost:50051)
        this.channel = ManagedChannelBuilder.forAddress("localhost", 50051)
                .usePlaintext()
                .build();
        this.blockingStub = VoiceConversationGrpc.newBlockingStub(channel);
    }

    public QuestionResponse getDailyQuestion() {
        return blockingStub.getDailyQuestion(Empty.newBuilder().build());
    }

    public RadioResponse generateRadio(String userId, String userName, String answerText) {
        RadioRequest request = RadioRequest.newBuilder()
                .setUserId(userId)
                .setUserName(userName)
                .setAnswerText(answerText)
                .build();
        return blockingStub.generateRadioContent(request);
    }

    @PreDestroy
    public void shutdown() {
        if (channel != null) {
            channel.shutdown();
        }
    }
}
