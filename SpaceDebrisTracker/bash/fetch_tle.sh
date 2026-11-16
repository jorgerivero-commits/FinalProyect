#!/bin/bash

# Trabajar siempre desde la raíz del proyecto (este script vive en bash/)
cd "$(dirname "$0")/.." || exit 1

# Cargar variables de entorno desde .env
if [ -f .env ]; then
  set -a
  source .env
  set +a
else
  echo "❌ Error: No se encontró el archivo .env en la raíz del proyecto."
  echo "Copia .env.example a .env y configura tus credenciales de MySQL."
  exit 1
fi

OUTPUT_DIR="./data"
mkdir -p "$OUTPUT_DIR"

# URL de CelesTrak para el grupo STATIONS
CELESTRAK_URL="https://celestrak.org/NORAD/elements/gp.php?GROUP=STATIONS&FORMAT=TLE"
OUTPUT_FILE="$OUTPUT_DIR/debris_tle.txt"

# Cuántos objetos se inyectan en la base de datos (miniprueba)
MAX_SATELLITES=5

echo "[$(date +'%Y-%m-%d %H:%M:%S')] Descargando datos TLE desde CelesTrak..."
curl -sfS "$CELESTRAK_URL" -o "$OUTPUT_FILE"

if [ $? -eq 0 ] && [ -s "$OUTPUT_FILE" ]; then
  echo "¡Descarga exitosa! Archivo guardado en $OUTPUT_FILE"
  echo "Inyectando hasta $MAX_SATELLITES satélites con órbitas distintas en MySQL (${DB_NAME:-space_debris_db})..."

  export OUTPUT_FILE MAX_SATELLITES

  python3 - <<'PYEOF'
import os
import mysql.connector

MAX_SATELLITES = int(os.environ["MAX_SATELLITES"])


def parse_orbit(l2):
    """Elementos de la línea 2 que identifican dónde está el objeto en su órbita."""
    return {
        "inc":  float(l2[8:16]),
        "raan": float(l2[17:25]),
        "argp": float(l2[34:42]),
        "m":    float(l2[43:51]),
        "n":    float(l2[52:63]),
    }


def ang_diff(a, b):
    d = abs(a - b) % 360.0
    return min(d, 360.0 - d)


def same_orbit(a, b):
    """True si dos objetos tienen prácticamente el mismo plano y la misma posición
    (p. ej. módulos acoplados a la ISS): se dibujarían uno encima del otro."""
    return (abs(a["inc"] - b["inc"]) < 0.05
            and ang_diff(a["raan"], b["raan"]) < 0.05
            and ang_diff(a["argp"] + a["m"], b["argp"] + b["m"]) < 1.0
            and abs(a["n"] - b["n"]) < 0.01)


with open(os.environ["OUTPUT_FILE"], "r") as f:
    lines = [line.strip() for line in f.readlines() if line.strip()]

selected = []
for i in range(0, len(lines) - 2, 3):
    if len(selected) >= MAX_SATELLITES:
        break

    name, line1, line2 = lines[i], lines[i + 1], lines[i + 2]

    if len(line1) < 69 or len(line2) < 69 or line1[0] != "1" or line2[0] != "2":
        continue
    try:
        norad_id = int(line1[2:7].strip())
        orbit = parse_orbit(line2)
    except ValueError:
        continue

    if any(same_orbit(orbit, o[4]) for o in selected):
        print(f"  - Omitido {name} ({norad_id}): misma órbita que un objeto ya elegido")
        continue

    selected.append((norad_id, name, line1, line2, orbit))

if not selected:
    print("Error: no se encontró ningún TLE válido en el archivo descargado.")
    raise SystemExit(1)

db = mysql.connector.connect(
    host=os.environ.get("DB_HOST", "localhost"),
    port=int(os.environ.get("DB_PORT", 3306)),
    user=os.environ["DB_USER"],
    password=os.environ["DB_PASS"],
    database=os.environ.get("DB_NAME", "space_debris_db")
)
cursor = db.cursor()

query = """
    INSERT INTO space_debris (norad_cat_id, object_name, tle_line1, tle_line2)
    VALUES (%s, %s, %s, %s)
    ON DUPLICATE KEY UPDATE
        object_name = VALUES(object_name),
        tle_line1 = VALUES(tle_line1),
        tle_line2 = VALUES(tle_line2)
"""
for norad_id, name, line1, line2, _ in selected:
    cursor.execute(query, (norad_id, name, line1, line2))
    print(f"  + {norad_id}  {name}")

db.commit()
cursor.close()
db.close()
print(f"¡{len(selected)} satélites inyectados correctamente en MySQL desde Bash!")
PYEOF

else
  echo "Error al descargar los datos de CelesTrak."
  exit 1
fi
