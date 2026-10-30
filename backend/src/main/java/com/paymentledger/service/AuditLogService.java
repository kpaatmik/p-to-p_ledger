
package com.paymentledger.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paymentledger.entity.AuditLog;
import com.paymentledger.entity.User;
import com.paymentledger.repository.AuditLogRepository;
import com.paymentledger.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditLogService(
            AuditLogRepository auditLogRepository,
            UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void recordAdminAction(
            UUID adminId,
            String action,
            String targetType,
            UUID targetId,
            String details) {

        User admin = userRepository.findById(adminId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Admin user not found: " + adminId));

        AuditLog auditLog = new AuditLog(
                admin,
                action,
                targetType,
                targetId,
                details
        );

        auditLogRepository.save(auditLog);
    }
}
