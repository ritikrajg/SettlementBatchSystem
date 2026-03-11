package com.iispl.repository;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

import com.iispl.connectionpool.ConnectionPool;
import com.iispl.entity.SettlementBatch;

public class SettlementBatchRepository {

    public void save(SettlementBatch batch) throws SQLException {
        String sql = "Insert into settlement_batch(batch_id, batch_date) values (?, ?)";

        try (Connection conn = ConnectionPool.getDataSource().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, batch.getBatchId());
            ps.setDate(2, Date.valueOf(batch.getDate()));
            ps.executeUpdate();
        }
    }

    public LocalDate findBatchDate(String batchId) throws SQLException {
        String sql = "select batch_date from settlement_batch where batch_id = ?";

        try (Connection conn = ConnectionPool.getDataSource().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, batchId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDate("batch_date").toLocalDate();
                }
                return null;
            }
        }
    }
}
