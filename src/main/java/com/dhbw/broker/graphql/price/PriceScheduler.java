package com.dhbw.broker.graphql.price;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PriceScheduler {

  private static final Logger log = LoggerFactory.getLogger(PriceScheduler.class);

  private final PriceIngestionService ingestion;

  // Passe die Liste an: deine Symbole
  private final List<String> symbols = List.of("XAU", "XAG", "BTC", "ETH", "XPD", "HG");

  /** Alle 60s: Preise je Symbol holen. */
  @Scheduled(fixedRateString = "60000") // 60s
  public void collect() {
    int ok=0, err=0;
    for (String s : symbols) {
      try { ingestion.recordNow(s); ok++; }
      catch (Exception e) { err++; log.error("record {} failed: {}", s, e.getMessage()); }
    }
    try { ingestion.purgeOlderThan24h(); } catch (Exception e) { log.error("purge failed: {}", e.getMessage()); }
    log.info("collect done ok={} err={}", ok, err);
  }
}