# 📚 Saga Documentation — Index

## Money Transfer Saga — Complete Guide

This folder contains everything you need to understand the Saga pattern as implemented in this project.

---

## Reading Order (Start Here)

| # | File | What You'll Learn |
|---|---|---|
| 1 | [01 — What is a Saga?](./01_saga_overview.md) | The problem sagas solve, what a saga is, orchestration vs choreography |
| 2 | [02 — Transfer Saga Flow](./02_transfer_saga_flow.md) | Step-by-step happy path and failure path with diagrams |
| 3 | [03 — Components Explained](./03_components.md) | Every class and its role, with code snippets |
| 4 | [04 — Database Tables](./04_database_tables.md) | Schema of all tables, how they relate, example rows |
| 5 | [05 — Compensation Deep Dive](./05_compensation.md) | How rollback works, ordering rules, idempotency |

---

## Quick Reference

### The 3 Steps of Money Transfer

```
DEBIT_SOURCE_WALLET_STEP
    → DebitSourceWalletStep.java
    → Debits source wallet. Compensate: credits it back.

CREDIT_DESTINATION_WALLET_STEP
    → CreditDestinationWalletStep.java
    → Credits destination wallet. Compensate: debits it back.

UPDATE_TRANSACTION_STATUS_STEP
    → UpdateTransactionStatus.java
    → Sets Transaction.status = SUCCESS. Compensate: restores original status.
```

### Saga Statuses

```
STARTED → RUNNING → COMPLETED        (happy path)
                 ↘ FAILED → COMPENSATING → COMPENSATED  (failure path)
```

### Adding a New Step

1. Create a class implementing `SagaStepInterface`
2. Add the step name to `SagaStepNames` enum
3. Add the step to the `transferMoneySagaSteps` list in `SagaStepFactory`
4. Add context key constants to `SagaContextKeys`

> No changes needed to `SagaOrchestratorImpl` or `SagaStepFactory` — Spring auto-registers new steps!

---

## Key Design Principles

| Principle | How it's applied |
|---|---|
| **Durability** | `SagaInstance.context` is a JSON column updated after every step |
| **Crash Recovery** | Context in DB means the saga state survives restarts |
| **Isolation** | `SELECT FOR UPDATE` on wallet rows prevents concurrent modifications |
| **Separation of Concerns** | Orchestrator handles flow; steps handle business logic |
| **Open/Closed** | New steps require no changes to existing classes |
