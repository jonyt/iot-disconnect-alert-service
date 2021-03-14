package com.sysaid.disconnectcheck;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class DisconnectHandler implements RequestHandler<SQSEvent, String>{
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public String handleRequest(SQSEvent event, Context context) {
        StringBuilder sb = new StringBuilder("Handled clients: ");
        for (SQSMessage message : event.getRecords()){
            System.out.println("Received disconnect message " + message.getBody());
            DisconnectMessage disconnectMessage = GSON.fromJson(message.getBody(), DisconnectMessage.class);
            new DynamoQuery().getMessages(disconnectMessage.getClientId());
            sb.append(disconnectMessage.getClientId()).append(", ");
        }

        return sb.toString();
    }
}
