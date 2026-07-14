package dev.tharbytes.identityCore.dto.response;

/**
 * Represents a single parsed log event for the structured log viewer.
 */
public class LogEntry {

    private int index;
    private String timestamp;
    private String formattedTime;
    private String formattedDate;
    private String severity;
    private String service;
    private String logger;          // populated from the "Component" header field
    private String correlationId;   // populated from "[CorrId=...]"
    private String userId;          // populated from "[UserId=...]" (null if anonymous)
    private String message;
    private String eventType;
    private String eventLabel;
    private String eventIcon;
    private String colorClass;
    private String user;            // friendlier display identity (email if found in message)
    private String provider;
    private String endpoint;
    private String extra;

    public LogEntry() {}

    // ── Getters & Setters ───────────────────────────────────

    public int getIndex() { return index; }
    public void setIndex(int index) { this.index = index; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getFormattedTime() { return formattedTime; }
    public void setFormattedTime(String formattedTime) { this.formattedTime = formattedTime; }

    public String getFormattedDate() { return formattedDate; }
    public void setFormattedDate(String formattedDate) { this.formattedDate = formattedDate; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getService() { return service; }
    public void setService(String service) { this.service = service; }

    public String getLogger() { return logger; }
    public void setLogger(String logger) { this.logger = logger; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getEventLabel() { return eventLabel; }
    public void setEventLabel(String eventLabel) { this.eventLabel = eventLabel; }

    public String getEventIcon() { return eventIcon; }
    public void setEventIcon(String eventIcon) { this.eventIcon = eventIcon; }

    public String getColorClass() { return colorClass; }
    public void setColorClass(String colorClass) { this.colorClass = colorClass; }

    public String getUser() { return user; }
    public void setUser(String user) { this.user = user; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }

    public String getExtra() { return extra; }
    public void setExtra(String extra) { this.extra = extra; }
}