package com.ravi.mds.shardedsagawallet.service;

import com.ravi.mds.shardedsagawallet.entity.Transaction;
import com.ravi.mds.shardedsagawallet.entity.TransactionStatus;

import java.math.BigDecimal;
import java.util.List;

public interface TransactionService {


    Transaction createNewTransaction(
            Long fromWalletId,
            Long toWalletId,
            BigDecimal amount,
            String description
    );

    public Transaction getTransactionById(Long id);
    public List<Transaction> getTransactionByWalletId(Long walletId);
    public List<Transaction> getTransactionByFromWalletId(Long fromWalletId);
    public List<Transaction> getTransactionByToWalletId(Long toWalletId);
    public List<Transaction> getTransactionBySagaInstanceId(Long sagaInstanceId);
    public List<Transaction> getTransactionByStatus(TransactionStatus status);

    void updateTransactionWithSagaInstanceId(Long transactionId, Long sagaInstanceId);
}
