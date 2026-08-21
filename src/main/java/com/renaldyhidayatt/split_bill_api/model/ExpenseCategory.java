package com.renaldyhidayatt.split_bill_api.model;

/**
 * Category of an expense used to group shared costs.
 */
public enum ExpenseCategory {
    /** Food and beverages. */
    FOOD,
    /** Transportation costs (gas, tickets, taxi, tolls). */
    TRANSPORT,
    /** Accommodation (lodging, hotel, venue rental). */
    ACCOMMODATION,
    /** Entertainment and recreation. */
    ENTERTAINMENT,
    /** Utilities / shared bills (electricity, water, internet). */
    UTILITIES,
    /** Other miscellaneous expenses. */
    OTHER
}
