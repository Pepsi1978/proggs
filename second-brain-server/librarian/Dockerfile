# sb-librarian — Nachtschicht-Bibliothekar (Sleep-Time-Agent): FastAPI + Gemini/OpenCode.
# Sicherheitsprinzip: bindet 0.0.0.0:8004 NUR intern; compose mappt an die WireGuard-IP.
# Laeuft als uid 1000 (= 'frank' auf dem Host) -> librarian-data/-logs gehoeren frank (chown beim Deploy).
FROM python:3.12-slim

# Nicht als root (Almanach self-hosted-ai-agent-server #1.3). uid 1000 == 'frank' auf dem VPS.
RUN useradd --create-home --uid 1000 appuser

WORKDIR /app
ENV PYTHONUNBUFFERED=1 PIP_NO_CACHE_DIR=1

COPY requirements.txt .
RUN pip install --upgrade pip && pip install -r requirements.txt

COPY app.py .

RUN mkdir -p /app/logs /app/data && chown -R appuser:appuser /app
USER appuser

EXPOSE 8004
CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "8004", "--timeout-keep-alive", "620", "--no-access-log"]
