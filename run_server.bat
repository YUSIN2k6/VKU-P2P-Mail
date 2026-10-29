@echo off
chcp 65001 > nul
echo [1/2] Đang kiểm tra và biên dịch dự án...
call mvn compile
if %ERRORLEVEL% NEQ 0 (
    echo [Lỗi] Biên dịch thất bại.
    pause
    exit /b %ERRORLEVEL%
)

echo [2/2] Đang khởi động TCP Mail Server...
mvn exec:java -Dexec.mainClass="com.vku.app.ServerApp"
pause

