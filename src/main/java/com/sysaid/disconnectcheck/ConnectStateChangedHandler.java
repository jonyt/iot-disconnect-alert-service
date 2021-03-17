package com.sysaid.disconnectcheck;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.gson.JsonSyntaxException;
import software.amazon.awssdk.core.exception.SdkException;

import java.util.Map;

public class ConnectStateChangedHandler implements RequestHandler<Map<String, String>, String> {

    @Override
    public String handleRequest(Map<String, String> input, Context context) {
        System.out.println("Handling request");

        try {
            DisconnectMessage message = new DisconnectMessage(input);
            System.out.println("Message: " + message);
            new DynamoQuery().writeMessage(message);
            System.out.println("Successfully wrote message");

            return "OK";
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
        }  catch (SdkException e) {
            e.printStackTrace();
        }

        return "Failed";
    }
}
