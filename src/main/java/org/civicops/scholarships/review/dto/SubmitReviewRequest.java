package org.civicops.scholarships.review.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import org.civicops.scholarships.review.ReviewRecommendation;

public record SubmitReviewRequest(
    @NotNull @DecimalMin("0.00") @DecimalMax("100.00") @Digits(integer = 3, fraction = 2)
        BigDecimal score,
    @NotNull ReviewRecommendation recommendation,
    @Size(max = 20000) String comments) {}
