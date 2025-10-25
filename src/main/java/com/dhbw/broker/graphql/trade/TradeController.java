package com.dhbw.broker.graphql.trade;

import com.dhbw.broker.graphql.price.AssetPriceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class TradeController {

    private final TradeRepository tradeRepository;
    private final AssetRepository assetRepository;
    private final AssetPriceRepository priceRepository;

    @QueryMapping
    public List<TradeRepository.Trade> userTrades() {
        UUID userId = getCurrentUserId();
        return tradeRepository.findByUserId(userId);
    }

    @QueryMapping
    public List<TradeRepository.Trade> userTradesByAsset(@Argument String assetSymbol) {
        UUID userId = getCurrentUserId();
        return tradeRepository.findByUserIdAndAsset(userId, assetSymbol);
    }

    @MutationMapping
    public TradeRepository.TradeResult executeTrade(@Argument Map<String, Object> input) {
        // Validierung
        String assetSymbol = (String) input.get("assetSymbol");
        String side = (String) input.get("side");
        Object quantityObj = input.get("quantity");
        
        if (assetSymbol == null || assetSymbol.isBlank()) {
            throw new IllegalArgumentException("Asset symbol is required");
        }
        
        if (side == null || (!side.equals("BUY") && !side.equals("SELL"))) {
            throw new IllegalArgumentException("Side must be BUY or SELL");
        }
        
        BigDecimal quantity;
        if (quantityObj instanceof Number) {
            quantity = new BigDecimal(quantityObj.toString());
        } else {
            throw new IllegalArgumentException("Quantity must be a number");
        }
        
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        // Asset-Validierung
        if (!assetRepository.isActiveAsset(assetSymbol)) {
            throw new IllegalArgumentException("Invalid or inactive asset: " + assetSymbol);
        }

        // Asset Details für min_trade_increment Validierung
        AssetRepository.Asset asset = assetRepository.findBySymbol(assetSymbol);
        if (asset != null && asset.minTradeIncrement() != null && 
            quantity.remainder(asset.minTradeIncrement()).compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalArgumentException("Quantity must be a multiple of " + asset.minTradeIncrement());
        }

        // Aktuellen Preis holen
        AssetPriceRepository.PriceTick currentPrice = priceRepository.findLatest(assetSymbol);
        if (currentPrice == null || currentPrice.priceUsd().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Unable to get current price for asset: " + assetSymbol);
        }

        // Trade ausführen
        UUID userId = getCurrentUserId();
        OffsetDateTime executedAt = OffsetDateTime.now();
        
        return tradeRepository.insertTrade(userId, assetSymbol, side, quantity, 
                                         currentPrice.priceUsd(), executedAt);
    }

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new IllegalStateException("User not authenticated");
        }
        
        // Hier würdest du normalerweise den User aus dem JWT Token extrahieren
        // Für Demo-Zwecke verwende ich eine feste UUID
        // In der Realität würdest du den userId aus dem JWT Claims extrahieren
        return UUID.fromString("550e8400-e29b-41d4-a716-446655440000"); // Demo User ID
    }
}