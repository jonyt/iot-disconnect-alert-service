package com.sysaid.disconnectcheck;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DynamoQuery {
    private static final String TABLE_NAME_ENV_VAR = "TABLE_NAME";
    private static final int EXPIRATION_SECONDS = 60;
    private static final String STATE_CHANGES_KEY = "state_changes";

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("");

    private final String tableName;
    private final DynamoDbClient client;

    public DynamoQuery() {
        tableName = System.getenv(TABLE_NAME_ENV_VAR);
        AwsCredentialsProvider credentialsProvider = DefaultCredentialsProvider.create();
        AwsRegionProvider regionProvider = DefaultAwsRegionProviderChain.builder().build();
        client = DynamoDbClient.builder()
                .credentialsProvider(credentialsProvider)
                .region(regionProvider.getRegion())
                .httpClient(UrlConnectionHttpClient.builder().build())
                .build();
        System.out.println("DynamoDb client created");
    }

    public void writeMessage(DisconnectMessage message) {
        System.out.println("Writing data for client " + message.getClientId());
        Map<String,AttributeValue> keyToUpdate = new HashMap<>();
        keyToUpdate.put("clientId", AttributeValue.builder().s(message.getClientId()).build());
        Map<String, AttributeValue> attributesToUpdate = new HashMap<>();
        attributesToUpdate.put(":state_change", AttributeValue.builder().ss(formatStateChange(message)).build());
        attributesToUpdate.put(":expiration", AttributeValue.builder().n(String.valueOf(Instant.now().getEpochSecond() + EXPIRATION_SECONDS)).build());

        UpdateItemRequest updateItemRequest = UpdateItemRequest.builder()
                .tableName(tableName)
                .key(keyToUpdate)
                .updateExpression("SET expiration = :expiration ADD state_changes :state_change")
                .expressionAttributeValues(attributesToUpdate)
                .build();
        client.updateItem(updateItemRequest);
    }

    public void getMessages(String clientId) {
        System.out.println("Getting data for client " + clientId);
        Map<String,AttributeValue> keyToGet = new HashMap<>();
        keyToGet.put("clientId", AttributeValue.builder().s(clientId).build());
        GetItemRequest request = GetItemRequest.builder().tableName(tableName).key(keyToGet).build();
        GetItemResponse response = client.getItem(request);
        Map<String,AttributeValue> item = response.item();
        if (item != null) {
            System.out.println("Data for " + clientId + " found");
            if (item.containsKey(STATE_CHANGES_KEY) && item.get(STATE_CHANGES_KEY).hasSs()) {
                System.out.println("State changes " + item.get(STATE_CHANGES_KEY).ss());
                System.out.println("Num state changes " + item.get(STATE_CHANGES_KEY).ss().size());
            } else {
                Set<String> keys = item.keySet();
                System.out.println("Num keys " + keys.size());
                for (String key1 : keys) {
                    System.out.format("%s: %s\n", key1, item.get(key1).toString());
                }
            }
        } else {
            System.out.println("No data for client " + clientId);
        }
    }

    private String formatStateChange(DisconnectMessage message) {
        ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(message.getTimestamp()), ZoneId.systemDefault());
        return String.format("%s - %s", message.getDisconnectReason(), zdt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    }
}
