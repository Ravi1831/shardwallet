package com.ravi.mds.shardedsagawallet.service;

import com.ravi.mds.shardedsagawallet.entity.Wallet;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

public interface WalletService {

    public Wallet ceateWallet(Long userId);

    public Wallet getWalletById(Long id);

    public List<Wallet> getWalletByUserId(Long userId);

    public void debit(Long walletId, BigDecimal amount);

    void credit(Long walletId, BigDecimal amount);

    BigDecimal getWalletBalance(Long walletId);
}
