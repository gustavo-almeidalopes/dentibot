from flask import Blueprint, request, jsonify
from datetime import date
from .models import db, Patient, MedicalRecord

patient_bp = Blueprint("patients", __name__, url_prefix="/patients")


@patient_bp.route("/health")
def health():
    return jsonify({"status": "ok", "service": "patient"})


@patient_bp.route("", methods=["GET"])
def list_patients():
    q = request.args.get("q", "")
    query = Patient.query.filter_by(is_active=True)
    if q:
        query = query.filter(
            Patient.full_name.ilike(f"%{q}%") | Patient.cpf.ilike(f"%{q}%")
        )
    patients = query.order_by(Patient.full_name).all()
    return jsonify([p.to_dict() for p in patients])


@patient_bp.route("", methods=["POST"])
def create_patient():
    data = request.get_json() or {}
    required = ("full_name", "cpf", "birth_date")
    if not all(k in data for k in required):
        return jsonify({"erro": "Campos obrigatórios: full_name, cpf, birth_date"}), 400

    if Patient.query.filter_by(cpf=data["cpf"]).first():
        return jsonify({"erro": "CPF já cadastrado."}), 409

    try:
        bd = date.fromisoformat(data["birth_date"])
    except ValueError:
        return jsonify({"erro": "birth_date inválido. Use YYYY-MM-DD."}), 400

    patient = Patient(
        full_name=data["full_name"],
        cpf=data["cpf"],
        birth_date=bd,
        gender=data.get("gender"),
        email=data.get("email"),
        phone=data.get("phone"),
        address=data.get("address"),
        insurance_name=data.get("insurance_name"),
        insurance_number=data.get("insurance_number"),
        allergies=data.get("allergies"),
        blood_type=data.get("blood_type"),
        observations=data.get("observations"),
    )
    db.session.add(patient)
    db.session.commit()
    return jsonify(patient.to_dict()), 201


@patient_bp.route("/<int:pid>", methods=["GET"])
def get_patient(pid):
    patient = Patient.query.get_or_404(pid)
    return jsonify(patient.to_dict())


@patient_bp.route("/<int:pid>", methods=["PUT"])
def update_patient(pid):
    patient = Patient.query.get_or_404(pid)
    data = request.get_json() or {}
    fields = ("full_name", "gender", "email", "phone", "address",
              "insurance_name", "insurance_number", "allergies", "blood_type", "observations")
    for field in fields:
        if field in data:
            setattr(patient, field, data[field])
    if "birth_date" in data:
        patient.birth_date = date.fromisoformat(data["birth_date"])
    db.session.commit()
    return jsonify(patient.to_dict())


@patient_bp.route("/<int:pid>", methods=["DELETE"])
def delete_patient(pid):
    patient = Patient.query.get_or_404(pid)
    patient.is_active = False
    db.session.commit()
    return jsonify({"mensagem": "Paciente desativado (LGPD: dados preservados)."})


# ─── Prontuários ──────────────────────────────────────────────────────────────
@patient_bp.route("/<int:pid>/records", methods=["GET"])
def list_records(pid):
    Patient.query.get_or_404(pid)
    records = MedicalRecord.query.filter_by(patient_id=pid).order_by(
        MedicalRecord.created_at.desc()
    ).all()
    return jsonify([r.to_dict() for r in records])


@patient_bp.route("/<int:pid>/records", methods=["POST"])
def create_record(pid):
    Patient.query.get_or_404(pid)
    data = request.get_json() or {}
    if not data.get("dentist_id"):
        return jsonify({"erro": "dentist_id obrigatório."}), 400

    record = MedicalRecord(
        patient_id=pid,
        dentist_id=data["dentist_id"],
        dentist_name=data.get("dentist_name"),
        record_type=data.get("record_type", "consulta"),
        chief_complaint=data.get("chief_complaint"),
        diagnosis=data.get("diagnosis"),
        treatment_plan=data.get("treatment_plan"),
        procedures_done=data.get("procedures_done"),
        prescription=data.get("prescription"),
        notes=data.get("notes"),
    )
    db.session.add(record)
    db.session.commit()
    return jsonify(record.to_dict()), 201


@patient_bp.route("/<int:pid>/records/<int:rid>", methods=["PUT"])
def update_record(pid, rid):
    record = MedicalRecord.query.filter_by(id=rid, patient_id=pid).first_or_404()
    data = request.get_json() or {}
    fields = ("chief_complaint", "diagnosis", "treatment_plan",
              "procedures_done", "prescription", "notes")
    for f in fields:
        if f in data:
            setattr(record, f, data[f])
    db.session.commit()
    return jsonify(record.to_dict())
