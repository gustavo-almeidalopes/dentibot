import os
from flask import Blueprint, request, jsonify, current_app
from flask_mail import Message

communication_bp = Blueprint("communications", __name__, url_prefix="/communications")


@communication_bp.route("/health")
def health():
    return jsonify({"status": "ok", "service": "communication"})


@communication_bp.route("/email", methods=["POST"])
def send_email():
    data = request.get_json() or {}
    required = ("to", "subject", "body")
    if not all(k in data for k in required):
        return jsonify({"erro": f"Campos obrigatórios: {', '.join(required)}"}), 400

    mail = current_app.extensions.get("mail")
    try:
        msg = Message(
            subject=data["subject"],
            sender=os.getenv("MAIL_USERNAME"),
            recipients=data["to"] if isinstance(data["to"], list) else [data["to"]],
        )
        msg.html = data["body"]
        mail.send(msg)
        return jsonify({"mensagem": "E-mail enviado com sucesso."})
    except Exception as e:
        return jsonify({"erro": f"Falha ao enviar e-mail: {str(e)}"}), 500


@communication_bp.route("/sms", methods=["POST"])
def send_sms():
    data = request.get_json() or {}
    if not data.get("to") or not data.get("message"):
        return jsonify({"erro": "to e message são obrigatórios."}), 400

    twilio_sid = os.getenv("TWILIO_SID")
    twilio_token = os.getenv("TWILIO_TOKEN")
    twilio_phone = os.getenv("TWILIO_PHONE")

    if not twilio_sid:
        print(f"[SMS SIMULADO] Para {data['to']}: {data['message']}")
        return jsonify({"mensagem": "SMS simulado (Twilio não configurado)."})

    try:
        from twilio.rest import Client
        client = Client(twilio_sid, twilio_token)
        message = client.messages.create(
            body=data["message"],
            from_=twilio_phone,
            to=data["to"],
        )
        return jsonify({"mensagem": "SMS enviado.", "sid": message.sid})
    except Exception as e:
        return jsonify({"erro": f"Falha ao enviar SMS: {str(e)}"}), 500


@communication_bp.route("/whatsapp", methods=["POST"])
def send_whatsapp():
    data = request.get_json() or {}
    if not data.get("to") or not data.get("message"):
        return jsonify({"erro": "to e message são obrigatórios."}), 400

    token = os.getenv("WHATSAPP_TOKEN")
    if not token:
        print(f"[WHATSAPP SIMULADO] Para {data['to']}: {data['message']}")
        return jsonify({"mensagem": "WhatsApp simulado (API não configurada)."})

    import requests as req
    phone_id = os.getenv("WHATSAPP_PHONE_ID", "")
    url = f"https://graph.facebook.com/v17.0/{phone_id}/messages"
    payload = {
        "messaging_product": "whatsapp",
        "to": data["to"],
        "type": "text",
        "text": {"body": data["message"]},
    }
    resp = req.post(url, json=payload, headers={"Authorization": f"Bearer {token}"}, timeout=10)
    if resp.ok:
        return jsonify({"mensagem": "WhatsApp enviado."})
    return jsonify({"erro": "Falha ao enviar WhatsApp.", "detalhes": resp.text}), 500


@communication_bp.route("/appointment-reminder", methods=["POST"])
def send_appointment_reminder():
    """Envia lembrete de consulta via canal preferido do paciente."""
    data = request.get_json() or {}
    channel = data.get("channel", "email")
    patient_name = data.get("patient_name", "Paciente")
    scheduled_at = data.get("scheduled_at", "")
    dentist_name = data.get("dentist_name", "")

    message = (
        f"Olá {patient_name}! Lembramos que você tem consulta marcada com "
        f"{dentist_name} em {scheduled_at}. DentiBot."
    )
    html_body = f"""
    <div style="font-family:sans-serif;max-width:500px;margin:auto">
      <h2 style="color:#059669">DentiBot – Lembrete de Consulta</h2>
      <p>Olá, <strong>{patient_name}</strong>!</p>
      <p>Sua consulta com <strong>{dentist_name}</strong> está agendada para:</p>
      <h3 style="color:#059669">{scheduled_at}</h3>
      <p style="color:#94a3b8;font-size:12px">Em caso de dúvidas entre em contato conosco.</p>
    </div>"""

    if channel == "email":
        mail = current_app.extensions.get("mail")
        try:
            msg = Message(
                "DentiBot – Lembrete de Consulta",
                sender=os.getenv("MAIL_USERNAME"),
                recipients=[data.get("to")],
            )
            msg.html = html_body
            mail.send(msg)
        except Exception as e:
            return jsonify({"erro": str(e)}), 500
    elif channel == "sms":
        print(f"[SMS] {data.get('to')}: {message}")
    elif channel == "whatsapp":
        print(f"[WHATSAPP] {data.get('to')}: {message}")

    return jsonify({"mensagem": f"Lembrete enviado via {channel.upper()}."})
