package com.sysaid.disconnectcheck;

import java.util.Map;

/**
 * @see <a href="https://docs.aws.amazon.com/iot/latest/developerguide/life-cycle-events.html">AWS docs</a>
 * {
 *     "clientId": "186b5",
 *     "timestamp": 1573002340451,
 *     "eventType": "disconnected",
 *     "sessionIdentifier": "a4666d2a7d844ae4ac5d7b38c9cb7967",
 *     "principalIdentifier": "12345678901234567890123456789012",
 *     "clientInitiatedDisconnect": true,
 *     "disconnectReason": "CLIENT_INITIATED_DISCONNECT",
 *     "versionNumber": 0
 * }
 */

public class DisconnectMessage {
    private String clientId;
    private long timestamp;
    private String eventType;
    private String sessionIdentifier;
    private String principalIdentifier;
    private String clientInitiatedDisconnect;
    private String disconnectReason;
    private String versionNumber;

    public DisconnectMessage(Map<String, String> input) {
        this.clientId = input.get("clientId");
        this.timestamp = Long.parseLong(input.get("timestamp"));
        this.eventType = input.get("eventType");
        this.sessionIdentifier = input.get("sessionIdentifier");
        this.principalIdentifier = input.get("principalIdentifier");
        this.clientInitiatedDisconnect = input.get("clientInitiatedDisconnect");
        this.disconnectReason = input.get("disconnectReason") == null ? "connect" : input.get("disconnectReason");
        this.versionNumber = input.get("versionNumber");
    }

    public String getClientId() {
        return clientId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getEventType() {
        return eventType;
    }

    public String getSessionIdentifier() {
        return sessionIdentifier;
    }

    public String getPrincipalIdentifier() {
        return principalIdentifier;
    }

    public String getClientInitiatedDisconnect() {
        return clientInitiatedDisconnect;
    }

    public String getDisconnectReason() {
        return disconnectReason;
    }

    @Override
    public String toString() {
        return "DisconnectMessage{" +
                "clientId='" + clientId + '\'' +
                ", disconnectReason='" + disconnectReason + '\'' +
                '}';
    }
}
