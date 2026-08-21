package com.renaldyhidayatt.split_bill_api.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

import org.springframework.beans.factory.annotation.Value;

/**
 * Derives the submission's personalized service charge from a GitHub username:
 * sum the ASCII codes of the lowercased username, then take that sum mod 10.
 * See README.md "Personalization" for the worked example this mirrors.
 */
@Component
public class ServiceChargeCalculator {

    private final int servicePct;

    public ServiceChargeCalculator(@Value("${app.github-username}") String githubUsername) {
        this.servicePct = calculatePct(githubUsername);
    }

    /** Pure function, kept static and package-visible so it's trivially unit-testable. */
    static int calculatePct(String githubUsername) {
        String lower = githubUsername.toLowerCase();
        int sum = 0;
        for (int i = 0; i < lower.length(); i++) {
            sum += lower.charAt(i);
        }
        return sum % 10;
    }

    public int getServicePct() {
        return servicePct;
    }

    public BigDecimal applyTo(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(servicePct))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
}
