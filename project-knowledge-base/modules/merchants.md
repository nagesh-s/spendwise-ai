# Merchants Module

> **Purpose**: Explains how merchant analytics work.

---

## 📌 Status: ✅ IMPLEMENTED (v1.0.0)

---

## 🎯 What This Screen Does

Merchants screen shows:
- Top merchants by total spending for the month
- Transaction count per merchant
- Average transaction amount
- Ranking with colored badges (gold/silver/bronze)

---

## 🔄 How It Works

### Loading Top Merchants
1. `MerchantsFragment` observes `getTopMerchants()` from ViewModel
2. ViewModel calls `getTopMerchants(startDay, endDay, limit)` on repository
3. DAO runs aggregate query joining merchants with expenses
4. Returns `MerchantAgg` with name, txn count, total, average

### Display
1. `MerchantAdapter` shows merchants in ranked list
2. Rank 1 gets gold badge, 2 silver, 3 bronze
3. Stats show: "12 transactions • Avg ₹450"

---

## 📁 Files

| File | Purpose |
|------|---------|
| `merchants/MerchantsFragment.java` | UI, month navigation |
| `merchants/MerchantsViewModel.java` | Data loading |
| `merchants/MerchantAdapter.java` | RecyclerView with rank badges |
| `fragment_merchants.xml` | Screen layout |
| `item_merchant.xml` | Merchant list item |
| `data/entity/Merchant.java` | Merchant table entity |
| `data/dao/MerchantDao.java` | CRUD + getTopMerchants query |
| `data/pojo/MerchantAgg.java` | Query result with stats |

---

## 🎨 Rank Badge Colors

| Rank | Color | Code |
|------|-------|------|
| 1 | 🥇 Gold | `#FFD700` |
| 2 | 🥈 Silver | `#C0C0C0` |
| 3 | 🥉 Bronze | `#CD7F32` |
| 4+ | Gray | `#607D8B` |

Colors defined in `MerchantAdapter.RANK_COLORS`.

---

## 🐛 Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| No merchants showing | No expenses with merchants | Add expenses with merchant field |
| Wrong month data | Date range wrong | Check getMonthStartEpochDay/EndEpochDay |
| Totals incorrect | Query grouping issue | Check MerchantDao.getTopMerchants query |

---

## 📅 Last Updated
2026-01-23 - v1.0.0 Complete
