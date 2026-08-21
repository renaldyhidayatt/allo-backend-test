# Split Bill API

A Spring Boot REST API for tracking shared group expenses and figuring out
who owes whom at the end.

- Java 17, Spring Boot 4.11, Maven
- H2 in-memory database (nothing to provision — data resets on restart)
- All monetary values are `BigDecimal`, never `float`/`double`
- Multi-stage `Dockerfile` in the project root

## GitHub username & service charge

```
GitHub username: renaldyhidayatt
service_charge_pct: 7
```

Worked out the same way the brief specifies: lowercase the username, sum the
ASCII value of every character, take that sum mod 10.

```
r+e+n+a+l+d+y+h+i+d+a+y+a+t+t
= 114+101+110+97+108+100+121+104+105+100+97+121+97+116+116
= 1607
1607 % 10 = 7
```

This isn't hardcoded — `ServiceChargeCalculator` computes it at startup from
`app.github-username` in `application.yml`, and every settlement response
includes the resulting `service_charge_pct` and `service_charge_amount`.
See `ServiceChargeCalculatorTest` for a test that matches this exact math.

## How to build and run

### Locally with Maven

```bash
mvn clean test        # runs the test suite
mvn spring-boot:run    # starts the API on http://localhost:8080
```

### With Docker

```bash
docker build -t split-bill-api .
docker run -p 8080:8080 split-bill-api
```

> A note on this submission's environment: I wrote and reviewed this code in
> a sandbox without access to Maven Central, so I could not run `mvn test`
> myself before submitting. I've read through every file carefully and I'm
> confident in it, but please treat `mvn clean test` as the first thing to
> run when you pull this down.

## Data model, in short

- **BillGroup** — a name and a list of `Participant`s.
- **Expense** — an amount, who paid it, a category, and a split strategy.
  Every expense is fully decomposed into `ExpenseShare`s (one per participant
  it applies to) at creation time, regardless of which split strategy was
  used — so everything downstream (settlement, category summaries) only
  ever has to deal with one shape: "participant X owes Y for expense Z".
- **Payment** — a real payment someone made to someone else, recorded to
  reduce outstanding balances without touching the read-only expense data.

## API walkthrough

The example below follows one running scenario end-to-end. Participant IDs
below (`1`, `2`, `3`) are illustrative — the API returns the real IDs it
generated, and you should use those on this and later calls.

**1. Create a group**

```bash
curl -X POST http://localhost:8080/api/groups \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Bali Trip",
    "participants": ["Renaldy", "Dimas", "Sari"]
  }'
```

**2. Get a group**

```bash
curl http://localhost:8080/api/groups/1
```

**3. Add an expense — equal split**

Renaldy (id 1) pays for the hotel, split evenly across all three.

```bash
curl -X POST http://localhost:8080/api/groups/1/expenses \
  -H "Content-Type: application/json" \
  -d '{
    "description": "Hotel",
    "amount": 900000,
    "paidByParticipantId": 1,
    "category": "ACCOMMODATION",
    "splitType": "EQUAL"
  }'
```

`participantIds` can be added to split among a subset instead of the whole
group; omitting it (as above) defaults to everyone in the group.

**4. Add an expense — split by percentage**

Dimas (id 2) pays for dinner, split 50/30/20.

```bash
curl -X POST http://localhost:8080/api/groups/1/expenses \
  -H "Content-Type: application/json" \
  -d '{
    "description": "Dinner",
    "amount": 300000,
    "paidByParticipantId": 2,
    "category": "FOOD",
    "splitType": "PERCENTAGE",
    "shares": [
      { "participantId": 1, "value": 50 },
      { "participantId": 2, "value": 30 },
      { "participantId": 3, "value": 20 }
    ]
  }'
```

Percentages must sum to exactly 100 or the request is rejected with a 400.

**5. Add an expense — exact amounts**

Sari (id 3) pays for souvenirs, with each person's exact share specified.

```bash
curl -X POST http://localhost:8080/api/groups/1/expenses \
  -H "Content-Type: application/json" \
  -d '{
    "description": "Souvenirs",
    "amount": 250000,
    "paidByParticipantId": 3,
    "category": "OTHER",
    "splitType": "EXACT",
    "shares": [
      { "participantId": 1, "value": 100000 },
      { "participantId": 2, "value": 80000 },
      { "participantId": 3, "value": 70000 }
    ]
  }'
```

