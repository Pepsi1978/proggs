# sb-dashboard — privates Web-Cockpit (FastAPI + statische Oberflaeche). Nur intern, compose mappt an WireGuard-IP.
FROM python:3.12-slim
RUN useradd --create-home --uid 10002 appuser
WORKDIR /app
ENV PYTHONUNBUFFERED=1 PIP_NO_CACHE_DIR=1
COPY requirements.txt .
RUN pip install --upgrade pip && pip install -r requirements.txt
COPY app.py .
COPY features.json .
COPY static ./static
RUN chown -R appuser:appuser /app
USER appuser
EXPOSE 8003
CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "8003", "--no-access-log"]
