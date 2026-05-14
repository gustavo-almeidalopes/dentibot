import os
import io
import base64
import random
import string
import pyotp
import qrcode
import jwt
from datetime import datetime, timedelta
from flask import Blueprint, request, jsonify
from flask_mail import Message
from werkzeug.security import generate_password_hash, check_password_hash
from .models import db, User

auth_bp = Blueprint("auth", __name__, url_prefix="/auth")

JWT_SECRET = os.getenv("JWT_SECRET", "fallback-secret")
JWT_EXPIRES_HOURS = 8


def _generate_pin():
    return "".join(random.choices(string.digits, k=6))


def _generate_token(user: User) -> str:
    payload = {
        "sub": user.id,
        "username": user.username,
        "role": user.role,
        "exp": datetime.utcnow() + timedelta(hours=JWT_EXPIRES_HOURS),
    }
    return jwt.encode(payload, JWT_SECRET, algorithm="HS256")


def _send_email_pin(mail, user_email: str, pin: str) -> bool:
    try:
        msg = Message(
            "DentiBot – Código de Verificação",
            sender=os.getenv("MAIL_USERNAME"),
            recipients=[user_email],
        )
        msg.html = f"""
        <div style="font-family:sans-serif;max-width:400px;margin:auto">
          <h2 style="color:#059669">DentiBot</h2>
          <p>Seu código de verificação é:</p>
          <h1 style="letter-spacing:8px;color:#059669">{pin}</h1>
          <p style="color:#94a3b8;font-size:12px">Válido por 5 minutos.</p>
        </div>"""
        mail.send(msg)
        return True
    except Exception as e:
        print(f"[ERRO EMAIL] {e}")
        return False


def _send_sms_pin(user_phone: str, pin: str) -> bool:
    print(f"[SMS] Para {user_phone}: {pin}")
    return True


@auth_bp.route("/health")
def health():
    return jsonify({"status": "ok", "service": "auth"})


@auth_bp.route("/register", methods=["POST"])
def register():
    data = request.get_json() or {}

    required = ("username", "password", "email")
    if not all(k in data for k in required):
        return jsonify({"erro": "Campos obrigatórios: username, password, email"}), 400

    if User.query.filter_by(username=data["username"]).first():
        return jsonify({"erro": "Usuário já existe."}), 409

    if User.query.filter_by(email=data["email"]).first():
        return jsonify({"erro": "E-mail já cadastrado."}), 409

    user = User(
        username=data["username"],
        email=data["email"],
        phone=data.get("phone"),
        role=data.get("role", "cliente"),
        password_hash=generate_password_hash(data["password"]),
        totp_secret=pyotp.random_base32(),
        two_factor_method="email",
    )
    db.session.add(user)
    db.session.commit()

    return jsonify({"mensagem": "Usuário criado com sucesso.", "id": user.id}), 201


@auth_bp.route("/setup-preference", methods=["POST"])
def setup_preference():
    data = request.get_json() or {}
    user = User.query.filter_by(username=data.get("username")).first()
    if not user:
        return jsonify({"erro": "Usuário não encontrado."}), 404

    method = data.get("method")
    if method not in ("email", "sms", "app"):
        return jsonify({"erro": "Método inválido. Use: email, sms, app"}), 400

    user.two_factor_method = method
    resposta = {"mensagem": f"Método 2FA atualizado para {method.upper()}"}

    if method == "app":
        totp_uri = pyotp.TOTP(user.totp_secret).provisioning_uri(
            name=user.username, issuer_name="DentiBot"
        )
        qr = qrcode.make(totp_uri)
        buf = io.BytesIO()
        qr.save(buf, format="PNG")
        resposta["qr_code_base64"] = base64.b64encode(buf.getvalue()).decode()
        resposta["manual_secret"] = user.totp_secret

    db.session.commit()
    return jsonify(resposta)


@auth_bp.route("/login", methods=["POST"])
def login():
    from flask import current_app
    data = request.get_json() or {}

    user = User.query.filter_by(username=data.get("username")).first()
    if not user or not check_password_hash(user.password_hash, data.get("password", "")):
        return jsonify({"erro": "Credenciais inválidas."}), 401

    if not user.is_active:
        return jsonify({"erro": "Conta desativada. Contate o administrador."}), 403

    if user.two_factor_method == "app":
        return jsonify({"mensagem": "Digite o código do app autenticador.", "metodo": "app",
                        "user_id": user.id})

    pin = _generate_pin()
    user.auth_code = pin
    user.code_expiration = datetime.utcnow() + timedelta(minutes=5)
    db.session.commit()

    mail = current_app.extensions.get("mail")
    if user.two_factor_method == "sms":
        _send_sms_pin(user.phone, pin)
    else:
        _send_email_pin(mail, user.email, pin)

    return jsonify({"mensagem": f"Código enviado via {user.two_factor_method.upper()}.",
                    "metodo": user.two_factor_method, "user_id": user.id})


@auth_bp.route("/verify-2fa", methods=["POST"])
def verify_2fa():
    data = request.get_json() or {}
    user_id = data.get("user_id")
    codigo = data.get("token")

    user = User.query.get(user_id)
    if not user:
        return jsonify({"erro": "Usuário não encontrado."}), 404

    autorizado = False

    if user.two_factor_method == "app":
        totp = pyotp.TOTP(user.totp_secret)
        autorizado = totp.verify(codigo)
    else:
        if user.auth_code and datetime.utcnow() < user.code_expiration:
            if codigo == user.auth_code:
                autorizado = True
                user.auth_code = None
                db.session.commit()
        else:
            return jsonify({"erro": "Código expirado."}), 403

    if not autorizado:
        return jsonify({"erro": "Código incorreto."}), 403

    token = _generate_token(user)
    return jsonify({
        "status": "sucesso",
        "mensagem": f"Bem-vindo, {user.username}!",
        "token": token,
        "user": user.to_dict(),
    })


@auth_bp.route("/me", methods=["GET"])
def me():
    auth = request.headers.get("Authorization", "")
    if not auth.startswith("Bearer "):
        return jsonify({"erro": "Token ausente."}), 401
    try:
        payload = jwt.decode(auth.split(" ", 1)[1], JWT_SECRET, algorithms=["HS256"])
    except jwt.InvalidTokenError:
        return jsonify({"erro": "Token inválido."}), 401

    user = User.query.get(payload["sub"])
    if not user:
        return jsonify({"erro": "Usuário não encontrado."}), 404
    return jsonify(user.to_dict())


@auth_bp.route("/users", methods=["GET"])
def list_users():
    users = User.query.filter_by(is_active=True).all()
    return jsonify([u.to_dict() for u in users])


@auth_bp.route("/users/<int:uid>", methods=["PUT"])
def update_user(uid):
    user = User.query.get_or_404(uid)
    data = request.get_json() or {}
    if "role" in data:
        user.role = data["role"]
    if "is_active" in data:
        user.is_active = data["is_active"]
    if "phone" in data:
        user.phone = data["phone"]
    db.session.commit()
    return jsonify(user.to_dict())


@auth_bp.route("/users/<int:uid>", methods=["DELETE"])
def delete_user(uid):
    user = User.query.get_or_404(uid)
    user.is_active = False
    db.session.commit()
    return jsonify({"mensagem": "Usuário desativado (LGPD: dados preservados)."})
