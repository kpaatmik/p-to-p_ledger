package com.paymentledger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * P2P Payment Ledger - main application entry point.
 *
 * <p>This is a simulated educational wallet application. It is not intended
 * for production use as a regulated payment service.
 *
 * <p>{@code @EnableScheduling} activates the five background jobs defined in
 * the {@code scheduler} package (settlement processor, daily-limit reset,
 * account auto-unlock, daily summary/reconciliation, reservation expiry).
 * All scheduled jobs use zone = ""Asia/Kolkata"" (IST, UTC+5:30, no DST).
 */
@SpringBootApplication
@EnableScheduling
public class PaymentLedgerApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentLedgerApplication.class, args);
    }
}
