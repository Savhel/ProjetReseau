# Dockerfile pour l'application Resource Management
FROM openjdk:21-jdk-slim

# Métadonnées
LABEL maintainer="Resource Management Team"
LABEL description="Resource Management Service with Redis Cache"
LABEL version="1.0.0"

# Variables d'environnement
ENV SPRING_PROFILES_ACTIVE=docker
ENV JAVA_OPTS="-Xmx1024m -Xms512m"
ENV SERVER_PORT=8081

# Créer un utilisateur non-root pour la sécurité
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Répertoire de travail
WORKDIR /app

# Copier les fichiers de configuration Maven
COPY pom.xml ./
COPY .mvn .mvn
COPY mvnw ./

# Rendre le script mvnw exécutable
RUN chmod +x mvnw

# Télécharger les dépendances (optimisation du cache Docker)
RUN ./mvnw dependency:go-offline -B

# Copier le code source
COPY src ./src

# Construire l'application
RUN ./mvnw clean package -DskipTests -B

# Copier le JAR construit
RUN cp target/*.jar app.jar

# Nettoyer les fichiers de build pour réduire la taille de l'image
RUN rm -rf target .mvn mvnw pom.xml src

# Changer le propriétaire des fichiers
RUN chown -R appuser:appuser /app

# Basculer vers l'utilisateur non-root
USER appuser

# Exposer le port
EXPOSE $SERVER_PORT

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:$SERVER_PORT/api/actuator/health || exit 1

# Point d'entrée
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

# Commande par défaut
CMD []