import cv2
import numpy as np
import uuid
import os
from datetime import datetime

DEFECT_TYPES = ['hot_spot', 'microcrack', 'shadow', 'delamination', 'broken', 'dirt']

DEFECT_NAMES = {
    'hot_spot': '热斑',
    'microcrack': '隐裂',
    'shadow': '遮挡',
    'delamination': '脱层',
    'broken': '破损',
    'dirt': '脏污'
}

DEFECT_COLORS = {
    'hot_spot': (0, 0, 255),
    'microcrack': (0, 165, 255),
    'shadow': (128, 128, 128),
    'delamination': (255, 0, 255),
    'broken': (0, 0, 255),
    'dirt': (0, 140, 255)
}


class DefectDetector:
    def __init__(self):
        self.confidence_threshold = 0.5
        self.min_defect_size = 20

    def detect(self, image_data, image_type='visible', confidence_threshold=None):
        if confidence_threshold is not None:
            self.confidence_threshold = confidence_threshold

        nparr = np.frombuffer(image_data, np.uint8)
        img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)

        if img is None:
            raise ValueError('无法解码图像')

        height, width = img.shape[:2]

        defects = []

        if image_type in ['infrared', 'thermal'] or self._is_infrared_image(img):
            defects = self._detect_infrared_defects(img)
        else:
            defects = self._detect_visible_defects(img, width, height)

        annotated_img = self._draw_annotations(img.copy(), defects)

        result = {
            'image_width': width,
            'image_height': height,
            'defect_count': len(defects),
            'defects': defects,
            'annotated_image': annotated_img
        }

        return result

    def _is_infrared_image(self, img):
        hsv = cv2.cvtColor(img, cv2.COLOR_BGR2HSV)
        mean_saturation = np.mean(hsv[:, :, 1])
        mean_value = np.mean(hsv[:, :, 2])
        if mean_saturation < 60 and mean_value > 100:
            return True
        b, g, r = cv2.split(img)
        if np.mean(r) > np.mean(g) + 30 and np.mean(r) > np.mean(b) + 30:
            return True
        return False

    def _detect_infrared_defects(self, img):
        defects = []

        gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)

        _, hot_mask = cv2.threshold(gray, 200, 255, cv2.THRESH_BINARY)
        hot_contours, _ = cv2.findContours(hot_mask, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)

        for i, contour in enumerate(hot_contours):
            area = cv2.contourArea(contour)
            if area < self.min_defect_size * self.min_defect_size:
                continue

            x, y, w, h = cv2.boundingRect(contour)

            mask = np.zeros(gray.shape, dtype=np.uint8)
            cv2.drawContours(mask, [contour], -1, 255, -1)

            roi = gray[mask > 0]
            if len(roi) == 0:
                continue

            max_temp = float(np.max(roi))
            min_temp = float(np.min(roi))
            avg_temp = float(np.mean(roi))
            delta_temp = max_temp - min_temp

            temp_min_actual = 25.0
            temp_max_actual = 85.0
            normalized_max = (max_temp / 255.0) * (temp_max_actual - temp_min_actual) + temp_min_actual
            normalized_min = (min_temp / 255.0) * (temp_max_actual - temp_min_actual) + temp_min_actual
            normalized_avg = (avg_temp / 255.0) * (temp_max_actual - temp_min_actual) + temp_min_actual
            normalized_delta = normalized_max - normalized_min

            confidence = min(0.98, 0.6 + delta_temp / 255.0 * 0.35)

            if normalized_delta > 10.0:
                defect_level = 3 if normalized_max > 70 else 2
            else:
                defect_level = 1

            defect = {
                'id': str(uuid.uuid4())[:8].upper(),
                'class': 'hot_spot',
                'class_name': '热斑',
                'confidence': round(confidence, 4),
                'bbox': [x, y, x + w, y + h],
                'bbox_x1': x,
                'bbox_y1': y,
                'bbox_x2': x + w,
                'bbox_y2': y + h,
                'center_x': x + w // 2,
                'center_y': y + h // 2,
                'bbox_width': w,
                'bbox_height': h,
                'area_ratio': round(area / (gray.shape[0] * gray.shape[1]) * 100, 4),
                'defect_level': defect_level,
                'temperature': round(normalized_avg, 2),
                'max_temperature': round(normalized_max, 2),
                'min_temperature': round(normalized_min, 2),
                'delta_temperature': round(normalized_delta, 2),
                'description': f'热斑缺陷，最高温度{normalized_max:.1f}℃，温差{normalized_delta:.1f}℃',
                'suggestion': '建议检查热斑组件，确认是否存在电池片故障或热斑效应'
            }
            defects.append(defect)

        _, dark_mask = cv2.threshold(gray, 80, 255, cv2.THRESH_BINARY_INV)
        dark_contours, _ = cv2.findContours(dark_mask, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)

        shadow_count = 0
        for contour in dark_contours:
            area = cv2.contourArea(contour)
            if area < self.min_defect_size * self.min_defect_size * 4:
                continue
            if shadow_count >= 3:
                break

            x, y, w, h = cv2.boundingRect(contour)
            aspect_ratio = w / h if h > 0 else 1
            if aspect_ratio < 0.3 or aspect_ratio > 3:
                continue

            roi = gray[y:y+h, x:x+w]
            roi_mean = np.mean(roi)
            global_mean = np.mean(gray)
            brightness_ratio = roi_mean / global_mean if global_mean > 0 else 1

            if brightness_ratio < 0.7:
                shadow_count += 1
                confidence = min(0.92, 0.5 + (1 - brightness_ratio) * 0.5)

                defect = {
                    'id': str(uuid.uuid4())[:8].upper(),
                    'class': 'shadow',
                    'class_name': '遮挡',
                    'confidence': round(confidence, 4),
                    'bbox': [x, y, x + w, y + h],
                    'bbox_x1': x,
                    'bbox_y1': y,
                    'bbox_x2': x + w,
                    'bbox_y2': y + h,
                    'center_x': x + w // 2,
                    'center_y': y + h // 2,
                    'bbox_width': w,
                    'bbox_height': h,
                    'area_ratio': round(area / (gray.shape[0] * gray.shape[1]) * 100, 4),
                    'defect_level': 1,
                    'description': f'遮挡区域，亮度比{brightness_ratio:.2f}',
                    'suggestion': '检查遮挡原因，清理遮挡物或修剪周边植被'
                }
                defects.append(defect)

        return defects

    def _detect_visible_defects(self, img, width, height):
        defects = []

        gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)

        edges = cv2.Canny(gray, 30, 100)
        contours, _ = cv2.findContours(edges, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)

        for i, contour in enumerate(contours[:15]):
            area = cv2.contourArea(contour)
            if area < self.min_defect_size * self.min_defect_size * 0.5:
                continue

            x, y, w, h = cv2.boundingRect(contour)
            aspect_ratio = w / h if h > 0 else 1

            roi = gray[y:y+h, x:x+w]
            if roi.size == 0:
                continue
            roi_std = np.std(roi)

            defect_type = None
            confidence = 0.6
            defect_level = 2

            if aspect_ratio > 3 or aspect_ratio < 0.3:
                defect_type = 'microcrack'
                confidence = min(0.9, 0.5 + roi_std / 100 * 0.4)
                defect_level = 3 if area > 1000 else 2
            elif roi_std > 50 and aspect_ratio > 0.5 and aspect_ratio < 2:
                defect_type = 'broken'
                confidence = min(0.92, 0.55 + roi_std / 150 * 0.35)
                defect_level = 3
            elif aspect_ratio > 1 and roi_std < 40:
                defect_type = 'delamination'
                confidence = min(0.85, 0.5 + (1 - roi_std / 80) * 0.35)
                defect_level = 2
            elif roi_std < 30 and area > 500:
                defect_type = 'dirt'
                confidence = min(0.82, 0.5 + (1 - roi_std / 60) * 0.3)
                defect_level = 1

            if defect_type and confidence >= self.confidence_threshold:
                mask = np.zeros(gray.shape, dtype=np.uint8)
                cv2.drawContours(mask, [contour], -1, 255, -1)

                defect = {
                    'id': str(uuid.uuid4())[:8].upper(),
                    'class': defect_type,
                    'class_name': DEFECT_NAMES.get(defect_type, defect_type),
                    'confidence': round(confidence, 4),
                    'bbox': [x, y, x + w, y + h],
                    'bbox_x1': x,
                    'bbox_y1': y,
                    'bbox_x2': x + w,
                    'bbox_y2': y + h,
                    'center_x': x + w // 2,
                    'center_y': y + h // 2,
                    'bbox_width': w,
                    'bbox_height': h,
                    'area_ratio': round(area / (height * width) * 100, 4),
                    'defect_level': defect_level,
                    'description': f'{DEFECT_NAMES.get(defect_type, defect_type)}缺陷，面积{int(area)}像素',
                    'suggestion': self._get_suggestion(defect_type)
                }
                defects.append(defect)

        if len(defects) > 10:
            defects = sorted(defects, key=lambda d: d['confidence'], reverse=True)[:10]

        return defects

    def _get_suggestion(self, defect_type):
        suggestions = {
            'hot_spot': '建议检查热斑组件，确认是否存在电池片故障或热斑效应',
            'microcrack': '建议更换隐裂组件，避免功率损失进一步扩大',
            'shadow': '检查遮挡原因，清理遮挡物或修剪周边植被',
            'delamination': '脱层缺陷需重点关注，建议安排专业检测评估',
            'broken': '破损组件需及时更换，防止进水造成安全隐患',
            'dirt': '建议清洁组件表面，提升发电效率'
        }
        return suggestions.get(defect_type, '建议安排现场检查确认')

    def _draw_annotations(self, img, defects):
        for defect in defects:
            bbox = defect.get('bbox', [0, 0, 0, 0])
            x1, y1, x2, y2 = bbox
            color = DEFECT_COLORS.get(defect['class'], (0, 255, 0))

            cv2.rectangle(img, (x1, y1), (x2, y2), color, 2)

            center_x = defect.get('center_x', (x1 + x2) // 2)
            center_y = defect.get('center_y', (y1 + y2) // 2)
            cv2.circle(img, (center_x, center_y), 4, color, -1)
            cv2.circle(img, (center_x, center_y), 6, (255, 255, 255), 1)

            label_parts = [DEFECT_NAMES.get(defect['class'], defect['class'])]
            label_parts.append(f"{defect['confidence'] * 100:.0f}%")

            if defect.get('temperature'):
                label_parts.append(f"{defect['temperature']:.1f}℃")

            label = ' '.join(label_parts)

            (text_width, text_height), baseline = cv2.getTextSize(
                label, cv2.FONT_HERSHEY_SIMPLEX, 0.5, 1
            )

            cv2.rectangle(img,
                          (x1, y1 - text_height - baseline - 8),
                          (x1 + text_width + 10, y1),
                          color, -1)

            cv2.putText(img, label,
                        (x1 + 5, y1 - baseline - 4),
                        cv2.FONT_HERSHEY_SIMPLEX,
                        0.5, (255, 255, 255), 1)

            coord_label = f"({center_x}, {center_y})"
            cv2.putText(img, coord_label,
                        (center_x + 10, center_y + 5),
                        cv2.FONT_HERSHEY_SIMPLEX,
                        0.4, (255, 255, 255), 1)

        return img

    def save_annotated_image(self, annotated_img, output_dir, filename):
        if not os.path.exists(output_dir):
            os.makedirs(output_dir, exist_ok=True)

        base_name = os.path.splitext(os.path.basename(filename))[0]
        output_filename = f"{base_name}_annotated.jpg"
        output_path = os.path.join(output_dir, output_filename)

        cv2.imwrite(output_path, annotated_img)
        return output_path
