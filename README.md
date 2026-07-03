# MboloPass

MboloPass est une PWA de passeport de sante numerique: compte securise, profil sante, carte numerique, QR code opaque, mode urgence limite, alertes de prevention et resume sante assiste par regles.

Slogan: **MboloPass - Votre passeport de sante numerique.**  
Sous-slogan: **Votre sante, votre acces.**

## Architecture

- `backend/`: Java 21, Spring Boot, Maven Wrapper, PostgreSQL, JWT.
- `frontend/`: React, TypeScript, Vite, React Router, PWA.
- `docs/`: architecture, API, securite, workflow.

## Prerequis

- Java 21
- PostgreSQL 18
- Base `mbolopass`
- Compte PostgreSQL `mbolopass_app`
- Node.js 20+ pour le frontend

## Configuration locale backend

CrÃƒÂ©er le fichier local suivant, sans le commiter:

```powershell
Copy-Item backend/src/main/resources/application-local.properties.example backend/src/main/resources/application-local.properties
```

Contenu attendu:

```properties
spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/mbolopass
spring.datasource.username=mbolopass_app
spring.datasource.password=CHANGE_ME

app.jwt.secret=CHANGE_ME_WITH_A_SECRET_OF_AT_LEAST_32_CHARACTERS
app.jwt.expiration-minutes=120
app.cors.allowed-origins=http://localhost:5173
```

## Demarrage backend

```powershell
cd backend
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local
```

Dans IntelliJ, le module a lancer est `backend`, avec la classe principale et le profil `local`:

```text
ga.cyber241.mbolopass.MboloPassApplication
```

Si PostgreSQL retourne `SQL State 57P03` avec le message `le systeme de bases de donnees est en cours de restauration`, l'application est correctement lancee mais PostgreSQL n'est pas encore pret. Attendez la fin de la restauration ou redemarrez proprement le service PostgreSQL avant de relancer MboloPass.

URLs:

- API: http://localhost:8080
- Health: http://localhost:8080/actuator/health

## Exemples API PowerShell

Register:

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/auth/register -ContentType "application/json" -Body '{"firstName":"Amina","lastName":"N.","email":"amina.demo@example.test","password":"Password123!"}'
```

Login:

```powershell
$login = Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/auth/login -ContentType "application/json" -Body '{"email":"amina.demo@example.test","password":"Password123!"}'
$headers = @{ Authorization = "Bearer $($login.accessToken)" }
```

Profil minimal:

```powershell
Invoke-RestMethod -Method Put -Uri http://localhost:8080/api/v1/health-profile/me -Headers $headers -ContentType "application/json" -Body '{"bloodType":"O+","lastMedicalVisitDate":"2026-07-03"}'
```

CrÃƒÂ©er un QR:

```powershell
$qr = Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/card/me/qr-token -Headers $headers
$qr.emergencyUrl
```

Fiche urgence:

```powershell
$token = ($qr.emergencyUrl -split "/")[-1]
Invoke-RestMethod -Method Get -Uri "http://localhost:8080/api/v1/public/emergency/$token"
```

## Tests

```powershell
cd backend
.\mvnw.cmd test
```

## Securite QR/NFC

Le QR code et le futur NFC ne contiennent jamais de donnees medicales ou personnelles. Ils contiennent uniquement une URL publique avec un jeton opaque. Le backend stocke seulement le hash SHA-256 du jeton, verifie son statut et limite strictement la reponse urgence.
## Creation du premier compte administrateur en local

Les routes publiques ne permettent jamais de creer un compte ADMIN. En developpement local, creez d'abord un utilisateur standard via `/inscription` ou `/api/v1/auth/register`, puis appliquez explicitement le script SQL local.

1. Appliquer le patch de schema admin, requis avec `ddl-auto=validate` :

```powershell
psql -h 127.0.0.1 -p 5432 -U mbolopass -d mbolopass -f backend/sql/admin_schema_patch.sql
```

2. Modifier l'adresse e-mail dans `backend/sql/promote_local_admin.sql`, puis promouvoir ce compte local :

```powershell
psql -h 127.0.0.1 -p 5432 -U mbolopass -d mbolopass -f backend/sql/promote_local_admin.sql
```

Ce script ne lit ni n'affiche aucun mot de passe. Il est reserve au developpement local.