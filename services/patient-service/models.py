from flask_sqlalchemy import SQLAlchemy
from datetime import datetime

db = SQLAlchemy()


class Patient(db.Model):
    __tablename__ = "patients"

    id = db.Column(db.Integer, primary_key=True)
    full_name = db.Column(db.String(150), nullable=False)
    cpf = db.Column(db.String(14), unique=True, nullable=False)
    birth_date = db.Column(db.Date, nullable=False)
    gender = db.Column(db.String(20))
    email = db.Column(db.String(120))
    phone = db.Column(db.String(20))
    address = db.Column(db.String(255))
    insurance_name = db.Column(db.String(100))
    insurance_number = db.Column(db.String(50))
    allergies = db.Column(db.Text)
    blood_type = db.Column(db.String(5))
    observations = db.Column(db.Text)
    is_active = db.Column(db.Boolean, default=True)
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    updated_at = db.Column(db.DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)

    records = db.relationship("MedicalRecord", backref="patient", lazy=True)

    def to_dict(self):
        return {
            "id": self.id,
            "full_name": self.full_name,
            "cpf": self.cpf,
            "birth_date": self.birth_date.isoformat() if self.birth_date else None,
            "gender": self.gender,
            "email": self.email,
            "phone": self.phone,
            "address": self.address,
            "insurance_name": self.insurance_name,
            "insurance_number": self.insurance_number,
            "allergies": self.allergies,
            "blood_type": self.blood_type,
            "observations": self.observations,
            "is_active": self.is_active,
            "created_at": self.created_at.isoformat() if self.created_at else None,
        }


class MedicalRecord(db.Model):
    __tablename__ = "medical_records"

    id = db.Column(db.Integer, primary_key=True)
    patient_id = db.Column(db.Integer, db.ForeignKey("patients.id"), nullable=False)
    dentist_id = db.Column(db.Integer, nullable=False)
    dentist_name = db.Column(db.String(150))
    record_type = db.Column(db.String(50), default="consulta")
    chief_complaint = db.Column(db.Text)
    diagnosis = db.Column(db.Text)
    treatment_plan = db.Column(db.Text)
    procedures_done = db.Column(db.Text)
    prescription = db.Column(db.Text)
    next_appointment = db.Column(db.DateTime)
    attachments = db.Column(db.Text)
    created_at = db.Column(db.DateTime, default=datetime.utcnow)

    def to_dict(self):
        return {
            "id": self.id,
            "patient_id": self.patient_id,
            "dentist_id": self.dentist_id,
            "dentist_name": self.dentist_name,
            "record_type": self.record_type,
            "chief_complaint": self.chief_complaint,
            "diagnosis": self.diagnosis,
            "treatment_plan": self.treatment_plan,
            "procedures_done": self.procedures_done,
            "prescription": self.prescription,
            "next_appointment": self.next_appointment.isoformat() if self.next_appointment else None,
            "created_at": self.created_at.isoformat() if self.created_at else None,
        }
