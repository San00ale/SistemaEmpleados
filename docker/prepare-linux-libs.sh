#!/bin/bash
# Sustituye binarios Windows de JavaCV por Linux (Maven / Bytedeco 1.5.9).
set -e

LIB_DIR="${1:-lib}"
BASE="https://repo1.maven.org/maven2/org/bytedeco"

echo "==> Preparando JavaCV/OpenCV para Linux en ${LIB_DIR}"

rm -f "${LIB_DIR}"/*-windows-x86_64.jar
rm -f "${LIB_DIR}"/javacv.jar "${LIB_DIR}"/javacpp.jar \
      "${LIB_DIR}"/opencv.jar "${LIB_DIR}"/ffmpeg.jar \
      "${LIB_DIR}"/openblas*.jar \
      "${LIB_DIR}"/javacv-*.jar "${LIB_DIR}"/javacpp-*.jar \
      "${LIB_DIR}"/opencv-*.jar "${LIB_DIR}"/ffmpeg-*.jar

fetch() {
  artifact="$1"
  version="$2"
  classifier="${3:-}"
  name="${artifact}-${version}${classifier}.jar"
  url="${BASE}/${artifact}/${version}/${name}"
  echo "    ${name}"
  curl -fsSL "${url}" -o "${LIB_DIR}/${name}"
}

# Versiones compatibles con javacv-1.5.9
fetch javacv  "1.5.9" ""
fetch javacpp "1.5.9" ""
fetch javacpp "1.5.9" "-linux-x86_64"
fetch opencv  "4.7.0-1.5.9" ""
fetch opencv  "4.7.0-1.5.9" "-linux-x86_64"
fetch openblas "0.3.23-1.5.9" ""
fetch openblas "0.3.23-1.5.9" "-linux-x86_64"
fetch ffmpeg  "6.0-1.5.9" ""
fetch ffmpeg  "6.0-1.5.9" "-linux-x86_64"

echo "==> JavaCV Linux listo (demas JARs de lib/ se conservan)"
