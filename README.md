# MboloPass

MboloPass est une plateforme de passeport de santé numérique sécurisé permettant de centraliser les informations essentielles, gérer les profils et les personnes à charge, accéder à une carte santé avec QR Code et faciliter la prise en charge en cas d’urgence.

MboloPass est une PWA de passeport de santé numérique avec compte sécurisé, profil santé, carte numérique, QR Code opaque, mode urgence limité, alertes de prévention et résumé santé assisté par règles.

**Slogan :** MboloPass — Votre passeport de santé numérique.
**Sous-slogan :** Votre santé, votre accès.

## Architecture

* `backend/` : Java 21, Spring Boot, Maven Wrapper, PostgreSQL, JWT.
* `frontend/` : React, TypeScript, Vite, React Router, PWA.
* `docs/` : architecture, API, sécurité, workflow.

## Prérequis

* Java 21
* PostgreSQL 18
* Base de données `mbolopass`
* Compte PostgreSQL `mbolopass_app`
* Node.js 20+ pour le frontend

## Configuration locale du backend

Créer le fichier local suivant, sans le commiter :

```powershell
Copy-Item backend/src/main/resources/application-local.properties.example backend/src/main/resources/application-local.properties
```

Contenu attendu :

```properties
spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/mbolopass
spring.datasource.username=mbolopass_app
spring.datasource.password=CHANGE_ME

app.jwt.secret=CHANGE_ME_WITH_A_SECRET_OF_AT_LEAST_32_CHARACTERS
app.jwt.expiration-minutes=120
app.cors.allowed-origins=http://localhost:5173
```

## Démarrage du backend

```powershell
cd backend
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local
```

Dans IntelliJ, le module à lancer est `backend`, avec la classe principale suivante et le profil `local` :

```text
ga.cyber241.mbolopass.MboloPassApplication
```

Si PostgreSQL retourne `SQL State 57P03` avec le message indiquant que le système de bases de données est en cours de restauration, l’application est correctement lancée mais PostgreSQL n’est pas encore prêt. Attends la fin de la restauration ou redémarre proprement le service PostgreSQL avant de relancer MboloPass.

## URLs locales

* API : `http://localhost:8080`
* Health check : `http://localhost:8080/actuator/health`

## Exemples API PowerShell

### Création de compte

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/auth/register -ContentType "application/json" -Body '{"firstName":"Amina","lastName":"N.","email":"amina.demo@example.test","password":"Password123!"}'
```

### Connexion

```powershell
$login = Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/auth/login -ContentType "application/json" -Body '{"email":"amina.demo@example.test","password":"Password123!"}'
$headers = @{ Authorization = "Bearer $($login.accessToken)" }
```

### Mise à jour du profil santé minimal

```powershell
Invoke-RestMethod -Method Put -Uri http://localhost:8080/api/v1/health-profile/me -Headers $headers -ContentType "application/json" -Body '{"bloodType":"O+","lastMedicalVisitDate":"2026-07-03"}'
```

### Génération d’un QR Code d’urgence

```powershell
$qr = Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/card/me/qr-token -Headers $headers
$qr.emergencyUrl
```

### Consultation d’une fiche d’urgence

```powershell
$token = ($qr.emergencyUrl -split "/")[-1]
Invoke-RestMethod -Method Get -Uri "http://localhost:8080/api/v1/public/emergency/$token"
```

## Tests

```powershell
cd backend
.\mvnw.cmd test
```

## Sécurité QR Code et NFC

Le QR Code et le futur NFC ne contiennent jamais de données médicales ou personnelles. Ils contiennent uniquement une URL publique avec un jeton opaque.

Le backend stocke uniquement le hash SHA-256 du jeton, vérifie son statut et limite strictement les informations exposées dans la réponse d’urgence.

## Création du premier compte administrateur en local

Les routes publiques ne permettent jamais de créer un compte `ADMIN`.

En développement local, crée d’abord un utilisateur standard via `/inscription` ou `/api/v1/auth/register`, puis applique explicitement le script SQL local.

1. Appliquer le patch de schéma administrateur, requis avec `ddl-auto=validate` :

```powershell
psql -h 127.0.0.1 -p 5432 -U mbolopass -d mbolopass -f backend/sql/admin_schema_patch.sql
```

2. Modifier l’adresse e-mail dans `backend/sql/promote_local_admin.sql`, puis promouvoir ce compte local :

```powershell
psql -h 127.0.0.1 -p 5432 -U mbolopass -d mbolopass -f backend/sql/promote_local_admin.sql
```

Ce script ne lit ni n’affiche aucun mot de passe. Il est réservé au développement local.
