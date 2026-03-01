# 📖 What is a Saga? (Beginner Guide)

## The Problem: Distributed Transactions Don't Exist

Imagine you want to transfer ₹500 from **Wallet A** to **Wallet B**.

This needs two operations:
1. **Debit** ₹500 from Wallet A
2. **Credit** ₹500 to Wallet B

In a normal single database, you wrap both in a single SQL transaction:

```sql
BEGIN;
  UPDATE wallet SET balance = balance - 500 WHERE id = 1;  -- debit
  UPDATE wallet SET balance = balance + 500 WHERE id = 2;  -- credit
COMMIT;
```

If step 2 fails, the database **automatically rolls back** step 1. Safe!

### But what if the two wallets are in different services / databases?

```
Service A (Wallet A) ─── debit ✅
Service B (Wallet B) ─── credit 💥 FAILS

// Now Wallet A has lost ₹500, but Wallet B never received it!
// Money just VANISHED.
```

Traditional database transactions **cannot span multiple services**. This is the core problem in distributed systems.

---

## The Solution: The Saga Pattern

A **Saga** is a sequence of local transactions where:
- Each step performs a **local DB transaction** (within one service/DB)
- If any step fails, **compensation transactions** are run in reverse to undo the damage

Think of it like this:

| Traditional ACID Transaction | Saga |
|---|---|
| All-or-nothing, guaranteed by DB | Best-effort, undone by compensation |
| Handled by DB engine | Handled by your code (Orchestrator) |
| Milliseconds, single DB | Can span many services over time |
| Automatic rollback | Manual "compensate" methods |

---

## Real World Analogy

Think of booking a trip:

1. ✅ Book flight (₹10,000 charged)
2. ✅ Book hotel (₹5,000 charged)
3. ❌ Book car rental — **FAILS** (no cars available)

Now you need to **undo** what already succeeded:
- Cancel hotel → ₹5,000 **refunded**
- Cancel flight → ₹10,000 **refunded**

Each of these "undo" actions is called a **compensating transaction**.

---

## Two Types of Sagas

| Type | How it works |
|---|---|
| **Choreography** | Each step publishes an event, next step listens and reacts. No central controller. |
| **Orchestration** | A central **Orchestrator** tells each step what to do and when. |

This project uses the **Orchestration** style — there is one central `SagaOrchestrator` in charge of the whole flow.

---

## Next Steps

- 📄 [02 — How the Money Transfer Saga Works](./02_transfer_saga_flow.md)
- 📄 [03 — Code Components Explained](./03_components.md)
- 📄 [04 — Database Tables](./04_database_tables.md)
- 📄 [05 — Compensation (Rollback) Deep Dive](./05_compensation.md)
