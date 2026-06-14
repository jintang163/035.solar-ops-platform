"""
光伏功率预测 HTTP 服务
同时提供 HTTP REST 接口作为 gRPC 的备选方案
"""
import os
import sys
import json
import time
import logging
from datetime import datetime
from http.server import HTTPServer, BaseHTTPRequestHandler
from urllib.parse import urlparse, parse_qs

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from solar_power_model import SolarPowerModel
from lifetime_prediction_model import LifetimePredictionModel

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s"
)
logger = logging.getLogger("http_prediction_server")

model = SolarPowerModel(model_dir="./models")
lifetime_model = LifetimePredictionModel(model_dir="./models/lifetime")


class PredictionHandler(BaseHTTPRequestHandler):

    def _send_json(self, data, status=200):
        response = json.dumps(data, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(response)))
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type")
        self.end_headers()
        self.wfile.write(response)

    def do_OPTIONS(self):
        self._send_json({"status": "ok"})

    def do_GET(self):
        parsed = urlparse(self.path)
        path = parsed.path

        if path == "/health":
            self._send_json({"status": "healthy", "time": datetime.now().isoformat()})
            return

        if path == "/model_status":
            params = parse_qs(parsed.query)
            station_id = int(params.get("station_id", [0])[0])
            inverter_id = int(params.get("inverter_id", [0])[0]) if "inverter_id" in params else None

            status = model.get_model_status(station_id, inverter_id)
            self._send_json(status)
            return

        if path == "/lifetime/model_status":
            params = parse_qs(parsed.query)
            inverter_id = int(params.get("inverter_id", [0])[0])

            status = lifetime_model.get_model_status(inverter_id)
            self._send_json(status)
            return

        self._send_json({"success": False, "message": "Not found"}, 404)

    def do_POST(self):
        parsed = urlparse(self.path)
        path = parsed.path

        content_length = int(self.headers.get("Content-Length", 0))
        body = self.rfile.read(content_length) if content_length > 0 else b"{}"

        try:
            data = json.loads(body.decode("utf-8"))
        except json.JSONDecodeError:
            data = {}

        if path == "/predict":
            self._handle_predict(data)
            return

        if path == "/train":
            self._handle_train(data)
            return

        if path == "/lifetime/predict":
            self._handle_lifetime_predict(data)
            return

        if path == "/lifetime/train":
            self._handle_lifetime_train(data)
            return

        if path == "/lifetime/health_score":
            self._handle_lifetime_health_score(data)
            return

        if path == "/lifetime/spare_part_advice":
            self._handle_spare_part_advice(data)
            return

        self._send_json({"success": False, "message": "Not found"}, 404)

    def _handle_predict(self, data):
        station_id = data.get("station_id", 0)
        inverter_id = data.get("inverter_id") or None
        features = data.get("features", [])

        logger.info(f"收到预测请求: station={station_id}, inverter={inverter_id}, features={len(features)}")

        predictions, confidences, version = model.predict(station_id, features, inverter_id)

        if predictions is None:
            self._send_json({
                "success": False,
                "message": version or "预测失败",
                "model_version": "",
                "predictions": []
            })
            return

        results = []
        base_time = int(time.time())
        for i, (pred, conf) in enumerate(zip(predictions, confidences)):
            target_time = base_time + (i + 1) * 3600
            if len(features) > i and features[i].get("timestamp"):
                target_time = features[i]["timestamp"]

            results.append({
                "target_time": target_time,
                "predicted_power": float(pred),
                "confidence": float(conf)
            })

        logger.info(f"预测成功: {len(results)}个点, 版本={version}")
        self._send_json({
            "success": True,
            "message": "预测成功",
            "model_version": version or "",
            "predictions": results
        })

    def _handle_train(self, data):
        station_id = data.get("station_id", 0)
        inverter_id = data.get("inverter_id") or None
        training_data = data.get("training_data", [])
        model_version = data.get("model_version")

        logger.info(f"收到训练请求: station={station_id}, inverter={inverter_id}, samples={len(training_data)}")

        success, message, train_score, val_score = model.train(
            station_id, training_data, inverter_id, model_version
        )

        logger.info(f"训练完成: success={success}, train_score={train_score:.4f}, val_score={val_score:.4f}")

        self._send_json({
            "success": success,
            "message": message,
            "model_version": "",
            "train_score": float(train_score),
            "validation_score": float(val_score)
        })

    def _handle_lifetime_predict(self, data):
        inverter_id = data.get("inverter_id", 0)
        recent_data = data.get("recent_data", [])
        forecast_days = data.get("forecast_days", 90)

        logger.info(f"收到寿命预测请求: inverter={inverter_id}, days={len(recent_data)}, forecast_days={forecast_days}")

        result, _, version = lifetime_model.predict(inverter_id, recent_data, forecast_days)

        if result is None:
            self._send_json({
                "success": False,
                "message": version or "预测失败",
                "model_version": ""
            })
            return

        logger.info(f"寿命预测成功: inverter={inverter_id}, 剩余寿命={result['remaining_life_days']}天")
        self._send_json({
            "success": True,
            "message": "预测成功",
            "model_version": result.get("model_version", ""),
            "data": result
        })

    def _handle_lifetime_train(self, data):
        inverter_id = data.get("inverter_id", 0)
        training_data = data.get("training_data", [])
        model_version = data.get("model_version")

        logger.info(f"收到寿命模型训练请求: inverter={inverter_id}, samples={len(training_data)}")

        success, message, train_score, val_score = lifetime_model.train(
            inverter_id, training_data, model_version
        )

        logger.info(f"寿命模型训练完成: success={success}, train_score={train_score:.4f}, val_score={val_score:.4f}")

        self._send_json({
            "success": success,
            "message": message,
            "model_version": "",
            "train_score": float(train_score),
            "validation_score": float(val_score)
        })

    def _handle_lifetime_health_score(self, data):
        inverter_id = data.get("inverter_id", 0)
        daily_data = data.get("daily_data", {})

        score = lifetime_model.calculate_health_score(inverter_id, daily_data)

        self._send_json({
            "success": True,
            "message": "计算成功",
            "health_score": float(score)
        })

    def _handle_spare_part_advice(self, data):
        inverter_id = data.get("inverter_id", 0)
        remaining_life_days = data.get("remaining_life_days", 365)
        current_health = data.get("current_health", 0.8)

        advice = lifetime_model.get_spare_part_advice(
            inverter_id, remaining_life_days, current_health
        )

        self._send_json({
            "success": True,
            "message": "获取成功",
            "data": advice
        })

    def log_message(self, format, *args):
        logger.debug(f"{self.address_string()} - {format % args}")


def serve():
    port = int(os.environ.get("PREDICTION_HTTP_PORT", "50052"))
    server = HTTPServer(("0.0.0.0", port), PredictionHandler)
    logger.info(f"预测 HTTP 服务已启动, 监听端口: {port}")
    server.serve_forever()


if __name__ == "__main__":
    serve()
