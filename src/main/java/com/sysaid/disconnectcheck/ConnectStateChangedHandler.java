package com.sysaid.disconnectcheck;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.gson.JsonSyntaxException;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class ConnectStateChangedHandler implements RequestHandler<Map<String, String>, String> {
    private static final String TABLE_NAME_ENV_VAR = "TABLE_NAME";
    private static final int EXPIRATION_SECONDS = 60;

    @Override
    public String handleRequest(Map<String, String> input, Context context) {
        System.out.println("Handling request");

        try {
            DisconnectMessage message = new DisconnectMessage(input);
            System.out.println("Message: " + message);
            new DynamoQuery().writeMessage(message);
//            writeToDb(message);
            System.out.println("Successfully wrote message");

            return "OK";
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
        }  catch (SdkException e) {
            e.printStackTrace();
        }

        return "Failed";
    }

    private void writeToDb(DisconnectMessage message) {
        String tableName = System.getenv(TABLE_NAME_ENV_VAR);
        AwsCredentialsProvider credentialsProvider = DefaultCredentialsProvider.create();
        AwsRegionProvider regionProvider = DefaultAwsRegionProviderChain.builder().build();
        DynamoDbClient client = DynamoDbClient.builder()
                .credentialsProvider(credentialsProvider)
                .region(regionProvider.getRegion())
                .httpClient(UrlConnectionHttpClient.builder().build())
                .build();
        System.out.println("DynamoDb client created");
        Map<String,AttributeValue> keyToUpdate = new HashMap<>();
        keyToUpdate.put("clientId", AttributeValue.builder().s(message.getClientId()).build());
        Map<String, AttributeValue> attributesToUpdate = new HashMap<>();
        attributesToUpdate.put(":state_change", AttributeValue.builder().ss(message.getDisconnectReason()).build());
        attributesToUpdate.put(":expiration", AttributeValue.builder().n(String.valueOf(Instant.now().getEpochSecond() + EXPIRATION_SECONDS)).build());

        UpdateItemRequest updateItemRequest = UpdateItemRequest.builder()
                .tableName(tableName)
                .key(keyToUpdate)
                .updateExpression("SET expiration = :expiration ADD state_changes :state_change")
                .expressionAttributeValues(attributesToUpdate)
                .build();
        client.updateItem(updateItemRequest);
    }
}
