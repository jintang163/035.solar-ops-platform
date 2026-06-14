"""
生成 Python gRPC 代码脚本
运行: python generate_grpc_code.py
"""
import os
import subprocess
import sys


def main():
    proto_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), "proto")
    proto_file = os.path.join(proto_dir, "prediction.proto")
    output_dir = os.path.dirname(os.path.abspath(__file__))

    cmd = [
        sys.executable, "-m", "grpc_tools.protoc",
        f"-I{proto_dir}",
        f"--python_out={output_dir}",
        f"--grpc_python_out={output_dir}",
        proto_file
    ]

    print("执行命令:", " ".join(cmd))
    result = subprocess.run(cmd, capture_output=True, text=True)

    if result.returncode == 0:
        print("✅ gRPC 代码生成成功!")
        print(result.stdout)
    else:
        print("❌ gRPC 代码生成失败!")
        print(result.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
