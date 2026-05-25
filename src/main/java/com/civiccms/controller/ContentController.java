package com.civiccms.controller;

import com.civiccms.entity.SiteContent;
import com.civiccms.repository.SiteContentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/**
 * REST API for the Content Management System.
 *
 * Public  GET  /api/content?page=index       → returns all entries for a page
 * Admin   GET  /api/content/all              → returns every entry (for the editor)
 * Admin   POST /api/content                  → upsert a text entry
 * Admin   POST /api/content/image            → upload an image and upsert the entry
 * Admin   DELETE /api/content/{id}           → remove an entry
 *
 * The GET endpoints are intentionally public so the front-end can load content
 * without a token; write endpoints are protected by SecurityConfig (ADMIN role).
 */
@RestController
@RequestMapping("/api/content")
public class ContentController {

    private static final Logger log = LoggerFactory.getLogger(ContentController.class);

    private final SiteContentRepository repo;

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    public ContentController(SiteContentRepository repo) {
        this.repo = repo;
    }

    // ── READ ────────────────────────────────────────────────────────

    /** Returns all content for one page as { "hero.headline": "...", ... } */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getByPage(@RequestParam String page) {
        List<SiteContent> items = repo.findByPageOrderByContentKey(page);
        Map<String, Object> result = new LinkedHashMap<>();
        for (SiteContent sc : items) {
            result.put(sc.getContentKey(), Map.of(
                    "value", sc.getValue() != null ? sc.getValue() : "",
                    "type",  sc.getContentType().name(),
                    "label", sc.getLabel() != null ? sc.getLabel() : sc.getContentKey(),
                    "id",    sc.getId()
            ));
        }
        return ResponseEntity.ok(result);
    }

    /** Returns all entries grouped by page — used by the admin Content Editor */
    @GetMapping("/all")
    public ResponseEntity<List<SiteContent>> getAll() {
        return ResponseEntity.ok(repo.findAllByOrderByPageAscContentKeyAsc());
    }

    // ── UPSERT TEXT ─────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<SiteContent> upsertText(@RequestBody UpsertRequest req) {
        SiteContent sc = repo.findByPageAndContentKey(req.page(), req.contentKey())
                .orElseGet(SiteContent::new);
        sc.setPage(req.page());
        sc.setContentKey(req.contentKey());
        sc.setContentType(SiteContent.ContentType.TEXT);
        sc.setValue(req.value());
        if (req.label() != null) sc.setLabel(req.label());
        repo.save(sc);
        log.info("Content updated — page={} key={}", req.page(), req.contentKey());
        return ResponseEntity.ok(sc);
    }

    // ── UPSERT IMAGE ────────────────────────────────────────────────

    @PostMapping("/image")
    public ResponseEntity<?> uploadImage(
            @RequestParam("page")       String page,
            @RequestParam("contentKey") String contentKey,
            @RequestParam(value = "label", required = false) String label,
            @RequestParam("file")       MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No file provided"));
        }

        String originalName = file.getOriginalFilename();
        String ext = (originalName != null && originalName.contains("."))
                ? originalName.substring(originalName.lastIndexOf('.'))
                : ".jpg";
        // Use a unique filename to avoid collisions
        String filename = "cms_" + page + "_" + contentKey.replace('.', '_')
                + "_" + System.currentTimeMillis() + ext;

        try {
            Path dir = Paths.get(uploadDir);
            Files.createDirectories(dir);
            Path dest = dir.resolve(filename);
            Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);

            String publicUrl = "/uploads/" + filename;

            SiteContent sc = repo.findByPageAndContentKey(page, contentKey)
                    .orElseGet(SiteContent::new);
            sc.setPage(page);
            sc.setContentKey(contentKey);
            sc.setContentType(SiteContent.ContentType.IMAGE);
            sc.setValue(publicUrl);
            if (label != null) sc.setLabel(label);
            repo.save(sc);

            log.info("Image uploaded — page={} key={} url={}", page, contentKey, publicUrl);
            return ResponseEntity.ok(Map.of(
                    "url",  publicUrl,
                    "id",   sc.getId(),
                    "page", page,
                    "key",  contentKey
            ));
        } catch (IOException e) {
            log.error("Image upload failed", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Upload failed: " + e.getMessage()));
        }
    }

    // ── DELETE ──────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id) {
        if (!repo.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repo.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Deleted"));
    }

    // ── DTOs ────────────────────────────────────────────────────────

    record UpsertRequest(String page, String contentKey, String value, String label) {}
}
