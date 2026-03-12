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
import com.iispl.enums.Bank;
import com.iispl.enums.Channel;
import com.iispl.enums.DrCr;
import com.iispl.enums.Status;

public class TransactionRepository {

    public void save(Transaction txn, String batchId) throws SQLException {
        try (Connection con = ConnectionPool.getDataSource().getConnection()) {
            save(con, txn, batchId);
        }
    }

    public void save(Connection con, Transaction txn, String batchId) throws SQLException {
        String sql = "insert into transactions(txn_id, sender_bank, receiver_bank, channel, amount, txn_time, dr_cr, status, batch_id) "
                + "values (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, txn.getTxnId());
            ps.setString(2, txn.getSenderBank().name());
            ps.setString(3, txn.getReceiverBank().name());
            ps.setString(4, txn.getChannel().name());
            ps.setBigDecimal(5, txn.getAmount());
            ps.setTimestamp(6, java.sql.Timestamp.from(txn.getTxnTime()));
            ps.setString(7, txn.getDrCr().name());
            ps.setString(8, txn.getStatus().name());
            ps.setString(9, batchId);

            ps.executeUpdate();
        }
    }

    public boolean existsByTxnId(String txnId) throws SQLException {
        try (Connection con = ConnectionPool.getDataSource().getConnection()) {
            return existsByTxnId(con, txnId);
        }
    }

    public boolean existsByTxnId(Connection con, String txnId) throws SQLException {
        String sql = "select 1 from transactions where txn_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, txnId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public List<Transaction> findByBatchId(String batchId) throws SQLException {
        String sql = "select txn_id, sender_bank, receiver_bank, channel, amount, txn_time, dr_cr, status "
                + "from transactions where batch_id = ?";

        try (Connection con = ConnectionPool.getDataSource().getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, batchId);
            try (ResultSet rs = ps.executeQuery()) {
                return mapTransactions(rs);
            }
        }
    }

    public List<Transaction> findAll() throws SQLException {
        String sql = "select txn_id, sender_bank, receiver_bank, channel, amount, txn_time, dr_cr, status from transactions";
        try (Connection con = ConnectionPool.getDataSource().getConnection();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            return mapTransactions(rs);
        }
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

    private List<Transaction> mapTransactions(ResultSet rs) throws SQLException {
        List<Transaction> transactions = new ArrayList<>();
        while (rs.next()) {
            Transaction txn = new Transaction(
                    rs.getString("txn_id"),
                    Bank.valueOf(rs.getString("sender_bank")),
                    Bank.valueOf(rs.getString("receiver_bank")),
                    Channel.valueOf(rs.getString("channel")),
                    rs.getBigDecimal("amount"),
                    rs.getTimestamp("txn_time").toInstant(),
                    DrCr.valueOf(rs.getString("dr_cr")),
                    Status.valueOf(rs.getString("status")));
            transactions.add(txn);
        }
        return transactions;
    }
}
