package com.dhbw.broker.graphql.trade;

import java.math.BigDecimal;
import java.time.OffsetDateTime;


public record HeldTrade(Long id, String assetSymbol, BigDecimal quantity, OffsetDateTime lastUpdated) {
}
