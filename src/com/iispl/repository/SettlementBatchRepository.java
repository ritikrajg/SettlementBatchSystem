package com.iispl.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.iispl.connectionpool.ConnectionPool;
import com.iispl.entity.SettlementBatch;

public class SettlementBatchRepository {

    public void save(SettlementBatch batch) throws SQLException{

        String sql="Insert into settlement_batch(batch_id, batch_date) values (?, ?)";

        try(Connection conn = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps=conn.prepareStatement(sql)){
                ps.setString(1,batch.getBatchId());
                ps.setDate(2, java.sql.Date.valueOf(batch.getDate()));
                ps.executeUpdate();
             }
    }
}