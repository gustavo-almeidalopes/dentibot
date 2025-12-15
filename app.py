import os
import random
import string
import pyotp
import qrcode
import io
import base64
from datetime import datetime, timedelta
from flask import Flask, request, jsonify, session
from flask_sqlalchemy import SQLAlchemy
from flask_mail import Mail, Message
from werkzeug.security import generate_password_hash, check_password_hash
from dotenv import load_dotenv
from flask_cors import CORS

# Carregar variáveis do .env
load_dotenv()

# --- Configurações do App ---
app = Flask(__name__)
app.secret_key = os.getenv("FLASK_SECRET_KEY")

# CORS precisa vir *depois* que o app existe
CORS(app, supports_credentials=True)

# --- Banco MySQL ---
app.config['SQLALCHEMY_DATABASE_URI'] = os.getenv("DATABASE_URL")
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False

db = SQLAlchemy(app)

# --- Email ---
app.config['MAIL_SERVER'] = os.getenv("MAIL_SERVER")
app.config['MAIL_PORT'] = int(os.getenv("MAIL_PORT"))
app.config['MAIL_USE_TLS'] = os.getenv("MAIL_USE_TLS") == "True"
app.config['MAIL_USERNAME'] = os.getenv("MAIL_USERNAME")
app.config['MAIL_PASSWORD'] = os.getenv("MAIL_PASSWORD")

mail = Mail(app)


# --- Modelo do Banco ---
class User(db.Model):
    __tablename__ = "user"

    id = db.Column(db.Integer, primary_key=True)
    username = db.Column(db.String(100), unique=True, nullable=False)
    password_hash = db.Column(db.String(200), nullable=False)
    email = db.Column(db.String(120), unique=True, nullable=False)
    phone = db.Column(db.String(20), nullable=True)

    two_factor_method = db.Column(db.String(10), default='email')
    totp_secret = db.Column(db.String(32), nullable=True)
    auth_code = db.Column(db.String(6), nullable=True)
    code_expiration = db.Column(db.DateTime, nullable=True)


# --- Funções Auxiliares ---
def generate_pin():
    return ''.join(random.choices(string.digits, k=6))


def send_email_pin(user_email, pin):
    try:
        msg = Message("Código de Login", sender=app.config['MAIL_USERNAME'], recipients=[user_email])
        msg.body = f"Seu código é: {pin}"
        mail.send(msg)
        return True
    except Exception as e:
        print(f"[ERRO EMAIL] {e}")
        return False


def send_sms_pin(user_phone, pin):
    print("\n" + "=" * 30)
    print(f"[SMS SIMULADO] Para: {user_phone}")
    print(f"Mensagem: Seu código de verificação é {pin}")
    print("=" * 30 + "\n")
    return True


# --- Rotas ---
@app.route('/')
def index():
    return jsonify({
        "mensagem": "Sistema Multi-2FA (Email, SMS, App)",
        "rotas": ["/register", "/setup-preference", "/login", "/verify-2fa"]
    })


@app.route('/register', methods=['POST'])
def register():
    data = request.get_json() or request.form

    if not all(k in data for k in ('username', 'password', 'email')):
        return jsonify({"erro": "Faltam dados obrigatórios."}), 400

    if User.query.filter_by(username=data['username']).first():
        return jsonify({"erro": "Usuário já existe."}), 400

    novo = User(
        username=data['username'],
        email=data['email'],
        phone=data.get('phone'),
        password_hash=generate_password_hash(data['password']),
        totp_secret=pyotp.random_base32(),
        two_factor_method='email'
    )

    db.session.add(novo)
    db.session.commit()

    return jsonify({
        "mensagem": "Usuário criado com sucesso.",
        "info": "Método de 2FA padrão: EMAIL."
    })


@app.route('/setup-preference', methods=['POST'])
def setup_preference():
    data = request.get_json() or request.form
    username = data.get('username')
    method = data.get('method')

    user = User.query.filter_by(username=username).first()
    if not user:
        return jsonify({"erro": "Usuário não encontrado."}), 404

    if method not in ['email', 'sms', 'app']:
        return jsonify({"erro": "Método inválido."}), 400

    user.two_factor_method = method
    resposta = {"mensagem": f"Método de 2FA atualizado para {method.upper()}"}

    if method == 'app':
        totp_uri = pyotp.TOTP(user.totp_secret).provisioning_uri(
            name=user.username,
            issuer_name='ClinicSystem2FA'
        )

        qr = qrcode.make(totp_uri)
        buf = io.BytesIO()
        qr.save(buf, format='PNG')
        img64 = base64.b64encode(buf.getvalue()).decode('utf-8')

        resposta["qr_code_base64"] = img64
        resposta["manual_secret"] = user.totp_secret

    db.session.commit()
    return jsonify(resposta)


@app.route('/login', methods=['POST'])
def login():
    session.clear()
    data = request.get_json() or request.form

    user = User.query.filter_by(username=data.get('username')).first()

    if not user or not check_password_hash(user.password_hash, data.get('password')):
        return jsonify({"erro": "Credenciais inválidas."}), 401

    session['pre_2fa_user_id'] = user.id
    metodo = user.two_factor_method

    if metodo == 'app':
        return jsonify({
            "mensagem": "Digite o código do app autenticador.",
            "metodo": "app"
        })

    pin = generate_pin()
    user.auth_code = pin
    user.code_expiration = datetime.now() + timedelta(minutes=5)
    db.session.commit()

    if metodo == 'sms':
        send_sms_pin(user.phone, pin)
    else:
        print(f"[DEBUG EMAIL] Código: {pin}")
        send_email_pin(user.email, pin)

    return jsonify({
        "mensagem": f"Código enviado via {metodo.upper()}.",
        "metodo": metodo
    })


@app.route('/verify-2fa', methods=['POST'])
def verify_2fa():
    if 'pre_2fa_user_id' not in session:
        return jsonify({"erro": "Faça login primeiro."}), 401

    data = request.get_json() or request.form
    codigo = data.get('token')

    user = User.query.get(session['pre_2fa_user_id'])
    metodo = user.two_factor_method

    autorizado = False

    if metodo == 'app':
        totp = pyotp.TOTP(user.totp_secret)
        if totp.verify(codigo):
            autorizado = True
    else:
        if user.auth_code and datetime.now() < user.code_expiration:
            if codigo == user.auth_code:
                autorizado = True
                user.auth_code = None
                db.session.commit()
        else:
            return jsonify({"erro": "Código expirado ou inválido."}), 403

    if autorizado:
        session.pop('pre_2fa_user_id')
        session['user_id'] = user.id
        return jsonify({
            "status": "sucesso",
            "mensagem": f"Bem-vindo {user.username}!"
        })

    return jsonify({"erro": "Código incorreto."}), 403


if __name__ == '__main__':
    with app.app_context():
        db.create_all()
    app.run(debug=True)