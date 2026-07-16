// REST API Configuration for Standalone UI
const API_BASE_URL = window.location.origin.includes("localhost") || window.location.origin.includes("127.0.0.1") || window.location.protocol === "file:"
    ? "http://localhost:8080"
    : "https://cloud.imtharvesh.me";

// Standardized fetch wrapper that handles credentials, base URL, and errors
async function apiFetch(path, options = {}) {
    // Inject credentials for CORS cookie transport
    options.credentials = "include";

    // Set headers
    options.headers = options.headers || {};
    if (options.body && typeof options.body === "string" && !options.headers["Content-Type"]) {
        options.headers["Content-Type"] = "application/json";
    }

    // Force API requests to accept JSON response
    if (!options.headers["Accept"]) {
        options.headers["Accept"] = "application/json";
    }

    const url = path.startsWith("http") ? path : `${API_BASE_URL}${path}`;
    try {
        const response = await fetch(url, options);
        
        // Handle unauthorized session expiration
        const isPublicPage = window.location.pathname.endsWith("index.html") || 
                             window.location.pathname.endsWith("/") || 
                             window.location.pathname.endsWith("/UI/") ||
                             window.location.pathname.endsWith("forgot-password.html") || 
                             window.location.pathname.endsWith("reset-password.html") ||
                             window.location.pathname.endsWith("terms.html") || 
                             window.location.pathname.endsWith("privacy.html") || 
                             window.location.pathname.endsWith("cookie-policy.html");

        if (response.status === 401 && !options.bypassAuthCheck && !isPublicPage) {
            handleSessionExpired();
            return;
        }

        // If the response is text-based (like raw logs)
        const contentType = response.headers.get("content-type");
        if (contentType && contentType.includes("text/plain")) {
            return await response.text();
        }

        const data = await response.json();
        if (!response.ok) {
            throw new Error(data.message || "An error occurred");
        }
        return data;
    } catch (error) {
        console.error(`API Fetch Error [${path}]:`, error);
        throw error;
    }
}

// Redirects the browser back to the login screen on session expiration
function handleSessionExpired() {
    window.location.href = getRelativePath("index.html") + "?error=Session+expired.+Please+login+again.";
}

// Calculates dynamic relative paths so that nesting does not break links
function getRelativePath(targetPage) {
    const depth = window.location.pathname.split("/").length - 2;
    const prefix = depth > 0 ? "../".repeat(depth) : "";
    return prefix + targetPage;
}

// Performs a full backend logout and cookie clearance
async function apiLogout() {
    try {
        await apiFetch("/auth/logout", { method: "POST" });
    } catch (e) {
        console.warn("API logout call failed, clearing local state anyway", e);
    }
    window.location.href = getRelativePath("index.html") + "?logout=SUCCESS";
}

// Check session on page load
async function requireAuth() {
    try {
        const res = await apiFetch("/user/welcome");
        return res.data;
    } catch (e) {
        handleSessionExpired();
    }
}

// --- Session Timeout Countdown & Activity Tracker ---
let countdownTimer = null;
let userActive = false;

function startSessionCountdown(timeoutSeconds, onTick) {
    let remaining = timeoutSeconds;
    if (countdownTimer) clearInterval(countdownTimer);

    function tick() {
        if (remaining <= 0) {
            clearInterval(countdownTimer);
            apiLogout();
            return;
        }
        if (onTick) onTick(remaining);
        remaining--;
    }

    tick();
    countdownTimer = setInterval(tick, 1000);

    // Track user activity to trigger auto-refresh
    ["mousemove", "keydown", "click", "input", "scroll"].forEach(ev => {
        document.addEventListener(ev, () => { userActive = true; });
    });

    // Run a background task to refresh session if the user is active
    const refreshInterval = Math.max((timeoutSeconds - 60), 30) * 1000;
    setInterval(async () => {
        if (!userActive) return;
        try {
            await apiFetch("/user/refresh-session", { method: "POST" });
            userActive = false;
            remaining = timeoutSeconds; // Reset counter locally
            logActivity("Session refreshed automatically");
        } catch (e) {
            console.error("Auto-session refresh failed", e);
        }
    }, refreshInterval);
}

function logActivity(msg) {
    console.log(`[i.Core Auth] ${msg} at ${new Date().toLocaleTimeString()}`);
}
