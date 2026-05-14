from flask import Blueprint, request, jsonify
from datetime import datetime
from .models import db, Appointment

appointment_bp = Blueprint("appointments", __name__, url_prefix="/appointments")


@appointment_bp.route("/health")
def health():
    return jsonify({"status": "ok", "service": "appointment"})


@appointment_bp.route("", methods=["GET"])
def list_appointments():
    dentist_id = request.args.get("dentist_id", type=int)
    patient_id = request.args.get("patient_id", type=int)
    status = request.args.get("status")
    date_from = request.args.get("date_from")
    date_to = request.args.get("date_to")

    query = Appointment.query

    if dentist_id:
        query = query.filter_by(dentist_id=dentist_id)
    if patient_id:
        query = query.filter_by(patient_id=patient_id)
    if status:
        query = query.filter_by(status=status)
    if date_from:
        query = query.filter(Appointment.scheduled_at >= datetime.fromisoformat(date_from))
    if date_to:
        query = query.filter(Appointment.scheduled_at <= datetime.fromisoformat(date_to))

    appointments = query.order_by(Appointment.scheduled_at).all()
    return jsonify([a.to_dict() for a in appointments])


@appointment_bp.route("", methods=["POST"])
def create_appointment():
    data = request.get_json() or {}
    required = ("patient_id", "dentist_id", "scheduled_at")
    if not all(k in data for k in required):
        return jsonify({"erro": f"Campos obrigatórios: {', '.join(required)}"}), 400

    try:
        scheduled_at = datetime.fromisoformat(data["scheduled_at"])
    except ValueError:
        return jsonify({"erro": "scheduled_at inválido. Use ISO 8601."}), 400

    conflict = Appointment.query.filter(
        Appointment.dentist_id == data["dentist_id"],
        Appointment.scheduled_at == scheduled_at,
        Appointment.status.notin_(["cancelado"]),
    ).first()
    if conflict:
        return jsonify({"erro": "Dentista já possui consulta neste horário."}), 409

    appt = Appointment(
        patient_id=data["patient_id"],
        patient_name=data.get("patient_name"),
        dentist_id=data["dentist_id"],
        dentist_name=data.get("dentist_name"),
        scheduled_at=scheduled_at,
        duration_min=data.get("duration_min", 30),
        procedure=data.get("procedure"),
        status="agendado",
        notes=data.get("notes"),
        room=data.get("room"),
        insurance_name=data.get("insurance_name"),
    )
    db.session.add(appt)
    db.session.commit()
    return jsonify(appt.to_dict()), 201


@appointment_bp.route("/<int:aid>", methods=["GET"])
def get_appointment(aid):
    appt = Appointment.query.get_or_404(aid)
    return jsonify(appt.to_dict())


@appointment_bp.route("/<int:aid>", methods=["PUT"])
def update_appointment(aid):
    appt = Appointment.query.get_or_404(aid)
    data = request.get_json() or {}
    fields = ("patient_name", "dentist_name", "duration_min", "procedure",
              "status", "notes", "room", "insurance_name", "confirmation_sent")
    for field in fields:
        if field in data:
            setattr(appt, field, data[field])
    if "scheduled_at" in data:
        appt.scheduled_at = datetime.fromisoformat(data["scheduled_at"])
    db.session.commit()
    return jsonify(appt.to_dict())


@appointment_bp.route("/<int:aid>/cancel", methods=["POST"])
def cancel_appointment(aid):
    appt = Appointment.query.get_or_404(aid)
    appt.status = "cancelado"
    db.session.commit()
    return jsonify({"mensagem": "Consulta cancelada.", "appointment": appt.to_dict()})


@appointment_bp.route("/<int:aid>/confirm", methods=["POST"])
def confirm_appointment(aid):
    appt = Appointment.query.get_or_404(aid)
    appt.status = "confirmado"
    appt.confirmation_sent = True
    db.session.commit()
    return jsonify({"mensagem": "Consulta confirmada.", "appointment": appt.to_dict()})


@appointment_bp.route("/slots", methods=["GET"])
def available_slots():
    """Retorna horários livres de um dentista em uma data."""
    dentist_id = request.args.get("dentist_id", type=int)
    date_str = request.args.get("date")
    if not dentist_id or not date_str:
        return jsonify({"erro": "dentist_id e date são obrigatórios."}), 400

    target_date = datetime.fromisoformat(date_str).date()
    booked = Appointment.query.filter(
        Appointment.dentist_id == dentist_id,
        Appointment.status.notin_(["cancelado"]),
    ).all()

    booked_hours = {
        a.scheduled_at.hour for a in booked
        if a.scheduled_at.date() == target_date
    }

    work_hours = list(range(8, 18))
    slots = [
        {"hour": h, "label": f"{h:02d}:00", "available": h not in booked_hours}
        for h in work_hours
    ]
    return jsonify(slots)
