package com.ravi.mds.shardedsagawallet.repository;

import com.ravi.mds.shardedsagawallet.entity.Wallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {

    Optional<Wallet> findByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT w
                    FROM Wallet w
                            WHERE w.id = :id
            """)
    Optional<Wallet> findByIdWithLock(@Param("id") Long id);

    @Modifying
    @Query("""
            UPDATE Wallet w
                SET w.balance = :balance
                    WHERE w.userId = :userId
            """)
    void updateBalanceByUserId(@Param("userId") Long userId, @Param("balance") BigDecimal balance);
}