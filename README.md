# OTP Backend (Java / Spring Boot)

A standalone Java backend that generates OTPs, sends them via SMS (Fast2SMS by default), stores them temporarily, and verifies them. Use this **instead of** Firebase Phone Auth if you want full control over OTP logic, cost, or provider — this replaces the `PhoneAuthProvider` calls in the Android app with plain REST calls to your own server.

---

## How it works

```
Android App                         Your Backend                    SMS Provider
    |                                     |                                |
    |--- POST /api/otp/send ------------->|                                |
    |     { "phone": "+91..." }           |--- generate 6-digit code ----->|
    |                                     |--- store code + 5 min expiry   |
    |                                     |--- send SMS ------------------>|--- delivers SMS
    |<---- { success: true } -------------|                                |
    |                                     |                                |
    |  (user reads SMS, types code)       |                                |
    |                                     |                                |
    |--- POST /api/otp/verify ----------->|                                |
    |   { "phone": "+91...", "code": .. } |--- check match + expiry        |
    |<---- { success: true/false } -------|                                |
```

## Endpoints

### `POST /api/otp/send`
```json
{ "phone": "+919876543210" }
```
Response:
```json
{ "success": true, "message": "OTP sent successfully" }
```

### `POST /api/otp/verify`
```json
{ "phone": "+919876543210", "code": "482913" }
```
Response:
```json
{ "success": true, "message": "OTP verified successfully" }
```

---

## Setup

### 1. Get a Fast2SMS API key
- Sign up at [fast2sms.com](https://www.fast2sms.com/), verify your account.
- Go to **Dev API** in the dashboard → copy your **Authorization Key**.
- Paste it into `src/main/resources/application.properties`:
  ```
  fast2sms.api-key=YOUR_KEY_HERE
  ```
- Note: Fast2SMS's OTP route works with Indian 10-digit numbers. The code strips the `+91` prefix automatically.

### 2. Run the server
```bash
cd OtpBackend
./mvnw spring-boot:run
```
Server starts on `http://localhost:8080`.

### 3. Test with curl
```bash
curl -X POST http://localhost:8080/api/otp/send \
  -H "Content-Type: application/json" \
  -d '{"phone": "+919876543210"}'

curl -X POST http://localhost:8080/api/otp/verify \
  -H "Content-Type: application/json" \
  -d '{"phone": "+919876543210", "code": "482913"}'
```

---

## Calling this from your Android app

Instead of `PhoneAuthProvider.verifyPhoneNumber()`, your Android app now just hits these two REST endpoints. Simplest version using `HttpURLConnection` (or use Retrofit if you already have it in the Rental Book app):

```java
// Send OTP
new Thread(() -> {
    try {
        URL url = new URL("http://YOUR_SERVER_IP:8080/api/otp/send");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        String json = "{\"phone\":\"" + phone + "\"}";
        conn.getOutputStream().write(json.getBytes());

        int code = conn.getResponseCode(); // 200 = sent
    } catch (Exception e) {
        e.printStackTrace();
    }
}).start();
```

Verification works the same way against `/api/otp/verify`, sending `phone` + `code`. On success, your backend should also return a session token (JWT) so the Android app knows the user is logged in for future requests — that part isn't included here since it depends on your auth strategy, but I'm happy to add it if you want.

---

## Important notes

- **In-memory storage**: OTPs are stored in a `ConcurrentHashMap` inside `OtpService`. This is fine for local testing or a single server instance, but **restarting the server wipes all pending OTPs**, and it won't work if you run multiple server instances behind a load balancer. For production, swap this for **Redis** with a TTL matching the OTP expiry.
- **Rate limiting**: This sample doesn't limit how often `/send` can be called per phone number — add that before going to production, or people can spam SMS costs.
- **HTTPS**: Always run this behind HTTPS in production; phone numbers and OTPs should never travel over plain HTTP.
- **Switching SMS providers**: To use Twilio or MSG91 instead of Fast2SMS, create a new class implementing `SmsSender` (see `Fast2SmsSender.java` as a template) and annotate it `@Primary`, or remove `@Service` from `Fast2SmsSender`.
