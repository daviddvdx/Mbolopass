# Backend MboloPass

Backend Spring Boot Java 21 pour le parcours:
inscription patient -> connexion JWT -> profil santÃ© minimal -> carte MboloPass -> QR opaque -> fiche urgence limitÃ©e.

## PrÃ©requis

- Java 21
- PostgreSQL 18
- Base `mbolopass`
- Compte PostgreSQL `mbolopass_app`

## Configuration locale

CrÃ©er le fichier local ignorÃ© par Git:

```powershell
Copy-Item src/main/resources/application-local.properties.example src/main/resources/application-local.properties
```

Renseigner uniquement localement:

```properties
spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/mbolopass
spring.datasource.username=mbolopass_app
spring.datasource.password=CHANGE_ME

app.jwt.secret=CHANGE_ME_WITH_A_SECRET_OF_AT_LEAST_32_CHARACTERS
app.jwt.expiration-minutes=120
app.cors.allowed-origins=http://localhost:5173
```

## DÃ©marrage

```powershell
cd backend
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local
```

Dans IntelliJ, configurez:

```text
Main class: ga.cyber241.mbolopass.MboloPassApplication
Active profiles: local
Working directory: backend
```

Si PostgreSQL retourne `SQL State 57P03` avec le message `le systeme de bases de donnees est en cours de restauration`, la base ne peut pas encore accepter de connexion. Attendez que PostgreSQL termine sa restauration ou redemarrez proprement le service PostgreSQL, puis relancez l'application.

API: http://localhost:8080  
Health: http://localhost:8080/actuator/health

## Tests

```powershell
cd backend
.\mvnw.cmd test
```

## Exemples PowerShell

Register:

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/auth/register -ContentType "application/json" -Body '{"firstName":"Amina","lastName":"N.","email":"amina.demo@example.test","password":"Password123!"}'
```

Login:

```powershell
$login = Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/auth/login -ContentType "application/json" -Body '{"email":"amina.demo@example.test","password":"Password123!"}'
$headers = @{ Authorization = "Bearer $($login.accessToken)" }
```

CrÃ©er le profil minimal:

```powershell
Invoke-RestMethod -Method Put -Uri http://localhost:8080/api/v1/health-profile/me -Headers $headers -ContentType "application/json" -Body '{"bloodType":"O+","lastMedicalVisitDate":"2026-07-03"}'
```

CrÃ©er un QR:

```powershell
$qr = Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/card/me/qr-token -Headers $headers
$qr.emergencyUrl
```

Ouvrir la fiche urgence:

```powershell
$token = ($qr.emergencyUrl -split "/")[-1]
Invoke-RestMethod -Method Get -Uri "http://localhost:8080/api/v1/public/emergency/$token"
```

