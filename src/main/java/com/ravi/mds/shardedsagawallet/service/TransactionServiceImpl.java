package com.ravi.mds.shardedsagawallet.service;

import com.ravi.mds.shardedsagawallet.entity.Transaction;
import com.ravi.mds.shardedsagawallet.entity.TransactionStatus;
import com.ravi.mds.shardedsagawallet.repository.TransactionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService{
    private final TransactionRepository transactionRepository;

    @Override
    @Transactional
    public Transaction createNewTransaction(
            Long fromWalletId, Long toWalletId,
            BigDecimal amount, String description
    ){
        log.info("Creating transaction from wallet {} to wallet {} with amount {} and description {}",fromWalletId,toWalletId,amount,description);
        Transaction transaction = Transaction.builder()
                .fromWalletId(fromWalletId)
                .toWalletId(toWalletId)
                .amount(amount)
                .description(description)
                .build();
        Transaction savedTransaction = transactionRepository.save(transaction);
        log.info("Transaction created with id {} ",savedTransaction.getId());
        return savedTransaction;
    }

    @Override
    public Transaction getTransactionById(Long id) {
        return transactionRepository.findById(id).orElseThrow(()-> new RuntimeException("Transaction not found"));
    }

    @Override
    public List<Transaction> getTransactionByWalletId(Long walletId) {
        return transactionRepository.findByToWalletId(walletId);
    }

    @Override
    public List<Transaction> getTransactionByFromWalletId(Long fromWalletId) {
        return transactionRepository.findByFromWalletId(fromWalletId);
    }

    @Override
    public List<Transaction> getTransactionByToWalletId(Long toWalletId) {
        return transactionRepository.findByToWalletId(toWalletId);
    }

    @Override
    public List<Transaction> getTransactionBySagaInstanceId(Long sagaInstanceId) {
        return transactionRepository.findBySagaInstanceId(sagaInstanceId);
    }

    @Override
    public List<Transaction> getTransactionByStatus(TransactionStatus status) {
        return transactionRepository.findByStatus(status);
    }

    @Override
    public void updateTransactionWithSagaInstanceId(Long transactionId, Long sagaInstanceId){
        Transaction transaction = getTransactionById(transactionId);
        transaction.setSagaInstanceId(sagaInstanceId);
        transactionRepository.save(transaction);
        log.info("Transaction updated with saga instance id {} ",transactionId);
    }
}
