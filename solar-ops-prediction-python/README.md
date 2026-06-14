# 光伏功率预测 Python gRPC 服务

## 功能说明
基于 XGBoost 的光伏电站短期功率预测服务，通过 gRPC 提供接口。

## 技术栈
- Python 3.8+
- XGBoost 2.0
- gRPC
- scikit-learn
- NumPy / Pandas

## 安装依赖

```bash
pip install -r requirements.txt
```

## 生成 gRPC 代码

```bash
python generate_grpc_code.py
```

## 启动服务

```bash
python prediction_server.py
```

默认端口: 50051

## 配置环境变量
- `PREDICTION_PORT`: 服务端口，默认 50051

## 接口说明

### 1. Predict - 功率预测
输入气象和历史发电特征，输出未来1-6小时功率预测值。

### 2. Train - 模型训练
使用历史数据训练或更新 XGBoost 预测模型。

### 3. GetModelStatus - 模型状态查询
查询指定电站/逆变器的模型训练状态和指标。
