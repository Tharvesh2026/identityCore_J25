package dev.tharbytes.identityCore.service;

import dev.tharbytes.identityCore.dto.response.LogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.*;
import java.util.regex.*;

@Service
public class LogParserService {

    private static final Logger log = LoggerFactory.getLogger(LogParserService.class);

    /** Maximum number of lines to read from the end of the log file */
    private static final int MAX_LINES = 5000;

    // ── Enterprise log line format ──────────────────────────────────────────
    // Example: 2026-07-14T07:08:53.637Z INFO  identityCore [d.t.i.security.AppUserDetailsService] [CorrId=3f2a9c11-8b4d-4e2a-9c1a-1a2b3c4d5e6f] [UserId=users@icore.dev] Authentication initiated for username [users@icore.dev].
    private static final Pattern LOG_LINE = Pattern.compile(
            "^(?<Timestamp>\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?Z?)\\s+" +
                    "(?<Level>TRACE|DEBUG|INFO|WARN|ERROR|FATAL)\\s+" +
                    "(?<Service>\\w+)\\s+" +
                    "\\[(?<Component>[^\\]]+)]\\s+" +
                    "\\[CorrId=(?<CorrelationId>[^\\]]+)]\\s+" +
                    "\\[UserId=(?<UserId>[^\\]]+)]\\s+" +
                    "(?<Message>.*)$"
    );

