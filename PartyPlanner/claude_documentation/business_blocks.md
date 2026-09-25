# PartyPlanner — Blocs métier

Vue d'ensemble des domaines fonctionnels de l'application.
Chaque bloc dispose de sa propre documentation détaillée dans un fichier `bloc_*.md`.

---

## 1. Authentification (`bloc_auth.md`)
Inscription, connexion, session JWT, réinitialisation de mot de passe.
Point d'entrée de l'app, toutes les autres fonctionnalités en dépendent.

## 2. Événements (`bloc_events.md`)
Création, lecture, modification, suppression d'événements.
L'événement est l'entité centrale autour de laquelle tout s'organise.
Inclut l'écran d'accueil avec le calendrier.

## 3. Invitations (`bloc_invitations.md`)
Inviter des participants (inscrits ou contacts externes), RSVP, deep links.
Gère aussi la réconciliation compte/contact externe lors d'une inscription tardive.

## 4. Courses / Items (`bloc_items.md`)
Gestion collaborative de ce que chacun apporte à l'événement.
Deux sémantiques distinctes : demandes de l'organisateur vs apports spontanés des invités.

## 5. Covoiturage (`bloc_carpool.md`)
Coordination du transport entre participants.
Offres de covoiturage avec gestion des places et des passagers.

## 6. Chat (`bloc_chat.md`)
Messagerie de groupe en temps réel par événement via WebSocket.
Accès restreint aux participants confirmés.

## 7. Profil utilisateur (`bloc_profile.md`)
Gestion du profil, préférences, thème de l'application.

## 8. Infrastructure transversale (`bloc_infra.md`)
Navigation (Decompose), injection de dépendances (Koin), stockage local (SQLDelight),
bus d'événements auth, configuration réseau Ktor client.

---

## Fonctionnalités prévues (non implémentées)

| Fonctionnalité | Statut | Phase |
|---|---|---|
| Push notifications FCM | ⬜ À faire | Phase 4 |
| Pot commun | ⬜ À faire | Phase future |
| Playlist collaborative | ⬜ À faire | Phase future |
| Publication Google Play | ⬜ À faire | Phase 5 |
| Publication TestFlight/App Store | ⬜ À faire | Phase 5 |
| RGPD / Suppression de compte | ⬜ À faire | Phase 5 |
