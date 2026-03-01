# 🧱 Code Components Explained

This document explains every class in the saga package and what role it plays.

---

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    HTTP Layer                               │
│           TransactionController                             │
└────────────────────────┬────────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────────┐
│                   Service Layer                             │
│           TransferSagaServiceImpl                           │
│   (creates context → calls orchestrator → drives the saga)  │
└────────────────────────┬────────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────────┐
│                  Orchestrator Layer                         │
│              SagaOrchestratorImpl                           │
│   (loads context from DB, calls steps, saves results to DB) │
└────┬──────────────────┬──────────────────┬──────────────────┘
     │                  │                  │
┌────▼───────┐  ┌───────▼──────┐  ┌───────▼──────────────┐
│  Debit     │  │  Credit      │  │  UpdateTransaction   │
│  Source    │  │  Destination │  │  Status              │
│  Wallet    │  │  Wallet      │  │                      │
│  Step      │  │  Step        │  │  Step                │
└────────────┘  └──────────────┘  └──────────────────────┘
     │                  │                  │
     └──────────────────┴──────────────────┘
                         │
              WalletRepository / TransactionRepository
```

---

## Component Reference

### 1. `TransferSagaServiceImpl` — The Entry Point

**File:** `service/TransferSagaServiceImpl.java`

This is what gets called when a user makes a transfer request. It does 4 things:
1. Creates a `Transaction` record in DB (status = PENDING)
2. Builds the initial `SagaContext` (a map of data the saga will need)
3. Calls `startSaga()` to save the `SagaInstance` to DB
4. Calls `executeTransferSaga()` which loops through all steps

```java
// Drives the saga step by step
for (SagaStepNames step : transferMoneySagaSteps) {
    boolean success = sagaOrchestrator.executeStep(sagaInstanceId, step.name());
    if (!success) {
        sagaOrchestrator.failSaga(sagaInstanceId);  // triggers compensation
        return;
    }
}
sagaOrchestrator.completeSaga(sagaInstanceId);
```

**The step order is defined here as a static list:**
```java
public static final List<SagaStepNames> transferMoneySagaSteps = List.of(
    SagaStepNames.DEBIT_SOURCE_WALLET_STEP,
    SagaStepNames.CREDIT_DESTINATION_WALLET_STEP,
    SagaStepNames.UPDATE_TRANSACTION_STATUS_STEP
);
```

---

### 2. `SagaContext` — The Shared Memory

**File:** `service/saga/SagaContext.java`

Think of `SagaContext` as a **backpack** that travels with the saga. Each step can read from and write to it.

It is a simple wrapper around a `Map<String, Object>`:

```java
// Writing to context (inside a step):
context.put("toWalletBalanceAfterCredit", wallet.getBalance());

// Reading from context (inside the next step):
Long transactionId = context.getLong("transactionId");
```

**Crucially**, after each step the orchestrator serializes this map to JSON and saves it to the `saga_instance.context` column. This means:
- Context **survives crashes** (stored in DB, not just RAM)
- Later steps can read what earlier steps wrote
- On retry, the full state is reloaded from the DB

**All context key names are constants in `SagaContextKeys`:**

| Key | Set By | Read By |
|---|---|---|
| `transactionId` | `TransferSagaServiceImpl` | `UpdateTransactionStatus` |
| `fromWalletId` | `TransferSagaServiceImpl` | `DebitSourceWalletStep` |
| `toWalletId` | `TransferSagaServiceImpl` | `CreditDestinationWalletStep` |
| `amount` | `TransferSagaServiceImpl` | Both wallet steps |
| `originalSourceWalletBalance` | `DebitSourceWalletStep.execute()` | `DebitSourceWalletStep.compensate()` |
| `sourceWalletBalanceAfterDebit` | `DebitSourceWalletStep.execute()` | Audit/logging |
| `originalWalletBalance` | `CreditDestinationWalletStep.execute()` | `CreditDestinationWalletStep.compensate()` |
| `toWalletBalanceAfterCredit` | `CreditDestinationWalletStep.execute()` | Audit/logging |
| `creditStepSuccess` | `CreditDestinationWalletStep.execute()` | Audit/logging |
| `originalTransactionStatus` | `UpdateTransactionStatus.execute()` | `UpdateTransactionStatus.compensate()` |

---

### 3. `SagaOrchestrator` / `SagaOrchestratorImpl` — The Brain

**File:** `service/saga/SagaOrchestratorImpl.java`

This is the **central controller**. It knows nothing about business logic (wallets, money) — it only knows how to run steps and save state. It has these key methods:

| Method | What it does |
|---|---|
| `startSaga(context)` | Serializes the initial context to JSON, creates `SagaInstance` in DB |
| `executeStep(sagaId, stepName)` | Loads context from DB → runs step → saves updated context + step status to DB |
| `compensateStep(sagaId, stepName)` | Loads context from DB → runs compensation → saves updated context + step status to DB |
| `completeSaga(sagaId)` | Marks `SagaInstance` as COMPLETED |
| `failSaga(sagaId)` | Marks as FAILED, then triggers `compensateSaga()` |
| `compensateSaga(sagaId)` | Finds all COMPLETED steps → compensates each one in reverse |

**Critical pattern inside `executeStep()`:**
```java
// 1. Load context from DB
SagaContext sagaContext = objectMapper.readValue(sagaInstance.getContext(), SagaContext.class);

