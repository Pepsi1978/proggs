# mem0-api — schlanker FastAPI-Wrapper um die Mem0-Bibliothek (Gemini + Qdrant)
# Sicherheitsprinzip: Container bindet 0.0.0.0:8000 NUR intern; compose mappt nach 127.0.0.1.
FROM python:3.12-slim

# Nicht als root laufen (Almanach self-hosted-ai-agent-server #1.3: Agent NIE als root)
RUN useradd --create-home --uid 10001 appuser

WORKDIR /app
ENV PYTHONUNBUFFERED=1 PIP_NO_CACHE_DIR=1

# Erst Deps (Layer-Caching), dann Code
COPY requirements.txt .
RUN pip install --upgrade pip && pip install -r requirements.txt

COPY app.py .

# Log-Verzeichnis (wird per Volume gemountet, fuer Live-Tail vom Host)
RUN mkdir -p /app/logs && chown -R appuser:appuser /app
USER appuser

EXPOSE 8000
CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "8000", "--no-access-log"]
