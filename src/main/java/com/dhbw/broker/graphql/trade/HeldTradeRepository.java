package com.dhbw.broker.graphql.trade;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Repository
@RequiredArgsConstructor
public class HeldTradeRepository {

    private final JdbcTemplate jdbc;

  
    private void ensureTable() {
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS held_trades (
                id BIGSERIAL PRIMARY KEY,
                user_id UUID NOT NULL,
                asset_symbol TEXT NOT NULL,
                quantity NUMERIC NOT NULL,
                last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
                UNIQUE (user_id, asset_symbol)
            )
            """);
    }

    private static HeldTrade mapHeldTrade(ResultSet rs) throws SQLException {
        return new HeldTrade(
                rs.getObject("id") == null ? null : rs.getLong("id"),
                rs.getString("asset_symbol"),
                rs.getBigDecimal("quantity"),
                rs.getObject("last_updated", OffsetDateTime.class)
        );
    }

    
    public void adjustHeldQuantity(UUID userId, String assetSymbol, BigDecimal delta) {
        ensureTable();

       
        jdbc.update("""
            INSERT INTO held_trades (user_id, asset_symbol, quantity, last_updated)
            VALUES (?, ?, ?, now())
            ON CONFLICT (user_id, asset_symbol)
            DO UPDATE SET quantity = held_trades.quantity + EXCLUDED.quantity, last_updated = now();
            """,
            ps -> {
                ps.setObject(1, userId);
                ps.setString(2, assetSymbol);
                ps.setBigDecimal(3, delta);
            });

     
        jdbc.update("DELETE FROM held_trades WHERE user_id = ? AND asset_symbol = ? AND quantity <= 0",
                ps -> {
                    ps.setObject(1, userId);
                    ps.setString(2, assetSymbol);
                });
    }

   
    public List<HeldTrade> findByUserId(UUID userId) {
        ensureTable();
        return jdbc.query("SELECT id, asset_symbol, quantity, last_updated FROM held_trades WHERE user_id = ? AND quantity > 0 ORDER BY asset_symbol",
                ps -> ps.setObject(1, userId),
                rs -> {
                    var list = new ArrayList<HeldTrade>();
                    while (rs.next()) {
                        list.add(mapHeldTrade(rs));
                    }
                    return list;
                });
    }

  
    public void recomputeHeldTradesForUser(UUID userId) {
        ensureTable();

        
        var rows = jdbc.query("""
            SELECT asset_symbol, 
                   SUM(CASE WHEN side::text = 'BUY' THEN quantity ELSE -quantity END) AS net_qty
            FROM trades
            WHERE user_id = ?
            GROUP BY asset_symbol
            """,
                ps -> ps.setObject(1, userId),
                rs -> {
                    var r = new ArrayList<java.util.Map.Entry<String, BigDecimal>>();
                    while (rs.next()) {
                        String asset = rs.getString("asset_symbol");
                        BigDecimal net = rs.getBigDecimal("net_qty");
                        r.add(new java.util.AbstractMap.SimpleEntry<>(asset, net));
                    }
                    return r;
                });

 
        jdbc.update("DELETE FROM held_trades WHERE user_id = ?", ps -> ps.setObject(1, userId));

        
        for (var e : rows) {
            if (e.getValue() != null && e.getValue().compareTo(BigDecimal.ZERO) > 0) {
                jdbc.update("INSERT INTO held_trades (user_id, asset_symbol, quantity, last_updated) VALUES (?, ?, ?, now())",
                        ps -> {
                            ps.setObject(1, userId);
                            ps.setString(2, e.getKey());
                            ps.setBigDecimal(3, e.getValue());
                        });
            }
        }
    }
}
