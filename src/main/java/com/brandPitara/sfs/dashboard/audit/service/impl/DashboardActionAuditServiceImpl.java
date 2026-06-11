package com.brandPitara.sfs.dashboard.audit.service.impl;

import com.brandPitara.sfs.dashboard.audit.dto.DashboardActionAuditEntryDto;
import com.brandPitara.sfs.dashboard.audit.entity.DashboardActionAuditEntity;
import com.brandPitara.sfs.dashboard.audit.repository.DashboardActionAuditRepository;
import com.brandPitara.sfs.dashboard.audit.service.DashboardActionAuditService;
import com.brandPitara.sfs.dashboard.auth.service.DashboardCurrentUserService;
import com.brandPitara.sfs.dashboard.common.enums.DashboardAuditAction;
import com.brandPitara.sfs.dashboard.common.enums.DashboardRole;
import com.brandPitara.sfs.dashboard.common.enums.ReviewEntityType;
import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;
import com.brandPitara.sfs.observability.LogEvents;
import com.brandPitara.sfs.observability.LoggingConstants;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardActionAuditServiceImpl implements DashboardActionAuditService {

    private static final Logger AUDIT_LOG = LoggerFactory.getLogger(LoggingConstants.LOGGER_AUDIT);

    private final DashboardActionAuditRepository auditRepository;
    private final DashboardCurrentUserService currentUserService;
    private final PlatformTransactionManager transactionManager;

    @Override
    public void record(DashboardAuditAction action, ReviewEntityType entityType, Long entityId, Long projectId) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        try {
            tx.executeWithoutResult(status -> {
                DashboardUserEntity user = tryGetCurrentUser();

                auditRepository.save(DashboardActionAuditEntity.builder()
                        .dashboardUserId(user != null ? user.getId() : null)
                        .dashboardUserName(user != null ? user.getName() : null)
                        .dashboardUserRole(user != null ? user.getRole() : null)
                        .action(action)
                        .entityType(entityType)
                        .entityId(entityId)
                        .projectId(projectId)
                        .summary(buildSummary(action, entityType, entityId, projectId, user))
                        .ipAddress(resolveIp())
                        .userAgent(resolveUserAgent())
                        .build());

                // Mirror to sfs-audit.log — fires only after DB save succeeds
                writeAuditLog(action, entityType, entityId, projectId, user);
            });
        } catch (Exception e) {
            log.warn("Failed to record dashboard action audit: action={}, entityType={}, entityId={}",
                    action, entityType, entityId, e);
        }
    }

    private void writeAuditLog(
            DashboardAuditAction action,
            ReviewEntityType entityType,
            Long entityId,
            Long projectId,
            DashboardUserEntity user
    ) {
        try {
            Map<String, Object> fields = new LinkedHashMap<>();
            fields.put("event",      LogEvents.AUDIT_ACTION);
            fields.put("action",     action.name());
            fields.put("entityType", entityType.name());
            fields.put("entityId",   entityId);
            if (projectId != null) fields.put("projectId", projectId);
            fields.put("actorId",    user != null ? user.getId()   : "unknown");
            fields.put("actorName",  user != null ? user.getName() : "unknown");
            fields.put("actorRole",  user != null ? user.getRole().name() : "unknown");
            AUDIT_LOG.info("{}", StructuredArguments.entries(fields));
        } catch (Exception ignored) {
            // Audit file logging must never break the business flow
        }
    }

    // ── Existing helpers (unchanged) ──────────────────────────────────────────

    private DashboardUserEntity tryGetCurrentUser() {
        try {
            return currentUserService.getCurrentUserOrThrow();
        } catch (Exception e) {
            return null;
        }
    }

    private String buildSummary(
            DashboardAuditAction action,
            ReviewEntityType entityType,
            Long entityId,
            Long projectId,
            DashboardUserEntity user
    ) {
        String actor = user != null ? user.getName() + " (" + roleLabel(user.getRole()) + ")" : "Unknown user";
        return actor + " " + actionPhrase(action) + targetPhrase(entityType, entityId, projectId) + ".";
    }

    private String targetPhrase(ReviewEntityType entityType, Long entityId, Long projectId) {
        if (projectId != null && !projectId.equals(entityId)) {
            return " for project #" + projectId + " (" + entityLabel(entityType) + " #" + entityId + ")";
        }
        if (projectId != null) {
            return " for project #" + projectId;
        }
        return " on " + entityLabel(entityType) + " #" + entityId;
    }

    private String roleLabel(DashboardRole role) {
        if (role == null) return "User";
        return switch (role) {
            case ADMIN -> "Admin";
            case REVIEWER -> "Reviewer";
            case DATA_ENTRY -> "Data Entry";
        };
    }

    private String actionPhrase(DashboardAuditAction action) {
        return switch (action) {
            case PROJECT_CREATED -> "created the project";
            case PROJECT_UPDATED -> "updated project details";
            case PROJECT_DELETED -> "deleted the project";
            case PROJECT_BUILDER_REASSIGNED -> "reassigned the project builder";
            case PROJECT_PUBLISHED -> "published the project";
            case PROJECT_UNPUBLISHED -> "unpublished the project";
            case PROJECT_ACTIVATED -> "activated the project";
            case PROJECT_DEACTIVATED -> "deactivated the project";
            case PROJECT_SUBMITTED_FOR_REVIEW -> "submitted the project for review";
            case PROJECT_APPROVED -> "approved and published the project";
            case PROJECT_APPROVAL_ROLLED_BACK -> "rolled back project approval";
            case PROJECT_REJECTED -> "rejected the project";
            case PROJECT_REOPENED -> "reopened the project";
            case MEDIA_ADDED -> "added project media";
            case MEDIA_UPDATED -> "updated project media";
            case MEDIA_DELETED -> "deleted project media";
            case FLOOR_PLAN_CREATED -> "added a floor plan";
            case FLOOR_PLAN_UPDATED -> "updated a floor plan";
            case FLOOR_PLAN_DELETED -> "deleted a floor plan";
            case FLOOR_PLAN_ACTIVATED -> "changed floor plan visibility";
            case HIGHLIGHT_CREATED -> "added a project highlight";
            case HIGHLIGHT_UPDATED -> "updated a project highlight";
            case HIGHLIGHT_DELETED -> "deleted a project highlight";
            case CONNECTIVITY_UPSERTED -> "updated connectivity overview";
            case CONNECTIVITY_PLACE_ADDED -> "added a connectivity place";
            case CONNECTIVITY_PLACE_UPDATED -> "updated a connectivity place";
            case CONNECTIVITY_PLACE_DELETED -> "deleted a connectivity place";
            case CONSTRUCTION_STAGE_CREATED -> "added a construction stage";
            case CONSTRUCTION_STAGE_UPDATED -> "updated a construction stage";
            case CONSTRUCTION_STAGE_DELETED -> "deleted a construction stage";
            case COMPLIANCE_ITEM_CREATED -> "added a compliance item";
            case COMPLIANCE_ITEM_UPDATED -> "updated a compliance item";
            case COMPLIANCE_ITEM_DELETED -> "deleted a compliance item";
            case AMENITY_CREATED -> "added an amenity";
            case AMENITY_UPDATED -> "updated an amenity";
            case AMENITY_DELETED -> "deleted an amenity";
            case PRICE_HISTORY_CREATED -> "added a price history point";
            case PRICE_HISTORY_UPDATED -> "updated a price history point";
            case PRICE_HISTORY_DELETED -> "deleted a price history point";
            case PAYMENT_MILESTONE_CREATED -> "added a payment milestone";
            case PAYMENT_MILESTONE_UPDATED -> "updated a payment milestone";
            case PAYMENT_MILESTONE_DELETED -> "deleted a payment milestone";
            case COST_BREAKDOWN_UPSERTED -> "updated cost breakdown";
            case LAND_UTILIZATION_UPSERTED -> "updated land utilization";
            case LOCATION_SCORE_UPSERTED -> "updated location score";
            case SNAPSHOT_RECALCULATED -> "recalculated the project meter snapshot";
            case SNAPSHOT_VERIFIED -> "verified the project meter snapshot";
            case SNAPSHOT_UNVERIFIED -> "marked the project meter snapshot unverified";
            case PRICE_INSIGHTS_UPDATED -> "updated price insights";
            case BUILDER_CREATED -> "created a builder";
            case BUILDER_UPDATED -> "updated a builder";
            case BUILDER_DELETED -> "deleted a builder";
            case BUILDER_PUBLISHED -> "published a builder";
            case BUILDER_LOGO_UPDATED -> "updated builder logo";
            case CITY_CREATED -> "created a city";
            case CITY_UPDATED -> "updated a city";
            case CITY_DELETED -> "deleted a city";
            case CATEGORY_CREATED -> "created a category";
            case CATEGORY_UPDATED -> "updated a category";
            case CATEGORY_DELETED -> "deleted a category";
            default -> humanize(action.name()).toLowerCase();
        };
    }

    private String entityLabel(ReviewEntityType entityType) {
        return entityType == null ? "item" : humanize(entityType.name()).toLowerCase();
    }

    private String humanize(String value) {
        String lower = value == null ? "" : value.toLowerCase().replace('_', ' ');
        if (lower.isBlank()) return "item";
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private String resolveIp() {
        try {
            HttpServletRequest req = currentRequest();
            if (req == null) return null;
            String xff = req.getHeader("X-Forwarded-For");
            if (StringUtils.hasText(xff)) return xff.split(",")[0].trim();
            String xri = req.getHeader("X-Real-IP");
            if (StringUtils.hasText(xri)) return xri.trim();
            return req.getRemoteAddr();
        } catch (Exception e) {
            return null;
        }
    }

    private String resolveUserAgent() {
        try {
            HttpServletRequest req = currentRequest();
            return req != null ? req.getHeader("User-Agent") : null;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DashboardActionAuditEntryDto> listByProject(Long projectId, Pageable pageable) {
        return auditRepository.findByProjectIdOrderByCreatedAtDesc(projectId, pageable)
                .map(DashboardActionAuditEntryDto::from);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DashboardActionAuditEntryDto> listByUser(Long dashboardUserId, Pageable pageable) {
        return auditRepository.findByDashboardUserIdOrderByCreatedAtDesc(dashboardUserId, pageable)
                .map(DashboardActionAuditEntryDto::from);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DashboardActionAuditEntryDto> listAll(
            OffsetDateTime fromDate,
            OffsetDateTime toDate,
            DashboardAuditAction action,
            ReviewEntityType entityType,
            DashboardRole userRole,
            Pageable pageable
    ) {
        Specification<DashboardActionAuditEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (fromDate   != null) predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate));
            if (toDate     != null) predicates.add(cb.lessThan(root.get("createdAt"), toDate));
            if (action     != null) predicates.add(cb.equal(root.get("action"), action));
            if (entityType != null) predicates.add(cb.equal(root.get("entityType"), entityType));
            if (userRole   != null) predicates.add(cb.equal(root.get("dashboardUserRole"), userRole));
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };

        Pageable effective = pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                        Sort.by(Sort.Direction.DESC, "createdAt"));
        return auditRepository.findAll(spec, effective).map(DashboardActionAuditEntryDto::from);
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }
}
