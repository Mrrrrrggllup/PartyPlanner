# Bloc métier — Authentification

## Rôle
Point d'entrée de l'application. Gère l'identité des utilisateurs, la session JWT, et la réinitialisation de mot de passe.

## Périmètre fonctionnel
- Inscription (email, mot de passe, displayName, téléphone optionnel)
- Connexion (email + mot de passe)
- Déconnexion
- Mot de passe oublié (envoi de mail via Resend)
- Réinitialisation de mot de passe (deep link `partyplanner://reset-password?token=...`)
- Persistance de session (token JWT + userId stockés dans SQLDelight)

## Fichiers clés

### Shared (domain)
- `shared/domain/model/User.kt` — entité utilisateur
- `shared/domain/repository/AuthRepository.kt` — interface
- `shared/domain/usecase/auth/LoginUseCase.kt`
- `shared/domain/usecase/auth/RegisterUseCase.kt`
- `shared/domain/usecase/auth/LogoutUseCase.kt`
- `shared/domain/usecase/auth/ForgotPasswordUseCase.kt`
- `shared/domain/usecase/auth/ResetPasswordUseCase.kt`

### Shared (data)
- `shared/data/remote/AuthApi.kt` — appels HTTP
- `shared/data/remote/dto/AuthDtos.kt` — RegisterRequest, LoginRequest, AuthResponse
- `shared/data/repository/AuthRepositoryImpl.kt`
- `shared/data/local/SessionStorage.kt` — persistance token + userId (SQLDelight)

### Shared (présentation)
- `shared/presentation/auth/AuthComponent.kt`
- `shared/presentation/auth/DefaultAuthComponent.kt`
- `shared/presentation/auth/ForgotPasswordComponent.kt`
- `shared/presentation/auth/DefaultForgotPasswordComponent.kt`
- `shared/presentation/auth/ResetPasswordComponent.kt`
- `shared/presentation/auth/DefaultResetPasswordComponent.kt`
- `shared/util/AuthEventBus.kt` — signale une déconnexion forcée (ex : token expiré)

### UI
- `composeApp/ui/auth/AuthScreen.kt` — formulaire login/register
- `composeApp/ui/auth/ForgotPasswordScreen.kt`
- `composeApp/ui/auth/ResetPasswordScreen.kt`

### Android
- `composeApp/src/androidMain/MainActivity.kt` — gestion du deep link reset-password

### Backend
- `backend/routes/AuthRoutes.kt` — POST /auth/register, POST /auth/login, POST /auth/forgot-password, POST /auth/reset-password
- `backend/services/AuthService.kt` — BCrypt, génération JWT, vérification token
- `backend/services/PasswordResetService.kt` — génération token reset, envoi mail, vérification expiration
- `backend/db/tables/Users.kt`
- `backend/db/tables/PasswordReset.kt` — `password_reset_tokens` (userId, token UNIQUE, expiresAt, usedAt)
- `backend/plugins/Security.kt` — configuration JWT Ktor

## Schéma de données

```kotlin
object Users : IntIdTable("users") {
    val email        = varchar("email", 255).uniqueIndex()
    val phone        = varchar("phone", 20).nullable()
    val displayName  = varchar("display_name", 100)
    val passwordHash = varchar("password_hash", 255)
    val createdAt    = datetime("created_at")
}

// Table temporaire, nettoyée après usage
object PasswordResetTokens : IntIdTable("password_reset_tokens") {
    val userId    = reference("user_id", Users)
    val token     = varchar("token", 100).uniqueIndex()
    val expiresAt = datetime("expires_at")
    val usedAt    = datetime("used_at").nullable()
}
```

## Flux principal

```
Register/Login → JWT token → SessionStorage (SQLDelight)
                              ↓
                    Toutes les requêtes API ajoutent le header Authorization: Bearer <token>
                              ↓
                    AuthEventBus.logout() si 401 → retour écran auth
```

## Règles métier
- La déconnexion efface le token local (SessionStorage)
- Le lien de reset-password expire (durée configurable dans `application.conf`)
- L'endpoint `/auth/forgot-password` répond toujours 200 (même si email inconnu) pour éviter l'enumeration
- Le mail de reset est envoyé via l'API Resend (pas de SDK, `java.net.http.HttpClient` JVM 17)

## Variables d'environnement backend
```
JWT_SECRET=...
RESEND_API_KEY=...
RESEND_FROM_EMAIL=...
APP_BASE_URL=...
```
