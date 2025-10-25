package com.dhbw.broker.graphql.trade;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class TradeRepository {

    private final JdbcTemplate jdbc;

    public record Trade(
            UUID tradeId,
            String assetSymbol,
            String side,
            BigDecimal quantity,
            BigDecimal priceUsd,
            OffsetDateTime executedAt,
            OffsetDateTime createdAt
    ) {}

    public record TradeResult(
            UUID tradeId,
            OffsetDateTime executedAt,
            BigDecimal priceUsd,
            String assetSymbol,
            String side,
            BigDecimal quantity
    ) {}

    private static Trade mapTrade(ResultSet rs) throws SQLException {
        return new Trade(
                (UUID) rs.getObject("trade_id"),
                rs.getString("asset_symbol"),
                rs.getString("side"),
                rs.getBigDecimal("quantity"),
                rs.getBigDecimal("price_usd"),
                rs.getObject("executed_at", OffsetDateTime.class),
                rs.getObject("created_at", OffsetDateTime.class)
        );
    }

    /** Speichert einen neuen Trade */
    public TradeResult insertTrade(UUID userId, String assetSymbol, String side, 
                                  BigDecimal quantity, BigDecimal priceUsd, OffsetDateTime executedAt) {
        return jdbc.query("""
            INSERT INTO trades (user_id, asset_symbol, side, quantity, price_usd, executed_at)
            VALUES (?, ?, ?::trade_side, ?, ?, ?)
            RETURNING trade_id, executed_at, price_usd, asset_symbol, side::text, quantity
            """,
            ps -> {
                ps.setObject(1, userId);
                ps.setString(2, assetSymbol);
                ps.setString(3, side);
                ps.setBigDecimal(4, quantity);
                ps.setBigDecimal(5, priceUsd);
                ps.setObject(6, executedAt);
            },
            rs -> {
                if (rs.next()) {
                    return new TradeResult(
                            (UUID) rs.getObject("trade_id"),
                            rs.getObject("executed_at", OffsetDateTime.class),
                            rs.getBigDecimal("price_usd"),
                            rs.getString("asset_symbol"),
                            rs.getString("side"),
                            rs.getBigDecimal("quantity")
                    );
                }
                return null;
            });
    }

    /** Holt alle Trades eines Users */
    public List<Trade> findByUserId(UUID userId) {
        return jdbc.query("""
            SELECT t.trade_id, t.asset_symbol, t.side::text as side, t.quantity, 
                   t.price_usd, t.executed_at, t.created_at
            FROM trades t
            WHERE t.user_id = ?
            ORDER BY t.executed_at DESC
            """,
            ps -> ps.setObject(1, userId),
            rs -> {
                var results = new java.util.ArrayList<Trade>();
                while (rs.next()) {
                    results.add(mapTrade(rs));
                }
                return results;
            });
    }

    /** Holt alle Trades eines Users für ein bestimmtes Asset */
    public List<Trade> findByUserIdAndAsset(UUID userId, String assetSymbol) {
        return jdbc.query("""
            SELECT t.trade_id, t.asset_symbol, t.side::text as side, t.quantity, 
                   t.price_usd, t.executed_at, t.created_at
            FROM trades t
            WHERE t.user_id = ? AND t.asset_symbol = ?
            ORDER BY t.executed_at DESC
            """,
            ps -> {
                ps.setObject(1, userId);
                ps.setString(2, assetSymbol);
            },
            rs -> {
                var results = new java.util.ArrayList<Trade>();
                while (rs.next()) {
                    results.add(mapTrade(rs));
                }
                return results;
            });
    }
}