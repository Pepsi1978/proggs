# brain-api — schlanker FastAPI-1:1-Speicher (qdrant-client + Gemini-Embedding, mem0-frei seit 2026-06-23)
# Sicherheitsprinzip: Container bindet 0.0.0.0:8000 NUR intern; compose mappt an die WireGuard-IP.
FROM python:3.12-slim

# Nicht als root laufen (Almanach self-hosted-ai-agent-server #1.3: Agent NIE als root)
RUN useradd --create-home --uid 10001 appuser

WORKDIR /app
ENV PYTHONUNBUFFERED=1 PIP_NO_CACHE_DIR=1

# Erst Deps (Layer-Caching), dann Code
COPY requirements.txt .
RUN pip install --upgrade pip && pip install -r requirements.txt

COPY app.py .

# Log- + Daten-Verzeichnis (per Volume gemountet): logs fuer Live-Tail, data fuer den Papierkorb (trash.json)
RUN mkdir -p /app/logs /app/data && chown -R appuser:appuser /app
USER appuser

EXPOSE 8000
CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "8000", "--timeout-keep-alive", "620", "--no-access-log"]
