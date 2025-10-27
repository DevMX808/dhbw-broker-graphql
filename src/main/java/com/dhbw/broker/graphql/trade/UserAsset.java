package com.dhbw.broker.graphql.trade;

import java.math.BigDecimal;

/**
 * Minimaler POJO-Ersatz für UserAsset um Kompilationsfehler zu vermeiden.
 */
public record UserAsset(Long id, String userId, String assetSymbol, BigDecimal quantity) {
}
