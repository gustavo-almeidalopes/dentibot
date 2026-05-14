from flask_sqlalchemy import SQLAlchemy
from datetime import datetime

db = SQLAlchemy()

STATUS_CHOICES = ("agendado", "confirmado", "realizado", "cancelado", "remarcado")


class Appointment(db.Model):
    __tablename__ = "appointments"

    id = db.Column(db.Integer, primary_key=True)
    patient_id = db.Column(db.Integer, nullable=False)
    patient_name = db.Column(db.String(150))
    dentist_id = db.Column(db.Integer, nullable=False)
    dentist_name = db.Column(db.String(150))
    scheduled_at = db.Column(db.DateTime, nullable=False)
    duration_min = db.Column(db.Integer, default=30)
    procedure = db.Column(db.String(200))
    status = db.Column(db.String(20), default="agendado")
    notes = db.Column(db.Text)
    room = db.Column(db.String(50))
    insurance_name = db.Column(db.String(100))
    confirmation_sent = db.Column(db.Boolean, default=False)
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    updated_at = db.Column(db.DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)

    def to_dict(self):
        return {
            "id": self.id,
            "patient_id": self.patient_id,
            "patient_name": self.patient_name,
            "dentist_id": self.dentist_id,
            "dentist_name": self.dentist_name,
            "scheduled_at": self.scheduled_at.isoformat() if self.scheduled_at else None,
            "duration_min": self.duration_min,
            "procedure": self.procedure,
            "status": self.status,
            "notes": self.notes,
            "room": self.room,
            "insurance_name": self.insurance_name,
            "confirmation_sent": self.confirmation_sent,
            "created_at": self.created_at.isoformat() if self.created_at else None,
        }
