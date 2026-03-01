package com.ravi.mds.shardedsagawallet.service.saga.steps;

import com.ravi.mds.shardedsagawallet.entity.Transaction;
import com.ravi.mds.shardedsagawallet.entity.TransactionStatus;
import com.ravi.mds.shardedsagawallet.repository.TransactionRepository;
import com.ravi.mds.shardedsagawallet.service.saga.SagaContext;
import com.ravi.mds.shardedsagawallet.service.saga.SagaStepInterface;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import static com.ravi.mds.shardedsagawallet.service.saga.steps.SagaContextKeys.*;
import static com.ravi.mds.shardedsagawallet.service.saga.steps.SagaStepNames.UPDATE_TRANSACTION_STATUS_STEP;

@Service
@RequiredArgsConstructor
@Slf4j
public class UpdateTransactionStatus implements SagaStepInterface {

    private final TransactionRepository transactionRepository;

    @Override
    public boolean execute(SagaContext context) {
        Long transactionId = context.getLong(TRANSACTION_ID);
        log.info("updating transaction status for transaction {}", transactionId);
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found with id " + transactionId));
        context.put(ORIGINAL_TRANSACTION_STATUS, transaction.getStatus());

        /*
         * todo: we have to fetch the transaction status form the target and update it
         */
        // context.get("from")
        transaction.setStatus(TransactionStatus.SUCCESS);
        transactionRepository.save(transaction);

        log.info("Transaction status updated for transaction {}", transactionId);
        context.put(TRANSACTION_STATUS_AFTER_UPDATE, transaction.getStatus());
        log.info("Update transaction status step executed successfully");

        return true;
    }

    @Override
    public boolean compensate(SagaContext context) {
        Long transactionId = context.getLong(TRANSACTION_ID);
        TransactionStatus originalTransactionStatus = TransactionStatus
                .valueOf(context.getString(ORIGINAL_TRANSACTION_STATUS));
        log.info("Compensating transaction status for transaction {}", transactionId);

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found with id " + transactionId));

        transaction.setStatus(originalTransactionStatus);
        transactionRepository.save(transaction);

        log.info("Transaction status compensated for transaction {}", transactionId);

        return true;
    }

    @Override
    public String getStepName() {
        return UPDATE_TRANSACTION_STATUS_STEP.name();
    }
}
