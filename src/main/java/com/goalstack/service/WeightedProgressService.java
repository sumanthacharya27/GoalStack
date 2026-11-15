package com.goalstack.service;

import com.goalstack.entity.Subgoal;
import com.goalstack.entity.SubgoalStatus;
import com.goalstack.entity.WeightingMode;
import com.goalstack.exception.ValidationFailedException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * All weight and progress maths. It never touches the database.
 *
 * Weights are whole "hundredths of a percent" (cents of 100.00%) so the arithmetic is exact:
 *   weight = hours / total hours * 100, using the largest-remainder method.
 * Every subgoal first gets the rounded-down value; the leftover cents go to the subgoals with the
 * biggest remainders (ties: the earlier subgoal). The weights therefore always total exactly 100.00.
 */
@Service
public class WeightedProgressService {

    public static final int MAX_SUBGOALS_PER_GOAL = 50;
    public static final BigDecimal MIN_HOURS = new BigDecimal("0.25");
    public static final BigDecimal MAX_HOURS = new BigDecimal("1000.00");

    private static final long TOTAL_CENTS = 10_000L; // 100.00% in hundredths
    private static final BigDecimal HUNDRED = new BigDecimal("100.00");

    /** Equal weights for {@code count} subgoals, for example 3 gives 33.34, 33.33, 33.33. */
    public List<BigDecimal> equalWeights(int count) {
        List<BigDecimal> weights = new ArrayList<>(count);
        if (count <= 0) {
            return weights;
        }
        long base = TOTAL_CENTS / count;
        long extra = TOTAL_CENTS % count;
        for (int i = 0; i < count; i++) {
            weights.add(cents(base + (i < extra ? 1 : 0)));
        }
        return weights;
    }

    /** Effort-based weights. Every value must be greater than zero. The result always totals 100.00. */
    public List<BigDecimal> weightsFromHours(List<BigDecimal> hours) {
        int n = hours.size();
        List<BigDecimal> result = new ArrayList<>(n);
        if (n == 0) {
            return result;
        }

        long[] units = new long[n]; // hours in hundredths of an hour
        long total = 0;
        for (int i = 0; i < n; i++) {
            BigDecimal h = hours.get(i);
            if (h == null || h.signum() <= 0) {
                throw new IllegalArgumentException("Estimated hours must be greater than zero.");
            }
            units[i] = h.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
            if (units[i] <= 0) {
                throw new IllegalArgumentException("Estimated hours must be greater than zero.");
            }
            total += units[i];
        }

        long[] cents = new long[n];
        long[] remainders = new long[n];
        long assigned = 0;
        for (int i = 0; i < n; i++) {
            long numerator = units[i] * TOTAL_CENTS;
            cents[i] = numerator / total;
            remainders[i] = numerator % total;
            assigned += cents[i];
        }

        long leftover = TOTAL_CENTS - assigned; // always 0 <= leftover < n
        List<Integer> order = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            order.add(i);
        }
        order.sort(Comparator.<Integer>comparingLong(i -> -remainders[i]).thenComparingInt(i -> i));
        for (int k = 0; k < leftover; k++) {
            cents[order.get(k)]++;
        }

        for (int i = 0; i < n; i++) {
            result.add(cents(cents[i]));
        }
        return result;
    }

    /**
     * Sets sort order (1, 2, 3...) and weights on the given subgoals, which must already be in the
     * order they should have. EFFORT goals use hours; EQUAL goals share the weight evenly.
     */
    public void applyWeights(WeightingMode mode, List<Subgoal> ordered) {
        List<BigDecimal> weights;
        if (mode == WeightingMode.EFFORT) {
            weights = weightsFromHours(ordered.stream().map(Subgoal::getEstimatedHours).toList());
        } else {
            weights = equalWeights(ordered.size());
        }
        for (int i = 0; i < ordered.size(); i++) {
            ordered.get(i).setSortOrder(i + 1);
            ordered.get(i).setWeight(weights.get(i));
        }
    }

    /** Progress = sum of the weights of COMPLETED subgoals, between 0.00 and 100.00. */
    public BigDecimal progress(List<Subgoal> subgoals) {
        BigDecimal sum = BigDecimal.ZERO;
        for (Subgoal s : subgoals) {
            if (s.getStatus() == SubgoalStatus.COMPLETED) {
                sum = sum.add(s.getWeight());
            }
        }
        return clampProgress(sum);
    }

    /** Keeps a progress value inside 0.00 to 100.00 with two decimals. */
    public BigDecimal clampProgress(BigDecimal value) {
        BigDecimal v = value == null ? BigDecimal.ZERO : value;
        if (v.signum() < 0) {
            v = BigDecimal.ZERO;
        }
        if (v.compareTo(HUNDRED) > 0) {
            v = HUNDRED;
        }
        return v.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal totalHours(List<Subgoal> subgoals) {
        BigDecimal sum = BigDecimal.ZERO;
        for (Subgoal s : subgoals) {
            if (s.getEstimatedHours() != null) {
                sum = sum.add(s.getEstimatedHours());
            }
        }
        return sum.setScale(2, RoundingMode.HALF_UP);
    }

    /** Checks one hours value for a goal that uses effort weighting. */
    public void requireValidHours(BigDecimal hours) {
        if (hours == null) {
            throw new ValidationFailedException("estimatedHours", "Estimated hours are required.");
        }
        if (hours.compareTo(MIN_HOURS) < 0 || hours.compareTo(MAX_HOURS) > 0) {
            throw new ValidationFailedException("estimatedHours", "Estimated hours must be between 0.25 and 1000.");
        }
        if (hours.stripTrailingZeros().scale() > 2) {
            throw new ValidationFailedException("estimatedHours", "Use at most 2 decimal places.");
        }
    }

    private BigDecimal cents(long cents) {
        return BigDecimal.valueOf(cents, 2);
    }
}
