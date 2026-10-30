package com.paymentledger.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Calculates business days excluding Saturdays and Sundays (weekends only).
 * Used for transfer settlement date calculations (T-051, T-052).
 * No holiday exclusions are applied — only weekends are excluded.
 */
public class BusinessDayCalculator {

    /**
     * Adds a specified number of business days to a base date.
     * Excludes Saturdays (DayOfWeek.SATURDAY = 6) and Sundays (DayOfWeek.SUNDAY = 7).
     *
     * @param baseDate the starting date
     * @param businessDaysToAdd number of business days to add
     * @return the calculated settlement date
     */
    public static LocalDate addBusinessDays(LocalDate baseDate, int businessDaysToAdd) {
        if (businessDaysToAdd <= 0) {
            return baseDate;
        }

        LocalDate currentDate = baseDate.plusDays(1);  // Start from tomorrow
        int businessDaysAdded = 0;

        while (businessDaysAdded < businessDaysToAdd) {
            int dayOfWeek = currentDate.getDayOfWeek().getValue();
            // ISO 8601: Monday=1, ..., Saturday=6, Sunday=7
            if (dayOfWeek < 6) {  // Monday through Friday
                businessDaysAdded++;
                if (businessDaysAdded == businessDaysToAdd) {
                    return currentDate;
                }
            }
            currentDate = currentDate.plusDays(1);
        }

        return currentDate;
    }

    /**
     * Adds 3 business days to the current date in UTC.
     * Convenience method for P2P transfer settlement date calculation.
     *
     * @return settlement date (today + 3 business days, excluding weekends)
     */
    public static LocalDate getSettlementDateUtc() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        return addBusinessDays(today, 3);
    }

    /**
     * Calculates the number of business days remaining from today until the settlement date.
     * Returns 0 if settlement date is today or in the past.
     *
     * @param settlementDate the target settlement date
     * @return number of business days remaining
     */
    public static long daysRemainingUntilSettlement(LocalDate settlementDate) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        
        if (settlementDate.isBefore(today) || settlementDate.isEqual(today)) {
            return 0;
        }

        long businessDaysRemaining = 0;
        LocalDate current = today.plusDays(1);

        while (!current.isAfter(settlementDate)) {
            int dayOfWeek = current.getDayOfWeek().getValue();
            if (dayOfWeek < 6) {  // Monday through Friday
                businessDaysRemaining++;
            }
            if (current.isEqual(settlementDate)) {
                break;
            }
            current = current.plusDays(1);
        }

        return businessDaysRemaining;
    }
}
