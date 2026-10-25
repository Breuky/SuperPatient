@echo off
REM 一条命令起开发环境：后端（会自动拉起 Agent 服务）+ 前端
setlocal

set AGENT_ENABLED=true
set AGENT_AUTO_START=true

start "SuperPatient backend :8080" cmd /k "cd /d %~dp0backend && mvn spring-boot:run -Dspring-boot.run.profiles=local"
timeout /t 3 >nul
start "SuperPatient web :3000" cmd /k "cd /d %~dp0frontend\Web && npm run dev"

echo.
echo 后端 http://localhost:8080   （Agent 会自动起在 8001）
echo 前端 http://localhost:3000
echo 关闭这两个窗口即停止服务。
endlocal
