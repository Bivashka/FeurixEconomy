# FeurixEconomy

Minecraft 1.16.5 plugin that stores player ruble balances in the existing MySQL `users` table.

## Storage

The plugin expects a table with at least these columns:

```sql
CREATE TABLE users (
    username VARCHAR(64) NOT NULL PRIMARY KEY,
    rubles BIGINT NOT NULL DEFAULT 0
);
```

This is an **example**, not an automatic migration. Do not replace an existing user table with it. Account names must map to unique database rows, using the same username collation as the Minecraft authorization system.

Set `database.host`, `database.port`, `database.name`, `database.user` and `database.password` in `plugins/feurixeconomy/config.yml`. Use a dedicated MySQL account with only the permissions needed for the existing table.

## Commands

- `/rubles` — show your balance
- `/rubles give <player> <amount>` — add balance (permission: `feurixeconomy.admin`)
- `/rubles take <player> <amount>` — remove balance (permission: `feurixeconomy.admin`)
- `/rubles set <player> <amount>` — set balance (permission: `feurixeconomy.admin`)
- `/rubles pay <player> <amount>` — transfer rubles

Transfers use an SQL transaction. Insufficient funds, a missing recipient, or an overflowing recipient balance must not deduct rubles from the sender. The administrator balance updates use single SQL statements to avoid lost updates.

## Build and verification

```sh
mvn test
mvn package
```

SQL tests use an in-memory H2 database in MySQL compatibility mode. They do **not** replace an integration test against the actual MySQL version and Minecraft server.

Before deploying to production, back up the database and test with a staging server. The Maven project currently includes PlaceholderAPI as a local system-scoped JAR in `libs/`; dependency cleanup is tracked separately.
