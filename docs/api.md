# API MboloPass

Base URL: `http://localhost:8080`

## Auth

`POST /api/v1/auth/register`

```json
{
  "firstName": "Profil",
  "lastName": "Demonstration",
  "email": "demo@example.test",
  "password": "ChangeMe123"
}
```

`POST /api/v1/auth/login`

```json
{
  "email": "demo@example.test",
  "password": "ChangeMe123"
}
```

`GET /api/v1/auth/me`

## Profil

`GET /api/v1/health-profile/me`

`PUT /api/v1/health-profile/me`

```json
{
  "bloodType": "O+",
  "gender": "Non renseigne",
  "lastMedicalVisitDate": "2026-01-15"
}
```

Listes disponibles: `allergies`, `conditions`, `medications`, `vaccinations`, `emergency-contacts`.

## Carte et urgence

`GET /api/v1/card/me`

`POST /api/v1/card/me/qr-token`

```json
{
  "qrCodeUrl": "http://localhost:5173/emergency/opaque-token",
  "expiresAt": "2027-07-03T10:00:00Z"
}
```

`GET /api/v1/public/emergency/{token}`

La réponse ne contient aucune donnée non nécessaire au contexte d'urgence.

## Prévention

- `POST /api/v1/prevention/refresh`
- `GET /api/v1/prevention/alerts`
- `PATCH /api/v1/prevention/alerts/{id}/dismiss`

## Résumé assisté

- `POST /api/v1/ai-summary/regenerate`
- `GET /api/v1/ai-summary/latest`
