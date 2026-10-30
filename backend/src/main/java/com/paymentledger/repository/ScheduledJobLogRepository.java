package com.paymentledger.repository;

import com.paymentledger.entity.ScheduledJobLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ScheduledJobLogRepository extends JpaRepository<ScheduledJobLog, UUID> {

    /**
     * Returns the most recent job log records across all job names,
     * ordered by start time descending.
     * Pass {@code Pageable.ofSize(20)} to get the last 20 runs.
     */
    List<ScheduledJobLog> findAllByOrderByStartedAtDesc(Pageable pageable);

    List<ScheduledJobLog> findByJobNameOrderByStartedAtDesc(String jobName, Pageable pageable);
}
