package com.brandPitara.sfs.projectcompare.builder;

import com.brandPitara.sfs.buildercredibility.dto.BuilderCredibilitySummaryResponse;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.entity.ProjectFloorPlanEntity;
import com.brandPitara.sfs.project.entity.ProjectMasterPlanEntity;
import com.brandPitara.sfs.project.enums.MasterPlanAreaUnit;
import com.brandPitara.sfs.projectcompare.dto.response.ComparisonInsightBlockResponse;
import com.brandPitara.sfs.projectcompare.dto.response.ComparisonInsightMetricResponse;
import com.brandPitara.sfs.projectcompare.dto.response.ComparisonOverviewInsightResponse;
import com.brandPitara.sfs.projectcompare.dto.response.ComparisonVerdictResponse;
import com.brandPitara.sfs.projectmeter.entity.ProjectLocationScoreEntity;
import com.brandPitara.sfs.projectmeter.entity.ProjectMeterSnapshotEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ProjectComparisonOverviewInsightBuilder {

    private static final long CRORE = 10_000_000L;
    private static final long LAKH = 100_000L;
    private static final DateTimeFormatter LONG_MONTH_YEAR = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);

    public ComparisonOverviewInsightResponse build(
            List<ProjectEntity> projects,
            Map<Long, ProjectMeterSnapshotEntity> snapshots,
            Map<Long, ProjectLocationScoreEntity> locationScores,
            Map<Long, List<ProjectFloorPlanEntity>> floorPlans,
            Map<Long, ProjectMasterPlanEntity> masterPlans,
            Map<Long, BuilderCredibilitySummaryResponse> credibilityByBuilderId
    ) {
        if (projects == null || projects.size() < 2) return null;

        Tracker tracker = new Tracker(projects);
        List<ComparisonInsightBlockResponse> blocks = new ArrayList<>();

        addIfPresent(blocks, buildPricing(projects, tracker));
        addIfPresent(blocks, buildPossession(projects, snapshots, tracker));
        addIfPresent(blocks, buildLifestyle(projects, snapshots, locationScores, floorPlans, masterPlans, tracker));
        addIfPresent(blocks, buildConfidence(projects, snapshots, credibilityByBuilderId, tracker));

        if (blocks.isEmpty()) return null;

        return ComparisonOverviewInsightResponse.builder()
                .title(buildTitle(projects))
                .blocks(blocks)
                .verdict(buildVerdict(projects, tracker))
                .build();
    }

    private ComparisonInsightBlockResponse buildPricing(List<ProjectEntity> projects, Tracker tracker) {
        Map<Long, Double> startingPrices = numeric(projects, p -> toDouble(p.getPriceMin()));
        Map<Long, Double> pricesPerSqft = numeric(projects, p -> toDouble(p.getAveragePricePerSqft()));
        if (startingPrices.isEmpty() && pricesPerSqft.isEmpty()) return null;

        Long entryWinner = best(startingPrices, false);
        Long sqftWinner = best(pricesPerSqft, false);
        Long winner = sqftWinner != null ? sqftWinner : entryWinner;

        if (winner != null) {
            tracker.award(winner, sqftWinner != null ? "a lower price per sq.ft." : "a lower entry price");
        }

        String body;
        if (projects.size() == 2) {
            ProjectEntity a = projects.get(0);
            ProjectEntity b = projects.get(1);
            List<String> sentences = new ArrayList<>();
            if (a.getPriceMin() != null && b.getPriceMin() != null) {
                if (a.getPriceMin().equals(b.getPriceMin())) {
                    sentences.add("Both projects have the same available starting price of " + formatPrice(a.getPriceMin()) + ".");
                } else {
                    ProjectEntity low = a.getPriceMin() < b.getPriceMin() ? a : b;
                    ProjectEntity other = low == a ? b : a;
                    sentences.add("For entry pricing, " + low.getName() + " is more accessible at "
                            + formatPrice(low.getPriceMin()) + ", compared with " + other.getName() + " at "
                            + formatPrice(other.getPriceMin()) + ".");
                }
            } else if (startingPrices.size() == 1) {
                ProjectEntity available = project(projects, startingPrices.keySet().iterator().next());
                sentences.add("The available starting price for " + available.getName() + " is "
                        + formatPrice(available.getPriceMin()) + "; the other project's entry price is not available.");
            }
            if (a.getAveragePricePerSqft() != null && b.getAveragePricePerSqft() != null) {
                if (a.getAveragePricePerSqft().equals(b.getAveragePricePerSqft())) {
                    sentences.add("Both projects have the same available price per sq.ft. at "
                            + formatPricePerSqft(a.getAveragePricePerSqft()) + ".");
                } else {
                    ProjectEntity low = a.getAveragePricePerSqft() < b.getAveragePricePerSqft() ? a : b;
                    ProjectEntity other = low == a ? b : a;
                    sentences.add(low.getName() + " also offers the lower quoted price per sq.ft. at "
                            + formatPricePerSqft(low.getAveragePricePerSqft()) + " versus "
                            + formatPricePerSqft(other.getAveragePricePerSqft()) + " for " + other.getName() + ".");
                }
            } else if (pricesPerSqft.size() == 1) {
                ProjectEntity available = project(projects, pricesPerSqft.keySet().iterator().next());
                sentences.add("The available rate for " + available.getName() + " is "
                        + formatPricePerSqft(available.getAveragePricePerSqft())
                        + "; a direct per-sq.ft. comparison is not yet possible.");
            }
            body = sentences.isEmpty()
                    ? "Pricing data is currently available for only part of this comparison, so no definitive value winner is declared."
                    : String.join(" ", sentences);
        } else {
            List<String> sentences = new ArrayList<>();
            if (sqftWinner != null) {
                ProjectEntity p = project(projects, sqftWinner);
                sentences.add("Among the compared projects, " + p.getName()
                        + " currently has the lowest quoted price per sq.ft. at "
                        + formatPricePerSqft(p.getAveragePricePerSqft()) + ".");
            }
            if (entryWinner != null) {
                ProjectEntity p = project(projects, entryWinner);
                sentences.add(p.getName() + " has the lowest entry price at " + formatPrice(p.getPriceMin()) + ".");
            }
            body = sentences.isEmpty()
                    ? "Available pricing is incomplete across the compared projects, so the backend does not declare a value leader."
                    : String.join(" ", sentences);
        }

        return block("PRICING_VALUE", "money_bag", "Pricing & Value", body, winner, projects,
                metrics(
                        metric("Starting Price", projects, p -> formatPriceOrNull(p.getPriceMin())),
                        metric("Average Price / sq.ft.", projects, p -> formatPricePerSqftOrNull(p.getAveragePricePerSqft()))
                ));
    }

    private ComparisonInsightBlockResponse buildPossession(
            List<ProjectEntity> projects,
            Map<Long, ProjectMeterSnapshotEntity> snapshots,
            Tracker tracker
    ) {
        Map<Long, LocalDate> dates = new LinkedHashMap<>();
        for (ProjectEntity p : projects) {
            LocalDate date = effectivePossessionDate(p, snapshots.get(p.getId()));
            if (date != null) dates.put(p.getId(), date);
        }
        if (dates.isEmpty()) return null;

        Long winner = earliest(dates);
        if (winner != null) tracker.award(winner, "an earlier expected possession timeline");

        String body;
        if (projects.size() == 2 && dates.size() == 2) {
            ProjectEntity a = projects.get(0);
            ProjectEntity b = projects.get(1);
            LocalDate aDate = dates.get(a.getId());
            LocalDate bDate = dates.get(b.getId());
            if (aDate.equals(bDate)) {
                body = "Both projects have a similar expected possession timeline around " + formatDate(aDate) + ".";
            } else {
                ProjectEntity early = aDate.isBefore(bDate) ? a : b;
                ProjectEntity late = early == a ? b : a;
                body = "If moving in sooner is the priority, " + early.getName() + " leads with expected readiness in "
                        + formatDate(dates.get(early.getId())) + ", whereas " + late.getName()
                        + " is expected around " + formatDate(dates.get(late.getId())) + ".";
            }
        } else if (winner != null) {
            ProjectEntity p = project(projects, winner);
            body = "Among the compared projects, " + p.getName() + " has the earliest available possession timeline at "
                    + formatDate(dates.get(winner)) + ".";
        } else {
            body = "Available possession dates are similar or incomplete, so no single timeline winner is declared.";
        }

        return block("POSSESSION_TIMELINE", "calendar", "Possession Timeline", body, winner, projects,
                List.of(metricFromDates("Expected Possession", projects, dates)));
    }

    private ComparisonInsightBlockResponse buildLifestyle(
            List<ProjectEntity> projects,
            Map<Long, ProjectMeterSnapshotEntity> snapshots,
            Map<Long, ProjectLocationScoreEntity> locationScores,
            Map<Long, List<ProjectFloorPlanEntity>> floorPlans,
            Map<Long, ProjectMasterPlanEntity> masterPlans,
            Tracker tracker
    ) {
        Map<Long, Double> openSpace = numeric(projects, p -> masterValue(masterPlans.get(p.getId()), ProjectMasterPlanEntity::getOpenSpacePercent));
        Map<Long, Double> density = numeric(projects, p -> density(masterPlans.get(p.getId())));
        Map<Long, Double> efficiency = numeric(projects, p -> averageEfficiency(floorPlans.getOrDefault(p.getId(), List.of())));
        Map<Long, Double> amenity = numeric(projects, p -> snapshotValue(snapshots.get(p.getId()), ProjectMeterSnapshotEntity::getAmenityScore));
        Map<Long, Double> location = numeric(projects, p -> locationValue(locationScores.get(p.getId())));

        if (openSpace.isEmpty() && density.isEmpty() && efficiency.isEmpty() && amenity.isEmpty() && location.isEmpty()) {
            return null;
        }

        Map<Long, Integer> lifestyleVotes = new LinkedHashMap<>();
        Long openWinner = best(openSpace, true);
        Long densityWinner = best(density, false);
        Long amenityWinner = best(amenity, true);
        Long locationWinner = best(location, true);
        Long efficiencyWinner = best(efficiency, true);
        vote(lifestyleVotes, openWinner);
        vote(lifestyleVotes, densityWinner);
        vote(lifestyleVotes, amenityWinner);
        vote(lifestyleVotes, locationWinner);
        Long lifestyleWinner = uniqueTop(lifestyleVotes);

        if (lifestyleWinner != null) tracker.award(lifestyleWinner, "stronger lifestyle and community metrics");
        if (efficiencyWinner != null) tracker.award(efficiencyWinner, "better carpet-area efficiency");

        List<String> sentences = new ArrayList<>();
        appendMetricSentence(sentences, projects, openSpace, openWinner, true, "open space", "%");
        appendMetricSentence(sentences, projects, density, densityWinner, false, "community density", " units per acre");
        appendMetricSentence(sentences, projects, efficiency, efficiencyWinner, true, "carpet efficiency", "%");
        appendMetricSentence(sentences, projects, amenity, amenityWinner, true, "amenity completion", "%");
        appendMetricSentence(sentences, projects, location, locationWinner, true, "location score", "/10");

        String body = projects.size() > 2
                ? "Across the compared projects, " + String.join(" ", sentences)
                : String.join(" ", sentences);

        Long blockWinner = lifestyleWinner != null ? lifestyleWinner : efficiencyWinner;
        return block("LIFESTYLE_LIVING", "leaf", "Lifestyle & Living Experience", body, blockWinner, projects,
                metrics(
                        metricFromDoubles("Open Space", projects, openSpace, this::formatPercent),
                        metricFromDoubles("Units / Acre", projects, density, this::formatDecimal),
                        metricFromDoubles("Carpet Efficiency", projects, efficiency, this::formatPercent),
                        metricFromDoubles("Amenity Completion", projects, amenity, this::formatPercent),
                        metricFromDoubles("Location Score", projects, location, value -> formatDecimal(value) + "/10")
                ));
    }

    private ComparisonInsightBlockResponse buildConfidence(
            List<ProjectEntity> projects,
            Map<Long, ProjectMeterSnapshotEntity> snapshots,
            Map<Long, BuilderCredibilitySummaryResponse> credibilityByBuilderId,
            Tracker tracker
    ) {
        Map<Long, Double> progress = numeric(projects, p -> snapshotValue(snapshots.get(p.getId()), ProjectMeterSnapshotEntity::getConstructionProgressPercent));
        Map<Long, Double> delay = numeric(projects, p -> snapshotValue(snapshots.get(p.getId()), ProjectMeterSnapshotEntity::getDelayDays));
        Map<Long, Double> credibility = numeric(projects, p -> {
            if (p.getBuilder() == null) return null;
            BuilderCredibilitySummaryResponse value = credibilityByBuilderId.get(p.getBuilder().getId());
            return value != null ? toDouble(value.getCredibilityScore()) : null;
        });
        if (progress.isEmpty() && delay.isEmpty() && credibility.isEmpty()) return null;

        Map<Long, Integer> votes = new LinkedHashMap<>();
        Long progressWinner = best(progress, true);
        Long delayWinner = best(delay, false);
        Long credibilityWinner = best(credibility, true);
        vote(votes, progressWinner);
        vote(votes, delayWinner);
        vote(votes, credibilityWinner);
        Long winner = uniqueTop(votes);
        if (winner != null) tracker.award(winner, "stronger construction and builder confidence");

        List<String> sentences = new ArrayList<>();
        appendMetricSentence(sentences, projects, progress, progressWinner, true, "construction progress", "%");
        appendMetricSentence(sentences, projects, delay, delayWinner, false, "reported delay", " days");
        appendMetricSentence(sentences, projects, credibility, credibilityWinner, true, "builder credibility", "/100");

        return block("CONSTRUCTION_CONFIDENCE", "construction", "Construction & Builder Confidence",
                String.join(" ", sentences), winner, projects,
                metrics(
                        metricFromDoubles("Construction Progress", projects, progress, this::formatPercent),
                        metricFromDoubles("Reported Delay", projects, delay, value -> formatDecimal(value) + " days"),
                        metricFromDoubles("Builder Credibility", projects, credibility, value -> formatDecimal(value) + "/100")
                ));
    }

    private ComparisonVerdictResponse buildVerdict(List<ProjectEntity> projects, Tracker tracker) {
        int max = tracker.scores.values().stream().max(Integer::compareTo).orElse(0);
        List<Long> leaders = tracker.scores.entrySet().stream()
                .filter(entry -> entry.getValue() == max)
                .map(Map.Entry::getKey)
                .toList();

        if (max > 0 && leaders.size() == 1) {
            Long winnerId = leaders.get(0);
            ProjectEntity winner = project(projects, winnerId);
            List<String> winnerReasons = tracker.strengths.getOrDefault(winnerId, List.of());
            String body = winner.getName() + " leads the available comparison categories, primarily through "
                    + naturalList(winnerReasons.stream().limit(2).toList()) + ".";

            Optional<ProjectEntity> counter = projects.stream()
                    .filter(p -> !p.getId().equals(winnerId))
                    .filter(p -> !tracker.strengths.getOrDefault(p.getId(), List.of()).isEmpty())
                    .findFirst();
            if (counter.isPresent()) {
                ProjectEntity other = counter.get();
                body += " " + other.getName() + " still stands out for "
                        + naturalList(tracker.strengths.get(other.getId()).stream().limit(2).toList()) + ".";
            }

            return ComparisonVerdictResponse.builder()
                    .winnerProjectId(winnerId)
                    .winnerProjectName(winner.getName())
                    .heading("Final Verdict")
                    .body(body)
                    .tone("POSITIVE")
                    .build();
        }

        String body;
        if (projects.size() == 2) {
            ProjectEntity a = projects.get(0);
            ProjectEntity b = projects.get(1);
            body = "Both projects serve different buyer priorities. " + a.getName() + " is stronger for "
                    + strengthsOrFallback(tracker, a.getId()) + ", while " + b.getName() + " performs better for "
                    + strengthsOrFallback(tracker, b.getId()) + ".";
        } else {
            List<String> summaries = projects.stream()
                    .filter(p -> !tracker.strengths.getOrDefault(p.getId(), List.of()).isEmpty())
                    .map(p -> p.getName() + " stands out for " + naturalList(tracker.strengths.get(p.getId())))
                    .toList();
            body = summaries.isEmpty()
                    ? "The available categories do not produce a single overall winner across these projects."
                    : "No single project dominates every category. " + String.join(" ", summaries) + ".";
        }

        return ComparisonVerdictResponse.builder()
                .heading("Final Verdict")
                .body(body)
                .tone("NEUTRAL")
                .build();
    }

    private ComparisonInsightBlockResponse block(
            String key, String icon, String heading, String body, Long winnerId,
            List<ProjectEntity> projects, List<ComparisonInsightMetricResponse> metrics
    ) {
        ProjectEntity winner = winnerId != null ? project(projects, winnerId) : null;
        return ComparisonInsightBlockResponse.builder()
                .key(key)
                .icon(icon)
                .heading(heading)
                .body(body)
                .winnerProjectId(winnerId)
                .winnerProjectName(winner != null ? winner.getName() : null)
                .metrics(metrics)
                .build();
    }

    private void appendMetricSentence(
            List<String> sentences, List<ProjectEntity> projects, Map<Long, Double> values,
            Long winnerId, boolean higherIsBetter, String label, String suffix
    ) {
        if (values.size() == 1) {
            Map.Entry<Long, Double> available = values.entrySet().iterator().next();
            ProjectEntity project = project(projects, available.getKey());
            sentences.add("Available " + label + " data for " + project.getName() + " is "
                    + formatDecimal(available.getValue()) + suffix + "; comparable data is not available for the other project(s).");
            return;
        }
        if (winnerId == null || values.size() < 2) return;
        ProjectEntity winner = project(projects, winnerId);
        if (projects.size() == 2) {
            ProjectEntity other = projects.get(0).getId().equals(winnerId) ? projects.get(1) : projects.get(0);
            Double otherValue = values.get(other.getId());
            if (otherValue == null) return;
            String qualifier = higherIsBetter ? "higher" : "lower";
            sentences.add(winner.getName() + " has the " + qualifier + " " + label + " at "
                    + formatDecimal(values.get(winnerId)) + suffix + " versus "
                    + formatDecimal(otherValue) + suffix + " for " + other.getName() + ".");
        } else {
            String qualifier = higherIsBetter ? "highest" : "lowest";
            sentences.add(winner.getName() + " has the " + qualifier + " " + label + " at "
                    + formatDecimal(values.get(winnerId)) + suffix + ".");
        }
    }

    private ComparisonInsightMetricResponse metric(
            String label, List<ProjectEntity> projects, Function<ProjectEntity, String> formatter
    ) {
        Map<String, String> values = new LinkedHashMap<>();
        boolean any = false;
        for (ProjectEntity p : projects) {
            String value = formatter.apply(p);
            values.put(String.valueOf(p.getId()), value);
            any |= value != null;
        }
        return any ? ComparisonInsightMetricResponse.builder().label(label).projectValues(values).build() : null;
    }

    private ComparisonInsightMetricResponse metricFromDoubles(
            String label, List<ProjectEntity> projects, Map<Long, Double> source, Function<Double, String> formatter
    ) {
        return metric(label, projects, p -> source.containsKey(p.getId()) ? formatter.apply(source.get(p.getId())) : null);
    }

    private ComparisonInsightMetricResponse metricFromDates(
            String label, List<ProjectEntity> projects, Map<Long, LocalDate> source
    ) {
        return metric(label, projects, p -> source.containsKey(p.getId()) ? formatDate(source.get(p.getId())) : null);
    }

    @SafeVarargs
    private final List<ComparisonInsightMetricResponse> metrics(ComparisonInsightMetricResponse... values) {
        return Arrays.stream(values).filter(Objects::nonNull).toList();
    }

    private Map<Long, Double> numeric(List<ProjectEntity> projects, Function<ProjectEntity, Double> supplier) {
        Map<Long, Double> values = new LinkedHashMap<>();
        for (ProjectEntity p : projects) {
            Double value = supplier.apply(p);
            if (value != null && Double.isFinite(value)) values.put(p.getId(), value);
        }
        return values;
    }

    private Long best(Map<Long, Double> values, boolean higherIsBetter) {
        if (values.size() < 2) return null;
        Comparator<Map.Entry<Long, Double>> comparator = Map.Entry.comparingByValue();
        Map.Entry<Long, Double> best = higherIsBetter
                ? values.entrySet().stream().max(comparator).orElse(null)
                : values.entrySet().stream().min(comparator).orElse(null);
        if (best == null) return null;
        long tied = values.values().stream().filter(v -> Math.abs(v - best.getValue()) < 0.0001).count();
        return tied == 1 ? best.getKey() : null;
    }

    private Long earliest(Map<Long, LocalDate> values) {
        if (values.size() < 2) return null;
        LocalDate date = values.values().stream().min(LocalDate::compareTo).orElse(null);
        if (date == null || values.values().stream().filter(date::equals).count() != 1) return null;
        return values.entrySet().stream().filter(e -> e.getValue().equals(date)).map(Map.Entry::getKey).findFirst().orElse(null);
    }

    private Long uniqueTop(Map<Long, Integer> votes) {
        if (votes.isEmpty()) return null;
        int max = votes.values().stream().max(Integer::compareTo).orElse(0);
        List<Long> leaders = votes.entrySet().stream().filter(e -> e.getValue() == max).map(Map.Entry::getKey).toList();
        return leaders.size() == 1 ? leaders.get(0) : null;
    }

    private void vote(Map<Long, Integer> votes, Long projectId) {
        if (projectId != null) votes.merge(projectId, 1, Integer::sum);
    }

    private LocalDate effectivePossessionDate(ProjectEntity project, ProjectMeterSnapshotEntity snapshot) {
        if (snapshot != null) {
            if (snapshot.getLatestReraCompletionDate() != null) return snapshot.getLatestReraCompletionDate();
            if (snapshot.getRevisedCompletionDate() != null) return snapshot.getRevisedCompletionDate();
            if (snapshot.getExpectedCompletionDate() != null) return snapshot.getExpectedCompletionDate();
        }
        return project.getPossessionDate();
    }

    private Double density(ProjectMasterPlanEntity plan) {
        if (plan == null || plan.getTotalUnits() == null || plan.getTotalUnits() <= 0
                || plan.getTotalLandAreaValue() == null || plan.getTotalLandAreaUnit() == null) return null;
        double acres = toAcres(plan.getTotalLandAreaValue().doubleValue(), plan.getTotalLandAreaUnit());
        return acres > 0 ? plan.getTotalUnits() / acres : null;
    }

    private double toAcres(double value, MasterPlanAreaUnit unit) {
        return switch (unit) {
            case ACRE -> value;
            case HECTARE -> value * 2.47105381;
            case SQ_FT -> value / 43_560.0;
            case SQ_MT -> value / 4_046.8564224;
        };
    }

    private Double averageEfficiency(List<ProjectFloorPlanEntity> plans) {
        List<BigDecimal> values = plans.stream()
                .map(ProjectFloorPlanEntity::getCarpetEfficiencyPercent)
                .filter(Objects::nonNull)
                .toList();
        if (values.isEmpty()) return null;
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP).doubleValue();
    }

    private Double masterValue(ProjectMasterPlanEntity plan, Function<ProjectMasterPlanEntity, BigDecimal> getter) {
        BigDecimal value = plan != null ? getter.apply(plan) : null;
        return value != null ? value.doubleValue() : null;
    }

    private Double snapshotValue(ProjectMeterSnapshotEntity snapshot, Function<ProjectMeterSnapshotEntity, Number> getter) {
        Number value = snapshot != null ? getter.apply(snapshot) : null;
        return value != null ? value.doubleValue() : null;
    }

    private Double locationValue(ProjectLocationScoreEntity score) {
        return score != null ? score.getFinalScore() : null;
    }

    private Double toDouble(Number value) {
        return value != null ? value.doubleValue() : null;
    }

    private ProjectEntity project(List<ProjectEntity> projects, Long id) {
        return projects.stream().filter(p -> p.getId().equals(id)).findFirst().orElse(null);
    }

    private String buildTitle(List<ProjectEntity> projects) {
        String names = projects.size() == 2
                ? projects.get(0).getName() + " vs " + projects.get(1).getName()
                : projects.stream().map(ProjectEntity::getName).collect(Collectors.joining(", "));
        return names + " — Clear, Data-Led Comparison";
    }

    private String formatPriceOrNull(Long value) {
        return value != null ? formatPrice(value) : null;
    }

    private String formatPricePerSqftOrNull(Long value) {
        return value != null ? formatPricePerSqft(value) : null;
    }

    private String formatPrice(Long amount) {
        if (amount >= CRORE) return "₹" + formatMoneyDecimal(amount / (double) CRORE) + " Cr";
        if (amount >= LAKH) return "₹" + formatMoneyDecimal(amount / (double) LAKH) + " L";
        return "₹" + String.format(Locale.ENGLISH, "%,d", amount);
    }

    private String formatPricePerSqft(Long amount) {
        return "₹" + String.format(Locale.ENGLISH, "%,d", amount) + "/sq.ft.";
    }

    private String formatPercent(Double value) {
        return formatDecimal(value) + "%";
    }

    private String formatMoneyDecimal(Double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private String formatDecimal(Double value) {
        if (value == null) return "—";
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private String formatDate(LocalDate date) {
        return date.format(LONG_MONTH_YEAR);
    }

    private String naturalList(List<String> values) {
        List<String> clean = values.stream().filter(Objects::nonNull).filter(s -> !s.isBlank()).distinct().toList();
        if (clean.isEmpty()) return "the available data";
        if (clean.size() == 1) return clean.get(0);
        return String.join(", ", clean.subList(0, clean.size() - 1)) + " and " + clean.get(clean.size() - 1);
    }

    private String strengthsOrFallback(Tracker tracker, Long projectId) {
        return naturalList(tracker.strengths.getOrDefault(projectId, List.of()));
    }

    private void addIfPresent(List<ComparisonInsightBlockResponse> blocks, ComparisonInsightBlockResponse block) {
        if (block != null && block.getBody() != null && !block.getBody().isBlank()) blocks.add(block);
    }

    private static final class Tracker {
        private final Map<Long, Integer> scores = new LinkedHashMap<>();
        private final Map<Long, List<String>> strengths = new LinkedHashMap<>();

        private Tracker(List<ProjectEntity> projects) {
            projects.forEach(p -> {
                scores.put(p.getId(), 0);
                strengths.put(p.getId(), new ArrayList<>());
            });
        }

        private void award(Long projectId, String strength) {
            scores.computeIfPresent(projectId, (id, score) -> score + 1);
            strengths.computeIfAbsent(projectId, id -> new ArrayList<>()).add(strength);
        }
    }
}
