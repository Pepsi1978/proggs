FROM python:3.12-slim
RUN apt-get update && apt-get install -y --no-install-recommends rclone ca-certificates && rm -rf /var/lib/apt/lists/* \
    && pip install --no-cache-dir aiohttp==3.11.11 && useradd -u 10010 -m relay && mkdir /data && chown relay /data
WORKDIR /app
COPY relay.py .
USER relay
CMD ["python", "relay.py"]
