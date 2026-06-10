# syntax=docker/dockerfile:1

# ── Compilación ───────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-jammy AS build

RUN apt-get update \
    && apt-get install -y --no-install-recommends ant curl ca-certificates \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY docker/ docker/
COPY lib/ lib/
COPY src/ src/
COPY build.xml manifest.mf ./

RUN sed -i 's/\r$//' docker/*.sh \
    && chmod +x docker/prepare-linux-libs.sh docker/entrypoint.sh \
    && bash docker/prepare-linux-libs.sh lib

# build.xml usa target "jar" que depende de clean; adecuado para imagen nueva
RUN ant -f build.xml jar

# ── Ejecución ───────────────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy AS runtime

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        netcat-openbsd \
        libgtk-3-0 \
        libgl1 \
        libglib2.0-0 \
        libxext6 \
        libxrender1 \
        libxtst6 \
        libfontconfig1 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY --from=build /app/dist /app/dist
COPY docker/entrypoint.sh /app/entrypoint.sh
RUN sed -i 's/\r$//' /app/entrypoint.sh && chmod +x /app/entrypoint.sh

ENV DB_HOST=mysql \
    DB_PORT=3306 \
    DB_NAME=bd_empleados \
    DB_USER=root \
    DB_PASSWORD=root \
    JAVA_AWT_HEADLESS=false

ENTRYPOINT ["/app/entrypoint.sh"]
