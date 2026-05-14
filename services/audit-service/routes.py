from flask import Blueprint, request, jsonify
from .models import db, AuditLog

audit_bp = Blueprint("audit", __name__, url_prefix="/audit")


@audit_bp.route("/health")
def health():
    return jsonify({"status": "ok", "service": "audit"})


@audit_bp.route("/log", methods=["POST"])
def create_log():
    data = request.get_json() or {}
    if not data.get("action"):
        return jsonify({"erro": "action é obrigatório."}), 400

    log = AuditLog(
        user_id=data.get("user_id"),
        user_name=data.get("user_name"),
        user_role=data.get("user_role"),
        action=data["action"],
        resource=data.get("resource"),
        resource_id=str(data.get("resource_id", "")),
        details=data.get("details"),
        ip_address=request.remote_addr,
        user_agent=request.headers.get("User-Agent", "")[:300],
        status=data.get("status", "success"),
    )
    db.session.add(log)
    db.session.commit()
    return jsonify(log.to_dict()), 201


@audit_bp.route("/logs", methods=["GET"])
def list_logs():
    user_id = request.args.get("user_id", type=int)
    action = request.args.get("action")
    resource = request.args.get("resource")
    limit = min(request.args.get("limit", 100, type=int), 500)
    offset = request.args.get("offset", 0, type=int)

    query = AuditLog.query
    if user_id:
        query = query.filter_by(user_id=user_id)
    if action:
        query = query.filter(AuditLog.action.ilike(f"%{action}%"))
    if resource:
        query = query.filter_by(resource=resource)

    total = query.count()
    logs = query.order_by(AuditLog.created_at.desc()).offset(offset).limit(limit).all()

    return jsonify({"total": total, "logs": [log.to_dict() for log in logs]})


@audit_bp.route("/logs/user/<int:uid>", methods=["GET"])
def user_logs(uid):
    logs = AuditLog.query.filter_by(user_id=uid).order_by(
        AuditLog.created_at.desc()
    ).limit(50).all()
    return jsonify([log.to_dict() for log in logs])


@audit_bp.route("/logs/summary", methods=["GET"])
def summary():
    from sqlalchemy import func
    actions = db.session.query(
        AuditLog.action, func.count(AuditLog.id).label("count")
    ).group_by(AuditLog.action).order_by(func.count(AuditLog.id).desc()).limit(10).all()

    return jsonify({
        "total_events": AuditLog.query.count(),
        "top_actions": [{"action": a, "count": c} for a, c in actions],
    })
