# Gym CRM: Docker bilan ishga tushirish

Ilova (Spring Boot, port 8081) va PostgreSQL alohida container'larda, `gym-net` network'ida ishlaydi.
Ilova DB'ga `postgres` (compose service nomi) orqali ulanadi. Login/parol image ichida yo'q, `.env` orqali beriladi.

## 1. Sozlash (bir marta)
```
copy .env.example .env      # Windows
cp .env.example .env        # Linux/Mac
```
Kerak bo'lsa `.env` ichidagi parolni o'zgartiring.

## 2. Build va run
```
docker compose up --build -d
```

## 3. Tekshirish
```
docker compose ps
curl.exe http://localhost:8081/actuator/health
docker compose logs -f gym-crm
```

## 4. REST API
```
curl.exe -X POST http://localhost:8081/api/v1/trainees -H "Content-Type: application/json" -d "{\"firstName\":\"John\",\"lastName\":\"Doe\",\"dateOfBirth\":\"2000-01-15\",\"address\":\"Tashkent\"}"
curl.exe -u USERNAME:PASSWORD http://localhost:8081/api/v1/trainees/USERNAME
curl.exe -u USERNAME:PASSWORD http://localhost:8081/api/v1/training-types
```
(`USERNAME` va `PASSWORD` POST javobida qaytadi.) Swagger UI: http://localhost:8081/swagger-ui.html

## 5. Ma'lumot DB'da ekanini ko'rish
```
docker exec -it gym-postgres psql -U gym_user -d gymdb -c "SELECT id, username, first_name FROM users;"
```
(`gym_user` va `gymdb` o'rniga `.env` dagi qiymatlarni yozing.)

## 6. Stop / restart
```
docker compose stop
docker compose start
docker compose logs --tail 30 gym-crm
```
Ma'lumotlar `gym-pgdata` volume'da saqlanadi, restartdan keyin ham qoladi.

## 7. Diagnostika (ixtiyoriy)
```
docker exec -it gym-crm-app sh
```

## 8. To'liq to'xtatish
```
docker compose down        # container'lar o'chadi, ma'lumot qoladi
docker compose down -v     # ma'lumot ham o'chadi
```