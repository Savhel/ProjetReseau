# Documentation du Format de Stratégie

## Descriptif

Ce document décrit le format de stratégie utilisé pour la communication entre les autres microservices et le `services de gestion des Ressources` via Kafka.

## Structure de Base

```json
{
  "strategies": [
    {
      "entityId": "uuid",
      "actionType": "TYPE_ACTION",
      "actionClass": "CLASSE_ACTION",
      "query" : "string", //Optionnel
      "eventStartDateTime": "LocalDateTime", // Optionnel
      "params": {} // Optionnel
    }
  ]
}
```

## Champs Obligatoires

### `entityId`
- **Type**: Chaîne UUID
- **Description**: Identifiant unique de l'entité concernée par l'action
- **Exemple**: `"550e8400-e29b-41d4-a716-446655440000"`

### `actionType`
- **Valeurs possibles**: 
  - `CREATE`
  - `READ`
  - `UPDATE`
  - `DELETE`
  - `CUSTOM`
- **Description**: Type d'action à réaliser sur l'entité

### `actionClass`
- **Valeurs possibles**:
  - `Resource`
  - `Service`
- **Description**: Classe de l'objet sur lequel l'action est effectuée
- **Valeur par défaut**: `Resource`

## Champ Optionnel `params`

Le champ `params` est optionnel. Il peut contenir des informations supplémentaires comme le moment d'exécution d'une action.

### `eventStartDateTime`
- **Type**: Chaîne de date et heure - format date-time
- **Description**: Date et heure programmée pour l'exécution de l'action
- **Optionnel**: Est omis pour une exécution immédiate

## Exemples Complets

### 1. Création Immédiate d'une Ressource
```json
{
  "strategies": [
    {
      "entityId": "550e8400-e29b-41d4-a716-446655440000",
      "actionType": "CREATE",
      "actionClass": "Resource",
      "params": {
        "name": "John Doe",
        "email": "john.doe@example.com",
        "age": 30,
        "state" : "free"
      }
    }
  ]
}
```

### 2. Création Programmée d'un Service
```json
{
  "strategies": [
    {
      "entityId": "550e8400-e29b-41d4-a716-446655440001",
      "actionType": "CREATE",
      "actionClass": "Services",
      "eventStartDateTime": "2024-02-06T10:00:00",
      "params": {
        "name": "Voyage 45",
        "status": "Planned"
      }
    }
  ]
}
```

### 3. Action Personnalisée
```json
{
  "strategies": [
    {
      "entityId": "550e8400-e29b-41d4-a716-446655440002",
      "actionType": "CUSTOM",
      "actionClass": "Resource",
      "query": "SELECT * FROM resources WHERE status = 'free'"
    }
  ]
}
```

### 4. Plusieurs Actions dans une Stratégie
```json
{
  "strategies": [
    {
      "entityId": "550e8400-e29b-41d4-a716-446655440003",
      "actionType": "UPDATE",
      "actionClass": "Resource",
      "eventStartDateTime": "2024-02-06T11:30:00",
      "params": {
        "state" : "IN_USE"
      }
    },
    {
      "entityId": "550e8400-e29b-41d4-a716-446655440004",
      "actionType": "UPDATE",
      "actionClass": "Services",
      "eventStartDateTime": "2024-02-06T11:30:00",
      "params": {
        "state" : "ONGOING"
      }
    }
  ]
}
```

## Bonnes Pratiques

1. **Actions différées**
   - Utilisez `eventStartDateTime` pour programmer des actions à une date/heure spécifique
   - Le format doit suivre ISO 8601 (exemple: "2024-02-06T15:30:00Z")
   - Sans `eventStartDateTime`, l'action est exécutée immédiatement

2. **Actions personnalisées (CUSTOM)**
   - Limitez l'utilisation de `CUSTOM` aux cas ne pouvant pas être traités par les actions standards
   - Assurez-vous que la requête CQL est valide et testée
   - Documentez clairement l'objectif de la requête personnalisée

## Validation
Assurez-vous que vos messages respectent le schéma JSON fourni.
- `query` (String, requis si actionType=CUSTOM) : Requête CQL pour les actions personnalisées

## Messages d'erreur courants
- "query is required" : Manque de requête CQL pour une action CUSTOM
- "query is not allowed" : Présence d'une requête pour une action non-CUSTOM
- "invalid format uuid" : Format d'entityId incorrect
- "invalid enum value" : Valeur non autorisée pour actionType ou actionClass
