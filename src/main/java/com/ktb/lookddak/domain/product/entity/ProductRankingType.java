package com.ktb.lookddak.domain.product.entity;

import com.ktb.lookddak.global.exception.BusinessException;
import com.ktb.lookddak.global.exception.ErrorCode;

public enum ProductRankingType {
    WISH,
    CLICK;

    public static ProductRankingType from(String type) {
        if (type == null || type.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_RANKING_TYPE);
        }

        try {
            return ProductRankingType.valueOf(type);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.INVALID_RANKING_TYPE);
        }
    }
}
