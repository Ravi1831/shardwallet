# 💸 The Money Transfer Saga — Step by Step

## Overview

When a user calls `POST /api/v1/transactions/transfer`, the following saga is triggered:

```
Step 1 → DEBIT_SOURCE_WALLET_STEP
Step 2 → CREDIT_DESTINATION_WALLET_STEP
Step 3 → UPDATE_TRANSACTION_STATUS_STEP
```

Each step must succeed before the next begins. If any step fails, **all completed steps are compensated (undone) in reverse order**.

---

## Happy Path (Everything Succeeds)

```
Client
  │
  ▼
TransactionController.transfer()
  │
  ▼
TransferSagaServiceImpl.initiateTransaction()
  │
  ├─ 1. Create a Transaction record (status = PENDING)
  ├─ 2. Build SagaContext (transactionId, fromWalletId, toWalletId, amount)
  ├─ 3. SagaOrchestrator.startSaga()  → saves SagaInstance to DB (status = STARTED)
  │
  ▼
TransferSagaServiceImpl.executeTransferSaga()
  │
  ├─ executeStep("DEBIT_SOURCE_WALLET_STEP")
  │     │
  │     ├─ Load SagaContext from DB
  │     ├─ Mark step RUNNING in DB
  │     ├─ DebitSourceWalletStep.execute()
  │     │     ├─ Fetch source wallet (with DB lock)
  │     │     ├─ wallet.debit(amount)       ← wallet DB updated ✅
  │     │     └─ context.put(sourceBalanceAfterDebit, ...)
  │     ├─ Mark step COMPLETED in DB
  │     └─ Save updated SagaContext JSON back to DB ✅
  │
  ├─ executeStep("CREDIT_DESTINATION_WALLET_STEP")
  │     │
  │     ├─ Load SagaContext from DB
  │     ├─ Mark step RUNNING in DB
  │     ├─ CreditDestinationWalletStep.execute()
  │     │     ├─ Fetch destination wallet (with DB lock)
  │     │     ├─ wallet.credit(amount)      ← wallet DB updated ✅
  │     │     └─ context.put(toWalletBalanceAfterCredit, ...)
  │     ├─ Mark step COMPLETED in DB
  │     └─ Save updated SagaContext JSON back to DB ✅
  │
  ├─ executeStep("UPDATE_TRANSACTION_STATUS_STEP")
  │     │
  │     ├─ Load SagaContext from DB
  │     ├─ Mark step RUNNING in DB
  │     ├─ UpdateTransactionStatus.execute()
  │     │     ├─ Fetch Transaction by transactionId (from context)
  │     │     ├─ transaction.setStatus(SUCCESS)  ← transaction DB updated ✅
  │     │     └─ context.put(transactionStatusAfterUpdate, SUCCESS)
  │     ├─ Mark step COMPLETED in DB
  │     └─ Save updated SagaContext JSON back to DB ✅
  │
  └─ SagaOrchestrator.completeSaga()  → SagaInstance status = COMPLETED ✅
```

---

## Failure Path (Step 2 Fails)

Imagine `CreditDestinationWalletStep` throws an exception (wallet not found, DB error, etc.):

```
executeStep("DEBIT_SOURCE_WALLET_STEP")       ✅  COMPLETED
executeStep("CREDIT_DESTINATION_WALLET_STEP") ❌  FAILED

  └─ TransferSagaServiceImpl catches failure
       │
       └─ sagaOrchestrator.failSaga(sagaInstanceId)
               │
               ├─ SagaInstance.status = FAILED
               │
               └─ compensateSaga(sagaInstanceId)
                     │
                     ├─ Find all COMPLETED steps in reverse
                     │     → [DEBIT_SOURCE_WALLET_STEP]
                     │
                     └─ compensateStep("DEBIT_SOURCE_WALLET_STEP")
                             │
                             ├─ Load SagaContext from DB
                             ├─ Mark step COMPENSATING
                             ├─ DebitSourceWalletStep.compensate()
                             │     ├─ Fetch source wallet (with DB lock)
                             │     ├─ wallet.credit(amount)   ← money returned! ✅
                             │     └─ context.put(sourceBalanceAfterCompensation, ...)
                             ├─ Mark step COMPENSATED
                             └─ Save updated SagaContext to DB ✅
                     │
                     └─ SagaInstance.status = COMPENSATED
```

> **Key point**: Money is never lost. If the credit fails, the debit is automatically reversed.

---

## Saga Status State Machine

```
                    ┌─────────┐
                    │ STARTED │
                    └────┬────┘
                         │ first step begins
                         ▼
                    ┌─────────┐
                    │ RUNNING │ ◄──── each step completes
                    └────┬────┘
               ┌─────────┴──────────┐
               │ all steps ok       │ any step fails
               ▼                    ▼
         ┌───────────┐        ┌────────┐
         │ COMPLETED │        │ FAILED │
         └───────────┘        └───┬────┘
                                  │ compensation starts
                                  ▼
                           ┌─────────────┐
                           │ COMPENSATING│
                           └──────┬──────┘
                                  │ all compensations done
                                  ▼
                           ┌─────────────┐
                           │ COMPENSATED │
                           └─────────────┘
```

---

## Step Status State Machine

```
PENDING → RUNNING → COMPLETED
                 ↘ FAILED

COMPLETED → COMPENSATING → COMPENSATED
                         ↘ FAILED
```
