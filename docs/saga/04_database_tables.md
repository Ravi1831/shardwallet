# 🗄️ Database Tables

The saga uses **4 database tables**. Here's what each stores.

---

## Table Overview

```
┌─────────────────┐         ┌─────────────────┐
│  saga_instance  │ 1 ───── N│   saga_step     │
│ (one per saga)  │         │ (one per step)  │
└─────────────────┘         └─────────────────┘

┌─────────────────┐         ┌─────────────────┐
│   transaction   │         │     wallet      │
│ (business data) │         │ (accounts)      │
└─────────────────┘         └─────────────────┘
```

---

## `saga_instance` Table

Represents **one complete saga run**. Created when the saga starts.

| Column | Type | Description |
|---|---|---|
| `id` | BIGINT (PK) | Auto-generated unique ID |
| `status` | VARCHAR | Current saga state (see below) |
| `context` | JSON | The `SagaContext` — the entire shared state map |
| `current_step` | VARCHAR | Name of the last completed step |
| `created_at` | TIMESTAMP | When the saga started |
| `updated_at` | TIMESTAMP | Last modified time |

**Status values:**

| Status | Meaning |
|---|---|
| `STARTED` | Saga was created, not yet executing |
| `RUNNING` | At least one step has been executed |
| `COMPLETED` | All steps finished successfully |
| `FAILED` | One step failed, compensation about to start |
| `COMPENSATING` | Compensation is in progress |
| `COMPENSATED` | All compensations done successfully |

**Example row during execution:**
```json
{
  "id": 42,
  "status": "RUNNING",
  "current_step": "DEBIT_SOURCE_WALLET_STEP",
  "context": {
    "data": {
      "transactionId": 101,
      "fromWalletId": 1,
      "toWalletId": 2,
      "amount": 500.00,
      "originalSourceWalletBalance": 1000.00,
      "sourceWalletBalanceAfterDebit": 500.00
    }
  }
}
```

---

## `saga_step` Table

Represents **one step within a saga**. There is one row per step per saga run.

| Column | Type | Description |
|---|---|---|
| `id` | BIGINT (PK) | Auto-generated unique ID |
| `saga_instance_id` | BIGINT (FK) | Links to `saga_instance.id` |
| `step_name` | VARCHAR | e.g. `DEBIT_SOURCE_WALLET_STEP` |
| `status` | VARCHAR | Current step state (see below) |
| `error_message` | VARCHAR | Populated on failure |
| `step_data` | JSON | Optional step-specific data |
| `created_at` | TIMESTAMP | When step was created |
| `updated_at` | TIMESTAMP | Last modified time |

**Status values:**

| Status | Meaning |
|---|---|
| `PENDING` | Step exists in DB, not yet started |
| `RUNNING` | Step is currently executing |
| `COMPLETED` | Step finished successfully |
| `FAILED` | Step threw an error |
| `COMPENSATING` | Compensation is running |
| `COMPENSATED` | Successfully compensated (undone) |
| `SKIPPED` | Step was skipped (not used yet) |

**Example rows for a successful saga:**
```
saga_instance_id | step_name                        | status
─────────────────────────────────────────────────────────────
42               | DEBIT_SOURCE_WALLET_STEP         | COMPLETED
42               | CREDIT_DESTINATION_WALLET_STEP   | COMPLETED
42               | UPDATE_TRANSACTION_STATUS_STEP   | COMPLETED
```

**Example rows after Step 2 failed and compensation ran:**
```
saga_instance_id | step_name                        | status
─────────────────────────────────────────────────────────────
43               | DEBIT_SOURCE_WALLET_STEP         | COMPENSATED   ← undone
43               | CREDIT_DESTINATION_WALLET_STEP   | FAILED        ← failed here
```

---

## `transaction` Table

A normal business record. Created before the saga starts.

| Column | Type | Description |
|---|---|---|
| `id` | BIGINT (PK) | Auto-generated |
| `from_wallet_id` | BIGINT | Source wallet |
| `to_wallet_id` | BIGINT | Destination wallet |
| `amount` | DECIMAL | Transfer amount |
| `status` | VARCHAR | PENDING → SUCCESS / FAILED |
| `saga_instance_id` | BIGINT | The saga that handles this transaction |
| `description` | VARCHAR | Optional note |

**Status flow:**

```
PENDING ──────► SUCCESS   (when UpdateTransactionStatus step completes)
       └──────► FAILED    (if saga fails and UpdateTransactionStatus compensates)
```

---

## `wallet` Table

Standard wallet table — updated directly by the debit/credit steps.

| Column | Type | Description |
|---|---|---|
| `id` | BIGINT (PK) | Wallet ID |
| `user_id` | BIGINT | Owner |
| `balance` | DECIMAL | Current balance |
| `status` | VARCHAR | ACTIVE / INACTIVE |

> **Why SELECT FOR UPDATE?** The wallet steps use `findByIdWithLock()`. This issues a database-level row lock so two concurrent sagas can't read and modify the same wallet at the same time, preventing double-spend issues.

---

## How the Tables Connect During a Transfer

```
POST /transfer → creates transaction (id=101, status=PENDING)
              → creates saga_instance (id=42, context={...})
              → links transaction.saga_instance_id = 42

Step 1 runs → creates saga_step (saga_instance_id=42, step=DEBIT, status=RUNNING)
           → updates wallet A balance
           → saga_step status → COMPLETED
           → saga_instance.context updated (balance recorded)

Step 2 runs → creates saga_step (saga_instance_id=42, step=CREDIT, status=RUNNING)
           → updates wallet B balance
           → saga_step status → COMPLETED
           → saga_instance.context updated

Step 3 runs → creates saga_step (saga_instance_id=42, step=UPDATE_TX, status=RUNNING)
           → updates transaction status → SUCCESS
           → saga_step status → COMPLETED
           → saga_instance status → COMPLETED
```
