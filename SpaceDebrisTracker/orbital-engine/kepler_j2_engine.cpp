
// Propagador orbital simplificado: Kepler + perturbaciones seculares J2.
// NO es SGP4 completo (ignora arrastre atmosferico y terminos periodicos),
// pero SI usa los elementos del TLE. Error tipico: unos km a decenas de km
// con TLE recientes; crece si el TLE es viejo.
//
// 
//   stdin : por satelite 4 lineas -> id, nombre, TLE linea 1, TLE linea 2
//   stdout: DEBRIS_DATA:id|nombre|lat|lon|alt   (grados, grados, km)
//   stderr: avisos (Java los muestra como "[C++ Output]")
//
// La posicion se calcula siempre para la hora actual del sistema.

#include <iostream>
#include <iomanip>
#include <string>
#include <vector>
#include <cmath>
#include <ctime>
#include <cstdlib>

namespace {

const double PI      = 3.14159265358979323846;
const double DEG2RAD = PI / 180.0;
const double MU      = 398600.4418;    // km^3/s^2
const double RE      = 6378.137;       // radio ecuatorial, km
const double J2      = 1.08262668e-3;
const double WGS_E2  = 0.00669437999014;

struct Tle {
    double epochUnix;   // epoca del TLE (segundos Unix, UTC)
    double inc, raan, ecc, argp, meanAnom;  // rad (ecc adimensional)
    double meanMotion;  // rad/s
};

struct Position { double lat, lon, alt; };

void limpiarLinea(std::string& s) {
    while (!s.empty() && (s.back() == '\r' || s.back() == '\n')) s.pop_back();
}

// Dias desde 1970-01-01 para una fecha civil (algoritmo de H. Hinnant)
long long diasDesdeEpoch(int y, unsigned m, unsigned d) {
    y -= m <= 2;
    const long long era = (y >= 0 ? y : y - 399) / 400;
    const unsigned yoe = static_cast<unsigned>(y - era * 400);
    const unsigned doy = (153 * (m + (m > 2 ? -3 : 9)) + 2) / 5 + d - 1;
    const unsigned doe = yoe * 365 + yoe / 4 - yoe / 100 + doy;
    return era * 146097 + static_cast<long long>(doe) - 719468;
}

// Parseo por columnas fijas del formato TLE. Devuelve false si es invalido.
bool parseTle(const std::string& l1, const std::string& l2, Tle& t) {
    if (l1.size() < 69 || l2.size() < 69) return false;
    if (l1[0] != '1' || l2[0] != '2') return false;

    try {
        int yy = std::stoi(l1.substr(18, 2));
        double dayOfYear = std::stod(l1.substr(20, 12));
        int year = (yy < 57) ? 2000 + yy : 1900 + yy;
        t.epochUnix = diasDesdeEpoch(year, 1, 1) * 86400.0 + (dayOfYear - 1.0) * 86400.0;

        t.inc      = std::stod(l2.substr(8, 8))  * DEG2RAD;
        t.raan     = std::stod(l2.substr(17, 8)) * DEG2RAD;
        t.ecc      = std::stod("0." + l2.substr(26, 7));   // punto decimal implicito
        t.argp     = std::stod(l2.substr(34, 8)) * DEG2RAD;
        t.meanAnom = std::stod(l2.substr(43, 8)) * DEG2RAD;
        double revsPerDay = std::stod(l2.substr(52, 11));
        t.meanMotion = revsPerDay * 2.0 * PI / 86400.0;
    } catch (...) {
        return false;
    }
    return t.meanMotion > 0.0 && t.ecc >= 0.0 && t.ecc < 1.0;
}

// Tiempo sidereo de Greenwich (rad) a partir de tiempo Unix (UT1 ~ UTC)
double gmst(double unixTime) {
    double jd = unixTime / 86400.0 + 2440587.5;
    double d  = jd - 2451545.0;
    double T  = d / 36525.0;
    double deg = 280.46061837 + 360.98564736629 * d + 0.000387933 * T * T;
    deg = std::fmod(deg, 360.0);
    if (deg < 0) deg += 360.0;
    return deg * DEG2RAD;
}

Position propagate(const Tle& t, double unixTime) {
    const double cosi = std::cos(t.inc);
    const double sini = std::sin(t.inc);
    const double e2   = t.ecc * t.ecc;
    const double beta = std::sqrt(1.0 - e2);

    // Recuperar semieje mayor (corrige que el TLE trae el movimiento medio "Kozai")
    double a1 = std::cbrt(MU / (t.meanMotion * t.meanMotion));
    double k  = 0.75 * J2 * (RE / a1) * (RE / a1) * (3.0 * cosi * cosi - 1.0) / (beta * beta * beta);
    double a0 = a1 * (1.0 - k / 3.0 - k * k - 134.0 * k * k * k / 81.0);
    double k0 = 0.75 * J2 * (RE / a0) * (RE / a0) * (3.0 * cosi * cosi - 1.0) / (beta * beta * beta);
    double n  = t.meanMotion / (1.0 + k0);
    double a  = std::cbrt(MU / (n * n));

    // Tasas seculares por J2
    double p = a * (1.0 - e2);
    double f = J2 * (RE / p) * (RE / p) * n;
    double raanDot = -1.5 * f * cosi;
    double argpDot = 0.75 * f * (5.0 * cosi * cosi - 1.0);
    double mDot    = n + 0.75 * f * beta * (3.0 * cosi * cosi - 1.0);

    // Estado en el instante pedido
    double dt = unixTime - t.epochUnix;
    double raan = t.raan + raanDot * dt;
    double argp = t.argp + argpDot * dt;
    double M    = std::fmod(t.meanAnom + mDot * dt, 2.0 * PI);
    if (M < 0) M += 2.0 * PI;

    // Ecuacion de Kepler (Newton)
    double E = (t.ecc < 0.8) ? M : PI;
    for (int i = 0; i < 30; ++i) {
        double dE = (E - t.ecc * std::sin(E) - M) / (1.0 - t.ecc * std::cos(E));
        E -= dE;
        if (std::fabs(dE) < 1e-12) break;
    }

    // Plano orbital -> ECI
    double xp = a * (std::cos(E) - t.ecc);
    double yp = a * beta * std::sin(E);

    double cw = std::cos(argp), sw = std::sin(argp);
    double cO = std::cos(raan), sO = std::sin(raan);

    double x1 = xp * cw - yp * sw;
    double y1 = xp * sw + yp * cw;

    double xe = x1 * cO - y1 * cosi * sO;
    double ye = x1 * sO + y1 * cosi * cO;
    double ze = y1 * sini;

    // ECI -> ECEF (rotar por el tiempo sidereo)
    double g = gmst(unixTime);
    double xf =  xe * std::cos(g) + ye * std::sin(g);
    double yf = -xe * std::sin(g) + ye * std::cos(g);
    double zf = ze;

    // ECEF -> geodesicas WGS84
    double lon = std::atan2(yf, xf);
    double pr  = std::sqrt(xf * xf + yf * yf);
    double lat = std::atan2(zf, pr * (1.0 - WGS_E2));
    double h = 0.0;
    for (int i = 0; i < 6; ++i) {
        double s = std::sin(lat);
        double N = RE / std::sqrt(1.0 - WGS_E2 * s * s);
        h = pr / std::cos(lat) - N;
        lat = std::atan2(zf, pr * (1.0 - WGS_E2 * N / (N + h)));
    }

    return { lat / DEG2RAD, lon / DEG2RAD, h };
}

}  // namespace

