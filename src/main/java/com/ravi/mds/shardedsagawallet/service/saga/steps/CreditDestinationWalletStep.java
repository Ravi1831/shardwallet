package com.ravi.mds.shardedsagawallet.service.saga.steps;

import com.ravi.mds.shardedsagawallet.entity.Wallet;
import com.ravi.mds.shardedsagawallet.exception.WalletNotFoundException;
import com.ravi.mds.shardedsagawallet.repository.WalletRepository;
import com.ravi.mds.shardedsagawallet.service.saga.SagaContext;
import com.ravi.mds.shardedsagawallet.service.saga.SagaStepInterface;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static com.ravi.mds.shardedsagawallet.service.saga.steps.SagaContextKeys.*;
import static com.ravi.mds.shardedsagawallet.service.saga.steps.SagaStepNames.CREDIT_DESTINATION_WALLET_STEP;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreditDestinationWalletStep implements SagaStepInterface {

    private final WalletRepository walletRepository;

    @Override
    @Transactional
    public boolean execute(SagaContext context) {
        // step 1: get the destination wallet context
        Long toWalletId = context.getLong(TO_WALLET_ID);
        BigDecimal amount = context.getBigDecimal(AMOUNT);
        log.info("Crediting destination wallet {} with amount {}", toWalletId, amount);

        // step 2: Fetch the destination wallet from the database with the lock
        Wallet wallet = walletRepository.findByIdWithLock(toWalletId)
                .orElseThrow(
                        () -> new WalletNotFoundException("Wallet not found with the given wallet id " + toWalletId));
        log.info("Wallet fetched with balance {}", wallet.getBalance());
        context.put(ORIGINAL_WALLET_BALANCE, wallet.getBalance());

        // step3: credit the destination wallet
        wallet.credit(amount);
        walletRepository.save(wallet);
        context.put(CREDIT_STEP_SUCCESS, true);
        log.info("Wallet saved with balance {}", wallet.getBalance());
        // step4: Update the context with the changes
        context.put(TO_WALLET_BALANCE_AFTER_CREDIT, wallet.getBalance());
        //TODO once the context is updated in the memory, we need to update in databases
        log.info("credit destination wallet step executed successfully");

        return true;
    }

    @Override
    @Transactional
    public boolean compensate(SagaContext context) {
        // step1 get destination wallet context
        Long toWalletId = context.getLong(TO_WALLET_ID);
        BigDecimal amount = context.getBigDecimal(AMOUNT);
        log.info("Compensation credit of destination wallet {} with amount {} ", toWalletId, amount);

        // step 2 fetch destination wallet with the lock
        Wallet wallet = walletRepository.findByIdWithLock(toWalletId)
                .orElseThrow(
                        () -> new WalletNotFoundException("Wallet not found with the given wallet id " + toWalletId));
        log.info("wallet fetched with balance {}", wallet.getBalance());

        // step3 credit the destination wallet
        wallet.debit(amount);
        walletRepository.save(wallet);
        log.info("wallet saved with balance {}", wallet.getBalance());
        // step4: Update the context with the changes
        context.put(TO_WALLET_BALANCE_AFTER_CREDIT_COMPENSATION, wallet.getBalance());
        //TODO once the context is updated in the memory, we need to update in databases
        log.info("Credit compensation of wallet step executed Successfully");
        return true;
    }

    @Override
    public String getStepName() {
        return CREDIT_DESTINATION_WALLET_STEP.name();
    }
}
