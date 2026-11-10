Suggest category by keyword/token match in description and recency-weighted choices to speed data entry.

Keep keyword outputs aligned to seeded default categories:
- `Food & Dining`
- `Transportation`
- `Shopping`
- `Entertainment`
- `Bills & Utilities`
- `Health & Fitness`

Offer Quick Add tiles for top frequent tuples (category, merchant, amount) by time-decayed frequency.

Investment signals:

- `InvestmentUnderstandingEngine` consults `UserCorrection` memory, then `InvestmentSmsParser`, then on-device semantic interpretation for uncertain text.
- `BucketAssignmentEngine` looks for learned mappings before comparing instrument/provider text with bucket descriptions using the bundled MediaPipe text embedder.
- If semantic inference fails, deterministic asset/keyword rules provide fallback scores. Uncertain assignments go to an Unassigned bucket for user confirmation; confirmed assignments update learned mapping memory.

Salary Detection & Bucket Allocation:
- `SalaryDetector` identifies salary credits from SMS using: salary keywords (salary, payroll, NEFT CR), amount threshold (≥₹10,000 default), NEFT/IMPS/RTGS transfer type, round number heuristic.
- Confidence scoring: keyword=0.50, amount=0.25, transfer=0.15, round=0.10. Threshold: ≥0.40.
- Non-salary rejection: refund, cashback, interest, dividend keywords immediately disqualify.
- `BucketAllocationEngine` implements Reverse Budgeting: allocates salary in priority order (Emergency→Retirement→Long→Medium→Short), remainder is guilt-free spending.
- `CashflowGuardrail` safety checks: emergency fund <3× monthly spend → boost allocation; upcoming bills > short-term allocation → warn; last month overspend → warn.
- Smart fallback: if user's bank doesn't send salary SMS, `BucketSalaryPrefs` stores expected salary and payday; manual "I Got Paid" flow prompts confirm/edit on payday.
