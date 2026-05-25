package com.civiccms.controller;

import com.civiccms.dto.*;
import com.civiccms.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // ── Register new citizen account ──────────────────────────────────
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> req) {
        String name     = req.get("name");
        String email    = req.get("email");
        String password = req.get("password");

        if (name == null || name.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "Full name is required."));
        if (email == null || !email.contains("@"))
            return ResponseEntity.badRequest().body(Map.of("error", "A valid email address is required."));
        if (password == null || password.length() < 6)
            return ResponseEntity.badRequest().body(Map.of("error", "Password must be at least 6 characters."));

        try {
            return ResponseEntity.ok(authService.register(name.trim(), email.trim().toLowerCase(), password));
        } catch (RuntimeException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        }
    }

    // ── Email / Password Login ─────────────────────────────────────────
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        try {
            return ResponseEntity.ok(authService.login(req));
        } catch (Exception e) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Invalid email or password"));
        }
    }

    // ── Social Login (Google) ──────────────────────────────────────────
    @PostMapping("/social-login")
    public ResponseEntity<?> socialLogin(@RequestBody SocialLoginRequest req) {
        try {
            return ResponseEntity.ok(authService.socialLogin(req));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    // ── Reset Password (Forgot Password) ──────────────────────────────
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> req) {
        String email       = req.get("email");
        String newPassword = req.get("newPassword");
        if (email == null || email.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "Email is required."));
        if (newPassword == null || newPassword.length() < 8)
            return ResponseEntity.badRequest().body(Map.of("error", "Password must be at least 8 characters."));
        try {
            authService.resetPassword(email, newPassword);
            return ResponseEntity.ok(Map.of("message", "Password reset successfully."));
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    // ── Phone OTP: Send ────────────────────────────────────────────────
    @PostMapping("/otp/send")
    public ResponseEntity<?> sendOtp(@RequestBody OtpSendRequest req) {
        if (req.phone() == null || !req.phone().matches("\\d{10}")) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Enter a valid 10-digit phone number."));
        }
        try {
            authService.sendOtp(req.phone());
            int remaining = authService.getRemainingOtpCount(req.phone());
            return ResponseEntity.ok(Map.of(
                "message",   "OTP sent successfully.",
                "remaining", remaining   // e.g. 9 (out of 10 daily limit)
            ));
        } catch (RuntimeException e) {
            // Includes rate limit exceeded message
            return ResponseEntity.status(429)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to send OTP. Please try again."));
        }
    }

    // ── Phone OTP: Peek (DEV ONLY — remove before production) ─────────
    @GetMapping("/otp/peek")
    public ResponseEntity<?> peekOtp(@RequestParam String phone) {
        String otp = authService.peekOtp(phone);
        if (otp == null) {
            return ResponseEntity.ok(Map.of("message", "No active OTP for this number."));
        }
        return ResponseEntity.ok(Map.of(
            "phone", phone,
            "otp",   otp,
            "note",  "⚠️ Remove this endpoint before going to production!"
        ));
    }

    // ── Phone OTP: Verify ──────────────────────────────────────────────
    @PostMapping("/otp/verify")
    public ResponseEntity<?> verifyOtp(@RequestBody OtpVerifyRequest req) {
        if (req.phone() == null || !req.phone().matches("\\d{10}")) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid phone number."));
        }
        if (req.otp() == null || req.otp().length() != 6) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "OTP must be exactly 6 digits."));
        }
        try {
            return ResponseEntity.ok(authService.verifyOtp(req));
        } catch (Exception e) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
