from flask_sqlalchemy import SQLAlchemy
from datetime import datetime

db = SQLAlchemy()


class Invoice(db.Model):
    __tablename__ = "invoices"

    id = db.Column(db.Integer, primary_key=True)
    patient_id = db.Column(db.Integer, nullable=False)
    patient_name = db.Column(db.String(150))
    appointment_id = db.Column(db.Integer, nullable=True)
    procedure = db.Column(db.String(200))
    amount = db.Column(db.Float, nullable=False)
    discount = db.Column(db.Float, default=0)
    insurance_coverage = db.Column(db.Float, default=0)
    net_amount = db.Column(db.Float)
    status = db.Column(db.String(20), default="pendente")
    due_date = db.Column(db.Date, nullable=True)
    notes = db.Column(db.Text)
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    updated_at = db.Column(db.DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)

    payments = db.relationship("Payment", backref="invoice", lazy=True)

    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        if self.net_amount is None:
            self.net_amount = (self.amount or 0) - (self.discount or 0) - (self.insurance_coverage or 0)

    def to_dict(self):
        return {
            "id": self.id,
            "patient_id": self.patient_id,
            "patient_name": self.patient_name,
            "appointment_id": self.appointment_id,
            "procedure": self.procedure,
            "amount": self.amount,
            "discount": self.discount,
            "insurance_coverage": self.insurance_coverage,
            "net_amount": self.net_amount,
            "status": self.status,
            "due_date": self.due_date.isoformat() if self.due_date else None,
            "notes": self.notes,
            "created_at": self.created_at.isoformat() if self.created_at else None,
        }


class Payment(db.Model):
    __tablename__ = "payments"

    id = db.Column(db.Integer, primary_key=True)
    invoice_id = db.Column(db.Integer, db.ForeignKey("invoices.id"), nullable=False)
    amount_paid = db.Column(db.Float, nullable=False)
    payment_method = db.Column(db.String(50))
    transaction_id = db.Column(db.String(100))
    notes = db.Column(db.Text)
    paid_at = db.Column(db.DateTime, default=datetime.utcnow)

    def to_dict(self):
        return {
            "id": self.id,
            "invoice_id": self.invoice_id,
            "amount_paid": self.amount_paid,
            "payment_method": self.payment_method,
            "transaction_id": self.transaction_id,
            "notes": self.notes,
            "paid_at": self.paid_at.isoformat() if self.paid_at else None,
        }
