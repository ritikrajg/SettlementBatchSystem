# Project Test Inputs and Edge Cases

This guide provides **copy-paste ready inputs** to validate the full CLI flow and important edge cases in `SettlementApp`.

## 1) Happy-path full flow (manual menu inputs)

Use this input sequence after launching the app:

```text
1
2
TXN1001
1
2
1
1200.50
2
1
2
TXN1002
2
3
4
9999
1
2
4
y
5
BATCH_ID_FROM_OPTION_1
6
BATCH_ID_FROM_OPTION_1
7
8
10
1
10
2
2025-01-01
11
1
TXN1001
11
3
1
11
4
1
11
5
1
11
6
1
11
7
1
1
11
8
1
1
11
9
1
1
1
12
y
```
```

Notes:
- Replace `BATCH_ID_FROM_OPTION_1` with the generated batch id printed by option 1.
- This single flow touches create, add, submit, summary/report, advanced batch views, and all advanced transaction filter variants.

## 2) Core input validation edge cases

Run each as a focused scenario.

### 2.1 Invalid menu choice

```text
abc
```
Expected: app should not crash and should show an invalid option response.

### 2.2 Blank transaction id

```text
1
2

```
Expected: validation error that transaction ID cannot be blank.

### 2.3 Sender bank equals receiver bank

```text
1
2
TXN2001
1
1
1
100
1
1
```
Expected: validation error that sender and receiver cannot be same.

### 2.4 Amount validations

```text
1
2
TXN2002
1
2
1
0
-1
abc
100
1
1
```
Expected: errors for non-positive and non-numeric amounts, then accept 100.

### 2.5 Enum selection validation (bank/channel/drcr/status)

At any enum prompt, try:

```text
0
999
abc
1
```
Expected: invalid selection messages until a valid option number is provided.

### 2.6 Submit without pending batch

```text
4
```
Expected: error indicating no pending batch or empty batch.

### 2.7 Exit with unsaved data guard

```text
1
2
TXN3001
1
2
1
100
1
1
12
n
```
Expected: app asks for confirmation because unsaved transactions exist.

## 3) CSV import test data

The CSV parser expects:

```text
txn_id,sender_bank,receiver_bank,channel,amount,dr_cr,status
```

### 3.1 Valid CSV (`docs/test-data/valid-transactions.csv`)

```csv
txn_id,sender_bank,receiver_bank,channel,amount,dr_cr,status
CSV1001,SBI,HDFC,UPI,100.00,DR,SUCCESS
CSV1002,ICICI,AXIS,ATM,250.25,CR,PENDING
CSV1003,PNB,BOB,NETBANKING,5000,DR,FAILED
```

### 3.2 Duplicate transaction id within file (`docs/test-data/duplicate-txnid.csv`)

```csv
txn_id,sender_bank,receiver_bank,channel,amount,dr_cr,status
CSV2001,SBI,HDFC,UPI,100.00,DR,SUCCESS
CSV2001,ICICI,AXIS,ATM,250.25,CR,PENDING
```

### 3.3 Invalid enum values (`docs/test-data/invalid-enum.csv`)

```csv
txn_id,sender_bank,receiver_bank,channel,amount,dr_cr,status
CSV3001,SBI,HDFC,IMPS,100.00,DR,SUCCESS
```

### 3.4 Same sender and receiver (`docs/test-data/same-bank.csv`)

```csv
txn_id,sender_bank,receiver_bank,channel,amount,dr_cr,status
CSV4001,SBI,SBI,UPI,100.00,DR,SUCCESS
```

### 3.5 Invalid amount (`docs/test-data/invalid-amount.csv`)

```csv
txn_id,sender_bank,receiver_bank,channel,amount,dr_cr,status
CSV5001,SBI,HDFC,UPI,abc,DR,SUCCESS
CSV5002,SBI,HDFC,UPI,-10,DR,SUCCESS
```

### 3.6 Missing columns (`docs/test-data/missing-columns.csv`)

```csv
txn_id,sender_bank,receiver_bank,channel,amount,dr_cr
CSV6001,SBI,HDFC,UPI,100.00,DR
```

### 3.7 Empty file (`docs/test-data/empty.csv`)

Expected: CSV file is empty validation error.

## 4) CSV import menu inputs

### 4.1 Valid CSV import

```text
1
3
docs/test-data/valid-transactions.csv
9
4
y
```

### 4.2 Error CSV import

```text
1
3
docs/test-data/invalid-enum.csv
```

Swap in any edge-case CSV path from section 3.

## 5) Database-related edge checks

1. Duplicate transaction id already persisted in DB:
   - Add & submit `TXN7001` once.
   - Try adding/importing `TXN7001` again.
   - Expected: duplicate transaction ID error.

2. Report with unknown batch id:
   - Option 5/6 with random id like `BATCH_DOES_NOT_EXIST`.
   - Expected: user-friendly no-data message.

3. Date filter with no rows:
   - Option 10.2 / 11.2 with old/future date.
   - Expected: empty report handling, no crash.

## 6) Quick execution matrix

- ✅ Basic create/add/submit/report flow
- ✅ All advanced transaction filters (1-9)
- ✅ Batch advanced views
- ✅ All input validators (blank, enum, date, amount)
- ✅ CSV import valid + invalid cases
- ✅ Duplicate id checks (in-memory and persisted)
- ✅ Exit confirmation with unsaved records

