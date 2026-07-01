# backend-springboot
Backend Spring Boot du projet final Tontine.

## Prerequis

- Java 21
- Maven (le wrapper `mvnw` est inclus, pas besoin d'installer Maven)
- MySQL en local (ou Docker) pour l'execution de l'application

## Configuration

La configuration (`src/main/resources/application.yaml`) utilise des variables d'environnement avec des valeurs par defaut adaptees a un MySQL local :

| Variable      | Defaut       |
|---------------|--------------|
| `DB_HOST`     | `localhost`  |
| `DB_PORT`     | `3306`       |
| `DB_NAME`     | `tontine_db` |
| `DB_USERNAME` | `root`       |
| `DB_PASSWORD` | `root`       |
| `SERVER_PORT` | `8080`       |

La base `tontine_db` est creee automatiquement si elle n'existe pas.

## Lancer le projet

```bash
cd tontine-backend/tontine-backend
./mvnw spring-boot:run
```

## Lancer les tests

Les tests utilisent une base H2 en memoire (voir `src/test/resources/application.yaml`), aucune base MySQL n'est necessaire pour les faire tourner :

```bash
cd tontine-backend/tontine-backend
./mvnw test
```
