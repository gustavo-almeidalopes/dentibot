@echo off
chcp 65001 >nul
setlocal
title DENTIBOT - API
color 0F
cd /d "%~dp0"

set "API=api"

rem Os mesmos valores do infrastructure\init.ps1 e do compose: locais, em claro
rem de propósito. Variável já definida no ambiente ganha.
if not defined DENTIBOT_DB_URL set "DENTIBOT_DB_URL=jdbc:postgresql://localhost:5433/dentibot"
if not defined DENTIBOT_DB_PASSWORD set "DENTIBOT_DB_PASSWORD=app_local_apenas"
if not defined DENTIBOT_DB_MIGRADOR_PASSWORD set "DENTIBOT_DB_MIGRADOR_PASSWORD=migrador_local_apenas"

rem O issuer tem de ser o par da publishable key do web, senão é 401 em tudo sem
rem pista de qual das duas está errada. O host está dentro da própria chave: o
rem trecho depois de pk_test_ é o base64 de "<host>$".
if defined DENTIBOT_CLERK_ISSUER goto :rodar
for /f "usebackq delims=" %%i in (`powershell -NoProfile -Command "try { $l = Select-String -Path 'web\.env.local' -Pattern '^VITE_CLERK_PUBLISHABLE_KEY=' -List -ErrorAction Stop; $b = ($l.Line -split '=', 2)[1].Trim().Trim([char]34) -replace '^pk_[a-z]+_', ''; $b = $b.PadRight([math]::Ceiling($b.Length / 4) * 4, '='); 'https://' + [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($b)).TrimEnd('$') } catch {}"`) do set "DENTIBOT_CLERK_ISSUER=%%i"
if not defined DENTIBOT_CLERK_ISSUER goto :sem_clerk

:rodar
echo DENTIBOT API
echo   código   %API%
echo   banco    %DENTIBOT_DB_URL%
echo   clerk    %DENTIBOT_CLERK_ISSUER%
echo   porta    http://localhost:8080
echo.
cd "%API%"
call .\mvnw.cmd spring-boot:run
if errorlevel 1 goto :falha
exit /b 0

:sem_clerk
color 0C
echo SEM ISSUER DO CLERK.
echo Falta VITE_CLERK_PUBLISHABLE_KEY em web\.env.local, ou ela não é uma chave pk_ válida.
echo Crie o arquivo com a chave do painel do Clerk, ou defina DENTIBOT_CLERK_ISSUER.
pause
exit /b 1

:falha
color 0C
echo.
echo A API PAROU COM ERRO. Infra no ar? Rode iniciar.bat.
pause
exit /b 1
