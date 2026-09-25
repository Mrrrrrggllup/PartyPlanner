# Bloc métier — Profil utilisateur

## Rôle
Gestion du compte utilisateur, des préférences visuelles (thème) et des informations personnelles.

## Périmètre fonctionnel
- Affichage du profil (displayName, email, téléphone)
- Changement de thème (clair / sombre / système)
- Déconnexion
- (Prévu Phase 5) Suppression de compte (RGPD)

## Fichiers clés

### Shared (présentation)
- `shared/presentation/profile/ProfileComponent.kt` + `ProfileState.kt` + `DefaultProfileComponent.kt`
- `shared/presentation/profile/ThemeManager.kt` — logique de switch de thème

### UI
- `composeApp/ui/profile/ProfileScreen.kt`

## Règles métier
- La déconnexion passe par `LogoutUseCase` qui vide le `SessionStorage` et émet sur `AuthEventBus`
- Le thème sélectionné doit persister entre les sessions (stockage local)
- La suppression de compte (Phase 5) devra supprimer toutes les données associées conformément au RGPD

## À faire (Phase 5)
- Politique de confidentialité accessible depuis l'écran profil
- Formulaire de suppression de compte avec confirmation
- Endpoint backend `DELETE /users/me` avec cascade suppression données
