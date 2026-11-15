package com.goalstack.service;

import com.goalstack.entity.Subgoal;
import com.goalstack.entity.SubgoalStatus;
import com.goalstack.entity.WeightingMode;
import com.goalstack.exception.ValidationFailedException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pure maths, no Spring and no database. */
class WeightedProgressServiceTest {

    private final WeightedProgressService service = new WeightedProgressService();

    // ----- equal weights (older goals) -----

    @Test
    void equalWeightsShareTheHundredPercentAndTheFirstSubgoalsGetTheExtraCent() {
        assertThat(service.equalWeights(3)).containsExactly(bd("33.34"), bd("33.33"), bd("33.33"));
        assertThat(service.equalWeights(4)).containsExactly(bd("25.00"), bd("25.00"), bd("25.00"), bd("25.00"));
        assertThat(service.equalWeights(1)).containsExactly(bd("100.00"));
        assertThat(service.equalWeights(0)).isEmpty();
    }

    @Test
    void equalWeightsAlwaysTotalExactly100() {
        for (int n = 1; n <= 60; n++) {
            assertThat(sum(service.equalWeights(n))).as("n = %d", n).isEqualByComparingTo("100.00");
        }
    }

    // ----- effort weights -----

    @Test
    void documentExampleGivesTheExpectedWeights() {
        List<BigDecimal> weights = service.weightsFromHours(hours("10", "25", "15", "35", "15"));

        assertThat(weights).containsExactly(bd("10.00"), bd("25.00"), bd("15.00"), bd("35.00"), bd("15.00"));
    }

    @Test
    void unevenHoursStillTotalExactly100() {
        List<BigDecimal> weights = service.weightsFromHours(hours("10", "30", "20"));

        // 16.666... 50 33.333... : the one leftover cent goes to the biggest remainder (the first)
        assertThat(weights).containsExactly(bd("16.67"), bd("50.00"), bd("33.33"));
        assertThat(sum(weights)).isEqualByComparingTo("100.00");
    }

    @Test
    void fractionalHoursTotalExactly100() {
        List<BigDecimal> weights = service.weightsFromHours(hours("1.5", "2.25", "3.1", "0.25", "7.75"));

        assertThat(sum(weights)).isEqualByComparingTo("100.00");
    }

