#!/bin/bash

# Trabajar siempre desde la carpeta del proyecto (rutas relativas)
cd "$(dirname "$0")" || exit 1

# Cargar variables de entorno desde .env
if [ -f .env ]; then
  set -a
  source .env
  set +a
else
  echo "❌ Error: No se encontró el archivo .env en la raíz del proyecto."
  echo "Copia .env.example a .env y configura tus variables locales."
  exit 1
fi

# Si no se definió FX_PATH en .env, usar carpeta relativa por defecto
FX_PATH="${FX_PATH:-lib/javafx}"

if [ ! -d "$FX_PATH" ]; then
  echo "❌ Error: No se encontró JavaFX en '$FX_PATH'."
  echo "Configura FX_PATH en tu archivo .env o coloca los JAR de JavaFX en lib/javafx."
  exit 1
fi

echo "Compilando motor de propagación C++ (Kepler + J2)..."
mkdir -p cpp
g++ -O2 -std=c++17 -o cpp/kepler_j2_engine cpp/kepler_j2_engine.cpp
if [ $? -ne 0 ]; then
  echo "❌ Error compilando el motor C++. Revisa los errores anteriores."
  exit 1
fi

echo "Limpiando binarios anteriores..."
rm -rf bin
mkdir -p bin

MYSQL_JAR="lib/mysql-connector-j.jar"

echo "Compilando clases Java con soporte JavaFX y MySQL..."
javac --module-path "$FX_PATH" \
  --add-modules javafx.controls,javafx.graphics,javafx.base \
  -cp "bin:$MYSQL_JAR" \
  $(find java -name "*.java") -d bin

if [ $? -eq 0 ]; then
  echo "¡Compilación exitosa! Iniciando Space Debris Tracker..."
  java --module-path "$FX_PATH" \
    --add-modules javafx.controls,javafx.graphics,javafx.base \
    -cp "bin:$MYSQL_JAR" \
    com.spacedebris.ui.MainApp
else
  echo "❌ Error en la compilación de Java. Revisa los errores anteriores."
  exit 1
fi
