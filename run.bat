@echo off
chcp 65001 > nul
echo ====================================================
echo        FIAP BANK - EMULADOR DE CAIXA ELETRÔNICO
echo        Checkpoint 4 - Refatoração DDD (multi-módulo)
echo ====================================================
echo.
echo Procurando o Maven do Apache NetBeans...

set MVN_PATH="C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd"

if exist %MVN_PATH% (
    echo Maven encontrado! Compilando os quatro módulos...
    call %MVN_PATH% clean install
    if errorlevel 1 goto :erro_build
    echo Iniciando a aplicação a partir do Composition Root...
    call %MVN_PATH% -pl infrastructure exec:java
) else (
    echo.
    echo [AVISO] Maven do NetBeans não encontrado no caminho padrão.
    echo Tentando usar comando 'mvn' global...
    where mvn >nul 2>nul
    if %errorlevel% equ 0 (
        call mvn clean install
        if errorlevel 1 goto :erro_build
        call mvn -pl infrastructure exec:java
    ) else (
        echo [ERRO] Maven não encontrado. Por favor, abra este projeto
        echo no Apache NetBeans e execute-o diretamente pelo editor,
        echo ou instale o Maven e adicione-o ao seu PATH.
        pause
    )
)
goto :fim

:erro_build
echo.
echo [ERRO] A compilação falhou. Verifique as mensagens acima.
pause

:fim
