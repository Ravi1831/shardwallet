package com.ravi.mds.shardedsagawallet.service;

import com.ravi.mds.shardedsagawallet.entity.Transaction;
import com.ravi.mds.shardedsagawallet.service.saga.SagaContext;
import com.ravi.mds.shardedsagawallet.service.saga.SagaOrchestrator;
import com.ravi.mds.shardedsagawallet.service.saga.steps.SagaStepNames;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

import static com.ravi.mds.shardedsagawallet.service.saga.steps.SagaStepFactory.transferMoneySagaSteps;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransferSagaServiceImpl implements TransferSagaService{

    private final TransactionService transactionService;
    private final SagaOrchestrator sagaOrchestrator;


    @Override
    public Long initiateTransaction(
            Long fromWalletId,
            Long toWalletId,
            BigDecimal amount,
            String description
    ){
        log.info("Initiating transfer from wallet {} to wallet {} with amount {} and description {}",fromWalletId,toWalletId,amount,description);
        Transaction newTransaction = transactionService.createNewTransaction(fromWalletId, toWalletId, amount, description);
        SagaContext sagaContext = SagaContext.builder()
                .data(Map.ofEntries(
                        Map.entry("transactionId",newTransaction.getId()),
                        Map.entry("fromWalletId",newTransaction.getFromWalletId()),
                        Map.entry("toWalletId",newTransaction.getToWalletId()),
                        Map.entry("amount",newTransaction.getAmount()),
                        Map.entry("description",newTransaction.getDescription())
                ))
                .build();

        Long sagaInstanceId = sagaOrchestrator.startSaga(sagaContext);
        log.info("Saga instance created with id {}",sagaInstanceId);
        transactionService.updateTransactionWithSagaInstanceId(newTransaction.getId(), sagaInstanceId);
        executeTransferSaga(sagaInstanceId);
        return sagaInstanceId;
    }

    @Override
    public void executeTransferSaga(Long sagaInstanceId){
        log.info("Executing transfer saga with id {}",sagaInstanceId);

        try{
            for (SagaStepNames step : transferMoneySagaSteps){
                boolean success = sagaOrchestrator.executeStep(sagaInstanceId, step.name());
                if (!success){
                    log.error("Failed to execute step {}",step.name());
                    sagaOrchestrator.failSaga(sagaInstanceId);
                    return;
                }
            }
            sagaOrchestrator.completeSaga(sagaInstanceId);
            log.info("Transfer saga completed with id {}",sagaInstanceId);
        }catch (Exception e){
            log.error("Failed to execute transfer saga with id {}",sagaInstanceId,e);
            sagaOrchestrator.failSaga(sagaInstanceId);
            throw e;
        }
    }
}
