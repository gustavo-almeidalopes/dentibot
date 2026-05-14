from flask import Blueprint, request, jsonify
from datetime import datetime, date
from .models import db, InventoryItem, StockMovement, StockRequest

inventory_bp = Blueprint("inventory", __name__, url_prefix="/inventory")


@inventory_bp.route("/health")
def health():
    return jsonify({"status": "ok", "service": "inventory"})


@inventory_bp.route("", methods=["GET"])
def list_items():
    category = request.args.get("category")
    low_stock = request.args.get("low_stock") == "true"
    query = InventoryItem.query.filter_by(is_active=True)
    if category:
        query = query.filter_by(category=category)
    items = query.order_by(InventoryItem.name).all()
    if low_stock:
        items = [i for i in items if i.is_low_stock]
    return jsonify([i.to_dict() for i in items])


@inventory_bp.route("", methods=["POST"])
def create_item():
    data = request.get_json() or {}
    if not data.get("name"):
        return jsonify({"erro": "name é obrigatório."}), 400

    item = InventoryItem(
        name=data["name"],
        category=data.get("category"),
        unit=data.get("unit", "un"),
        quantity=data.get("quantity", 0),
        min_quantity=data.get("min_quantity", 0),
        max_quantity=data.get("max_quantity"),
        unit_cost=data.get("unit_cost", 0),
        supplier=data.get("supplier"),
        location=data.get("location"),
        notes=data.get("notes"),
    )
    if data.get("expiration_date"):
        item.expiration_date = date.fromisoformat(data["expiration_date"])

    db.session.add(item)
    db.session.commit()
    return jsonify(item.to_dict()), 201


@inventory_bp.route("/<int:iid>", methods=["GET"])
def get_item(iid):
    item = InventoryItem.query.get_or_404(iid)
    return jsonify(item.to_dict())


@inventory_bp.route("/<int:iid>", methods=["PUT"])
def update_item(iid):
    item = InventoryItem.query.get_or_404(iid)
    data = request.get_json() or {}
    fields = ("name", "category", "unit", "min_quantity", "max_quantity",
              "unit_cost", "supplier", "location", "notes")
    for f in fields:
        if f in data:
            setattr(item, f, data[f])
    db.session.commit()
    return jsonify(item.to_dict())


@inventory_bp.route("/<int:iid>/movement", methods=["POST"])
def add_movement(iid):
    item = InventoryItem.query.get_or_404(iid)
    data = request.get_json() or {}
    mov_type = data.get("movement_type")
    qty = data.get("quantity", 0)

    if mov_type not in ("entrada", "saida", "ajuste"):
        return jsonify({"erro": "movement_type deve ser: entrada, saida, ajuste"}), 400
    if qty <= 0:
        return jsonify({"erro": "quantity deve ser positivo."}), 400

    if mov_type == "saida" and item.quantity < qty:
        return jsonify({"erro": "Estoque insuficiente."}), 400

    if mov_type == "entrada":
        item.quantity += qty
    elif mov_type == "saida":
        item.quantity -= qty
    else:
        item.quantity = qty

    movement = StockMovement(
        item_id=iid,
        movement_type=mov_type,
        quantity=qty,
        reason=data.get("reason"),
        user_id=data.get("user_id"),
        user_name=data.get("user_name"),
    )
    db.session.add(movement)
    db.session.commit()
    return jsonify({"item": item.to_dict(), "movement": movement.to_dict()}), 201


@inventory_bp.route("/<int:iid>/movements", methods=["GET"])
def list_movements(iid):
    InventoryItem.query.get_or_404(iid)
    movements = StockMovement.query.filter_by(item_id=iid).order_by(
        StockMovement.created_at.desc()
    ).limit(50).all()
    return jsonify([m.to_dict() for m in movements])


# ─── Solicitações de Materiais ────────────────────────────────────────────────
@inventory_bp.route("/requests", methods=["GET"])
def list_requests():
    status = request.args.get("status")
    query = StockRequest.query
    if status:
        query = query.filter_by(status=status)
    reqs = query.order_by(StockRequest.created_at.desc()).all()
    return jsonify([r.to_dict() for r in reqs])


@inventory_bp.route("/requests", methods=["POST"])
def create_request():
    data = request.get_json() or {}
    if not data.get("item_id") or not data.get("quantity_requested"):
        return jsonify({"erro": "item_id e quantity_requested são obrigatórios."}), 400

    InventoryItem.query.get_or_404(data["item_id"])
    req = StockRequest(
        item_id=data["item_id"],
        quantity_requested=data["quantity_requested"],
        requester_id=data.get("requester_id"),
        requester_name=data.get("requester_name"),
        requester_role=data.get("requester_role"),
        notes=data.get("notes"),
    )
    db.session.add(req)
    db.session.commit()
    return jsonify(req.to_dict()), 201


@inventory_bp.route("/requests/<int:rid>", methods=["PUT"])
def update_request(rid):
    req = StockRequest.query.get_or_404(rid)
    data = request.get_json() or {}
    if "status" in data:
        req.status = data["status"]
        if data["status"] in ("aprovado", "rejeitado"):
            req.approved_by = data.get("approved_by")
            req.resolved_at = datetime.utcnow()
            if data["status"] == "aprovado":
                item = InventoryItem.query.get(req.item_id)
                if item:
                    item.quantity -= req.quantity_requested
                    movement = StockMovement(
                        item_id=item.id,
                        movement_type="saida",
                        quantity=req.quantity_requested,
                        reason=f"Solicitação #{req.id} aprovada",
                        user_name=req.approved_by,
                    )
                    db.session.add(movement)
    db.session.commit()
    return jsonify(req.to_dict())
