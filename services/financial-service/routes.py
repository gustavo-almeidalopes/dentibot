from flask import Blueprint, request, jsonify
from datetime import date, datetime
from .models import db, Invoice, Payment

financial_bp = Blueprint("financial", __name__, url_prefix="/financial")


@financial_bp.route("/health")
def health():
    return jsonify({"status": "ok", "service": "financial"})


@financial_bp.route("/overview", methods=["GET"])
def overview():
    total_invoices = Invoice.query.count()
    total_paid = Invoice.query.filter_by(status="pago").count()
    total_pending = Invoice.query.filter_by(status="pendente").count()
    total_overdue = Invoice.query.filter_by(status="vencido").count()

    revenue_paid = db.session.query(db.func.sum(Invoice.net_amount)).filter_by(status="pago").scalar() or 0
    revenue_pending = db.session.query(db.func.sum(Invoice.net_amount)).filter_by(status="pendente").scalar() or 0

    return jsonify({
        "total_invoices": total_invoices,
        "paid": total_paid,
        "pending": total_pending,
        "overdue": total_overdue,
        "revenue_paid": round(revenue_paid, 2),
        "revenue_pending": round(revenue_pending, 2),
    })


@financial_bp.route("/invoices", methods=["GET"])
def list_invoices():
    patient_id = request.args.get("patient_id", type=int)
    status = request.args.get("status")
    query = Invoice.query
    if patient_id:
        query = query.filter_by(patient_id=patient_id)
    if status:
        query = query.filter_by(status=status)
    invoices = query.order_by(Invoice.created_at.desc()).all()
    return jsonify([i.to_dict() for i in invoices])


@financial_bp.route("/invoices", methods=["POST"])
def create_invoice():
    data = request.get_json() or {}
    if not data.get("patient_id") or data.get("amount") is None:
        return jsonify({"erro": "patient_id e amount são obrigatórios."}), 400

    invoice = Invoice(
        patient_id=data["patient_id"],
        patient_name=data.get("patient_name"),
        appointment_id=data.get("appointment_id"),
        procedure=data.get("procedure"),
        amount=data["amount"],
        discount=data.get("discount", 0),
        insurance_coverage=data.get("insurance_coverage", 0),
        notes=data.get("notes"),
    )
    invoice.net_amount = invoice.amount - invoice.discount - invoice.insurance_coverage
    if data.get("due_date"):
        invoice.due_date = date.fromisoformat(data["due_date"])

    db.session.add(invoice)
    db.session.commit()
    return jsonify(invoice.to_dict()), 201


@financial_bp.route("/invoices/<int:iid>", methods=["GET"])
def get_invoice(iid):
    invoice = Invoice.query.get_or_404(iid)
    data = invoice.to_dict()
    data["payments"] = [p.to_dict() for p in invoice.payments]
    return jsonify(data)


@financial_bp.route("/invoices/<int:iid>", methods=["PUT"])
def update_invoice(iid):
    invoice = Invoice.query.get_or_404(iid)
    data = request.get_json() or {}
    for field in ("procedure", "amount", "discount", "insurance_coverage", "status", "notes"):
        if field in data:
            setattr(invoice, field, data[field])
    invoice.net_amount = invoice.amount - invoice.discount - invoice.insurance_coverage
    db.session.commit()
    return jsonify(invoice.to_dict())


@financial_bp.route("/invoices/<int:iid>/pay", methods=["POST"])
def register_payment(iid):
    invoice = Invoice.query.get_or_404(iid)
    data = request.get_json() or {}
    if not data.get("amount_paid"):
        return jsonify({"erro": "amount_paid é obrigatório."}), 400

    payment = Payment(
        invoice_id=iid,
        amount_paid=data["amount_paid"],
        payment_method=data.get("payment_method", "dinheiro"),
        transaction_id=data.get("transaction_id"),
        notes=data.get("notes"),
    )
    db.session.add(payment)

    total_paid = sum(p.amount_paid for p in invoice.payments) + data["amount_paid"]
    if total_paid >= invoice.net_amount:
        invoice.status = "pago"
    else:
        invoice.status = "parcial"

    db.session.commit()
    return jsonify({"invoice": invoice.to_dict(), "payment": payment.to_dict()}), 201


@financial_bp.route("/payments", methods=["GET"])
def list_payments():
    payments = Payment.query.order_by(Payment.paid_at.desc()).limit(100).all()
    return jsonify([p.to_dict() for p in payments])
