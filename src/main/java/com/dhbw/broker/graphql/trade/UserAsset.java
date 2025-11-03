package com.dhbw.broker.graphql.trade;

import java.math.BigDecimal;


public record UserAsset(Long id, String userId, String assetSymbol, BigDecimal quantity) {
}
