package dev.tharbytes.identityCore.controller;

import dev.tharbytes.identityCore.dto.response.LogEntry;
import dev.tharbytes.identityCore.entity.UserEntity;
import dev.tharbytes.identityCore.security.AuthHelper;
import dev.tharbytes.identityCore.service.LogParserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Controller
public class LogController {

    private static final Logger log = LoggerFactory.getLogger(LogController.class);

    /** Number of log entries shown per page */
    private static final int PAGE_SIZE = 7;

    private final LogParserService logParserService;
    private final AuthHelper authHelper;

    public LogController(LogParserService logParserService, AuthHelper authHelper) {
        this.logParserService = logParserService;
        this.authHelper = authHelper;
    }

    /**
     * GET /log             → structured log viewer (Thymeleaf template), page 0
     * GET /log?page=N       → structured log viewer, page N (0-indexed)
     * GET /log?view=raw     → raw plain-text log file (unpaginated)
     */
    @GetMapping("/log")
    public String logPage(
            @RequestParam(value = "view", required = false) String view,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            Model model,
            HttpServletResponse response) throws IOException {

        UserEntity user = authHelper.requireCurrentUser();

        // ── Raw view mode ──────────────────────────────────────
        if ("raw".equalsIgnoreCase(view)) {
            log.info("Raw log file requested by user [{}].", user.getId());
            response.setContentType("text/plain;charset=UTF-8");
            response.setHeader("Content-Disposition", "inline; filename=\"application.log\"");
            PrintWriter out = response.getWriter();
            out.print(logParserService.getRawLog());
            out.flush();
            return null; // response already written
        }

        // ── Structured log view ────────────────────────────────
        log.info("Application log page accessed by user [{}].", user.getId());

        List<LogEntry> allEntries = logParserService.parseLogFile();

        // Stats are computed over the FULL log, not just the current page
        Map<String, Long> stats = logParserService.computeStats(allEntries);

        int totalItems = allEntries.size();
        int totalPages = totalItems == 0 ? 1 : (int) Math.ceil(totalItems / (double) PAGE_SIZE);

        // Clamp requested page into valid range [0, totalPages - 1]
        int currentPage = Math.max(0, Math.min(page, totalPages - 1));

        int fromIndex = currentPage * PAGE_SIZE;
        int toIndex = Math.min(fromIndex + PAGE_SIZE, totalItems);

        List<LogEntry> pageEntries = fromIndex >= totalItems
                ? Collections.emptyList()
                : allEntries.subList(fromIndex, toIndex);

        model.addAttribute("logEntries", pageEntries);
        model.addAttribute("logStats", stats);

        // ── Pagination metadata for the view ───────────────────
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("pageSize", PAGE_SIZE);
        model.addAttribute("totalItems", totalItems);
        model.addAttribute("hasPrev", currentPage > 0);
        model.addAttribute("hasNext", currentPage < totalPages - 1);

        return "log";
    }
}