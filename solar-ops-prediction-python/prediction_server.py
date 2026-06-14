"""
光伏功率预测 gRPC 服务端
提供模型训练、功率预测、模型状态查询接口
"""
import os
import sys
import time
import grpc
import logging
from concurrent import futures
from datetime import datetime

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import prediction_pb2
import prediction_pb2_grpc
from solar_power_model import SolarPowerModel

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s"
)
logger = logging.getLogger("prediction_server")


class PredictionService(prediction_pb2_grpc.PowerPredictionServiceServicer):
    """预测服务实现"""

    def __init__(self):
        self.model = SolarPowerModel(model_dir="./models")
        logger.info("预测模型服务初始化完成")

    def Predict(self, request, context):
        """执行预测"""
        logger.info(f"收到预测请求: station={request.station_id}, "
                    f"inverter={request.inverter_id}, horizon={request.horizon}")

        try:
            features = []
            for fp in request.features:
                features.append({
                    "temperature": fp.temperature,
                    "humidity": fp.humidity,
                    "irradiance": fp.irradiance,
                    "cloud_cover": fp.cloud_cover,
                    "hour": fp.hour,
                    "day_of_year": fp.day_of_year,
                    "historical_power": fp.historical_power
                })

            predictions, confidences, version = self.model.predict(
                request.station_id,
                features,
                request.inverter_id if request.inverter_id else None
            )

            if predictions is None:
                return prediction_pb2.PredictionResponse(
                    success=False,
                    message=version,
                    model_version="",
                    predictions=[]
                )

            results = []
            base_time = int(time.time())
            for i, (pred, conf) in enumerate(zip(predictions, confidences)):
                target_time = base_time + (i + 1) * 3600
                if len(request.features) > i and request.features[i].timestamp > 0:
                    target_time = request.features[i].timestamp

                results.append(prediction_pb2.PredictionResult(
                    target_time=target_time,
                    predicted_power=float(pred),
                    confidence=float(conf)
                ))

            logger.info(f"预测成功: {len(results)}个点, 模型版本={version}")
            return prediction_pb2.PredictionResponse(
                success=True,
                message="预测成功",
                model_version=version or "",
                predictions=results
            )

        except Exception as e:
            logger.error(f"预测异常: {e}", exc_info=True)
            return prediction_pb2.PredictionResponse(
                success=False,
                message=f"预测服务异常: {str(e)}",
                model_version="",
                predictions=[]
            )

    def Train(self, request, context):
        """训练模型"""
        logger.info(f"收到训练请求: station={request.station_id}, "
                    f"inverter={request.inverter_id}, samples={len(request.training_data)}")

        try:
            training_data = []
            for dp in request.training_data:
                training_data.append({
                    "temperature": dp.temperature,
                    "humidity": dp.humidity,
                    "irradiance": dp.irradiance,
                    "cloud_cover": dp.cloud_cover,
                    "hour": dp.hour,
                    "day_of_year": dp.day_of_year,
                    "historical_power": dp.historical_power,
                    "target_power": dp.target_power
                })

            success, message, train_score, val_score = self.model.train(
                request.station_id,
                training_data,
                request.inverter_id if request.inverter_id else None,
                request.model_version if request.model_version else None
            )

            logger.info(f"训练完成: success={success}, train_score={train_score:.4f}, "
                        f"val_score={val_score:.4f}")

            return prediction_pb2.TrainResponse(
                success=success,
                message=message,
                model_version="",
                train_score=train_score,
                validation_score=val_score
            )

        except Exception as e:
            logger.error(f"训练异常: {e}", exc_info=True)
            return prediction_pb2.TrainResponse(
                success=False,
                message=f"训练服务异常: {str(e)}",
                model_version="",
                train_score=0,
                validation_score=0
            )

    def GetModelStatus(self, request, context):
        """查询模型状态"""
        try:
            status = self.model.get_model_status(
                request.station_id,
                request.inverter_id if request.inverter_id else None
            )

            return prediction_pb2.ModelStatusResponse(
                exists=status["exists"],
                model_version=status.get("model_version") or "",
                last_train_time=status.get("last_train_time") or "",
                sample_count=status.get("sample_count", 0),
                last_metric=status.get("last_metric", 0)
            )

        except Exception as e:
            logger.error(f"模型状态查询异常: {e}", exc_info=True)
            return prediction_pb2.ModelStatusResponse(
                exists=False,
                model_version="",
                last_train_time="",
                sample_count=0,
                last_metric=0
            )


def serve():
    """启动服务"""
    port = os.environ.get("PREDICTION_PORT", "50051")
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))
    prediction_pb2_grpc.add_PowerPredictionServiceServicer_to_server(
        PredictionService(), server
    )
    server.add_insecure_port(f"[::]:{port}")
    server.start()
    logger.info(f"预测 gRPC 服务已启动, 监听端口: {port}")
    server.wait_for_termination()


if __name__ == "__main__":
    serve()
