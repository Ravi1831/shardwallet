package com.ravi.mds.shardedsagawallet.service;

import java.math.BigDecimal;

public interface TransferSagaService {
    Long initiateTransaction(
            Long fromWalletId,
            Long toWalletId,
            BigDecimal amount,
            String description
    );

    void executeTransferSaga(Long sagaInstanceId);
}