Exact amounts must sum to the expense total or the request is rejected.

**6. List expenses / per-category summary**

```bash
curl http://localhost:8080/api/groups/1/expenses
curl http://localhost:8080/api/groups/1/expenses/summary/by-category
```

**7. Get the settlement summary**

```bash
curl http://localhost:8080/api/groups/1/settlement
```

At this point, before any payments are recorded, the response looks like:

```json
{
  "groupId": 1,
  "groupName": "Bali Trip",
  "totalExpenses": 1450000.00,
  "service_charge_pct": 7,
  "service_charge_amount": 101500.00,
  "balances": [
    { "participantId": 1, "name": "Renaldy", "netBalance": 350000.00 },
    { "participantId": 2, "name": "Dimas", "netBalance": -170000.00 },
    { "participantId": 3, "name": "Sari", "netBalance": -180000.00 }
  ],
  "transactions": [
    { "fromParticipantId": 3, "fromName": "Sari", "toParticipantId": 1, "toName": "Renaldy", "amount": 180000.00 },
    { "fromParticipantId": 2, "fromName": "Dimas", "toParticipantId": 1, "toName": "Renaldy", "amount": 170000.00 }
  ]
}
```

(The Java DTO uses `serviceChargePct` / `serviceChargeAmount` as field names,
but `SettlementDtos.SettlementResponse` annotates them with
`@JsonProperty("service_charge_pct")` / `@JsonProperty("service_charge_amount")`,
so the actual JSON on the wire matches the brief's snake_case names exactly,
as shown above.)

**8. Record a payment**

Dimas pays Renaldy back the 170,000 he owes.

```bash
curl -X POST http://localhost:8080/api/groups/1/payments \
  -H "Content-Type: application/json" \
  -d '{
    "fromParticipantId": 2,
    "toParticipantId": 1,
    "amount": 170000
  }'
```

Fetching the settlement again now shows Dimas at a net balance of `0.00`,
and only one remaining transaction: Sari owes Renaldy `180000.00`.

```bash
curl http://localhost:8080/api/groups/1/payments   # list recorded payments
```

## What's beyond the minimum

- **Three split strategies** (`EQUAL`, `PERCENTAGE`, `EXACT`), all funneled
  through the same `ExpenseShare` model so settlement logic doesn't need to
  know how a split was derived.
- **Debt-simplifying settlement** — instead of listing every individual
  expense debt, `/settlement` returns the minimal-ish set of payments needed
  to zero everyone out, via a greedy largest-creditor/largest-debtor match.
- **Payment recording** — `/payments` lets you mark money as actually
  having changed hands, which then offsets the settlement balances.
- **Expense categories + summary** — `/expenses/summary/by-category` totals
  spend per category.
- **Audit timestamps** — groups, expenses, and payments all record
  `createdAt`.
- **Careful rounding** — see `MoneySplitter`: every split strategy is
  reconciled back to the exact original amount in whole cents, so splits
  never silently lose or gain a cent to rounding.

## Testing

```bash
mvn test
```

- `SettlementCalculatorTest` — the core settlement logic: balance
  computation across multiple expenses, how recorded payments offset
  balances, and the debt-optimization algorithm, including the zero-debt
  edge case.
- `MoneySplitterTest` — equal/percentage/exact splitting, including the
  rounding-remainder reconciliation behavior and rejection of invalid
  percentage/exact inputs.
- `ServiceChargeCalculatorTest` — the personalization formula, checked
  against both the brief's own worked example and this submission's value.

## Submission question

**What was the hardest design decision you made while building this, and
what trade-off did you accept?**

The hardest decision was how to represent a split once it's created, given
that I support three different split strategies. I decided to fully
decompose every expense into per-participant `ExpenseShare` rows at write
time, rather than storing "split type + parameters" and recomputing shares
on every read. That makes settlement, category summaries, and anything else
downstream strategy-agnostic — they just sum shares — at the cost of losing
the original split parameters (e.g. the exact percentages used) if I ever
wanted to let someone edit an expense later; I'd have to either store both
or recompute from scratch. Closely related was deciding where rounding
remainders go when a split doesn't divide evenly — I chose to always
reconcile back to the original total (remainder cents to the first
participants for equal splits, drift to the largest share for percentage
splits) rather than let individual shares be off by a cent, since a
split-bill app with inaccurate math undermines the entire point of the
tool.
