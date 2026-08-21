package com.renaldyhidayatt.split_bill_api.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import com.renaldyhidayatt.split_bill_api.exception.InvalidRequestException;

/**
 * Splits a monetary amount across N participants, always in whole cents,
 * and always summing back to exactly the original amount.
 *
 * <p>Naively dividing a BigDecimal by N (or applying a percentage) produces
 * fractional cents that get rounded away, and those lost/gained fractions
 * can add up to a visible discrepancy against the original total - a classic
 * source of "off by one cent" bugs in split-bill apps. Every method here
 * reconciles its rounding error back into the split before returning.
 */
public final class MoneySplitter {

    private MoneySplitter() {
    }

    /**
     * Splits {@code total} into {@code count} equal (or near-equal) shares.
     * Any leftover cents from integer division are handed out one-by-one,
     * in order, to the first participants - so a 10.00 split three ways is
     * 3.34 / 3.33 / 3.33, not 3.33 / 3.33 / 3.34, but always sums to 10.00.
     */
    public static List<BigDecimal> splitEqually(BigDecimal total, int count) {
        if (count <= 0) {
            throw new InvalidRequestException("Cannot split an expense between zero participants");
        }

        long totalCents = toCents(total);
        long baseCents = totalCents / count;
        long remainderCents = totalCents % count;

        List<BigDecimal> shares = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            long cents = baseCents + (i < remainderCents ? 1 : 0);
            shares.add(fromCents(cents));
        }
        return shares;
    }

    /**
     * Splits {@code total} according to percentages that must sum to exactly 100.
     * Rounding drift between the percentage math and the total is corrected by
     * adjusting the single largest share, which keeps every other share exactly
     * proportional and confines the correction to a fraction of a cent, at most.
     */
    public static List<BigDecimal> splitByPercentage(BigDecimal total, List<BigDecimal> percentages) {
        BigDecimal sum = percentages.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sum.compareTo(BigDecimal.valueOf(100)) != 0) {
            throw new InvalidRequestException("Percentages must sum to 100, got " + sum);
        }

        List<BigDecimal> raw = new ArrayList<>(percentages.size());
        for (BigDecimal pct : percentages) {
            raw.add(total.multiply(pct).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
        }
        return reconcileToTotal(raw, total);
    }

    /** Validates that exact per-participant amounts sum to the expense total, then returns them unchanged. */
    public static List<BigDecimal> splitExact(BigDecimal total, List<BigDecimal> amounts) {
        BigDecimal sum = amounts.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sum.setScale(2, RoundingMode.HALF_UP).compareTo(total.setScale(2, RoundingMode.HALF_UP)) != 0) {
            throw new InvalidRequestException(
                    "Exact shares must sum to the expense amount (" + total.setScale(2, RoundingMode.HALF_UP)
                            + "), got " + sum.setScale(2, RoundingMode.HALF_UP));
        }
        return amounts;
    }

    private static List<BigDecimal> reconcileToTotal(List<BigDecimal> raw, BigDecimal total) {
        BigDecimal target = total.setScale(2, RoundingMode.HALF_UP);
        BigDecimal sum = raw.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal diff = target.subtract(sum);
        if (diff.compareTo(BigDecimal.ZERO) == 0) {
            return raw;
        }

        int largestIndex = 0;
        for (int i = 1; i < raw.size(); i++) {
            if (raw.get(i).compareTo(raw.get(largestIndex)) > 0) {
                largestIndex = i;
            }
        }
        List<BigDecimal> result = new ArrayList<>(raw);
        result.set(largestIndex, result.get(largestIndex).add(diff));
        return result;
    }

    private static long toCents(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact();
    }

    private static BigDecimal fromCents(long cents) {
        return BigDecimal.valueOf(cents, 2);
    }
}
