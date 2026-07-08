# sb-agent — Bibliothekar-Agent (Schicht 3, Speicher-Seite): FastAPI + Gemini, mem0-frei.
# Sicherheitsprinzip: bindet 0.0.0.0:8002 NUR intern; compose mappt an die WireGuard-IP.
# Laeuft als uid 1000 (= 'frank' auf dem Host) -> Logbuch-.txt gehoeren frank (sauber ueber Samba/Z:).
FROM python:3.12-slim

# Nicht als root (Almanach self-hosted-ai-agent-server #1.3). uid 1000 == 'frank' auf dem VPS.
RUN useradd --create-home --uid 1000 appuser

WORKDIR /app
ENV PYTHONUNBUFFERED=1 PIP_NO_CACHE_DIR=1

COPY requirements.txt .
RUN pip install --upgrade pip && pip install -r requirements.txt

COPY app.py .
COPY rules.py .

RUN mkdir -p /app/logs && chown -R appuser:appuser /app
USER appuser

EXPOSE 8002
CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "8002", "--no-access-log"]
