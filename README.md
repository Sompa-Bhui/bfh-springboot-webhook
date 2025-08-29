# BFH Spring Boot Webhook App

A minimal Spring Boot app that:
1. Calls **generateWebhook** on startup to get a `webhook` URL and `accessToken` (JWT).
2. Determines which question applies based on the last two digits of your `regNo` (logged for your reference).
3. Submits your **final SQL query** (`finalQuery`) to the returned `webhook` using the JWT in the `Authorization` header (no `Bearer` prefix).

> Built to satisfy: _Bajaj Finserv Health | Qualifier 1 | JAVA_ requirements.

## Tech
- Java 17
- Spring Boot 3.3.x
- RestTemplate
- No controllers (runs automatically on startup via `CommandLineRunner`).

## Configure
Edit `src/main/resources/application.yml`:
```yaml
bfh:
  name: "Your Name"
  regNo: "REG12347"
  email: "you@example.com"
  finalQuery: "YOUR_SQL_QUERY_HERE"
```

Alternatively, override via environment variables:
```bash
export BFH_NAME="Your Name"
export BFH_REGNO="REG12347"
export BFH_EMAIL="you@example.com"
export BFH_FINALQUERY="SELECT * FROM your_table;"
```

## Build
```bash
./mvnw -v   # if you have Maven Wrapper; otherwise ensure Maven 3.9+ is installed
mvn clean package
```

## Run
```bash
java -jar target/bfh-springboot-webhook-1.0.0.jar
```

## How it works
- On startup, the app POSTs to:
  `https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA`
  with JSON body:
  ```json
  {"name": "...", "regNo": "...", "email": "..."}
  ```
- The response is parsed to read `webhook` and `accessToken`.
- The app logs whether your `regNo` maps to Question 1 (odd) or Question 2 (even).
- Then it POSTs your `finalQuery` to the **returned webhook URL** with headers:
  - `Authorization: <accessToken>`
  - `Content-Type: application/json`
  Body:
  ```json
  {"finalQuery": "YOUR_SQL_QUERY_HERE"}
  ```

## Notes
- The challenge text also mentions a fixed submit URL; this implementation **uses the returned `webhook` URL** as instructed.
- If the API expects `Bearer <token>`, change the header logic in `WebhookService` accordingly (currently it sends the token raw as specified).
- Timeouts: connect 10s, read 20s.
- No external endpoints are exposed by this app.

## Repo packaging
- Include the built JAR (`target/bfh-springboot-webhook-1.0.0.jar`) in your public GitHub repo and share the raw downloadable link.
