package com.ravi.mds.shardedsagawallet.repository;

import com.ravi.mds.shardedsagawallet.entity.Transaction;
import com.ravi.mds.shardedsagawallet.entity.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByFromWalletId(Long fromWalletId); //all debit transaction

    List<Transaction> findByToWalletId(Long walletId); // all credit transaction

    @Query("""
            SELECT t
            FROM Transaction t
            WHERE t.fromWalletId = :walletId
            OR
            t.toWalletId = :walletId
    """)
    List<Transaction> findByWalletId(@Param("walletId") Long walletId);

    List<Transaction> findByStatus(TransactionStatus status );

    List<Transaction> findBySagaInstanceId(Long sagaInstanceId);


}