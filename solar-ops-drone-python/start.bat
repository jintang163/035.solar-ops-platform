@echo off
chcp 65001
echo ========================================
echo 光伏组件缺陷检测AI服务启动
echo ========================================
echo.

echo [INFO] 检查Python环境...
python --version

echo [INFO] 安装依赖...
pip install -r requirements.txt

echo.
echo [INFO] 启动服务，端口: 50053
echo [INFO] API文档: http://localhost:50053/docs
echo.

python drone_http_server.py --host 0.0.0.0 --port 50053

pause
