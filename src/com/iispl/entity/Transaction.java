package com.iispl.entity;

import java.math.BigDecimal;
import java.time.Instant;

import com.iispl.enums.Channel;
import com.iispl.enums.DrCr;
import com.iispl.enums.Status;

public final class Transaction {

    private final String txnId;
    private final Channel channel;
    private final BigDecimal amount;
    private final Instant txnTime;
    private final DrCr drCr;
    private final Status status;
    public Transaction(String txnId, Channel channel, BigDecimal amount, Instant txnTime, DrCr drCr, Status status) {
        this.txnId = txnId;
        this.channel = channel;
        this.amount = amount;
        this.txnTime = txnTime;
        this.drCr = drCr;
        this.status = status;
    }
    public String getTxnId() {
        return txnId;
    }
    public Channel getChannel() {
        return channel;
    }
    public BigDecimal getAmount() {
        return amount;
    }
    public Instant getTxnTime() {
        return txnTime;
    }
    public DrCr getDrCr() {
        return drCr;
    }
    public Status getStatus() {
        return status;
    }
}
