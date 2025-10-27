package com.dhbw.broker.graphql.trade;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Einfaches Jdbc-basiertes Repository als Ersatz für das JPA-Interface.
 * Die Implementierung ist bewusst minimal, da die Hauptfunktionalität über held_trades läuft.
 */
@Repository
@RequiredArgsConstructor
public class UserAssetRepository {

    private final JdbcTemplate jdbc;

    private static UserAsset map(ResultSet rs) throws SQLException {
        return new UserAsset(rs.getObject("id") == null ? null : rs.getLong("id"),
                rs.getString("user_id"), rs.getString("asset_symbol"), rs.getBigDecimal("quantity"));
    }

    public List<UserAsset> findByUserId(String userId) {
        var rows = jdbc.query("SELECT id, user_id, asset_symbol, quantity FROM user_assets WHERE user_id = ?",
                ps -> ps.setString(1, userId), rs -> {
                    var list = new ArrayList<UserAsset>();
                    while (rs.next()) list.add(map(rs));
                    return list;
                });
        return rows;
    }

    public Optional<UserAsset> findByUserIdAndAssetSymbol(String userId, String assetSymbol) {
        var list = jdbc.query("SELECT id, user_id, asset_symbol, quantity FROM user_assets WHERE user_id = ? AND asset_symbol = ?",
                ps -> {
                    ps.setString(1, userId);
                    ps.setString(2, assetSymbol);
                }, rs -> {
                    var res = new ArrayList<UserAsset>();
                    while (rs.next()) res.add(map(rs));
                    return res;
                });
        if (list.isEmpty()) return Optional.empty();
        return Optional.of(list.get(0));
    }
}