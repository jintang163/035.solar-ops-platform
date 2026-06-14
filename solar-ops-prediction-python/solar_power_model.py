"""
XGBoost 光伏功率预测模型
用于训练和推理光伏电站短期功率预测
"""
import os
import json
import pickle
import numpy as np
import pandas as pd
from datetime import datetime
from sklearn.model_selection import train_test_split
from sklearn.metrics import mean_squared_error, mean_absolute_error, r2_score
import xgboost as xgb
import joblib


class SolarPowerModel:
    """光伏功率预测XGBoost模型"""

    def __init__(self, model_dir="./models"):
        self.model_dir = model_dir
        os.makedirs(model_dir, exist_ok=True)
        self.models = {}
        self.model_info = {}

    def _get_model_path(self, station_id, inverter_id=None):
        if inverter_id:
            return os.path.join(self.model_dir, f"model_station{station_id}_inv{inverter_id}.pkl")
        return os.path.join(self.model_dir, f"model_station{station_id}.pkl")

    def _get_info_path(self, station_id, inverter_id=None):
        if inverter_id:
            return os.path.join(self.model_dir, f"info_station{station_id}_inv{inverter_id}.json")
        return os.path.join(self.model_dir, f"info_station{station_id}.json")

    def _prepare_features(self, data_points):
        """准备特征矩阵"""
        features = []
        for dp in data_points:
            features.append([
                dp.get("temperature", 0),
                dp.get("humidity", 0),
                dp.get("irradiance", 0),
                dp.get("cloud_cover", 0),
                dp.get("hour", 12),
                dp.get("day_of_year", 180),
                dp.get("historical_power", 0)
            ])
        return np.array(features)

    def train(self, station_id, training_data, inverter_id=None, model_version=None):
        """训练模型"""
        try:
            if not training_data or len(training_data) < 10:
                return False, "训练数据不足，至少需要10条数据", 0, 0

            X = self._prepare_features(training_data)
            y = np.array([dp.get("target_power", 0) for dp in training_data])

            X_train, X_val, y_train, y_val = train_test_split(
                X, y, test_size=0.2, random_state=42
            )

            model = xgb.XGBRegressor(
                n_estimators=200,
                max_depth=6,
                learning_rate=0.05,
                subsample=0.8,
                colsample_bytree=0.8,
                min_child_weight=3,
                reg_alpha=0.1,
                reg_lambda=1.0,
                random_state=42,
                objective="reg:squarederror"
            )

            model.fit(
                X_train, y_train,
                eval_set=[(X_val, y_val)],
                verbose=False
            )

            train_pred = model.predict(X_train)
            val_pred = model.predict(X_val)
            train_score = r2_score(y_train, train_pred)
            val_score = r2_score(y_val, val_pred)

            model_path = self._get_model_path(station_id, inverter_id)
            joblib.dump(model, model_path)

            version = model_version or f"v{datetime.now().strftime('%Y%m%d%H%M')}"
            info = {
                "model_version": version,
                "station_id": station_id,
                "inverter_id": inverter_id,
                "last_train_time": datetime.now().isoformat(),
                "sample_count": len(training_data),
                "train_score": float(train_score),
                "validation_score": float(val_score),
                "feature_names": ["temperature", "humidity", "irradiance",
                                  "cloud_cover", "hour", "day_of_year", "historical_power"]
            }

            info_path = self._get_info_path(station_id, inverter_id)
            with open(info_path, "w", encoding="utf-8") as f:
                json.dump(info, f, ensure_ascii=False, indent=2)

            key = self._make_key(station_id, inverter_id)
            self.models[key] = model
            self.model_info[key] = info

            return True, "训练成功", float(train_score), float(val_score)

        except Exception as e:
            return False, f"训练失败: {str(e)}", 0, 0

    def predict(self, station_id, features, inverter_id=None):
        """执行预测"""
        try:
            key = self._make_key(station_id, inverter_id)

            if key not in self.models:
                model_path = self._get_model_path(station_id, inverter_id)
                if os.path.exists(model_path):
                    self.models[key] = joblib.load(model_path)
                    info_path = self._get_info_path(station_id, inverter_id)
                    if os.path.exists(info_path):
                        with open(info_path, "r", encoding="utf-8") as f:
                            self.model_info[key] = json.load(f)
                else:
                    return None, None, "模型不存在，请先训练模型"

            model = self.models[key]
            X = self._prepare_features(features)

            predictions = model.predict(X)
            predictions = np.maximum(predictions, 0)

            confidence = self._calculate_confidence(features)

            version = self.model_info.get(key, {}).get("model_version", "unknown")

            return predictions, confidence, version

        except Exception as e:
            return None, None, f"预测失败: {str(e)}"

    def _calculate_confidence(self, features):
        """计算预测置信度"""
        confidences = []
        for f in features:
            irradiance = f.get("irradiance", 0)
            cloud_cover = f.get("cloud_cover", 50)
            hour = f.get("hour", 12)

            score = 1.0
            if irradiance < 50:
                score *= 0.7
            elif irradiance < 200:
                score *= 0.85

            if cloud_cover > 80:
                score *= 0.75
            elif cloud_cover > 50:
                score *= 0.9

            if hour < 7 or hour > 17:
                score *= 0.6

            confidences.append(min(max(score, 0.3), 1.0))

        return confidences

    def get_model_status(self, station_id, inverter_id=None):
        """获取模型状态"""
        key = self._make_key(station_id, inverter_id)

        if key not in self.model_info:
            info_path = self._get_info_path(station_id, inverter_id)
            if os.path.exists(info_path):
                with open(info_path, "r", encoding="utf-8") as f:
                    self.model_info[key] = json.load(f)

        info = self.model_info.get(key)
        model_path = self._get_model_path(station_id, inverter_id)
        exists = os.path.exists(model_path)

        return {
            "exists": exists,
            "model_version": info.get("model_version") if info else None,
            "last_train_time": info.get("last_train_time") if info else None,
            "sample_count": info.get("sample_count", 0) if info else 0,
            "last_metric": info.get("validation_score", 0) if info else 0
        }

    def _make_key(self, station_id, inverter_id):
        if inverter_id:
            return f"{station_id}_{inverter_id}"
        return str(station_id)
