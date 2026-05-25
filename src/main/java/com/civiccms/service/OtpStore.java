package com.civiccms.service;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory OTP store with:
 *  - 6-digit OTP
 *  - 5-minute TTL
 *  - Max 10 OTP requests per phone per day (rate limit)
 */
@Component
public class OtpStore {

    private static final long TTL_SECONDS   = 300; // 5 minutes
    private static final int  DAILY_LIMIT   = 10;  // max OTPs per phone per day

    // ── OTP entry ──────────────────────────────────────────────────────
    private record OtpEntry(String otp, Instant expiresAt) {}

    // ── Rate limit entry ───────────────────────────────────────────────
    private record RateEntry(AtomicInteger count, LocalDate date) {}

    private final Map<String, OtpEntry>  store     = new ConcurrentHashMap<>();
    private final Map<String, RateEntry> rateLimit = new ConcurrentHashMap<>();

    /**
     * Check if phone has exceeded daily OTP limit.
     * @return true if limit exceeded
     */
    public boolean isRateLimited(String phone) {
        RateEntry entry = rateLimit.get(phone);
        if (entry == null) return false;
        // Reset count if it's a new day
        if (!entry.date().equals(LocalDate.now())) {
            rateLimit.remove(phone);
            return false;
        }
        return entry.count().get() >= DAILY_LIMIT;
    }

    /**
     * How many OTPs has this phone used today.
     */
    public int getDailyCount(String phone) {
        RateEntry entry = rateLimit.get(phone);
        if (entry == null || !entry.date().equals(LocalDate.now())) return 0;
        return entry.count().get();
    }

    /** Save OTP for a phone number and increment daily counter */
    public void save(String phone, String otp) {
        // Store the OTP
        store.put(phone, new OtpEntry(otp, Instant.now().plusSeconds(TTL_SECONDS)));

        // Increment rate limit counter
        rateLimit.compute(phone, (k, existing) -> {
            if (existing == null || !existing.date().equals(LocalDate.now())) {
                return new RateEntry(new AtomicInteger(1), LocalDate.now());
            }
            existing.count().incrementAndGet();
            return existing;
        });
    }

    /** Validate OTP — returns true if correct and not expired, then removes it */
    public boolean validate(String phone, String otp) {
        OtpEntry entry = store.get(phone);
        if (entry == null) return false;
        if (Instant.now().isAfter(entry.expiresAt())) {
            store.remove(phone);
            return false;
        }
        if (!entry.otp().equals(otp)) return false;
        store.remove(phone); // consume after successful verify
        return true;
    }

    /** Peek the stored OTP (for demo/dev mode only — remove in production) */
    public String peek(String phone) {
        OtpEntry entry = store.get(phone);
        return entry != null ? entry.otp() : null;
    }
}