int main(int argc, char* argv[]) {
    double targetTime = static_cast<double>(std::time(nullptr));
    if (argc > 1) {
        try {
            targetTime = std::stod(argv[1]);
        } catch (...) {
            std::cerr << "Aviso: Timestamp invalido '" << argv[1]
                      << "', usando hora actual del sistema." << std::endl;
        }
    }

    std::string lineId, name, line1, line2;
    int procesados = 0;

    std::cout << std::fixed << std::setprecision(4);

    while (std::getline(std::cin, lineId)) {
        limpiarLinea(lineId);
        if (lineId.empty()) continue;

        if (!std::getline(std::cin, name))  break;
        if (!std::getline(std::cin, line1)) break;
        if (!std::getline(std::cin, line2)) break;
        limpiarLinea(name);
        limpiarLinea(line1);
        limpiarLinea(line2);

        int id;
        try {
            id = std::stoi(lineId);
        } catch (...) {
            std::cerr << "Aviso: ID invalido '" << lineId << "', bloque ignorado." << std::endl;
            continue;
        }

        Tle tle;
        if (!parseTle(line1, line2, tle)) {
            std::cerr << "Aviso: TLE invalido para " << name << " (ID " << id << "), ignorado." << std::endl;
            continue;
        }

        double edadDias = (targetTime - tle.epochUnix) / 86400.0;
        if (std::fabs(edadDias) > 30.0) {
            std::cerr << "Aviso: TLE de " << name << " proyectado a " << std::fixed << std::setprecision(1)
                      << edadDias << " dias desde su epoca; la posicion es aproximada."
                      << std::setprecision(4) << std::endl;
        }

        Position pos = propagate(tle, targetTime);

        std::cout << "DEBRIS_DATA:" << id << "|" << name << "|"
                  << pos.lat << "|" << pos.lon << "|" << pos.alt << std::endl;
        ++procesados;
    }

    if (procesados == 0) {
        std::cerr << "Aviso: no se calculo ningun satelite (entrada vacia o TLE invalidos)." << std::endl;
    }
    return 0;
}