@echo off
echo ========================================
echo  Resource Management - Development Setup
echo ========================================
echo.

echo [1/4] Checking Docker...
docker --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Docker is not installed or not running
    echo Please install Docker Desktop and try again
    pause
    exit /b 1
)
echo Docker is available

echo.
echo [2/4] Starting Redis and dependencies...
docker-compose up -d redis cassandra kafka zookeeper
if %errorlevel% neq 0 (
    echo ERROR: Failed to start services
    pause
    exit /b 1
)

echo.
echo [3/4] Waiting for services to be ready...
echo This may take a few minutes on first run...

:wait_loop
timeout /t 5 /nobreak >nul
docker-compose ps | findstr "healthy" >nul
if %errorlevel% neq 0 (
    echo Still waiting for services...
    goto wait_loop
)

echo.
echo [4/4] Services are ready!
echo.
echo ========================================
echo  Development Environment Status
echo ========================================
echo Redis:     http://localhost:6379
echo Cassandra: localhost:9042
echo Kafka:     localhost:9092
echo.
echo You can now start your Spring Boot application:
echo   mvn spring-boot:run
echo.
echo Or build and run with Docker:
echo   docker-compose --profile full-stack up
echo.
echo Monitoring endpoints (after app start):
echo   Health:     http://localhost:8081/api/actuator/health
echo   Metrics:    http://localhost:8081/api/actuator/metrics
echo   Cache:      http://localhost:8081/api/actuator/caches
echo   Swagger:    http://localhost:8081/api/docs
echo.
echo To stop all services: docker-compose down
echo ========================================
pause