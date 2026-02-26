package com.ravi.mds.shardedsagawallet.service.saga.steps;

public class SagaContextKeys {


    // Input / shared
    public static final String TRANSACTION_ID = "transactionId";
    public static final String FROM_WALLET_ID = "fromWalletId";
    public static final String TO_WALLET_ID = "toWalletId";
    public static final String AMOUNT = "amount";

    // Debit step
    public static final String ORIGINAL_SOURCE_WALLET_BALANCE = "originalSourceWalletBalance";
    public static final String SOURCE_WALLET_BALANCE_AFTER_DEBIT = "sourceWalletBalanceAfterDebit";
    public static final String SOURCE_WALLET_BALANCE_AFTER_CREDIT_COMPENSATION = "sourceWalletBalanceAfterCreditCompensation";


    // Credit step
    public static final String ORIGINAL_WALLET_BALANCE = "originalWalletBalance";
    public static final String CREDIT_STEP_SUCCESS = "creditStepSuccess";
    public static final String TO_WALLET_BALANCE_AFTER_CREDIT = "toWalletBalanceAfterCredit";
    public static final String TO_WALLET_BALANCE_AFTER_CREDIT_COMPENSATION = "toWalletBalanceAfterCreditCompensation";

    // Update transaction status step
    public static final String ORIGINAL_TRANSACTION_STATUS = "originalTransactionStatus";
    public static final String TRANSACTION_STATUS_AFTER_UPDATE = "transactionStatusAfterUpdate";

}
