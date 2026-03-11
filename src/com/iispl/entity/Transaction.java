package com.iispl.entity;

import java.math.BigDecimal;
import java.time.Instant;

import com.iispl.enums.Bank;
import com.iispl.enums.Channel;
import com.iispl.enums.DrCr;
import com.iispl.enums.Status;

public final class Transaction {

    private final String txnId;
    private final Bank senderBank;
    private final Bank receiverBank;
    private final Channel channel;
    private final BigDecimal amount;
    private final Instant txnTime;
    private final DrCr drCr;
    private final Status status;

    public Transaction(
            String txnId,
            Bank senderBank,
            Bank receiverBank,
            Channel channel,
            BigDecimal amount,
            Instant txnTime,
            DrCr drCr,
            Status status) {
        this.txnId = txnId;
        this.senderBank = senderBank;
        this.receiverBank = receiverBank;
        this.channel = channel;
        this.amount = amount;
        this.txnTime = txnTime;
        this.drCr = drCr;
        this.status = status;
    }

    public String getTxnId() {
        return txnId;
    }

    public Bank getSenderBank() {
        return senderBank;
    }

    public Bank getReceiverBank() {
        return receiverBank;
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

    @Override
    public String toString() {
        return "Txn{" +
                "txnId='" + txnId + '\'' +
                ", senderBank=" + senderBank +
                ", receiverBank=" + receiverBank +
                ", channel=" + channel +
                ", amount=" + amount +
                ", txnTime=" + txnTime +
                ", drCr=" + drCr +
                ", status=" + status +
                '}';
    }
}
