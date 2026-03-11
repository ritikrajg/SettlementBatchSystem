package com.iispl.service;

import java.sql.SQLException;

import com.iispl.entity.SettlementBatch;
import com.iispl.entity.Transaction;
import com.iispl.repository.SettlementBatchRepository;
import com.iispl.repository.TransactionRepository;

public class SettlementService {

    private final SettlementBatchRepository batchRepo;
    private final TransactionRepository txnRepo;

    public SettlementService(SettlementBatchRepository batchRepo,TransactionRepository txnRepo){

        this.batchRepo=batchRepo;
        this.txnRepo=txnRepo;
    }

    public void processBatch(SettlementBatch batch) throws SQLException{
        batchRepo.save(batch);

        for(Transaction t:batch.getTransaction()){
            txnRepo.save(t, batch.getBatchId());
        }
    }
}
