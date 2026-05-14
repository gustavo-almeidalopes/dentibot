import os
import jwt
import requests
from flask import Flask, request, jsonify, Response
from flask_cors import CORS
from dotenv import load_dotenv
from functools import wraps

load_dotenv()

app = Flask(__name__)
CORS(app, supports_credentials=True)

JWT_SECRET = os.getenv("JWT_SECRET", "fallback-secret")

SERVICES = {
    "auth":          os.getenv("AUTH_SERVICE_URL",          "http://localhost:5001"),
    "patients":      os.getenv("PATIENT_SERVICE_URL",       "http://localhost:5002"),
    "appointments":  os.getenv("APPOINTMENT_SERVICE_URL",   "http://localhost:5003"),
    "inventory":     os.getenv("INVENTORY_SERVICE_URL",     "http://localhost:5004"),
    "communications":os.getenv("COMMUNICATION_SERVICE_URL", "http://localhost:5005"),
    "financial":     os.getenv("FINANCIAL_SERVICE_URL",     "http://localhost:5006"),
    "audit":         os.getenv("AUDIT_SERVICE_URL",         "http://localhost:5007"),
}

PUBLIC_ROUTES = [
    "/api/auth/register",
    "/api/auth/login",
    "/api/auth/verify-2fa",
    "/api/auth/setup-preference",
    "/health",
]


def verify_token(token: str) -> dict | None:
    try:
        return jwt.decode(token, JWT_SECRET, algorithms=["HS256"])
    except jwt.ExpiredSignatureError:
        return None
    except jwt.InvalidTokenError:
        return None


def require_auth(f):
    @wraps(f)
    def decorated(*args, **kwargs):
        if request.path in PUBLIC_ROUTES:
            return f(*args, **kwargs)
        auth_header = request.headers.get("Authorization", "")
        if not auth_header.startswith("Bearer "):
            return jsonify({"erro": "Token de autenticação ausente."}), 401
        token = auth_header.split(" ", 1)[1]
        payload = verify_token(token)
        if payload is None:
            return jsonify({"erro": "Token inválido ou expirado."}), 401
        request.user = payload
        return f(*args, **kwargs)
    return decorated


def proxy(service_key: str, path: str) -> Response:
    base = SERVICES.get(service_key)
    if not base:
        return jsonify({"erro": "Serviço não encontrado."}), 502

    url = f"{base}{path}"
    headers = {k: v for k, v in request.headers if k.lower() not in ("host", "content-length")}

    try:
        resp = requests.request(
            method=request.method,
            url=url,
            headers=headers,
            params=request.args,
            data=request.get_data(),
            timeout=10,
        )
        return Response(
            resp.content,
            status=resp.status_code,
            content_type=resp.headers.get("Content-Type", "application/json"),
        )
    except requests.exceptions.ConnectionError:
        return jsonify({"erro": f"Serviço '{service_key}' indisponível."}), 503
    except requests.exceptions.Timeout:
        return jsonify({"erro": f"Serviço '{service_key}' demorou muito para responder."}), 504


@app.route("/health")
def health():
    statuses = {}
    for name, url in SERVICES.items():
        try:
            r = requests.get(f"{url}/health", timeout=2)
            statuses[name] = "ok" if r.status_code == 200 else "degraded"
        except Exception:
            statuses[name] = "down"
    overall = "ok" if all(s == "ok" for s in statuses.values()) else "degraded"
    return jsonify({"status": overall, "services": statuses})


# ─── Rotas de Auth ────────────────────────────────────────────────────────────
@app.route("/api/auth/<path:subpath>", methods=["GET", "POST", "PUT", "DELETE", "PATCH"])
@require_auth
def route_auth(subpath):
    return proxy("auth", f"/auth/{subpath}")


# ─── Rotas de Pacientes ───────────────────────────────────────────────────────
@app.route("/api/patients", methods=["GET", "POST"])
@app.route("/api/patients/<path:subpath>", methods=["GET", "POST", "PUT", "DELETE", "PATCH"])
@require_auth
def route_patients(subpath=""):
    path = f"/patients/{subpath}" if subpath else "/patients"
    return proxy("patients", path)


# ─── Rotas de Agendamentos ────────────────────────────────────────────────────
@app.route("/api/appointments", methods=["GET", "POST"])
@app.route("/api/appointments/<path:subpath>", methods=["GET", "POST", "PUT", "DELETE", "PATCH"])
@require_auth
def route_appointments(subpath=""):
    path = f"/appointments/{subpath}" if subpath else "/appointments"
    return proxy("appointments", path)


# ─── Rotas de Estoque ─────────────────────────────────────────────────────────
@app.route("/api/inventory", methods=["GET", "POST"])
@app.route("/api/inventory/<path:subpath>", methods=["GET", "POST", "PUT", "DELETE", "PATCH"])
@require_auth
def route_inventory(subpath=""):
    path = f"/inventory/{subpath}" if subpath else "/inventory"
    return proxy("inventory", path)


# ─── Rotas de Comunicação ─────────────────────────────────────────────────────
@app.route("/api/communications", methods=["GET", "POST"])
@app.route("/api/communications/<path:subpath>", methods=["GET", "POST", "PUT", "DELETE", "PATCH"])
@require_auth
def route_communications(subpath=""):
    path = f"/communications/{subpath}" if subpath else "/communications"
    return proxy("communications", path)


# ─── Rotas Financeiras ────────────────────────────────────────────────────────
@app.route("/api/financial", methods=["GET", "POST"])
@app.route("/api/financial/<path:subpath>", methods=["GET", "POST", "PUT", "DELETE", "PATCH"])
@require_auth
def route_financial(subpath=""):
    path = f"/financial/{subpath}" if subpath else "/financial"
    return proxy("financial", path)


# ─── Rotas de Auditoria ───────────────────────────────────────────────────────
@app.route("/api/audit", methods=["GET", "POST"])
@app.route("/api/audit/<path:subpath>", methods=["GET", "POST", "PUT", "DELETE", "PATCH"])
@require_auth
def route_audit(subpath=""):
    path = f"/audit/{subpath}" if subpath else "/audit"
    return proxy("audit", path)


if __name__ == "__main__":
    port = int(os.getenv("PORT", 5000))
    app.run(host="0.0.0.0", port=port, debug=False)
