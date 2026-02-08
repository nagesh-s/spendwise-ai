# Merchants Module

> **Purpose**: Explains how merchant analytics work.

---

## 📌 Status: ✅ IMPLEMENTED + ENHANCED (v1.1.1)

---

## 🎯 What This Screen Does

Merchants screen shows:
- Top merchants by total spending for selected month
- Transaction count and average per merchant
- Ranking badges (gold/silver/bronze/default)
- Merchant summary card:
  - total merchant spend for month
  - top merchant with spend amount

---

## 🔄 How It Works

### Loading Data
1. `MerchantsViewModel` tracks `analyticsMonth`.
2. `switchMap` loads `getTopMerchants(startDay, endDay, limit)`.
3. Fragment observes list and updates summary + recycler state.

### Summary Card
- If list is empty: show guidance text.
- Else: compute list total + use first row as top merchant highlight.

---

## 📁 Files

| File | Purpose |
|------|---------|
| `merchants/MerchantsFragment.java` | UI, month navigation, summary card |
| `merchants/MerchantsViewModel.java` | Month state + merchant aggregate query |
| `merchants/MerchantAdapter.java` | Ranked merchant rows |
| `fragment_merchants.xml` | Screen layout |
| `item_merchant.xml` | Merchant list item |
| `data/dao/MerchantDao.java` | Merchant aggregate query |
| `data/pojo/MerchantAgg.java` | Merchant aggregate model |

---

## 🎨 Rank Badge Colors

| Rank | Color | Code |
|------|-------|------|
| 1 | 🥇 Gold | `#FFD700` |
| 2 | 🥈 Silver | `#C0C0C0` |
| 3 | 🥉 Bronze | `#CD7F32` |
| 4+ | Gray | `#607D8B` |

---

## 🐛 Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| Summary card says no data | Expenses missing merchant values | Save merchant names while adding expenses |
| Totals differ from dashboard | Merchant-only subset | Dashboard includes all expenses, not only merchant-tagged |
| Wrong month data | Month range mismatch | Check `analyticsMonth` and date range calculations |

---

## 📅 Last Updated
2026-02-08 - v1.1.1 merchant summary card added
