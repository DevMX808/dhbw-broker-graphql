package com.dhbw.broker.graphql.trade;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Repräsentiert eine aggregierte, aktuell gehaltene Menge eines Assets pro User.
 */
public record HeldTrade(Long id, String assetSymbol, BigDecimal quantity, OffsetDateTime lastUpdated) {
}
