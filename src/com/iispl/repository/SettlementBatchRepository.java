package com.iispl.repository;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import com.iispl.connectionpool.ConnectionPool;
import com.iispl.entity.SettlementBatch;

public class SettlementBatchRepository {

    public void save(SettlementBatch batch) throws SQLException {
        try (Connection conn = ConnectionPool.getDataSource().getConnection()) {
            save(conn, batch);
        }
    }

    public void save(Connection conn, SettlementBatch batch) throws SQLException {
        String sql = "Insert into settlement_batch(batch_id, batch_date) values (?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, batch.getBatchId());
            ps.setDate(2, Date.valueOf(batch.getDate()));
            ps.executeUpdate();
        }
    }

    public boolean existsByBatchId(String batchId) throws SQLException {
        String sql = "select 1 from settlement_batch where batch_id = ?";
        try (Connection conn = ConnectionPool.getDataSource().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, batchId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
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

    public Map<String, LocalDate> findAllBatchDates() throws SQLException {
        String sql = "select batch_id, batch_date from settlement_batch order by batch_date desc, batch_id";
        return queryBatchDates(sql, null);
    }

    public Map<String, LocalDate> findBatchDatesByDate(LocalDate batchDate) throws SQLException {
        String sql = "select batch_id, batch_date from settlement_batch where batch_date = ? order by batch_id";
        return queryBatchDates(sql, batchDate);
    }

    private Map<String, LocalDate> queryBatchDates(String sql, LocalDate batchDate) throws SQLException {
        Map<String, LocalDate> batchDates = new LinkedHashMap<>();
        try (Connection conn = ConnectionPool.getDataSource().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            if (batchDate != null) {
                ps.setDate(1, Date.valueOf(batchDate));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    batchDates.put(rs.getString("batch_id"), rs.getDate("batch_date").toLocalDate());
                }
            }
        }
        return batchDates;
    }
}
