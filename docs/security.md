# Sécurité

## Données de santé

Les API protégées exigent un JWT. Un patient ne peut accéder qu'à son propre profil. Les mots de passe sont hashés avec BCrypt.

## QR et NFC

QR et NFC ne sont pas des contenants de données médicales. Ils servent uniquement de clé d'accès vers une fiche urgence limitée. Ils doivent être renouvelables, révocables et expirants.

Le token brut n'est jamais stocké. Seul son hash SHA-256 est conservé en base. Un token révoqué ou expiré ne doit jamais exposer de données.

## Mode urgence

La réponse publique est limitée au contexte vital: prénom et initiale, groupe sanguin, allergies HIGH/CRITICAL, pathologies actives, médicaments critiques et contact principal. Elle ne retourne pas l'email, l'adresse, l'historique ou les documents.

## Logs

Les logs applicatifs ne doivent jamais contenir allergies, pathologies, traitements ou contenus médicaux. Les accès urgence sont journalisés avec issue et hash d'IP.

## Secrets et CORS

Les secrets restent dans les fichiers locaux ignorés par Git ou dans les variables d'environnement. CORS est configurable par variable.

## Évolutions

Chiffrement de champs sensibles, MFA, consentement temporaire professionnel, audit renforcé et cookies HttpOnly pour l'authentification.
