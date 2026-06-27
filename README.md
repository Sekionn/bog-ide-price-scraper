# Bog & ide price scraper

Spring Boot application that stores public Bog & ide product prices and keeps known products fresh on a schedule.

The scraper stays on public `/products/...` pages, skips cart/checkout/account/internal endpoints, uses a configurable delay between every outbound request, and stores scraped prices in MySQL through JPA/Hibernate.

Bog & ide's robots.txt mentions UCP/MCP for agent catalog operations. This project only follows known public product URLs and does not call internal recommendation/cart/checkout APIs. If the storefront blocks direct HTTP fetching, use the official catalog route instead of trying to bypass the block.

## Run

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

Swagger UI is available after startup:

```text
http://localhost:8080/swagger-ui.html
```

The generated OpenAPI document is available at:

```text
http://localhost:8080/v3/api-docs
```

## Docker

Run the app and MySQL together:

```powershell
docker compose up --build
```

This starts:

```text
mysql:8.4 on localhost:3306
app on https://localhost:8443
```

## HTTPS

The Docker image includes the PKCS12 keystore at `/app/certs/bog-ide-price-scraper.p12`. Generate or replace the keystore before building the image:

```powershell
New-Item -ItemType Directory -Force -Path certs
keytool -genkeypair `
  -alias bog-ide-price-scraper `
  -keyalg RSA `
  -keysize 2048 `
  -storetype PKCS12 `
  -keystore certs\bog-ide-price-scraper.p12 `
  -validity 365 `
  -storepass change-this-password `
  -dname "CN=your-domain.example, OU=Private, O=Private, L=Copenhagen, ST=Denmark, C=DK"
```

Run the app with HTTPS enabled:

```Bash
docker run -d \
  --name bog-ide-price-scraper \
  --network bogide-net \
  --memory=1200m \
  --memory-swap=1400m \
  -p 8443:8443 \
  -e SERVER_PORT=8443 \
  -e SERVER_SSL_ENABLED=true \
  -e SERVER_SSL_KEY_STORE=/app/certs/bog-ide-price-scraper.p12 \
  -e SERVER_SSL_KEY_STORE_PASSWORD=change-this-password \
  -e SERVER_SSL_KEY_STORE_TYPE=PKCS12 \
  -e SERVER_SSL_KEY_ALIAS=bog-ide-price-scraper \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://bog-ide-price-mysql:3306/bog_ide_prices \
  -e SPRING_DATASOURCE_USERNAME=bogide \
  -e SPRING_DATASOURCE_PASSWORD=bogide \
  -e SPRING_DATA_REDIS_HOST=bog-ide-price-redis \
  -e SPRING_DATA_REDIS_PORT=6379 \
  -e JAVA_TOOL_OPTIONS="-Xms128m -Xmx700m -XX:MaxMetaspaceSize=192m -XX:MaxDirectMemorySize=128m" \
  bog-ide-price-scraper:latest
```

The Docker image only exposes `8443`; when HTTPS is enabled with `SERVER_PORT=8443`, the app does not listen on HTTP port `8080`. Because the keystore is copied into the image, treat the image as containing private key material.

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

```powershell
curl.exe http://localhost:8080/api
```

This returns the HATEOAS API index with links, methods, request examples, response examples, and endpoint constraints.

Start a manual known-product refresh:

```powershell
curl.exe -X POST http://localhost:8080/api/scrape/run
```

Discover product numbers and URLs from product sitemaps, adding missing products as placeholder rows:

```powershell
curl.exe -X POST "http://localhost:8080/api/discovery/run?limit=10"
```

Use `limit=0` for no product-count limit. Discovery still stops after:

```properties
scraper.discovery-max-run-time=PT5H
```

Check discovery status:

```powershell
curl.exe http://localhost:8080/api/discovery/status
```

Check status:

```powershell
curl.exe http://localhost:8080/api/scrape/status
```

Read the latest stored database rows:

```powershell
curl.exe http://localhost:8080/api/prices/latest
```

Look up the latest stored rows by `Varenr.` or EAN number, up to 100 identifiers per request:

```powershell
curl.exe -X POST http://localhost:8080/api/prices/batch `
  -H "Content-Type: application/json" `
  -d '["2287895", "9788711477960"]'
```

If a matched product was last scraped more than `scraper.refresh-after` ago, the batch endpoint refreshes that exact known product URL before returning the response.

If a `Varenr.` has never been seen before, the app scans product sitemaps until it finds a public product URL ending in `-{Varenr.}`. If it finds one, it scrapes that product, stores it, and returns it. Unknown EAN/GTIN-shaped values are never used for sitemap URL discovery and are omitted until the product has been seeded by `Varenr.`.

For a small scheduled refresh test, set:

```properties
scraper.max-products-per-run=10
```

Do this before your first run if you already have known products in the database.

For a small scheduled sitemap discovery test, set:

```properties
scraper.max-discovered-products-per-run=10
```

The default discovery schedule runs once per week:

```properties
scraper.discovery-cron=0 0 3 ? * MON
scraper.discovery-zone=Europe/Copenhagen
```

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
