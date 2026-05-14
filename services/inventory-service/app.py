import os
from flask import Flask
from flask_cors import CORS
from dotenv import load_dotenv
from .models import db
from .routes import inventory_bp

load_dotenv()


def create_app():
    app = Flask(__name__)
    CORS(app, supports_credentials=True)
    app.config["SQLALCHEMY_DATABASE_URI"] = os.getenv("DATABASE_URL", "sqlite:///inventory.db")
    app.config["SQLALCHEMY_TRACK_MODIFICATIONS"] = False
    db.init_app(app)
    app.register_blueprint(inventory_bp)
    with app.app_context():
        db.create_all()
    return app


app = create_app()

if __name__ == "__main__":
    port = int(os.getenv("PORT", 5004))
    app.run(host="0.0.0.0", port=port, debug=False)
