from flask_sqlalchemy import SQLAlchemy
from datetime import datetime

db = SQLAlchemy()


class InventoryItem(db.Model):
    __tablename__ = "inventory_items"

    id = db.Column(db.Integer, primary_key=True)
    name = db.Column(db.String(200), nullable=False)
    category = db.Column(db.String(100))
    unit = db.Column(db.String(30))
    quantity = db.Column(db.Float, default=0)
    min_quantity = db.Column(db.Float, default=0)
    max_quantity = db.Column(db.Float, nullable=True)
    unit_cost = db.Column(db.Float, default=0)
    supplier = db.Column(db.String(150))
    location = db.Column(db.String(100))
    expiration_date = db.Column(db.Date, nullable=True)
    notes = db.Column(db.Text)
    is_active = db.Column(db.Boolean, default=True)
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    updated_at = db.Column(db.DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)

    movements = db.relationship("StockMovement", backref="item", lazy=True)
    requests = db.relationship("StockRequest", backref="item", lazy=True)

    @property
    def is_low_stock(self):
        return self.quantity <= self.min_quantity

    def to_dict(self):
        return {
            "id": self.id,
            "name": self.name,
            "category": self.category,
            "unit": self.unit,
            "quantity": self.quantity,
            "min_quantity": self.min_quantity,
            "max_quantity": self.max_quantity,
            "unit_cost": self.unit_cost,
            "supplier": self.supplier,
            "location": self.location,
            "expiration_date": self.expiration_date.isoformat() if self.expiration_date else None,
            "is_low_stock": self.is_low_stock,
            "notes": self.notes,
            "is_active": self.is_active,
        }


class StockMovement(db.Model):
    __tablename__ = "stock_movements"

    id = db.Column(db.Integer, primary_key=True)
    item_id = db.Column(db.Integer, db.ForeignKey("inventory_items.id"), nullable=False)
    movement_type = db.Column(db.String(20), nullable=False)
    quantity = db.Column(db.Float, nullable=False)
    reason = db.Column(db.String(200))
    user_id = db.Column(db.Integer)
    user_name = db.Column(db.String(150))
    created_at = db.Column(db.DateTime, default=datetime.utcnow)

    def to_dict(self):
        return {
            "id": self.id,
            "item_id": self.item_id,
            "movement_type": self.movement_type,
            "quantity": self.quantity,
            "reason": self.reason,
            "user_name": self.user_name,
            "created_at": self.created_at.isoformat() if self.created_at else None,
        }


class StockRequest(db.Model):
    __tablename__ = "stock_requests"

    id = db.Column(db.Integer, primary_key=True)
    item_id = db.Column(db.Integer, db.ForeignKey("inventory_items.id"), nullable=False)
    quantity_requested = db.Column(db.Float, nullable=False)
    requester_id = db.Column(db.Integer)
    requester_name = db.Column(db.String(150))
    requester_role = db.Column(db.String(50))
    status = db.Column(db.String(20), default="pendente")
    notes = db.Column(db.Text)
    approved_by = db.Column(db.String(150))
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    resolved_at = db.Column(db.DateTime, nullable=True)

    def to_dict(self):
        return {
            "id": self.id,
            "item_id": self.item_id,
            "quantity_requested": self.quantity_requested,
            "requester_name": self.requester_name,
            "requester_role": self.requester_role,
            "status": self.status,
            "notes": self.notes,
            "approved_by": self.approved_by,
            "created_at": self.created_at.isoformat() if self.created_at else None,
            "resolved_at": self.resolved_at.isoformat() if self.resolved_at else None,
        }