    // Tolerant timestamp parser: fractional seconds optional (0-9 digits),
    // trailing 'Z' optional. Always treated as UTC (no offset field in format).
    private static final DateTimeFormatter TIMESTAMP_PARSER = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
            .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
            .optionalStart().appendLiteral('Z').optionalEnd()
            .toFormatter()
            .withZone(ZoneOffset.UTC);

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm:ss a");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    // ── Extraction patterns (still needed — these describe things that live
    //    inside the free-text Message, not in the structured header) ────────
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[\\w.+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}"
    );
    private static final Pattern PROVIDER_BRACKET = Pattern.compile(
            "provider\\s*\\[([^\\]]+)]", Pattern.CASE_INSENSITIVE
    );

    // ═══════════════════════════════════════════════════════════════════════════
    //  Public API
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Parse the application log file and return structured log entries (newest first).
     */
    public List<LogEntry> parseLogFile() {
        Path logPath = Paths.get("logs/application.log");
        if (!Files.exists(logPath)) {
            log.warn("Log file not found: {}", logPath.toAbsolutePath());
            return Collections.emptyList();
        }

        List<String> lines;
        try {
            lines = Files.readAllLines(logPath);
        } catch (IOException e) {
            log.error("Failed to read log file.", e);
            return Collections.emptyList();
        }

        int start = Math.max(0, lines.size() - MAX_LINES);
        List<LogEntry> entries = new ArrayList<>();
        LogEntry current = null;

        for (int i = start; i < lines.size(); i++) {
            String line = lines.get(i);
            Matcher m = LOG_LINE.matcher(line);
            if (m.matches()) {
                if (current != null) entries.add(current);
                current = buildEntry(m);
            } else if (current != null && !line.isBlank()) {
                // Continuation line (stack trace, multi-line message)
                current.setMessage(current.getMessage() + "\n" + line);
                if ("ERROR".equals(current.getSeverity()) && "SYSTEM".equals(current.getEventType())) {
                    setEvent(current, "ERROR", "Error", "ti-circle-x", "error");
                }
            }
        }
        if (current != null) entries.add(current);

        // Newest first
        Collections.reverse(entries);

        // Assign indices
        for (int i = 0; i < entries.size(); i++) {
            entries.get(i).setIndex(i);
        }

        return entries;
    }

    /**
     * Get raw log file content as a plain string.
     */
    public String getRawLog() {
        Path logPath = Paths.get("logs/application.log");
        if (!Files.exists(logPath)) return "Log file not found.";
        try {
            return Files.readString(logPath);
        } catch (IOException e) {
            log.error("Failed to read log file.", e);
            return "Error reading log file.";
        }
    }

    /**
     * Compute summary statistics from parsed log entries.
     */
    public Map<String, Long> computeStats(List<LogEntry> entries) {
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("totalEvents", (long) entries.size());
        stats.put("loginEvents", entries.stream()
                .filter(e -> "LOGIN_SUCCESS".equals(e.getEventType()) || "LOGIN_FAILED".equals(e.getEventType()))
                .count());
        stats.put("oauthLogins", entries.stream()
                .filter(e -> "OAUTH_LOGIN".equals(e.getEventType()))
                .count());
        stats.put("warnings", entries.stream()
                .filter(e -> "WARN".equals(e.getSeverity()))
                .count());
        stats.put("errors", entries.stream()
                .filter(e -> "ERROR".equals(e.getSeverity()))
                .count());
        stats.put("logAccesses", entries.stream()
                .filter(e -> "LOG_VIEWED".equals(e.getEventType()))
                .count());
        return stats;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Private — Entry building
    // ═══════════════════════════════════════════════════════════════════════════

    private LogEntry buildEntry(Matcher m) {
        String timestamp     = m.group("Timestamp");
        String severity      = m.group("Level");
        String service       = m.group("Service");
        String component     = m.group("Component");
        String correlationId = m.group("CorrelationId");
        String headerUserId  = m.group("UserId");
        String message       = m.group("Message");

        LogEntry entry = new LogEntry();
        entry.setTimestamp(timestamp);
        entry.setSeverity(severity);
        entry.setService(service);
        entry.setLogger(shortenLogger(component));
        entry.setCorrelationId(correlationId);
        entry.setUserId("anonymous".equalsIgnoreCase(headerUserId) || "N/A".equalsIgnoreCase(headerUserId)
                ? null : headerUserId);
        entry.setMessage(message);

        // Format time and date
        try {
            Instant instant = Instant.from(TIMESTAMP_PARSER.parse(timestamp));
            OffsetDateTime odt = instant.atOffset(ZoneOffset.UTC);
            entry.setFormattedTime(odt.format(TIME_FMT));
            entry.setFormattedDate(odt.format(DATE_FMT));
        } catch (Exception e) {
            entry.setFormattedTime(timestamp);
            entry.setFormattedDate("");
        }

        // Classify event type
        classifyEvent(entry, message, severity, component);

        // Extract metadata still living in free text (email, OAuth provider name)
        extractUser(entry, message);
        extractProvider(entry, message);

        // Fall back to the structured header UserId for display if the message
        // itself didn't contain a friendlier identifier (e.g. an email).
        if (entry.getUser() == null && entry.getUserId() != null) {
            entry.setUser(entry.getUserId());
        }

        return entry;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Private — Event classification
    // ═══════════════════════════════════════════════════════════════════════════

    private void classifyEvent(LogEntry entry, String msg, String severity, String component) {
        String lc = msg.toLowerCase();

        // ── Server lifecycle ───────────────────────────────────
        if (lc.contains("started identitycoreapplication") ||
                lc.contains("started application") ||
                lc.contains("tomcat started on port") ||
                lc.contains("tomcat initialized with port")) {
            setEvent(entry, "SERVER_START", "Application Started", "ti-rocket", "system");
            return;
        }

        // ── Local authentication ──────────────────────────────
        if (lc.contains("authentication successful")) {
            setEvent(entry, "LOGIN_SUCCESS", "Login Successful", "ti-key", "success");
            entry.setProvider("Local");
            return;
        }
        if (lc.contains("authentication failed")) {
            setEvent(entry, "LOGIN_FAILED", "Login Failed", "ti-key", "error");
            entry.setProvider("Local");
            return;
        }
        if (lc.contains("authentication initiated")) {
            setEvent(entry, "SYSTEM", "Auth Initiated", "ti-key", "system");
            entry.setProvider("Local");
            return;
        }

        // ── OAuth2 ────────────────────────────────────────────
        if (lc.contains("oauth2 login successful")) {
            String prov = resolveProvider(msg);
            setEvent(entry, "OAUTH_LOGIN", prov + " Login",
                    providerIcon(prov), "security");
            entry.setProvider(prov);
            return;
        }
        if (lc.contains("oauth2 login denied") || lc.contains("oauth2 login failed")) {
            String prov = resolveProvider(msg);
            setEvent(entry, "LOGIN_FAILED", prov + " Login Failed",
                    providerIcon(prov), "error");
            entry.setProvider(prov);
            return;
        }
        if (lc.contains("oauth2 login initiated")) {
            String prov = resolveProvider(msg);
            setEvent(entry, "SYSTEM", prov + " Auth Initiated",
                    providerIcon(prov), "system");
            entry.setProvider(prov);
            return;
        }
        if (lc.contains("provisioned") || lc.contains("provisioning")) {
            String prov = resolveProvider(msg);
            setEvent(entry, "OAUTH_LOGIN", "User Auto-Provisioned", "ti-user-plus", "security");
            entry.setProvider(prov);
            return;
        }

        // ── User registration ─────────────────────────────────
        if (lc.contains("user registered") || lc.contains("registered successfully") ||
                lc.contains("registration successful")) {
            setEvent(entry, "USER_CREATED", "User Registered", "ti-user-plus", "info");
            return;
        }

        // ── Profile updates ───────────────────────────────────
        if (lc.contains("profile updated") || (lc.contains("profile update") && lc.contains("success"))) {
            setEvent(entry, "USER_UPDATED", "Profile Updated", "ti-user-edit", "info");
            return;
        }

        // ── Status updates ────────────────────────────────────
        if (lc.contains("status_update") || lc.contains("status updated")) {
            setEvent(entry, "USER_UPDATED", "Status Updated", "ti-toggle-right", "info");
            return;
        }

        // ── Role management ───────────────────────────────────
        if (lc.contains("role updated") || lc.contains("role assigned") ||
                lc.contains("role_changed")) {
            setEvent(entry, "ROLE_CHANGED", "Role Updated", "ti-shield", "info");
            return;
        }
        if (lc.contains("role") && (lc.contains("created") || lc.contains("renamed") ||
                lc.contains("activated") || lc.contains("deactivated"))) {
            setEvent(entry, "ROLE_CHANGED", "Role Modified", "ti-shield", "info");
            return;
        }
        if (lc.contains("permissions updated")) {
            setEvent(entry, "ROLE_CHANGED", "Permissions Updated", "ti-shield-check", "info");
            return;
        }

        // ── Password ──────────────────────────────────────────
        if (lc.contains("password reset") || lc.contains("password_reset") ||
                lc.contains("password changed")) {
            setEvent(entry, "PASSWORD_RESET", "Password Reset", "ti-refresh", "warning");
            return;
        }

        // ── Log access ────────────────────────────────────────
        if (lc.contains("log file access") || lc.contains("log file served") ||
                lc.contains("log page accessed")) {
            setEvent(entry, "LOG_VIEWED", "Logs Accessed", "ti-file-description", "info");
            entry.setEndpoint("/logs");
            return;
        }

        // ── Session ───────────────────────────────────────────
        if (lc.contains("session") && (lc.contains("refreshed") || lc.contains("refresh"))) {
            setEvent(entry, "SYSTEM", "Session Refresh", "ti-refresh", "system");
            return;
        }

        // ── Database / Hibernate ──────────────────────────────
        if (component.contains("hibernate") || component.contains("HikariPool") ||
                component.contains("Hikari") || lc.contains("hikaripool")) {
            setEvent(entry, "SYSTEM", "Database Event", "ti-database", "system");
            return;
        }

        // ── Severity-based fallback ───────────────────────────
        if ("ERROR".equals(severity)) {
            setEvent(entry, "ERROR", "Error", "ti-circle-x", "error");
            return;
        }
        if ("WARN".equals(severity)) {
            setEvent(entry, "WARNING", "Warning", "ti-alert-triangle", "warning");
            return;
        }

        // ── Default ───────────────────────────────────────────
        setEvent(entry, "SYSTEM", "System Event", "ti-settings", "system");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Private — Metadata extraction (free-text only; header fields are direct)
    // ═══════════════════════════════════════════════════════════════════════════

    private void extractUser(LogEntry entry, String msg) {
        // Priority 1: find an email address in the message (nicer for display
        // than a raw numeric/opaque UserId from the header).
        Matcher emailMatcher = EMAIL_PATTERN.matcher(msg);
        if (emailMatcher.find()) {
            entry.setUser(emailMatcher.group());
            return;
        }
        // Priority 2: extract username from patterns like "User registered: admin"
        if (msg.contains("registered:")) {
            String[] parts = msg.split("registered:\\s*");
            if (parts.length > 1 && !parts[1].isBlank()) {
                entry.setUser(parts[1].trim());
            }
        }
    }

    private void extractProvider(LogEntry entry, String msg) {
        if (entry.getProvider() != null) return;
        Matcher m = PROVIDER_BRACKET.matcher(msg);
        if (m.find()) {
            entry.setProvider(capitalize(m.group(1)));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Private — Utilities
    // ═══════════════════════════════════════════════════════════════════════════

    private void setEvent(LogEntry entry, String type, String label, String icon, String color) {
        entry.setEventType(type);
        entry.setEventLabel(label);
        entry.setEventIcon(icon);
        entry.setColorClass(color);
    }

    private String resolveProvider(String msg) {
        Matcher m = PROVIDER_BRACKET.matcher(msg);
        if (m.find()) return capitalize(m.group(1));
        String lc = msg.toLowerCase();
        if (lc.contains("google")) return "Google";
        if (lc.contains("github")) return "GitHub";
        return "OAuth";
    }

    private String providerIcon(String provider) {
        if ("GitHub".equalsIgnoreCase(provider)) return "ti-brand-github";
        if ("Google".equalsIgnoreCase(provider)) return "ti-brand-google";
        return "ti-key";
    }

    private String shortenLogger(String component) {
        if (component == null) return "";
        int lastDot = component.lastIndexOf('.');
        return lastDot >= 0 ? component.substring(lastDot + 1) : component;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        if ("github".equalsIgnoreCase(s)) return "GitHub";
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
}