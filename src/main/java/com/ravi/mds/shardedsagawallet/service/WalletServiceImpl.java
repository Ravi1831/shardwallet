package com.ravi.mds.shardedsagawallet.service;

import com.ravi.mds.shardedsagawallet.entity.Wallet;
import com.ravi.mds.shardedsagawallet.exception.InsufficientBalanceException;
import com.ravi.mds.shardedsagawallet.exception.WalletInactiveException;
import com.ravi.mds.shardedsagawallet.exception.WalletNotFoundException;
import com.ravi.mds.shardedsagawallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Slf4j
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;

    @Override
    @Transactional
    public Wallet ceateWallet(Long userId) {
        log.info("checking if the wallet exist for the user by userId ");
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseGet(() -> {
                    log.info("wallet not found. creating new wallet");
                    return Wallet.builder()
                            .userId(userId)
                            .isActive(true)
                            .balance(BigDecimal.ZERO)
                            .build();
                });
        log.info("if wallet exisit {}", wallet);

        if (Boolean.FALSE.equals(wallet.getIsActive()) || wallet.getId() == null) {
            log.info("wallet exists but inactive. activating it.");
            wallet.setIsActive(true);
            wallet = walletRepository.save(wallet);
        }

        log.info("creating wallet with the data {}", wallet);

        log.info("wallet ready with id {}", wallet.getId());
        return wallet;
    }

    // getting wallet by wallet id
    @Override
    public Wallet getWalletById(Long id) {
        return walletRepository.findById(id)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found with the id " + id));
    }

    // getting wallet by userId
    @Override
    public Wallet getWalletByUserId(Long userId) {
        log.info("Getting wallet by user id {}", userId);
        return walletRepository.findByUserId(userId)
                .orElseThrow(
                        () -> new WalletNotFoundException("wallet does not exist for the user!! please create one"));
    }

    @Override
    @Transactional
    public void debit(Long userId, BigDecimal amount) {
        log.info("Debiting {} from wallet {}", amount, userId);
        Wallet wallet = getWalletByUserId(userId);
        if (Boolean.FALSE.equals(wallet.getIsActive())) {
            throw new WalletInactiveException("Wallet is inactive please reactive it");
        }
        if (wallet.hasSufficientBalance(amount)) {
            throw new InsufficientBalanceException("Insufficient balance to complete the debit");
        }
        walletRepository.updateBalanceByUserId(userId, wallet.getBalance().subtract(amount));
        log.info("Debit successful for wallet {}", wallet);
    }

    @Override
    @Transactional
    public void credit(Long userId, BigDecimal amount) {
        log.info("credit {} to wallet {}", amount, userId);
        Wallet wallet = getWalletByUserId(userId);
        if (Boolean.FALSE.equals(wallet.getIsActive())) {
            throw new WalletInactiveException("Wallet is inactive please reactive it");
        }
        walletRepository.updateBalanceByUserId(userId, wallet.getBalance().add(amount));
        log.info("credit successful for wallet {}", wallet);
    }

    // getting balance by userId
    @Override
    public BigDecimal getWalletBalance(Long userId) {
        log.info("Getting balance for userId {}", userId);
        BigDecimal balance = getWalletByUserId(userId).getBalance();
        log.info("Balance for userId {} is {}", userId, balance);
        return balance;
    }

}
