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

/**
 * Verwaltet die Tabelle held_trades, enthält einfache DDL zum Anlegen der Tabelle
 * und Methoden zum anpassen / abrufen der gehaltenen Mengen pro User.
 */
@Repository
@RequiredArgsConstructor
public class HeldTradeRepository {

    private final JdbcTemplate jdbc;

    // Erzeuge Tabelle falls nicht vorhanden (einmalig beim Aufruf)
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

    /**
     * Adjustiert die gehaltene Menge eines Assets für einen User. Positive delta = Buy, negative = Sell.
     * Legt die Zeile an, falls noch nicht vorhanden. Hält nur die Nettomenge (kann 0 werden).
     */
    public void adjustHeldQuantity(UUID userId, String assetSymbol, BigDecimal delta) {
        ensureTable();

        // Verwende Postgres upsert (ON CONFLICT) um Menge zu addieren/subtrahieren
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

        // Optional: entferne Zeilen mit Menge <= 0
        jdbc.update("DELETE FROM held_trades WHERE user_id = ? AND asset_symbol = ? AND quantity <= 0",
                ps -> {
                    ps.setObject(1, userId);
                    ps.setString(2, assetSymbol);
                });
    }

    /**
     * Liefert alle gehaltenen Assets (quantity > 0) für einen User.
     */
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

    /**
     * Rekonstruiert die held_trades-Einträge für einen User aus der trades-Tabelle (Netto BUY - SELL pro Asset).
     * Überschreibt vorherige Einträge.
     */
    public void recomputeHeldTradesForUser(UUID userId) {
        ensureTable();

        // Aggregiere netto-quantity per asset aus trades
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

        // Entferne bestehende für user
        jdbc.update("DELETE FROM held_trades WHERE user_id = ?", ps -> ps.setObject(1, userId));

        // Insertiere nur positive Netto-Mengen
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
