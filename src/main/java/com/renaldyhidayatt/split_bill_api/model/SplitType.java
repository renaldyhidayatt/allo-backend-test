package com.renaldyhidayatt.split_bill_api.model;


/**
 * How an expense's amount is divided among the participants it applies to.
 */
public enum SplitType {
    /** Divided evenly among the given participants (remainder cents assigned deterministically). */
    EQUAL,
    /** Divided by a percentage per participant; percentages must sum to 100. */
    PERCENTAGE,
    /** An exact amount per participant; amounts must sum to the expense total. */
    EXACT
}