# Bonnes pratiques — Collaboration Claude / PartyPlanner

## Règle n°1 : Demander des précisions avant d'agir

En cas de doute sur le périmètre, l'approche technique, le comportement attendu ou l'impact d'une modification, **toujours demander des précisions à l'utilisateur** avant de prendre une décision arbitraire.

Ne jamais supposer ou deviner l'intention derrière une demande ambiguë. Mieux vaut une question courte qu'une implémentation incorrecte à défaire.

Exemples de situations qui nécessitent une clarification :
- Choix entre deux approches techniques équivalentes non documentées
- Modification qui touche plusieurs blocs métier à la fois
- Comportement attendu non spécifié (ex : que se passe-t-il si l'utilisateur fait X ?)
- Suppression ou refactoring de code potentiellement utilisé ailleurs
- Toute décision d'architecture non couverte par `CLAUDE.md` ou les docs métier

---

## Règle n°2 : Documenter les opérations au fil de l'eau

**Toute opération significative doit être documentée dans un fichier `.md`** dans ce dossier `claude_documentation/`, au moment où elle est réalisée.

### Quoi documenter
- Nouvelles fonctionnalités implémentées (backend + shared + UI)
- Modifications de schéma de base de données
- Décisions techniques prises pendant l'implémentation
- Bugs identifiés et corrigés (avec leur cause)
- Routes API ajoutées ou modifiées
- Changements de navigation ou de structure de composants

### Où documenter
- **Avancement** : mettre à jour `progress.md` pour chaque fonctionnalité terminée
- **Bloc métier** : mettre à jour le fichier `bloc_*.md` correspondant si la structure change
- **Opérations en cours** : créer un fichier `ops_YYYY-MM-DD_sujet.md` pour les sessions de travail avec plusieurs étapes

### Format recommandé pour un fichier d'opération

```markdown
# Opération : [titre court]

_Date : YYYY-MM-DD_

## Objectif
[Ce qui est demandé / le problème à résoudre]

## Périmètre
[Fichiers / modules impactés]

## Étapes réalisées
- [ ] Étape 1
- [ ] Étape 2

## Décisions prises
- [Décision] — [Pourquoi]

## Points d'attention
- [Ce qui pourrait casser / effets de bord connus]
```

---

## Règle n°3 : Respecter l'architecture existante

Toute nouvelle fonctionnalité doit suivre les patterns déjà en place :
- Use cases dans `shared/domain/usecase/`
- Repository interface dans `shared/domain/repository/`, impl dans `shared/data/repository/`
- API client dans `shared/data/remote/`, DTOs dans `shared/data/remote/dto/`
- Composable UI dans `composeApp/ui/`
- Component (Decompose) dans `shared/presentation/`

Ne pas introduire de nouveau pattern sans validation préalable.

---

## Règle n°4 : Lire la documentation au début de chaque nouvelle conversation

**Au début de chaque renouvellement de contexte (nouvelle conversation), lire systématiquement :**

1. `claude_documentation/CLAUDE.md` — stack technique, modèle de données, décisions d'architecture
2. `claude_documentation/progress.md` — état d'avancement par phase
3. `claude_documentation/business_blocks.md` — vue d'ensemble des blocs métier
4. Les fichiers `bloc_*.md` concernés par la tâche en cours
5. Les fichiers `ops_*.md` récents s'il en existe (opérations en cours ou récemment terminées)

Ne jamais démarrer une session de développement sans avoir lu ces documents. Ils contiennent l'état réel du projet et les décisions prises — l'ignorer risque de produire du code incohérent avec l'existant.

---

## Règle n°5 : Sécurité et données

- Ne jamais commiter de secrets (JWT secret, clés API, mots de passe)
- Le fichier `.env` backend ne doit pas être versionné
- `keystore.properties` ne doit pas être versionné
- Toujours incrémenter `versionCode` ET `versionName` avant un déploiement Firebase App Distribution
