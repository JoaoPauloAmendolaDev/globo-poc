@echo off
REM Script de inicialização - Globo Streaming

echo.
echo ====================================
echo   GLOBO STREAMING
echo ====================================
echo.

echo [1/2] Verificando Docker...
docker --version >nul 2>&1
if errorlevel 1 (
    echo ERRO: Docker nao encontrado! Instale o Docker Desktop.
    pause
    exit /b 1
)
echo OK - Docker encontrado
echo.

echo [2/2] Subindo aplicacao...
echo.
echo ATENCAO: O primeiro build pode demorar alguns minutos...
echo.

docker compose up --build

echo.
docker compose logs -f app
pause

