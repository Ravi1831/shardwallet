package com.ravi.mds.shardedsagawallet.service.saga.steps;

import com.ravi.mds.shardedsagawallet.entity.Wallet;
import com.ravi.mds.shardedsagawallet.repository.WalletRepository;
import com.ravi.mds.shardedsagawallet.service.saga.SagaContext;
import com.ravi.mds.shardedsagawallet.service.saga.SagaStep;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class DebitSourceWalletStep implements SagaStep {

    private final WalletRepository walletRepository;

    @Override
    public boolean execute(SagaContext context) {
    Long fromWalletId = context.getLong("fromWalletId");
        BigDecimal amount = context.getBigDecimal("amount");
        log.info("Debiting source wallet {} with amount {}", fromWalletId,amount);

        Wallet wallet = walletRepository.findByIdWithLock(fromWalletId)
                .orElseThrow(() -> new RuntimeException("Wallet not found with id " + fromWalletId));

        log.info("Wallet fetched with balance {}",wallet.getBalance());
        context.put("originalSourceWalletBalance",wallet.getBalance());

        wallet.debit(amount);
        walletRepository.save(wallet);
        log.info("wallet saved with balance {}",wallet.getBalance());
        context.put("sourceWalletBalancerDebit",wallet.getBalance());
        return true;
    }

    @Override
    @Transactional
    public boolean compensate(SagaContext context) {
        Long fromWalletId = context.getLong("fromWalletId");
        BigDecimal amount = context.getBigDecimal("amount");
        log.info("Compensating source wallet {} with amount {}", fromWalletId,amount);
        Wallet wallet = walletRepository.findByIdWithLock(fromWalletId)
                .orElseThrow(() -> new RuntimeException("Wallet not found with id " + fromWalletId));

        wallet.credit(amount);
        walletRepository.save(wallet);
        log.info("Wallet saved with balance {}",wallet.getBalance());
        context.put("sourceWalletBalanceBeforeCreditCompensation",wallet.getBalance());
        log.info("Compensating source wallet step executed successfully");
        return true;
    }

    @Override
    public String getStepName() {
        return "DebitSourceWalletStep";
    }
}
