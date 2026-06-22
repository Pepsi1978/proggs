# mem0-api — schlanker FastAPI-Wrapper um die Mem0-Bibliothek (Gemini + Qdrant)
# Sicherheitsprinzip: Container bindet 0.0.0.0:8000 NUR intern; compose mappt nach 127.0.0.1.
FROM python:3.12-slim

# Nicht als root laufen (Almanach self-hosted-ai-agent-server #1.3: Agent NIE als root)
RUN useradd --create-home --uid 10001 appuser

WORKDIR /app
ENV PYTHONUNBUFFERED=1 PIP_NO_CACHE_DIR=1
# HuggingFace-Cache in ein gemountetes Volume -> HHEM-Modell wird nur EINMAL geladen (Container-Restart-fest)
ENV HF_HOME=/app/.cache/huggingface

# Erst Deps (Layer-Caching), dann Code.
# torch CPU-only ZUERST aus dem PyTorch-CPU-Index (kein CUDA -> Image ~2GB statt ~7GB), dann der Rest.
COPY requirements.txt .
RUN pip install --upgrade pip \
    && pip install --index-url https://download.pytorch.org/whl/cpu torch \
    && pip install -r requirements.txt

COPY app.py .

# Log- + HF-Cache-Verzeichnis (per Volume gemountet, fuer Live-Tail bzw. Modell-Persistenz)
RUN mkdir -p /app/logs /app/.cache/huggingface && chown -R appuser:appuser /app
USER appuser

EXPOSE 8000
CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "8000", "--no-access-log"]
