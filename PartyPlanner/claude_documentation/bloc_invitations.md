# Bloc métier — Invitations

## Rôle
Gère la liste des participants à un événement. Permet d'inviter des utilisateurs enregistrés ou des contacts externes (email/téléphone). Gère les RSVP et la réconciliation des comptes.

## Périmètre fonctionnel
- Invitation d'un utilisateur enregistré (par userId ou email)
- Invitation d'un contact externe (email ou téléphone uniquement)
- Token d'invitation unique par événement (lien partageable)
- RSVP : ACCEPTED / DECLINED / MAYBE (par l'invité)
- Écran RSVP pour invité non connecté (lecture du token via deep link)
- Liste des invités dans l'onglet "Invités" de l'EventDetail
- Stats : nombre total d'invités + confirmés (ACCEPTED)
- Suggestions d'utilisateurs lors de l'invitation (recherche par nom)
- Nettoyage automatique des `ItemsBrought` quand un invité passe en DECLINED

## Fichiers clés

### Shared (domain)
- `shared/domain/model/Invitation.kt` — entité + enum InvitationStatus
- `shared/domain/model/UserSuggestion.kt` — résultat de recherche utilisateur
- `shared/domain/repository/InvitationRepository.kt`
- `shared/domain/usecase/invitation/GetInviteInfoUseCase.kt`
- `shared/domain/usecase/invitation/RsvpToInvitationUseCase.kt`
- `shared/domain/usecase/invitation/GetEventInvitationsUseCase.kt`
- `shared/domain/usecase/invitation/InviteByEmailUseCase.kt`
- `shared/domain/usecase/invitation/InviteByUserIdUseCase.kt`
- `shared/domain/usecase/invitation/GetInviteSuggestionsUseCase.kt`

### Shared (data)
- `shared/data/remote/InvitationApi.kt`
- `shared/data/remote/dto/InvitationDtos.kt`
- `shared/data/repository/InvitationRepositoryImpl.kt`

### Shared (présentation)
- `shared/presentation/invitation/InvitationComponent.kt` + `InvitationState.kt` + `DefaultInvitationComponent.kt`

### UI
- `composeApp/ui/invitation/InvitationScreen.kt` — écran RSVP standalone (deep link)

### Backend
- `backend/routes/InvitationRoutes.kt`
- `backend/services/InvitationService.kt`
- `backend/db/tables/Invitations.kt`

## Schéma de données

```kotlin
object Invitations : IntIdTable("invitations") {
    val eventId       = reference("event_id", Events)
    val invitedUserId = reference("invited_user_id", Users).nullable()  // null si contact externe
    val invitedEmail  = varchar("invited_email", 255).nullable()
    val invitedPhone  = varchar("invited_phone", 20).nullable()
    val status        = enumerationByName("status", 20, InvitationStatus::class)
    val token         = varchar("token", 100).uniqueIndex()             // token unique par événement
    val sentAt        = datetime("sent_at")
    val respondedAt   = datetime("responded_at").nullable()
}

enum class InvitationStatus { PENDING, ACCEPTED, DECLINED, MAYBE }
```

## Routes API

| Méthode | Route | Description |
|---|---|---|
| GET | `/events/{id}/invitations` | Liste des invités d'un événement |
| POST | `/events/{id}/invitations` | Inviter quelqu'un |
| PUT | `/events/{id}/invitations/{iid}` | Changer le statut RSVP |
| DELETE | `/events/{id}/invitations/{iid}` | Retirer une invitation |
| GET | `/invite/{token}` | Info invitation via token (public) |
| POST | `/invite/{token}/rsvp` | Répondre via token (public) |
| GET | `/i/{token}` | Page HTML redirect (public, pour deep link partageable) |

## Règles métier
- Un token est généré par événement, pas par invité individuel
- Un contact externe (email/phone uniquement) peut être réconcilié avec un compte lors de son inscription
- Seul l'organisateur peut voir la liste complète des invités et gérer les invitations
- Quand un invité passe en DECLINED : ses `ItemsBrought` pour cet événement sont supprimés automatiquement
- L'accès aux onglets Courses, Covoit, Chat est restreint aux participants ACCEPTED + organisateur

## Deep link d'invitation

```
Flux partagé : http://[serveur]/i/{token}
  → Page HTML avec bouton "Ouvrir dans PartyPlanner"
  → Redirect JS vers partyplanner://invite/{token}
  → MainActivity intercepte l'intent et navigue vers InvitationScreen
```
