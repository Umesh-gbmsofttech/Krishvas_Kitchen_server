# Krishva's Kitchen - Spring Boot Backend

Production backend for Krishva's Kitchen.

## Stack
- Spring Boot 3.3
- Spring Security + JWT
- Spring Data JPA (MySQL)
- Spring WebSocket (STOMP)
- Swagger/OpenAPI
- Render-ready Docker deployment

## Core APIs
- `/api/auth` register/login
- `/api/menus` daily menu, scheduler, suggestions, banners
- `/api/orders` place order, tracking, status updates, assignment
- `/api/delivery-partners` apply, approval, dashboard
- `/api/tracking` delivery live location updates
- `/api/payments` COD/UPI/mock payment records
- `/api/notifications` list, unread count, read update
- `/api/admin` dashboard, users, banners

## Realtime topics
- `/topic/notifications/user/{userId}`
- `/topic/notifications/admin`
- `/topic/orders/{orderId}`

WebSocket endpoint: `/ws`

## Local run
```bash
./mvnw spring-boot:run
```

## Environment variables
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `JWT_EXPIRATION_MS`
- `ADMIN_EMAIL`
- `ADMIN_PASSWORD`
- `ADMIN_NAME`

## Render deployment
- `Dockerfile` included
- `render.yaml` included
- Set DB/JWT env vars in Render dashboard

## Seed data
On first run:
- Admin user seeded from env vars
- 2 realistic sample menus + items seeded automatically
# Krishvas_Kitchen_server
