import os
from flask import Flask
from flask_mail import Mail
from flask_cors import CORS
from dotenv import load_dotenv
from .routes import communication_bp

load_dotenv()


def create_app():
    app = Flask(__name__)
    CORS(app, supports_credentials=True)

    app.config["MAIL_SERVER"] = os.getenv("MAIL_SERVER", "smtp.gmail.com")
    app.config["MAIL_PORT"] = int(os.getenv("MAIL_PORT", 587))
    app.config["MAIL_USE_TLS"] = os.getenv("MAIL_USE_TLS", "True") == "True"
    app.config["MAIL_USERNAME"] = os.getenv("MAIL_USERNAME")
    app.config["MAIL_PASSWORD"] = os.getenv("MAIL_PASSWORD")

    Mail(app)
    app.register_blueprint(communication_bp)
    return app


app = create_app()

if __name__ == "__main__":
    port = int(os.getenv("PORT", 5005))
    app.run(host="0.0.0.0", port=port, debug=False)
