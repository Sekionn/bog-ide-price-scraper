# Bog & ide price scraper

Spring Boot application that stores public Bog & ide product prices and keeps known products fresh on a schedule.

The scraper stays on public `/products/...` pages, skips cart/checkout/account/internal endpoints, uses a configurable delay between every outbound request, and stores scraped prices in MySQL through JPA/Hibernate.

Bog & ide's robots.txt mentions UCP/MCP for agent catalog operations. This project only follows known public product URLs and does not call internal recommendation/cart/checkout APIs. If the storefront blocks direct HTTP fetching, use the official catalog route instead of trying to bypass the block.

## Run

```bash
./mvnw spring-boot:run
```

On Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

The app expects MySQL to be available. Defaults:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/bog_ide_prices
spring.datasource.username=bogide
spring.datasource.password=bogide
```

Override them with `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD`.

## Docker

Run the app and MySQL together:

```bash
docker compose up --build
```

This starts:

```text
mysql:8.4 on localhost:3306
app on localhost:8080
```

The default schedule is Monday, Wednesday, and Friday at 22:00 in `Europe/Copenhagen`.

Change it in `src/main/resources/application.properties`:

```properties
scraper.schedule.cron=0 0 22 ? * MON,WED,FRI
scraper.schedule.zone=Europe/Copenhagen
```

Spring cron format is:

```text
second minute hour day-of-month month day-of-week
```

Examples:

```properties
# Every Sunday at 04:30
scraper.schedule.cron=0 30 4 ? * SUN

# Every Wednesday at 22:15
scraper.schedule.cron=0 15 22 ? * WED
```

Scheduled runs refresh products that are already known in the database. They do not crawl the full product sitemap catalog. Each scheduled run stops after:

```properties
scraper.max-run-time=PT5H
```

Rows are considered fresh for:

```properties
scraper.refresh-after=P7D
```

## Manual test run

Start the app, then call:

```bash
curl http://localhost:8080/api
```

This returns the HATEOAS API index with links, methods, request examples, response examples, and endpoint constraints.

Start a manual known-product refresh:

```bash
curl -X POST http://localhost:8080/api/scrape/run
```

Check status:

```bash
curl http://localhost:8080/api/scrape/status
```

Read the latest stored database rows:

```bash
curl http://localhost:8080/api/prices/latest
```

Look up the latest stored rows by `Varenr.` or EAN number, up to 100 identifiers per request:

```bash
curl -X POST http://localhost:8080/api/prices/batch \
  -H "Content-Type: application/json" \
  -d '["2287895", "9788711477960"]'
```

If a matched product was last scraped more than `scraper.refresh-after` ago, the batch endpoint refreshes that exact known product URL before returning the response.

If a `Varenr.` has never been seen before, the app scans product sitemaps until it finds a public product URL ending in `-{Varenr.}`. If it finds one, it scrapes that product, stores it, and returns it. Unknown EAN/GTIN-shaped values are never used for sitemap URL discovery and are omitted until the product has been seeded by `Varenr.`.

For a small scheduled refresh test, set:

```properties
scraper.max-products-per-run=10
```

Do this before your first run if you already have known products in the database.

## Request pacing

All outbound requests are rate-limited through one shared throttle, including `robots.txt`, the sitemap index, product sitemaps, and product pages.

The default delay is two seconds between request starts:

```properties
scraper.request-delay=PT2S
scraper.minimum-request-delay=PT1S
```

`scraper.minimum-request-delay` prevents accidental fast crawls if `scraper.request-delay` is set too low. For scheduled refresh jobs, increase `scraper.request-delay` if you want to be even more conservative.

## Output

Successful scrape rows are stored in the MySQL `product_prices` table. `product_number` stores the Danish `Varenr.` value, and `ean_number` stores the EAN/GTIN value. Both are stored as strings.
