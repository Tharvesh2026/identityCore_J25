// Dynamic Navbar generator based on User Permissions and Bootstrap Theme
function initNavbar(user) {
    const sidebar = document.getElementById("sidebar");
    if (!sidebar) return;

    const currentPath = window.location.pathname;
    const permissions = user.permissions || [];

    const welcomeActive = currentPath.endsWith("welcome.html") ? "active" : "";
    const profileActive = currentPath.endsWith("profile.html") ? "active" : "";
    const usersActive = currentPath.endsWith("users.html") || currentPath.endsWith("manage-user.html") ? "active" : "";
    const rolesActive = currentPath.endsWith("roles.html") || currentPath.endsWith("manage-role.html") ? "active" : "";
    const logsActive = currentPath.endsWith("log.html") ? "active" : "";
    const settingsActive = currentPath.endsWith("settings.html") ? "active" : "";

    let html = `
    <!-- Logo -->
    <div class="sidebar-logo">
        <div class="logo-mark">
            <i class="ti ti-cloud"></i>
        </div>
        <div>
            <div class="logo-text">i.Core</div>
            <div class="logo-ver">tharbytes.io</div>
        </div>
    </div>

    <!-- User pill -->
    <div class="sidebar-user">
        <div class="user-pill">
            <div class="d-flex align-items-center justify-content-center text-white font-weight-bold" 
                 style="width: 32px; height: 32px; border-radius: 6px; background-color: var(--brand); font-size: 13px;">
                ${user.name.charAt(0).toUpperCase()}
            </div>
            <div style="overflow:hidden; line-height:1.2;">
                <div style="font-size:13px; font-weight:600; color:var(--text-white); white-space:nowrap; overflow:hidden; text-overflow:ellipsis;">
                    ${user.name}
                </div>
                <div style="font-size:10.5px; color:var(--text-muted); text-transform:uppercase; margin-top:2px;">
                    ${user.role}
                </div>
            </div>
        </div>
    </div>

    <!-- Main nav -->
    <div class="nav-section-label">Main</div>

    <a href="${getRelativePath("welcome.html")}" class="nav-item-link ${welcomeActive}">
        <i class="ti ti-layout-dashboard"></i> Dashboard
    </a>

    <a href="${getRelativePath("profile.html")}" class="nav-item-link ${profileActive}">
        <i class="ti ti-user-circle"></i> Profile
    </a>
    `;

    if (permissions.includes("USER_READ")) {
        html += `
        <a href="${getRelativePath("users.html")}" class="nav-item-link ${usersActive}">
            <i class="ti ti-users"></i> Users
        </a>
        `;
    }

    if (permissions.includes("ROLE_READ")) {
        html += `
        <a href="${getRelativePath("roles.html")}" class="nav-item-link ${rolesActive}">
            <i class="ti ti-shield-lock"></i> Roles
        </a>
        `;
    }

    html += `
    <!-- System nav -->
    <div class="nav-section-label">System</div>
    `;

    if (permissions.includes("LOG_VIEW")) {
        html += `
        <a href="${getRelativePath("log.html")}" class="nav-item-link ${logsActive}">
            <i class="ti ti-file-description"></i> Audit Logs
        </a>
        `;
    }

    html += `
    <a href="${API_BASE_URL}/swagger-ui/index.html" target="_blank" class="nav-item-link">
        <i class="ti ti-document"></i> Api Docs
    </a>

    <a href="${getRelativePath("settings.html")}" class="nav-item-link ${settingsActive}">
        <i class="ti ti-settings"></i> Settings
    </a>

    <!-- Footer / Logout -->
    <div class="sidebar-footer">
        <button onclick="apiLogout()" class="nav-item-link text-danger border-0 bg-transparent w-100 text-start" style="cursor:pointer;">
            <i class="ti ti-logout"></i> Sign out
        </button>
    </div>
    `;

    sidebar.innerHTML = html;
}