// 2. Run the step (step mutates sagaContext in memory)
boolean success = step.execute(sagaContext);

// 3. Save updated context back to DB  ← THIS IS THE KEY!
String updatedContextJson = objectMapper.writeValueAsString(sagaContext);
sagaInstance.setContext(updatedContextJson);
sagaInstanceRepository.save(sagaInstance);
```

---

### 4. `SagaStepInterface` — The Step Contract

**File:** `service/saga/SagaStepInterface.java`

Every saga step **must** implement this interface:

```java
public interface SagaStepInterface {
    boolean execute(SagaContext context);     // do the work
    boolean compensate(SagaContext context);  // undo the work
    String getStepName();                     // unique step name
}
```

- `execute()` returns `true` for success, `false` for failure
- `compensate()` must reliably undo what `execute()` did

---

### 5. The Steps — The Business Logic

#### `DebitSourceWalletStep`
| Method | What it does |
|---|---|
| `execute()` | Fetches source wallet with DB lock, debits amount, saves wallet |
| `compensate()` | Fetches source wallet with DB lock, **credits** amount back (refund) |

#### `CreditDestinationWalletStep`
| Method | What it does |
|---|---|
| `execute()` | Fetches destination wallet with DB lock, credits amount, saves wallet |
| `compensate()` | Fetches destination wallet with DB lock, **debits** amount back (reverse credit) |

#### `UpdateTransactionStatus`
| Method | What it does |
|---|---|
| `execute()` | Fetches Transaction, sets status = SUCCESS |
| `compensate()` | Fetches Transaction, restores the original status from context |

> **Why DB lock?** Each wallet step uses `findByIdWithLock()` which issues a `SELECT ... FOR UPDATE`. This prevents two concurrent sagas from modifying the same wallet simultaneously, avoiding race conditions.

---

### 6. `SagaStepFactory` — The Step Registry

**File:** `service/saga/steps/SagaStepFactory.java`

At startup, Spring injects all beans implementing `SagaStepInterface`. The factory builds a map from step name → step bean, so the orchestrator can look up the right step by name.

```java
// At startup (auto-wired by Spring):
stepByName = {
  "DEBIT_SOURCE_WALLET_STEP"         → DebitSourceWalletStep bean,
  "CREDIT_DESTINATION_WALLET_STEP"   → CreditDestinationWalletStep bean,
  "UPDATE_TRANSACTION_STATUS_STEP"   → UpdateTransactionStatus bean
}

// Usage:
SagaStepInterface step = sagaStepFactory.getSagaStep("DEBIT_SOURCE_WALLET_STEP");
```

This makes adding a **new step** as simple as creating a new class implementing `SagaStepInterface` — no changes needed to the factory or orchestrator.

---

### 7. `SagaStepNames` — The Step Name Registry

**File:** `service/saga/steps/SagaStepNames.java`

A simple enum listing all step names. Used to prevent typos when wiring steps together.

```java
public enum SagaStepNames {
    DEBIT_SOURCE_WALLET_STEP,
    CREDIT_DESTINATION_WALLET_STEP,
    UPDATE_TRANSACTION_STATUS_STEP
}
```
