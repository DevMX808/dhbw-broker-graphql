package com.dhbw.broker.graphql.price;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class AssetPriceRepository {

  private final JdbcTemplate jdbc;

  public record PriceTick(
      String assetSymbol, int slot, BigDecimal priceUsd,
      Instant sourceTsUtc, Instant ingestedTsUtc, boolean isCarry) {}

  private static PriceTick map(ResultSet rs) throws SQLException {
    return new PriceTick(
        rs.getString("asset_symbol"),
        rs.getInt("slot"),
        rs.getBigDecimal("price_usd"),
        rs.getTimestamp("source_ts_utc").toInstant(),
        rs.getTimestamp("ingested_ts_utc").toInstant(),
        rs.getBoolean("is_carry")
    );
  }

  
  public int insertTick(String assetSymbol, BigDecimal priceUsd, Instant sourceTsUtc, boolean isCarry) {
    return jdbc.update("""
      INSERT INTO broker.asset_prices_ring (asset_symbol, slot, price_usd, source_ts_utc, ingested_ts_utc, is_carry)
      VALUES (
        ?, 
        (EXTRACT(EPOCH FROM ?::timestamp)::int / 60) % 1440,
        ?, 
        ?, 
        (now() AT TIME ZONE 'UTC'), 
        ?
      )
      """,
      ps -> {
        ps.setString(1, assetSymbol);
        ps.setTimestamp(2, java.sql.Timestamp.from(sourceTsUtc));        
        ps.setBigDecimal(3, priceUsd);
        ps.setTimestamp(4, java.sql.Timestamp.from(sourceTsUtc));          
        ps.setBoolean(5, isCarry);
      });
  }


  public int purgeOlderThan24h() {
    return jdbc.update("""
      DELETE FROM broker.asset_prices_ring
      WHERE source_ts_utc < (now() AT TIME ZONE 'UTC') - interval '24 hours'
    """);
  }

 
  public PriceTick findLatest(String assetSymbol) {
    return jdbc.query("""
      SELECT asset_symbol, slot, price_usd, source_ts_utc, ingested_ts_utc, is_carry
      FROM broker.asset_prices_ring
      WHERE asset_symbol = ?
      ORDER BY source_ts_utc DESC
      LIMIT 1
    """, ps -> ps.setString(1, assetSymbol),
      rs -> rs.next() ? map(rs) : null);
  }

 
  public List<PriceTick> find24hHistory(String assetSymbol) {
    return jdbc.query("""
      SELECT asset_symbol, slot, price_usd, source_ts_utc, ingested_ts_utc, is_carry
      FROM broker.asset_prices_ring
      WHERE asset_symbol = ? 
        AND source_ts_utc >= (now() AT TIME ZONE 'UTC') - interval '24 hours'
      ORDER BY source_ts_utc ASC
    """, ps -> ps.setString(1, assetSymbol),
      rs -> {
        var results = new java.util.ArrayList<PriceTick>();
        while (rs.next()) {
          results.add(map(rs));
        }
        return results;
      });
  }
}