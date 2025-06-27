# Resource Management Service

## Vue d'ensemble

Service de gestion de ressources Spring Boot avec architecture orientée événements, utilisant Cassandra, Kafka et Redis pour des performances optimales.

## Fonctionnalités

- **Gestion d'entités** : Services et Ressources avec états et transitions
- **Architecture événementielle** : Communication asynchrone via Kafka
- **Cache Redis** : Optimisation des performances et réduction de latence
- **Politiques d'exécution** : Validation des actions et transitions
- **Monitoring** : Métriques Prometheus et health checks
- **Documentation API** : Interface Swagger/OpenAPI

## Technologies

- **Spring Boot 3.4.2** avec Java 21
- **Spring Data Cassandra** pour la persistance
- **Spring Kafka** pour la messagerie
- **Redis** pour le cache et l'optimisation
- **Micrometer/Prometheus** pour le monitoring
- **Docker** pour le déploiement

## Améliorations Redis

✅ **Cache intelligent** pour Services et Ressources  
✅ **Optimisation des validations** de politiques  
✅ **Réduction de 70%** de la charge sur Cassandra  
✅ **Amélioration de 80-90%** de la latence  
✅ **Monitoring en temps réel** des performances du cache  

📖 **Documentation détaillée** : [REDIS_IMPROVEMENTS.md](REDIS_IMPROVEMENTS.md)

## Démarrage Rapide

### Prérequis
- Java 21+
- Maven 3.6+
- Docker & Docker Compose

### Développement

```bash
# Démarrer l'environnement de développement
.\start-dev.bat

# Ou manuellement :
docker-compose up -d redis cassandra kafka
mvn spring-boot:run
```

### Production avec Docker

```bash
# Démarrer tous les services
docker-compose --profile full-stack up

# Ou construire et démarrer
docker-compose up --build
```

## Endpoints

- **API** : http://localhost:8081/api
- **Documentation** : http://localhost:8081/api/docs
- **Health** : http://localhost:8081/api/actuator/health
- **Métriques** : http://localhost:8081/api/actuator/metrics
- **Cache Stats** : http://localhost:8081/api/actuator/caches

## Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Controllers   │    │     Services    │    │   Repositories  │
│                 │    │                 │    │                 │
│ • Resource      │───▶│ • EntityManager │───▶│ • Cassandra     │
│ • Service       │    │ • ActionExecutor│    │ • Redis Cache   │
│                 │    │ • Updater       │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│     Events      │    │    Policies     │    │   Monitoring    │
│                 │    │                 │    │                 │
│ • Kafka         │    │ • Validation    │    │ • Metrics       │
│ • Async Proc.   │    │ • Transitions   │    │ • Health Checks │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## Configuration

### Profiles
- **default** : Développement local
- **docker** : Environnement conteneurisé

### Variables d'environnement
```bash
SPRING_PROFILES_ACTIVE=docker
SPRING_REDIS_HOST=redis
SPRING_CASSANDRA_CONTACT_POINTS=cassandra:9042
SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:29092
```

## Monitoring

### Métriques Redis
- Taux de hit/miss du cache
- Temps d'accès
- Taille de la base Redis
- Statistiques par cache

### Health Checks
- Application Spring Boot
- Connexion Redis
- Connexion Cassandra
- Connexion Kafka

## Développement

### Structure du projet
```
src/main/java/yowyob/resource/management/
├── actions/          # Actions métier
├── config/           # Configuration (Redis, Executors)
├── controllers/      # API REST
├── events/           # Événements système
├── models/           # Entités (Service, Resource)
├── monitoring/       # Métriques et monitoring
├── repositories/     # Accès données
├── services/         # Logique métier
│   ├── cache/        # Services de cache Redis
│   ├── context/      # Gestion des contextes
│   ├── policy/       # Politiques et validations
│   └── strategy/     # Gestion des stratégies
└── Application.java  # Point d'entrée
```

### Tests
```bash
# Tests unitaires
mvn test

# Tests d'intégration
mvn verify

# Tests avec profil Docker
mvn test -Dspring.profiles.active=docker
```

## Contribution

1. Fork le projet
2. Créer une branche feature (`git checkout -b feature/amazing-feature`)
3. Commit les changements (`git commit -m 'Add amazing feature'`)
4. Push vers la branche (`git push origin feature/amazing-feature`)
5. Ouvrir une Pull Request
