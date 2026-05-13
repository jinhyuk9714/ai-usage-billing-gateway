# Performance Result

## Measured

No load test has been executed yet.

## Verified

- k6 scenario added and inspectable.
- Integration tests verify functional correctness for security, idempotency, invoice, webhook, ledger, and audit behavior.

## Pending

- Run `k6 run k6/mixed-usage-test.js` against a local or deployed environment.
- Record hardware, JVM, database, Redis, dataset size, and command used.
- Add measured latency, throughput, and error rate only after the run is complete.

Do not add synthetic or estimated benchmark numbers.
