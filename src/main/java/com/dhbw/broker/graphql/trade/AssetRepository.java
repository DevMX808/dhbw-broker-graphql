package com.dhbw.broker.graphql.trade;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;

@Repository
@RequiredArgsConstructor
public class AssetRepository {

    private final JdbcTemplate jdbc;

    public record Asset(
            String assetSymbol,
            String name,
            boolean isActive,
            BigDecimal minTradeIncrement
    ) {}

    private static Asset mapAsset(ResultSet rs) throws SQLException {
        return new Asset(
                rs.getString("asset_symbol"),
                rs.getString("name"),
                rs.getBoolean("is_active"),
                rs.getBigDecimal("min_trade_increment")
        );
    }

    /** Prüft ob ein Asset existiert und aktiv ist */
    public boolean isActiveAsset(String assetSymbol) {
        return jdbc.query("""
            SELECT COUNT(*) as count
            FROM assets
            WHERE asset_symbol = ? AND is_active = true
            """,
            ps -> ps.setString(1, assetSymbol),
            rs -> rs.next() && rs.getInt("count") > 0);
    }

    /** Holt ein Asset mit Details */
    public Asset findBySymbol(String assetSymbol) {
        return jdbc.query("""
            SELECT asset_symbol, name, is_active, min_trade_increment
            FROM assets
            WHERE asset_symbol = ?
            """,
            ps -> ps.setString(1, assetSymbol),
            rs -> rs.next() ? mapAsset(rs) : null);
    }
}