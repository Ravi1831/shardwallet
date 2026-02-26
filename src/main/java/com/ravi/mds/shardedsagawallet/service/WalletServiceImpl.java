package com.ravi.mds.shardedsagawallet.service;

import com.ravi.mds.shardedsagawallet.entity.Wallet;
import com.ravi.mds.shardedsagawallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService{

    private final WalletRepository walletRepository;

    @Override
    @Transactional
    public Wallet ceateWallet(Long userId) {
        Wallet wallet = Wallet.builder()
                .userId(userId)
                .isActive(true)
                .balance(BigDecimal.ZERO)
                .build();
        log.info("creating wallet with the data {}",wallet);
        Wallet savedWallet = walletRepository.save(wallet);
        log.info("wallet created with id {}",savedWallet.getId());
        return null;
    }

    @Override
    public Wallet getWalletById(Long id) {
        return walletRepository.findById(id)
                .orElseThrow(()-> new RuntimeException("Wallet not found with the id "+id));
    }

    @Override
    public List<Wallet> getWalletByUserId(Long userId) {
        return walletRepository.findByUserId(userId);
    }

    @Override
    @Transactional
    public void debit(Long walletId, BigDecimal amount) {
        log.info("Debiting {} from wallet {}",amount,walletId);
        Wallet walletById = getWalletById(walletId);
        walletById.debit(amount);
        walletRepository.save(walletById);
        log.info("Debit successful for wallet {}",walletById);
    }

    @Override
    @Transactional
    public void credit(Long walletId, BigDecimal amount) {
        log.info("credit {} to wallet {}",amount,walletId);
        Wallet walletById = getWalletById(walletId);
        walletById.credit(amount);
        walletRepository.save(walletById);
        log.info("credit successful for wallet {}",walletById);
    }

    @Override
    public BigDecimal getWalletBalance(Long walletId){
        log.info("Getting balance for wallet {}",walletId);
        BigDecimal balance = getWalletById(walletId).getBalance();
        log.info("Balance for wallet {} is {}",walletId,balance);
        return balance;
    }

}
