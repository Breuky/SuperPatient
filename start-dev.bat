@echo off
REM 三个进程分别启动：Agent(8001) / 后端(8080) / 前端(3000)
REM 对应「后端与 Agent 分别启动」的常规做法，改 Agent 只重启第一个窗口即可
setlocal

start "SuperPatient agent :8001" cmd /k "cd /d %~dp0 && python agent\server.py --port 8001"
timeout /t 2 >nul
start "SuperPatient backend :8080" cmd /k "cd /d %~dp0backend && set AGENT_ENABLED=true && mvn spring-boot:run -Dspring-boot.run.profiles=local"
timeout /t 3 >nul
start "SuperPatient web :3000" cmd /k "cd /d %~dp0frontend\Web && npm run dev"

echo.
echo Agent  http://localhost:8001
echo 后端   http://localhost:8080
echo 前端   http://localhost:3000
echo 关闭对应窗口即停止该服务。
endlocal
