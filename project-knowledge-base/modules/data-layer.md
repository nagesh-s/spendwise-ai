# Data Layer Module

> **Purpose**: Explains how data is stored and accessed in the app.

---

## 📌 Status: NOT YET IMPLEMENTED

This module will be implemented in Phase 1.

---

## 🎯 What This Module Does

The data layer is the foundation of the app. It handles:
- **Storing** expenses, categories, merchants, and budgets in a local database
- **Retrieving** data for display on screens
- **Calculating** totals, averages, and aggregates

---

## 📊 Data Entities (Tables)

| Entity | Purpose | Key Fields |
|--------|---------|------------|
| **Expense** | Individual spending records | amount, date, category, merchant, description |
| **Category** | Expense categories (Food, Transport, etc.) | name, color, icon |
| **Merchant** | Where money was spent (Amazon, Starbucks) | name |
| **Budget** | Monthly spending limits per category | month, category, limit amount |
| **QuickTile** | Frequently used expense templates | label, amount, category, merchant |

---

## 🔄 How It Will Work

1. User logs an expense → Saved to **Expense** table
2. Dashboard loads → Queries **Expense** + **Category** for totals
3. Calendar shows a day → Queries **Expense** filtered by date
4. Budget progress → Compares **Expense** sum vs **Budget** limit

---

## 📁 Files (After Implementation)

| File | Purpose |
|------|---------|
| `data/entity/Expense.java` | Expense table definition |
| `data/entity/Category.java` | Category table definition |
| `data/entity/Merchant.java` | Merchant table definition |
| `data/entity/Budget.java` | Budget table definition |
| `data/entity/QuickTile.java` | QuickTile table definition |
| `data/dao/ExpenseDao.java` | Database queries for expenses |
| `data/dao/CategoryDao.java` | Database queries for categories |
| `data/dao/MerchantDao.java` | Database queries for merchants |
| `data/dao/BudgetDao.java` | Database queries for budgets |
| `data/AppDatabase.java` | Main database configuration |

---

## 📅 Last Updated
2026-01-22 - Initial placeholder created
