package com.sysaid.disconnectcheck;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DisconnectMessageTest {

    @Test
    void messageDeserializationWorks() {
        String messageJson = "{" +
                "\"clientId\": \"186b5\"," +
                "\"timestamp\": 1573002340451," +
                "\"eventType\": \"disconnected\"," +
                "\"sessionIdentifier\": \"a4666d2a7d844ae4ac5d7b38c9cb7967\"," +
                "\"principalIdentifier\": \"12345678901234567890123456789012\"," +
                "\"clientInitiatedDisconnect\": true," +
                "\"disconnectReason\": \"CLIENT_INITIATED_DISCONNECT\"," +
                "\"versionNumber\": 0" +
                "}";
        Gson gson = new GsonBuilder().create();

        DisconnectMessage message = gson.fromJson(messageJson, DisconnectMessage.class);
        assertEquals("186b5", message.getClientId());
    }
}