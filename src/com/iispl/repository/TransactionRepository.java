package com.iispl.repository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.iispl.connectionpool.ConnectionPool;
import com.iispl.entity.Transaction;
import com.iispl.enums.Channel;
import com.iispl.enums.DrCr;
import com.iispl.enums.Status;

public class TransactionRepository {

    public void save(Transaction txn, String batchId) throws SQLException {
        String sql = "Insert into transactions(txn_id, channel, amount, txn_time, dr_cr, status, batch_id) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection con = ConnectionPool.getDataSource().getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, txn.getTxnId());
            ps.setString(2, txn.getChannel().toString());
            ps.setBigDecimal(3, txn.getAmount());
            ps.setTimestamp(4, java.sql.Timestamp.from(txn.getTxnTime()));
            ps.setString(5, txn.getDrCr().toString());
            ps.setString(6, txn.getStatus().toString());
            ps.setString(7, batchId);

            ps.executeUpdate();
        }
    }

    public List<Transaction> findByBatchId(String batchId) throws SQLException {
        String sql = "select txn_id, channel, amount, txn_time, dr_cr, status from transactions where batch_id = ?";
        List<Transaction> transactions = new ArrayList<>();

        try (Connection con = ConnectionPool.getDataSource().getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, batchId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Transaction txn = new Transaction(
                            rs.getString("txn_id"),
                            Channel.valueOf(rs.getString("channel")),
                            rs.getBigDecimal("amount"),
                            rs.getTimestamp("txn_time").toInstant(),
                            DrCr.valueOf(rs.getString("dr_cr")),
                            Status.valueOf(rs.getString("status")));
                    transactions.add(txn);
                }
            }
        }

        return transactions;
    }

    public BigDecimal totalAmountForBatch(String batchId) throws SQLException {
        String sql = "select coalesce(sum(amount), 0) as total from transactions where batch_id = ?";

        try (Connection con = ConnectionPool.getDataSource().getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, batchId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal("total");
                }
            }
        }
        return BigDecimal.ZERO;
    }
}
