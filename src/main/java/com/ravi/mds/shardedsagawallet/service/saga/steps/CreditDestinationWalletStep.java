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
public class CreditDestinationWalletStep implements SagaStep {

    private final WalletRepository walletRepository;


    @Override
    @Transactional
    public boolean execute(SagaContext context) {
        //step 1: get the destination wallet context
        Long toWalletId = context.getLong("toWalletId");
        BigDecimal amount = context.getBigDecimal("amount");
        log.info("Crediting destination wallet {} with amount {}", toWalletId, amount);

        //step 2: Fetch the destination wallet from the database  with the lock
        Wallet wallet = walletRepository.findByIdWithLock(toWalletId)
                .orElseThrow(() -> new RuntimeException("Wallet not found with the given wallet id " + toWalletId));
        log.info("Wallet fetched with balance {}", wallet.getBalance());
        context.put("originalWalletBalance", wallet.getBalance());

        //step3:  credit the destination wallet
        wallet.credit(amount);
        walletRepository.save(wallet);
        log.info("Wallet saved with balance {}", wallet.getBalance());
        //step4: Update the context with the changes
        context.put("toWalletBalanceAfterCredit", wallet.getBalance());
        log.info("credit destination wallet step executed successfully");

        return true;
    }

    @Override
    @Transactional
    public boolean compensate(SagaContext context) {
        //step1 get destination wallet context
        Long toWalletId = context.getLong("toWalletId");
        BigDecimal amount = context.getBigDecimal("amount");
        log.info("Compensation credit of destination wallet {} with amount {} ", toWalletId, amount);

        //step 2 fetch destination wallet with the lock
        Wallet wallet = walletRepository.findByIdWithLock(toWalletId)
                .orElseThrow(() -> new RuntimeException("Wallet not found with the given wallet id " + toWalletId));
        log.info("wallet fetched with balance {}", wallet.getBalance());

        //step3 credit the destination wallet
        wallet.debit(amount);
        walletRepository.save(wallet);
        log.info("wallet saved with balance {}", wallet.getBalance());
        //step4: Update the context with the changes
        context.put("toWalletBalanceAfterCreditCompensation", wallet.getBalance());
        log.info("Credit compensation of wallet step executed Successfully");
        return true;
    }

    @Override
    public String getStepName() {
        return "CreditDestinationWalletStep";
    }
}
