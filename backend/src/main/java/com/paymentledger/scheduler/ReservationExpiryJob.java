package com.paymentledger.scheduler;

import com.paymentledger.entity.CheckPleaseReservation;
import com.paymentledger.repository.CheckPleaseReservationRepository;
import com.paymentledger.service.CheckPleaseService;
import com.paymentledger.service.JobLoggingService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * T-097: Reservation expiry processor job.
 * Runs on a configurable schedule (default hourly).
 * Processes ACTIVE reservations that have expired and releases their reserved balance.
 */
@Component
@RequiredArgsConstructor
public class ReservationExpiryJob {

    private final CheckPleaseReservationRepository reservationRepository;
    private final CheckPleaseService checkPleaseService;
    private final JobLoggingService jobLoggingService;

    @Value("${check.please.reservation.expiry.cron:0 0 * * * *}")
    private String expirationCron;

    private static final String JOB_NAME = "ReservationExpiry";
    private static final int BATCH_SIZE = 100;

    /**
     * Run on configured cron expression (default hourly).
     */
    @Scheduled(cron = "${check.please.reservation.expiry.cron:0 0 * * * *}")
    public void processExpiredReservations() {
        UUID logId = jobLoggingService.startJob(JOB_NAME);

        try {
            Instant now = Instant.now();
            int totalProcessed = 0;
            int pageNumber = 0;

            // Process in bounded batches
            while (true) {
                Pageable pageable = PageRequest.of(pageNumber, BATCH_SIZE);
                var page = reservationRepository.findExpiredActive(now, pageable);

                if (page.isEmpty()) {
                    break;
                }

                // Process each expired reservation
                for (CheckPleaseReservation reservation : page.getContent()) {
                    try {
                        checkPleaseService.expireReservation(reservation);
                        totalProcessed++;
                    } catch (Exception ex) {
                        // Log and continue with next reservation
                        System.err.println("Failed to expire reservation " + reservation.getId() + ": " + ex.getMessage());
                    }
                }

                // Stop if no more pages
                if (!page.hasNext()) {
                    break;
                }

                pageNumber++;
            }

            jobLoggingService.successJob(logId, totalProcessed);

        } catch (Exception ex) {
            jobLoggingService.failJob(logId, "Reservation expiry job failed: " + ex.getMessage());
        }
    }
}
