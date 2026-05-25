package com.civiccms.service;

import com.civiccms.dto.*;
import com.civiccms.entity.User;
import com.civiccms.repository.UserRepository;
import com.civiccms.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;

@Service
public class AuthService implements UserDetailsService {

    private final UserRepository   userRepo;
    private final JwtTokenProvider jwtProvider;
    private final PasswordEncoder  passwordEncoder;
    private final OtpStore         otpStore;

    // ── Fast2SMS config ────────────────────────────────────────────────
    @Value("${fast2sms.api.key:}")
    private String fast2smsApiKey;

    public AuthService(UserRepository userRepo,
                       JwtTokenProvider jwtProvider,
                       PasswordEncoder passwordEncoder,
                       OtpStore otpStore) {
        this.userRepo        = userRepo;
        this.jwtProvider     = jwtProvider;
        this.passwordEncoder = passwordEncoder;
        this.otpStore        = otpStore;
    }

    // ── UserDetailsService ─────────────────────────────────────────────
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPasswordHash() != null ? user.getPasswordHash() : "",
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }

    // ── Email / Password Login ─────────────────────────────────────────
    // ── Register new citizen account ───────────────────────────────────
    public AuthResponse register(String name, String email, String password) {
        if (userRepo.findByEmail(email).isPresent()) {
            throw new RuntimeException("An account with this email already exists.");
        }
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(User.Role.CITIZEN);
        user.setProvider("LOCAL");
        userRepo.save(user);

        UserDetails ud = loadUserByUsername(email);
        String token = jwtProvider.generateToken(ud);
        return new AuthResponse(token, user.getName(), user.getEmail(), user.getRole().name());
    }

    public AuthResponse login(LoginRequest req) {
        User user = userRepo.findByEmail(req.email())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (user.getPasswordHash() == null ||
            !passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid email or password");
        }

        UserDetails ud = loadUserByUsername(user.getEmail());
        String token = jwtProvider.generateToken(ud);
        return new AuthResponse(token, user.getName(), user.getEmail(), user.getRole().name());
    }

    // ── Reset Password ─────────────────────────────────────────────────
    public void resetPassword(String email, String newPassword) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("No account found with this email address."));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepo.save(user);
    }

    // ── Social Login (Google) ──────────────────────────────────────────
    public AuthResponse socialLogin(SocialLoginRequest req) {
        Optional<User> opt = userRepo.findByEmail(req.email());
        User user;

        if (opt.isPresent()) {
            user = opt.get();
            if (user.getProvider() == null) {
                user.setProvider(req.provider());
                user.setProviderId(req.providerId());
                userRepo.save(user);
            }
        } else {
            user = new User();
            user.setName(req.name() != null && !req.name().isBlank() ? req.name() : req.email().split("@")[0]);
            user.setEmail(req.email());
            user.setProvider(req.provider());
            user.setProviderId(req.providerId());
            user.setRole(User.Role.CITIZEN);
            user = userRepo.save(user);
        }

        UserDetails ud = loadUserByUsername(user.getEmail());
        String token = jwtProvider.generateToken(ud);
        return new AuthResponse(token, user.getName(), user.getEmail(), user.getRole().name());
    }

    // ── Phone OTP: Send ────────────────────────────────────────────────
    public void sendOtp(String phone) {
        // ── Rate limit check: max 10 OTPs per phone per day ───────────
        if (otpStore.isRateLimited(phone)) {
            int used = otpStore.getDailyCount(phone);
            throw new RuntimeException(
                "OTP limit reached. You have requested " + used + "/" + 10 +
                " OTPs today. Please try again tomorrow."
            );
        }

        // Generate secure 6-digit OTP
        String otp = generateOtp();

        // Save to store (also increments daily counter)
        otpStore.save(phone, otp);

        // Try Fast2SMS first; falls back to console log in dev mode
        boolean smsSent = trySendFast2Sms(phone, otp);

        if (!smsSent) {
            // Dev fallback — OTP visible in console and on screen badge
            System.out.println("════════════════════════════════════════════");
            System.out.println("  📱 OTP for +91" + phone + "  →  " + otp);
            System.out.println("  (Fast2SMS not configured — dev mode)");
            System.out.println("  Daily usage: " + otpStore.getDailyCount(phone) + "/10");
            System.out.println("════════════════════════════════════════════");
        }
    }

    /** Returns the current OTP for a phone (dev peek endpoint only) */
    public String peekOtp(String phone) {
        return otpStore.peek(phone);
    }

    /** Returns remaining OTP requests allowed today for this phone */
    public int getRemainingOtpCount(String phone) {
        return 10 - otpStore.getDailyCount(phone);
    }

    // ── Phone OTP: Verify ──────────────────────────────────────────────
    public AuthResponse verifyOtp(OtpVerifyRequest req) {
        boolean valid = otpStore.validate(req.phone(), req.otp());
        if (!valid) throw new RuntimeException("Invalid or expired OTP. Please request a new one.");

        String email = req.phone() + "@phone.civiccms.in";
        Optional<User> opt = userRepo.findByEmail(email);
        User user;

        if (opt.isPresent()) {
            user = opt.get();
        } else {
            user = new User();
            user.setName("User " + req.phone());
            user.setEmail(email);
            user.setProvider("PHONE");
            user.setProviderId(req.phone());
            user.setRole(User.Role.CITIZEN);
            user = userRepo.save(user);
        }

        UserDetails ud = loadUserByUsername(user.getEmail());
        String token = jwtProvider.generateToken(ud);
        return new AuthResponse(token, user.getName(), user.getEmail(), user.getRole().name());
    }

    // ── Fast2SMS Integration ───────────────────────────────────────────
    /**
     * Sends OTP via Fast2SMS OTP route (route=v3).
     *
     * FIX #2: Changed from route=q  (Quick SMS — needs DLT-registered sender ID
     *         and government-approved template, rejected without DLT registration)
     *         to     route=v3 (OTP route — no DLT registration required, works
     *         immediately after getting an API key from fast2sms.com).
     *
     * FIX #3: Replaced URLEncoder.encode(msg, "UTF-8")  [deprecated String charset,
     *         throws checked UnsupportedEncodingException]
     *         with    URLEncoder.encode(otp, StandardCharsets.UTF_8)  [safe, no throws].
     *
     * Sign up at https://www.fast2sms.com → Dashboard → Dev API → copy API Key.
     * Then add to application.properties:  fast2sms.api.key=YOUR_KEY_HERE
     */
    private boolean trySendFast2Sms(String phone, String otp) {
        if (fast2smsApiKey == null || fast2smsApiKey.isBlank()) {
            return false; // not configured — fall through to console log
        }

        try {
            // FIX #2 — route=v3 is the OTP/transactional route.
            // variables_values carries the OTP value that Fast2SMS injects
            // into their pre-approved OTP template.
            // FIX #3 — StandardCharsets.UTF_8 (no throws, not deprecated).
            String encodedOtp = java.net.URLEncoder.encode(otp, StandardCharsets.UTF_8);

            String url = "https://www.fast2sms.com/dev/bulkV2"
                    + "?authorization=" + fast2smsApiKey
                    + "&route=v3"
                    + "&variables_values=" + encodedOtp + "%7C"   // %7C = URL-encoded pipe "|"
                    + "&flash=0"
                    + "&numbers=" + phone;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("cache-control", "no-cache")
                    .GET()
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("Fast2SMS response: " + response.body());

            // Fast2SMS returns {"return":true,...} on success
            return response.body().contains("\"return\":true");

        } catch (Exception e) {
            System.err.println("Fast2SMS error: " + e.getMessage());
            return false;
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────
    /** Generates a cryptographically secure 6-digit OTP */
    private String generateOtp() {
        return String.format("%06d", new SecureRandom().nextInt(1_000_000));
    }
}
