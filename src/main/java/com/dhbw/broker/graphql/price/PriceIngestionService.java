package com.dhbw.broker.graphql.price;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class PriceIngestionService {

  private static final Logger log = LoggerFactory.getLogger(PriceIngestionService.class);

  private final ExternalPriceClient external;
  private final AssetPriceRepository repo;
  private final ObjectMapper om;

 
  public void recordNow(String assetSymbol) {
    try {
      String json = external.fetchPrice(assetSymbol); 
      if (json == null || json.isBlank()) {
        log.warn("Empty price response for {}", assetSymbol);
        return;
      }
      BigDecimal price = parsePrice(json, assetSymbol);
      if (price == null) {
        log.warn("Cannot parse price for {} from {}", assetSymbol, json);
        return;
      }
      Instant nowUtc = Instant.now().atOffset(ZoneOffset.UTC).toInstant();

      repo.insertTick(assetSymbol, price, nowUtc, false);
      log.info("Recorded {} = {} @ {}", assetSymbol, price, nowUtc);

    } catch (Exception e) {
      log.error("recordNow failed for {}: {}", assetSymbol, e.getMessage(), e);
    }
  }

  
  public void purgeOlderThan24h() {
    int n = repo.purgeOlderThan24h();
    if (n > 0) log.info("Purged {} old ticks (>24h)", n);
  }

  private BigDecimal parsePrice(String json, String symbol) {
    try {
      JsonNode root = om.readTree(json);
      if (root.has("price")) return new BigDecimal(root.get("price").asText());
      if (root.has("data") && root.get("data").has("price")) return new BigDecimal(root.get("data").get("price").asText());
      if (root.has(symbol)) return new BigDecimal(root.get(symbol).asText());
      if (root.has("rates") && root.get("rates").has(symbol)) return new BigDecimal(root.get("rates").get(symbol).asText());
   
      return findFirstNumber(root);
    } catch (Exception e) {
      log.error("parsePrice error for {}: {}", symbol, e.getMessage());
      return null;
    }
  }

  private BigDecimal findFirstNumber(JsonNode node) {
    if (node.isNumber()) return new BigDecimal(node.asText());
    if (node.isObject()) {
      var it = node.fieldNames();
      while (it.hasNext()) {
        var v = findFirstNumber(node.get(it.next()));
        if (v != null) return v;
      }
    }
    if (node.isArray()) {
      for (JsonNode n : node) {
        var v = findFirstNumber(n);
        if (v != null) return v;
      }
    }
    return null;
  }
}