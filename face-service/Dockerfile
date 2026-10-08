FROM python:3.10-slim

# Install system dependencies required by opencv and insightface
RUN apt-get update && apt-get install -y \
    libgl1 \
    libglib2.0-0 \
    build-essential \
    g++ \
    && rm -rf /var/lib/apt/lists/*

# Hugging Face Spaces requires a non-root user with UID 1000
RUN useradd -m -u 1000 user

# Switch to the non-root user
USER user
ENV PATH="/home/user/.local/bin:$PATH"

WORKDIR /home/user/app

# Install Python dependencies
COPY --chown=user:user requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Pre-download the InsightFace model at build time to prevent cold-start delays
RUN python -c "from insightface.app import FaceAnalysis; face_app = FaceAnalysis(name='buffalo_l', providers=['CPUExecutionProvider']); face_app.prepare(ctx_id=-1, det_size=(640, 640))"

# Copy the rest of the application code
COPY --chown=user:user . .

EXPOSE 7860

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "7860"]