    @Test
    void equalHoursGiveTheSameWeightsAsTheEqualRule() {
        for (int n = 1; n <= 50; n++) {
            List<BigDecimal> same = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                same.add(bd("4.00"));
            }
            assertThat(service.weightsFromHours(same)).as("n = %d", n).isEqualTo(service.equalWeights(n));
        }
    }

    @Test
    void theSameInputAlwaysGivesTheSameOutput() {
        List<BigDecimal> input = hours("1", "1", "1", "2", "2", "5");

        assertThat(service.weightsFromHours(input)).isEqualTo(service.weightsFromHours(input));
    }

    @Test
    void zeroNegativeAndMissingHoursAreRejected() {
        assertThatThrownBy(() -> service.weightsFromHours(hours("5", "0"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.weightsFromHours(hours("5", "-1"))).isInstanceOf(IllegalArgumentException.class);
        List<BigDecimal> withNull = new ArrayList<>(hours("5"));
        withNull.add(null);
        assertThatThrownBy(() -> service.weightsFromHours(withNull)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void hoursValidationAcceptsTheAllowedRangeOnly() {
        service.requireValidHours(bd("0.25"));
        service.requireValidHours(bd("1000"));
        service.requireValidHours(bd("12.5"));

        assertThatThrownBy(() -> service.requireValidHours(null)).isInstanceOf(ValidationFailedException.class);
        assertThatThrownBy(() -> service.requireValidHours(bd("0"))).isInstanceOf(ValidationFailedException.class);
        assertThatThrownBy(() -> service.requireValidHours(bd("0.10"))).isInstanceOf(ValidationFailedException.class);
        assertThatThrownBy(() -> service.requireValidHours(bd("1000.01"))).isInstanceOf(ValidationFailedException.class);
        assertThatThrownBy(() -> service.requireValidHours(bd("1.005"))).isInstanceOf(ValidationFailedException.class);
    }

    // ----- applying weights to subgoals -----

    @Test
    void applyWeightsSetsSortOrderFromOneAndWeights() {
        List<Subgoal> subgoals = List.of(sub("10", SubgoalStatus.PENDING), sub("30", SubgoalStatus.PENDING));

        service.applyWeights(WeightingMode.EFFORT, subgoals);

        assertThat(subgoals.get(0).getSortOrder()).isEqualTo(1);
        assertThat(subgoals.get(1).getSortOrder()).isEqualTo(2);
        assertThat(subgoals.get(0).getWeight()).isEqualByComparingTo("25.00");
        assertThat(subgoals.get(1).getWeight()).isEqualByComparingTo("75.00");
    }

    @Test
    void equalModeIgnoresHours() {
        List<Subgoal> subgoals = List.of(sub(null, SubgoalStatus.PENDING), sub(null, SubgoalStatus.PENDING),
                sub(null, SubgoalStatus.PENDING));

        service.applyWeights(WeightingMode.EQUAL, subgoals);

        assertThat(subgoals).extracting(Subgoal::getWeight)
                .containsExactly(bd("33.34"), bd("33.33"), bd("33.33"));
    }

    // ----- progress -----

    @Test
    void progressIsZeroWithNoSubgoalsOrOnlyPendingOnes() {
        assertThat(service.progress(List.of())).isEqualByComparingTo("0.00");

        List<Subgoal> pending = weighted(WeightingMode.EFFORT, "10", SubgoalStatus.PENDING, "20", SubgoalStatus.PENDING);
        assertThat(service.progress(pending)).isEqualByComparingTo("0.00");
    }

    @Test
    void progressIsTheSumOfTheWeightsOfCompletedSubgoalsOnly() {
        // the example from the requirements: 35% (not 40%, which two of five subgoals would give)
        List<Subgoal> subgoals = weighted(WeightingMode.EFFORT,
                "10", SubgoalStatus.COMPLETED,
                "25", SubgoalStatus.COMPLETED,
                "15", SubgoalStatus.IN_PROGRESS,
                "35", SubgoalStatus.PENDING,
                "15", SubgoalStatus.PENDING);

        assertThat(service.progress(subgoals)).isEqualByComparingTo("35.00");
    }

    @Test
    void reopeningACompletedSubgoalLowersProgress() {
        List<Subgoal> subgoals = weighted(WeightingMode.EFFORT, "10", SubgoalStatus.COMPLETED, "30", SubgoalStatus.COMPLETED);
        assertThat(service.progress(subgoals)).isEqualByComparingTo("100.00");

        subgoals.get(1).setStatus(SubgoalStatus.IN_PROGRESS);

        assertThat(service.progress(subgoals)).isEqualByComparingTo("25.00");
    }

    @Test
    void progressIsExactly100WhenEverythingIsCompletedEvenWithAwkwardWeights() {
        List<Subgoal> subgoals = weighted(WeightingMode.EFFORT,
                "1", SubgoalStatus.COMPLETED, "1", SubgoalStatus.COMPLETED, "1", SubgoalStatus.COMPLETED);

        assertThat(service.progress(subgoals)).isEqualByComparingTo("100.00");
    }

    @Test
    void progressIsKeptBetween0And100() {
        assertThat(service.clampProgress(bd("100.50"))).isEqualByComparingTo("100.00");
        assertThat(service.clampProgress(bd("-3"))).isEqualByComparingTo("0.00");
        assertThat(service.clampProgress(null)).isEqualByComparingTo("0.00");
    }

    @Test
    void totalHoursAddsUpAndIgnoresMissingValues() {
        List<Subgoal> subgoals = List.of(sub("10.5", SubgoalStatus.PENDING), sub(null, SubgoalStatus.PENDING),
                sub("4.25", SubgoalStatus.PENDING));

        assertThat(service.totalHours(subgoals)).isEqualByComparingTo("14.75");
    }

    // ----- helpers -----

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    private static List<BigDecimal> hours(String... values) {
        List<BigDecimal> list = new ArrayList<>();
        for (String v : values) {
            list.add(new BigDecimal(v));
        }
        return list;
    }

    private static BigDecimal sum(List<BigDecimal> weights) {
        return weights.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static Subgoal sub(String hours, SubgoalStatus status) {
        Subgoal subgoal = new Subgoal();
        subgoal.setTitle("Step");
        subgoal.setEstimatedHours(hours == null ? null : new BigDecimal(hours));
        subgoal.setStatus(status);
        return subgoal;
    }

    /** Arguments alternate: hours, status, hours, status... Weights are calculated like in the real service. */
    private List<Subgoal> weighted(WeightingMode mode, Object... hoursAndStatus) {
        List<Subgoal> list = new ArrayList<>();
        for (int i = 0; i < hoursAndStatus.length; i += 2) {
            list.add(sub((String) hoursAndStatus[i], (SubgoalStatus) hoursAndStatus[i + 1]));
        }
        service.applyWeights(mode, list);
        return list;
    }
}
