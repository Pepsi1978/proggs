# sb-mcp — MCP-Server fuers zweite Gehirn (schlankes Python-Image, non-root)
FROM python:3.12-slim

# Non-root: ein Dienst laeuft nie als root (Sicherheits-Best-Practice fuer Agent-Server)
RUN useradd -m -u 10002 sbmcp

WORKDIR /app

# Erst Requirements (Layer-Cache), dann Code
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY server.py .

USER sbmcp
ENV PYTHONUNBUFFERED=1
EXPOSE 8001

CMD ["python", "server.py"]
