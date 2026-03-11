package com.iispl.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.iispl.connectionpool.ConnectionPool;
import com.iispl.entity.Transaction;

public class TransactionRepository {

    public void save(Transaction txn, String batchId) throws SQLException{

        String sql="Insert into transactions(txn_id, channel, amount, txn_time, dr_cr, status, batch_id) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try(Connection con=ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps=con.prepareStatement(sql)){

                ps.setString(1,txn.getTxnId());
                ps.setString(2, txn.getChannel().toString());
                ps.setBigDecimal(3, txn.getAmount());
                ps.setTimestamp(4, java.sql.Timestamp.from(txn.getTxnTime()));
                ps.setString(5,txn.getDrCr().toString());
                ps.setString(6, txn.getStatus().toString());
                ps.setString(7, batchId);
                
                ps.executeUpdate();
             }
    }
}
