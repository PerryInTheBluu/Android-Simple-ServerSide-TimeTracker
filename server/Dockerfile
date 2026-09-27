FROM python:3.12-slim

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY app ./app
COPY migrations ./migrations
COPY run_migrations.py .

RUN mkdir -p /data

ENV DATA_DIR=/data
ENV SECRET_KEY_FILE=/data/secret.key

# Listen only on a LAN IP or local port. TLS is terminated by the external Caddy.
ENV LISTEN_HOST=0.0.0.0
ENV LISTEN_PORT=8080

EXPOSE 8080

CMD ["sh", "-c", "python run_migrations.py && uvicorn app.main:app --host $LISTEN_HOST --port $LISTEN_PORT"]
