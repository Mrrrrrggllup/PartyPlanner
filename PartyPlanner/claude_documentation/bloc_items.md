# Bloc métier — Courses (Items)

## Rôle
Permet de gérer collaborativement ce que chaque participant apporte à l'événement. Deux sémantiques distinctes coexistent : les demandes de l'organisateur (ItemRequest) et les apports spontanés des invités (ItemBrought).

## Périmètre fonctionnel
- Créer une demande ("on a besoin de X") — organisateur et participants ACCEPTED
- Marquer une demande comme remplie (fulfill) — n'importe quel participant
- Déclarer spontanément ce qu'on apporte (ItemBrought) — n'importe quel participant
- Supprimer ses propres entrées
- Catégorisation des items (arbre max 2 niveaux, seedé au démarrage)
- Groupement visuel par catégorie dans la liste
- Badge onglet "Courses" : nombre d'items non remplis
- Tracking "vu / non vu" : badge disparaît quand l'onglet est ouvert

## Fichiers clés

### Shared (domain)
- `shared/domain/model/Item.kt` — sealed class `EventItem` (ItemRequest | ItemBrought)
- `shared/domain/model/ItemCategory.kt` — catégorie avec parentId optionnel
- `shared/domain/repository/ItemRepository.kt`
- `shared/domain/usecase/item/GetItemsUseCase.kt`
- `shared/domain/usecase/item/AddItemRequestUseCase.kt`
- `shared/domain/usecase/item/AddItemBroughtUseCase.kt`
- `shared/domain/usecase/item/FulfillItemRequestUseCase.kt`
- `shared/domain/usecase/item/DeleteItemRequestUseCase.kt`
- `shared/domain/usecase/item/DeleteItemBroughtUseCase.kt`
- `shared/domain/usecase/item/GetCategoriesUseCase.kt`
- `shared/domain/usecase/item/MarkItemsSeenUseCase.kt`

### Shared (data)
- `shared/data/remote/ItemApi.kt`
- `shared/data/remote/dto/ItemDtos.kt`
- `shared/data/repository/ItemRepositoryImpl.kt`

### UI
- Onglet "Courses" dans `composeApp/ui/event/EventDetailScreen.kt`
- `AddItemSheet` — sheet ajout avec sélecteur de catégorie (chips) + compteur quantité

### Backend
- `backend/routes/ItemRoutes.kt`
- `backend/services/ItemService.kt`
- `backend/db/tables/Items.kt` — tables `item_requests` + `items_brought`
- `backend/db/tables/ItemCategories.kt`

## Schéma de données

```kotlin
// Arbre de catégories (max 2 niveaux)
// Seedé au démarrage : Nourriture, Boissons, Desserts, Matériel, Autre
object ItemCategories : IntIdTable("item_categories") {
    val label    = varchar("label", 100)
    val parentId = reference("parent_id", ItemCategories).nullable()
    val icon     = varchar("icon", 50).nullable()
}

// Demandé par l'organisateur (ou tout participant)
object ItemRequests : IntIdTable("item_requests") {
    val eventId      = reference("event_id", Events)
    val label        = varchar("label", 200)
    val quantity     = integer("quantity").default(1)
    val categoryId   = reference("category_id", ItemCategories).nullable()
    val assignedToId = reference("assigned_to", Users).nullable()
    val isFulfilled  = bool("is_fulfilled").default(false)
}

// Déclaré spontanément par un invité
object ItemsBrought : IntIdTable("items_brought") {
    val eventId    = reference("event_id", Events)
    val userId     = reference("user_id", Users)
    val label      = varchar("label", 200)
    val quantity   = integer("quantity").default(1)
    val categoryId = reference("category_id", ItemCategories).nullable()
}

// Colonne price présente en base sur ItemRequests (réservée pot commun futur, non exposée en API)
```

## Routes API

| Méthode | Route | Description |
|---|---|---|
| GET | `/items/categories` | Liste globale des catégories (authentifié) |
| GET | `/events/{id}/items` | Items de l'événement (requests + brought) |
| POST | `/events/{id}/items` | Ajouter un item request ou brought |
| POST | `/events/{id}/items/requests/{rid}/fulfill` | Marquer une demande comme remplie |
| DELETE | `/events/{id}/items/requests/{rid}` | Supprimer une demande |
| DELETE | `/events/{id}/items/brought/{bid}` | Supprimer un apport spontané |

## Règles métier
- Tout participant ACCEPTED (+ organisateur) peut ajouter des items
- `isFulfilled` passe à true via `/fulfill` : n'importe quel participant peut le faire
- Les ItemsBrought d'un participant sont supprimés automatiquement s'il passe en DECLINED
- Tri : `ORDER BY categoryId ASC NULLS LAST, id ASC`
- Badge onglet = nombre d'ItemRequests non remplis (isFulfilled = false)
- Le label placeholder dans AddItemSheet est "chips, jus d'orange…" (sans alcool)
- La colonne `price` existe en base mais n'est pas exposée (réservée pour le pot commun futur)
