package za.co.int216d.carwash.booking.membership.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import za.co.int216d.carwash.booking.membership.domain.Membership;
import za.co.int216d.carwash.booking.membership.repository.MembershipRepository;
import za.co.int216d.carwash.booking.notification.producer.MembershipEventProducer;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(value = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class MembershipRenewalScheduler {

    private final MembershipRepository membershipRepository;
    private final MembershipEventProducer eventProducer;

    @Scheduled(cron = "${app.scheduling.renewal-cron:0 0 3 * * ?}")
    public void processExpiredAutoRenewals() {
        log.info("Running auto-renewal check for expired memberships");
        List<Membership> expiringMemberships = membershipRepository.findExpiredAutoRenewMemberships();

        for (Membership membership : expiringMemberships) {
            log.info("Auto-renew triggered for client {} (membership {})",
                membership.getClientId(), membership.getId());

            membership.setExpiryDate(LocalDateTime.now().plusMonths(1));
            membership.setCreditsRemaining(membership.getPlan().getCreditsPerMonth());
            membership.setWashesUsedThisMonth(0);
            membership.setStatus(Membership.MembershipStatus.ACTIVE);
            membershipRepository.save(membership);

            eventProducer.publishRenewalEvent(
                membership.getClientId(),
                membership.getPlan().getId(),
                membership.getPlan().getName(),
                null,
                null
            );
        }

        if (!expiringMemberships.isEmpty()) {
            log.info("Auto-renewed {} memberships", expiringMemberships.size());
        }
    }

    @Scheduled(cron = "${app.scheduling.expiry-check-cron:0 30 0 * * ?}")
    public void checkAndExpireMemberships() {
        log.info("Running membership expiry check");
        List<Membership> activeMemberships = membershipRepository.findAllByStatus(Membership.MembershipStatus.ACTIVE);

        int expiredCount = 0;
        for (Membership membership : activeMemberships) {
            if (membership.getExpiryDate().isBefore(LocalDateTime.now())
                && !Boolean.TRUE.equals(membership.getAutoRenew())) {
                membership.setStatus(Membership.MembershipStatus.EXPIRED);
                membershipRepository.save(membership);
                expiredCount++;

                eventProducer.publishCancellationEvent(
                    membership.getClientId(),
                    membership.getPlan().getId(),
                    membership.getPlan().getName(),
                    null,
                    null
                );
            }
        }

        if (expiredCount > 0) {
            log.info("Expired {} memberships", expiredCount);
        }
    }
}
