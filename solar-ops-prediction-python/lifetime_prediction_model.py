"""
LSTM 设备剩余寿命预测模型
基于逆变器运行温度、工作时长、故障历史预测剩余使用寿命（RUL）
"""
import os
import json
import numpy as np
import pandas as pd
from datetime import datetime
from sklearn.preprocessing import MinMaxScaler
from sklearn.metrics import mean_absolute_error, mean_squared_error, r2_score

try:
    import tensorflow as tf
    from tensorflow.keras.models import Sequential, load_model
    from tensorflow.keras.layers import LSTM, Dense, Dropout
    from tensorflow.keras.callbacks import EarlyStopping, ModelCheckpoint
    TENSORFLOW_AVAILABLE = True
except ImportError:
    TENSORFLOW_AVAILABLE = False


class LifetimePredictionModel:
    """逆变器剩余寿命预测LSTM模型"""

    def __init__(self, model_dir="./models/lifetime"):
        self.model_dir = model_dir
        os.makedirs(model_dir, exist_ok=True)
        self.models = {}
        self.model_info = {}
        self.scalers = {}

    def _get_model_path(self, inverter_id):
        return os.path.join(self.model_dir, f"lifetime_model_inv{inverter_id}.h5")

    def _get_info_path(self, inverter_id):
        return os.path.join(self.model_dir, f"lifetime_info_inv{inverter_id}.json")

    def _get_scaler_path(self, inverter_id):
        return os.path.join(self.model_dir, f"lifetime_scaler_inv{inverter_id}.json")

    def _prepare_sequences(self, data, sequence_length=30):
        """
        准备LSTM序列数据
        data: 按时间排序的历史数据列表
        sequence_length: 输入序列长度（天）
        """
        if len(data) < sequence_length + 1:
            return None, None

        features = []
        targets = []

        for i in range(len(data) - sequence_length):
            seq = data[i:i + sequence_length]
            seq_features = []
            for dp in seq:
                seq_features.append([
                    dp.get("avg_temperature", 25),
                    dp.get("max_temperature", 30),
                    dp.get("operating_hours", 0),
                    dp.get("fault_count", 0),
                    dp.get("fault_severity", 0),
                    dp.get("output_power_ratio", 1.0),
                    dp.get("efficiency_drop", 0.0),
                ])
            features.append(seq_features)

            target = data[i + sequence_length].get("health_score", 1.0)
            targets.append(target)

        return np.array(features), np.array(targets)

    def train(self, inverter_id, training_data, model_version=None):
        """
        训练LSTM寿命预测模型
        training_data: 历史运行数据，按时间排序
        """
        try:
            if not TENSORFLOW_AVAILABLE:
                return False, "TensorFlow未安装，无法训练LSTM模型", 0, 0

            if not training_data or len(training_data) < 60:
                return False, "训练数据不足，至少需要60天数据", 0, 0

            sorted_data = sorted(training_data, key=lambda x: x.get("record_date", ""))

            sequence_length = min(30, len(sorted_data) // 2)

            scaler = MinMaxScaler(feature_range=(0, 1))
            all_features = []
            for dp in sorted_data:
                all_features.append([
                    dp.get("avg_temperature", 25),
                    dp.get("max_temperature", 30),
                    dp.get("operating_hours", 0),
                    dp.get("fault_count", 0),
                    dp.get("fault_severity", 0),
                    dp.get("output_power_ratio", 1.0),
                    dp.get("efficiency_drop", 0.0),
                ])

            scaler.fit(all_features)

            X, y = self._prepare_sequences(sorted_data, sequence_length)
            if X is None or len(X) == 0:
                return False, "无法生成足够的序列数据", 0, 0

            X_scaled = np.array([scaler.transform(seq) for seq in X])

            train_size = int(len(X_scaled) * 0.8)
            X_train, X_val = X_scaled[:train_size], X_scaled[train_size:]
            y_train, y_val = y[:train_size], y[train_size:]

            model = self._build_lstm_model(sequence_length, 7)

            early_stopping = EarlyStopping(
                monitor='val_loss',
                patience=10,
                restore_best_weights=True
            )

            history = model.fit(
                X_train, y_train,
                epochs=50,
                batch_size=16,
                validation_data=(X_val, y_val),
                callbacks=[early_stopping],
                verbose=0
            )

            train_pred = model.predict(X_train, verbose=0).flatten()
            val_pred = model.predict(X_val, verbose=0).flatten()

            model_path = self._get_model_path(inverter_id)
            model.save(model_path)

            train_score = r2_score(y_train, train_pred) if len(train_pred) > 1 else 0.5
            val_score = r2_score(y_val, val_pred) if len(val_pred) > 1 else 0.5

            version = model_version or f"v{datetime.now().strftime('%Y%m%d%H%M')}"
            info = {
                "model_version": version,
                "inverter_id": inverter_id,
                "last_train_time": datetime.now().isoformat(),
                "sample_count": len(sorted_data),
                "sequence_length": sequence_length,
                "train_score": float(train_score),
                "validation_score": float(val_score),
                "feature_names": ["avg_temperature", "max_temperature", "operating_hours",
                                  "fault_count", "fault_severity", "output_power_ratio",
                                  "efficiency_drop"],
                "tensorflow_available": True
            }

            info_path = self._get_info_path(inverter_id)
            with open(info_path, "w", encoding="utf-8") as f:
                json.dump(info, f, ensure_ascii=False, indent=2)

            scaler_path = self._get_scaler_path(inverter_id)
            scaler_data = {
                "min_": scaler.min_.tolist(),
                "scale_": scaler.scale_.tolist(),
                "data_min_": scaler.data_min_.tolist(),
                "data_max_": scaler.data_max_.tolist()
            }
            with open(scaler_path, "w", encoding="utf-8") as f:
                json.dump(scaler_data, f, ensure_ascii=False, indent=2)

            key = str(inverter_id)
            self.models[key] = True
            self.model_info[key] = info
            self.scalers[key] = scaler

            return True, "训练成功", float(train_score), float(val_score)

        except Exception as e:
            return False, f"训练失败: {str(e)}", 0, 0

    def _build_lstm_model(self, sequence_length, n_features):
        """构建LSTM模型"""
        model = Sequential([
            LSTM(64, return_sequences=True, input_shape=(sequence_length, n_features)),
            Dropout(0.2),
            LSTM(32, return_sequences=False),
            Dropout(0.2),
            Dense(16, activation='relu'),
            Dense(1, activation='sigmoid')
        ])
        model.compile(optimizer='adam', loss='mse', metrics=['mae'])
        return model

    def predict(self, inverter_id, recent_data, forecast_days=90):
        """
        预测剩余寿命
        recent_data: 最近的运行数据
        forecast_days: 预测未来的天数
        """
        try:
            if not TENSORFLOW_AVAILABLE:
                return None, None, "TensorFlow未安装，无法进行LSTM预测"

            key = str(inverter_id)

            if key not in self.model_info:
                info_path = self._get_info_path(inverter_id)
                if os.path.exists(info_path):
                    with open(info_path, "r", encoding="utf-8") as f:
                        self.model_info[key] = json.load(f)
                else:
                    return None, None, "模型不存在，请先训练模型"

            if key not in self.scalers:
                scaler_path = self._get_scaler_path(inverter_id)
                if os.path.exists(scaler_path):
                    with open(scaler_path, "r", encoding="utf-8") as f:
                        scaler_data = json.load(f)
                    scaler = MinMaxScaler()
                    scaler.min_ = np.array(scaler_data["min_"])
                    scaler.scale_ = np.array(scaler_data["scale_"])
                    scaler.data_min_ = np.array(scaler_data["data_min_"])
                    scaler.data_max_ = np.array(scaler_data["data_max_"])
                    self.scalers[key] = scaler
                else:
                    return None, None, "缩放器不存在，请先训练模型"

            info = self.model_info[key]
            sequence_length = info.get("sequence_length", 30)
            scaler = self.scalers[key]

            sorted_data = sorted(recent_data, key=lambda x: x.get("record_date", ""))
            if len(sorted_data) < sequence_length:
                return None, None, f"数据不足，需要至少{sequence_length}天数据"

            model_path = self._get_model_path(inverter_id)
            if not os.path.exists(model_path):
                return None, None, "模型文件不存在，请先训练模型"

            try:
                model = load_model(model_path)
            except Exception as e:
                return None, None, f"模型加载失败: {str(e)}"

            recent_seq = sorted_data[-sequence_length:]
            features = []
            for dp in recent_seq:
                features.append([
                    dp.get("avg_temperature", 25),
                    dp.get("max_temperature", 30),
                    dp.get("operating_hours", 0),
                    dp.get("fault_count", 0),
                    dp.get("fault_severity", 0),
                    dp.get("output_power_ratio", 1.0),
                    dp.get("efficiency_drop", 0.0),
                ])

            current_seq = scaler.transform(np.array(features))
            current_seq = current_seq.reshape(1, sequence_length, 7)

            health_scores = []
            confidence_scores = []
            remaining_life_days = 0
            current_health = 1.0

            temp_seq = current_seq.copy()
            for day in range(forecast_days):
                pred = model.predict(temp_seq, verbose=0)[0][0]
                health_scores.append(float(pred))
                current_health = float(pred)

                temp = temp_seq[0, 1:, :].copy()
                new_point = temp[-1].copy()
                new_point[5] = max(0.1, pred)
                new_point[6] = (1.0 - pred) * 100
                temp = np.vstack([temp, new_point.reshape(1, -1)])
                temp_seq = temp.reshape(1, sequence_length, 7)

                conf = max(0.3, 1.0 - (day / forecast_days) * 0.5)
                confidence_scores.append(conf)

                if current_health < 0.3 and remaining_life_days == 0:
                    remaining_life_days = day

            if remaining_life_days == 0 and current_health > 0.3:
                remaining_life_days = forecast_days + int((current_health - 0.3) / 0.002)

            if remaining_life_days == 0:
                remaining_life_days = forecast_days

            version = info.get("model_version", "unknown")

            result = {
                "health_scores": health_scores,
                "confidence_scores": confidence_scores,
                "remaining_life_days": remaining_life_days,
                "current_health_score": current_health if health_scores else 1.0,
                "forecast_days": forecast_days,
                "model_version": version,
                "tensorflow_available": True
            }

            return result, None, version

        except Exception as e:
            return None, None, f"预测失败: {str(e)}"

    def calculate_health_score(self, inverter_id, daily_data):
        """
        计算单日健康度评分（基于多维度综合评估）
        """
        try:
            required_fields = ["avg_temperature", "max_temperature", "operating_hours",
                               "fault_count", "fault_severity", "output_power_ratio"]
            for field in required_fields:
                if field not in daily_data or daily_data[field] is None:
                    raise ValueError(f"缺少必要字段: {field}")

            temp_score = 1.0
            avg_temp = daily_data.get("avg_temperature", 25)
            if avg_temp > 50:
                temp_score = max(0.5, 1.0 - (avg_temp - 50) * 0.02)
            elif avg_temp > 40:
                temp_score = max(0.7, 1.0 - (avg_temp - 40) * 0.03)

            fault_score = 1.0
            fault_count = daily_data.get("fault_count", 0)
            fault_severity = daily_data.get("fault_severity", 0)
            if fault_count > 0:
                fault_penalty = fault_count * 0.05 + fault_severity * 0.1
                fault_score = max(0.3, 1.0 - fault_penalty)

            efficiency_score = daily_data.get("output_power_ratio", 1.0)

            hours = daily_data.get("operating_hours", 0)
            age_factor = max(0.8, 1.0 - hours * 0.00001)

            health_score = (temp_score * 0.25 + fault_score * 0.3 +
                           efficiency_score * 0.3 + age_factor * 0.15)

            return float(max(0.0, min(1.0, health_score)))

        except Exception as e:
            raise ValueError(f"健康度计算失败: {str(e)}")

    def get_model_status(self, inverter_id):
        """获取模型状态"""
        key = str(inverter_id)

        if key not in self.model_info:
            info_path = self._get_info_path(inverter_id)
            if os.path.exists(info_path):
                with open(info_path, "r", encoding="utf-8") as f:
                    self.model_info[key] = json.load(f)

        info = self.model_info.get(key)
        model_path = self._get_model_path(inverter_id)
        exists = os.path.exists(model_path)

        return {
            "exists": exists,
            "model_version": info.get("model_version") if info else None,
            "last_train_time": info.get("last_train_time") if info else None,
            "sample_count": info.get("sample_count", 0) if info else 0,
            "last_metric": info.get("validation_score", 0) if info else 0,
            "sequence_length": info.get("sequence_length", 30) if info else 30,
            "tensorflow_available": TENSORFLOW_AVAILABLE
        }

    def get_spare_part_advice(self, inverter_id, remaining_life_days, current_health):
        """
        生成备件更换建议
        提前3个月（90天）预警
        """
        if remaining_life_days is None or current_health is None:
            raise ValueError("缺少必要参数: remaining_life_days 和 current_health")

        warnings = []
        suggestions = []

        if remaining_life_days <= 90:
            warnings.append({
                "level": "critical",
                "message": "设备剩余寿命不足3个月，建议立即安排备件更换",
                "spare_part": "逆变器整机",
                "urgency": "high"
            })
        elif remaining_life_days <= 180:
            warnings.append({
                "level": "warning",
                "message": "设备剩余寿命不足6个月，建议提前备库",
                "spare_part": "逆变器整机",
                "urgency": "medium"
            })

        if current_health < 0.5:
            suggestions.append({
                "component": "IGBT模块",
                "reason": "温度异常，效率下降明显",
                "recommendation": "检查散热系统，必要时更换IGBT",
                "estimated_cost": "¥15,000-30,000"
            })

        if current_health < 0.7:
            suggestions.append({
                "component": "电解电容",
                "reason": "长期高温运行可能导致电容老化",
                "recommendation": "检测电容容值，考虑预防性更换",
                "estimated_cost": "¥2,000-5,000"
            })
            suggestions.append({
                "component": "散热风扇",
                "reason": "散热不良加速设备老化",
                "recommendation": "清洁或更换散热风扇",
                "estimated_cost": "¥500-1,500"
            })

        return {
            "warnings": warnings,
            "suggestions": suggestions,
            "remaining_life_days": remaining_life_days,
            "current_health": current_health,
            "replacement_advice": remaining_life_days <= 90
        }
