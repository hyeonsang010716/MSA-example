# ---- Build Stage ----
FROM python:3.12-slim AS build

COPY --from=ghcr.io/astral-sh/uv:latest /uv /uvx /bin/

WORKDIR /app

COPY pyproject.toml uv.lock ./

RUN uv sync --frozen --no-install-project

COPY . .

RUN uv sync --frozen

# ---- Runtime Stage ----
FROM python:3.12-slim

WORKDIR /app

COPY --from=build /app /app

ENV PATH="/app/.venv/bin:$PATH"

EXPOSE 8084

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8084"]