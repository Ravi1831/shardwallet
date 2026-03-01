# ↩️ Compensation (Rollback) Deep Dive

## What is a Compensating Transaction?

A **compensating transaction** is a deliberate operation that **logically undoes** a previously completed step.

It is NOT an automatic database rollback — it's business logic you write yourself that reverses the effect of what was done.

| Original Action | Compensating Action |
|---|---|
| Debit ₹500 from Wallet A | Credit ₹500 back to Wallet A |
| Credit ₹500 to Wallet B | Debit ₹500 from Wallet B |
| Set transaction status = SUCCESS | Set transaction status back to PENDING (or FAILED) |

---

## When Does Compensation Trigger?

Compensation is triggered when **any step returns `false` or throws an exception**.

The call chain is:

```
TransferSagaServiceImpl.executeTransferSaga()
  │
  └─ if step fails → sagaOrchestrator.failSaga(sagaInstanceId)
                          │
                          ├─ SagaInstance.status = FAILED
                          │
                          └─ compensateSaga(sagaInstanceId)
                                │
                                ├─ SagaInstance.status = COMPENSATING
                                │
                                ├─ Find all steps with status = COMPLETED (sorted)
                                │
                                └─ for each COMPLETED step → compensateStep()
```

---

## The Golden Rule of Compensation

> **Only COMPLETED steps are compensated. FAILED steps are NOT compensated.**

Why? Because if a step **failed**, it means it didn't complete its work — there's nothing to undo.

Example:
```
Step 1 (DEBIT)    → COMPLETED   ← needs compensation
Step 2 (CREDIT)   → FAILED      ← skip (nothing was done)
```
Only Step 1 is compensated.

---

## Compensation is Idempotent (Should Be!)

An important property: **compensating twice should not cause double-damage**.

For example, `DebitSourceWalletStep.compensate()` credits money back to the source wallet. If this is called twice (due to a retry or bug), the wallet would get credited twice — which is bad!

> **Best Practice:** Add idempotency guards, e.g. check if the balance already matches the original before reversing, or use a unique compensation key.

This project doesn't implement idempotency guards yet — this is a potential improvement area.

---

## Compensation Order Matters

Compensation must happen **in reverse order** of execution. Here's why:

```
Step 1: Debit A  (A has $900)
Step 2: Credit B (B has $600)
Step 3: Update Transaction → SUCCESS

If you compensate in FORWARD order:
  Undo Step 1: Credit A back → A has $1000 ✅
  Undo Step 2: Debit B back  → B has $500  ✅
  Undo Step 3: Revert status ✅

If you compensate in FORWARD order but Step 2 undo fails:
  Undo Step 1 ✅
  Undo Step 2 ❌ — B still has $600 but A is already credited back to $1000!
  Total money in system: $1500 instead of $1400 — money created from thin air!
```

Reversing in **reverse order** ensures each step's undo doesn't depend on undoing a later step first.

In this project, `compensateSaga()` fetches completed steps from `SagaStepRepository.findCompletedBySagaInstanceId()`. The sorting of these steps must be in reverse execution order to be safe.

---

## Step-by-Step Compensation in Code

### Step 1: `failSaga()` is called

```java
// SagaOrchestratorImpl.java
public void failSaga(Long sagaInstanceId) {
    sagaInstance.markAsFailed();          // status = FAILED
    sagaInstanceRepository.save(sagaInstance);
    compensateSaga(sagaInstanceId);       // kicks off rollback
}
```

### Step 2: `compensateSaga()` orchestrates

```java
public void compensateSaga(Long sagaInstanceId) {
    sagaInstance.markAsCompensating();    // status = COMPENSATING
    sagaInstanceRepository.save(sagaInstance);

    // Find only the COMPLETED steps
    List<SagaStep> completedSteps = sagaStepRepository.findCompletedBySagaInstanceId(sagaInstanceId);

    for (SagaStep step : completedSteps) {
        compensateStep(sagaInstanceId, step.getStepName());
    }

    sagaInstance.markAsCompensated();     // status = COMPENSATED
    sagaInstanceRepository.save(sagaInstance);
}
```

### Step 3: `compensateStep()` runs each undo

```java
public boolean compensateStep(Long sagaInstanceId, String stepName) {
    // Load the current enriched context from DB
    SagaContext sagaContext = objectMapper.readValue(sagaInstance.getContext(), SagaContext.class);

    sagaStepDB.markAsCompensating();
    sagaStepRepository.save(sagaStepDB);

    boolean success = step.compensate(sagaContext);  // run the undo

    if (success) {
        sagaStepDB.markAsCompensated();
        sagaStepRepository.save(sagaStepDB);

        // Save updated context back to DB (compensation may add new keys)
        String updatedContext = objectMapper.writeValueAsString(sagaContext);
        sagaInstance.setContext(updatedContext);
        sagaInstanceRepository.save(sagaInstance);
    }
}
```

---

## What Each Step's Compensation Does

### `DebitSourceWalletStep.compensate()`

```java
// The debit step CREDITED money out — compensation CREDITS it back
wallet.credit(amount);   // ← reverses the original debit
walletRepository.save(wallet);
context.put(SOURCE_WALLET_BALANCE_AFTER_CREDIT_COMPENSATION, wallet.getBalance());
```

### `CreditDestinationWalletStep.compensate()`

```java
// The credit step CREDITED money in — compensation DEBITS it back
wallet.debit(amount);   // ← reverses the original credit
walletRepository.save(wallet);
context.put(TO_WALLET_BALANCE_AFTER_CREDIT_COMPENSATION, wallet.getBalance());
```

### `UpdateTransactionStatus.compensate()`

```java
// Restore original transaction status from context
TransactionStatus originalStatus =
    TransactionStatus.valueOf(context.getString(ORIGINAL_TRANSACTION_STATUS));
transaction.setStatus(originalStatus);   // ← set back to PENDING
transactionRepository.save(transaction);
```

---

## Compensation Context Keys

These keys are written during `execute()` and read back during `compensate()`:

| Stored By (execute) | Key | Used By (compensate) |
|---|---|---|
| `DebitSourceWalletStep` | `originalSourceWalletBalance` | Audit during compensation |
| `CreditDestinationWalletStep` | `originalWalletBalance` | Audit during compensation |
| `UpdateTransactionStatus` | `originalTransactionStatus` | Restore old status |

This is exactly **why the context is persisted to DB after each step** — so the compensation step can read back values that were recorded during the forward execution.
