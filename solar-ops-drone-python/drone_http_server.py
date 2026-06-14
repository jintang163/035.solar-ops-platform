import os
import io
import uuid
import base64
from datetime import datetime
from fastapi import FastAPI, UploadFile, File, Form, HTTPException
from fastapi.responses import JSONResponse, FileResponse
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import Optional, List

from defect_detector import DefectDetector

app = FastAPI(title="光伏组件缺陷检测AI服务", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

detector = DefectDetector()

ANNOTATED_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "data", "annotated")
os.makedirs(ANNOTATED_DIR, exist_ok=True)


class DetectResult(BaseModel):
    id: str
    defect_type: str
    confidence: float
    bbox: List[int]
    center_x: int
    center_y: int
    temperature: Optional[float] = None
    max_temperature: Optional[float] = None
    min_temperature: Optional[float] = None
    delta_temperature: Optional[float] = None
    defect_level: int
    description: str
    suggestion: str


class DetectResponse(BaseModel):
    code: int = 200
    message: str = "success"
    data: List[dict] = []
    image_width: int = 0
    image_height: int = 0
    defect_count: int = 0


@app.get("/")
async def root():
    return {
        "service": "光伏组件缺陷检测AI服务",
        "version": "1.0.0",
        "status": "running",
        "defect_types": ["hot_spot", "microcrack", "shadow", "delamination", "broken", "dirt"]
    }


@app.get("/health")
async def health():
    return {"status": "ok", "timestamp": datetime.now().isoformat()}


@app.post("/api/v1/detect", response_model=DetectResponse)
async def detect_defects(
    image: UploadFile = File(...),
    confidence_threshold: float = Form(0.5),
    image_type: str = Form("visible"),
    task_id: Optional[str] = Form(None),
    image_id: Optional[str] = Form(None)
):
    try:
        image_data = await image.read()

        result = detector.detect(
            image_data,
            image_type=image_type,
            confidence_threshold=confidence_threshold
        )

        date_dir = datetime.now().strftime("%Y%m%d")
        output_dir = os.path.join(ANNOTATED_DIR, date_dir)
        os.makedirs(output_dir, exist_ok=True)

        base_name = os.path.splitext(image.filename)[0] if image.filename else "image"
        annotated_filename = f"{base_name}_{uuid.uuid4().hex[:8]}_annotated.jpg"
        annotated_path = os.path.join(output_dir, annotated_filename)

        import cv2
        cv2.imwrite(annotated_path, result['annotated_image'])

        defects = []
        for d in result['defects']:
            defect = {
                "id": d['id'],
                "class": d['class'],
                "class_name": d.get('class_name', d['class']),
                "confidence": d['confidence'],
                "bbox": d['bbox'],
                "bbox_x1": d['bbox_x1'],
                "bbox_y1": d['bbox_y1'],
                "bbox_x2": d['bbox_x2'],
                "bbox_y2": d['bbox_y2'],
                "x_min": d['bbox_x1'],
                "y_min": d['bbox_y1'],
                "x_max": d['bbox_x2'],
                "y_max": d['bbox_y2'],
                "center_x": d['center_x'],
                "center_y": d['center_y'],
                "bbox_width": d['bbox_width'],
                "bbox_height": d['bbox_height'],
                "area_ratio": d['area_ratio'],
                "defect_level": d['defect_level'],
                "description": d['description'],
                "suggestion": d['suggestion']
            }

            if d.get('temperature') is not None:
                defect['temperature'] = d['temperature']
                defect['max_temperature'] = d.get('max_temperature')
                defect['min_temperature'] = d.get('min_temperature')
                defect['delta_temperature'] = d.get('delta_temperature')

            defects.append(defect)

        return DetectResponse(
            code=200,
            message="检测成功",
            data=defects,
            image_width=result['image_width'],
            image_height=result['image_height'],
            defect_count=result['defect_count']
        )

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"检测失败: {str(e)}")


@app.post("/api/v1/detect/batch")
async def batch_detect(
    images: List[UploadFile] = File(...),
    confidence_threshold: float = Form(0.5),
    image_type: str = Form("visible")
):
    results = []
    for image in images:
        try:
            image_data = await image.read()
            result = detector.detect(
                image_data,
                image_type=image_type,
                confidence_threshold=confidence_threshold
            )
            results.append({
                "filename": image.filename,
                "defect_count": result['defect_count'],
                "defects": result['defects']
            })
        except Exception as e:
            results.append({
                "filename": image.filename,
                "error": str(e)
            })

    return {
        "code": 200,
        "message": "批量检测完成",
        "total": len(images),
        "success": sum(1 for r in results if 'defects' in r),
        "data": results
    }


@app.get("/api/v1/annotated/{date}/{filename}")
async def get_annotated_image(date: str, filename: str):
    file_path = os.path.join(ANNOTATED_DIR, date, filename)
    if not os.path.exists(file_path):
        raise HTTPException(status_code=404, detail="图片不存在")
    return FileResponse(file_path)


@app.get("/api/v1/defect/types")
async def get_defect_types():
    return {
        "code": 200,
        "data": [
            {"code": "hot_spot", "name": "热斑", "level": "high", "description": "光伏组件局部过热"},
            {"code": "microcrack", "name": "隐裂", "level": "high", "description": "电池片内部裂纹"},
            {"code": "shadow", "name": "遮挡", "level": "low", "description": "组件被物体遮挡"},
            {"code": "delamination", "name": "脱层", "level": "medium", "description": "封装材料层间分离"},
            {"code": "broken", "name": "破损", "level": "high", "description": "组件物理破损"},
            {"code": "dirt", "name": "脏污", "level": "low", "description": "组件表面灰尘污垢"}
        ]
    }


if __name__ == "__main__":
    import uvicorn
    import argparse

    parser = argparse.ArgumentParser(description="光伏组件缺陷检测AI服务")
    parser.add_argument("--host", default="0.0.0.0", help="监听地址")
    parser.add_argument("--port", type=int, default=50053, help="监听端口")
    args = parser.parse_args()

    print(f"=" * 50)
    print(f"光伏组件缺陷检测AI服务启动")
    print(f"服务地址: http://{args.host}:{args.port}")
    print(f"API文档: http://{args.host}:{args.port}/docs")
    print(f"检测接口: POST /api/v1/detect")
    print(f"缺陷类型: 热斑、隐裂、遮挡、脱层、破损、脏污")
    print(f"=" * 50)

    uvicorn.run(app, host=args.host, port=args.port)
