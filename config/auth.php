<?php
session_start();
require_once __DIR__ . "/db.php";

function login($email, $password) {
    global $pdo;

    $email = filter_var($email, FILTER_SANITIZE_EMAIL);

    $stmt = $pdo->prepare("SELECT * FROM usuarios WHERE email = :email LIMIT 1");
    $stmt->bindParam(":email", $email);
    $stmt->execute();
    $user = $stmt->fetch(PDO::FETCH_ASSOC);

    if ($user && password_verify($password, $user["senha"])) {
        session_regenerate_id(true); // evita session fixation
        $_SESSION["user_id"] = $user["id"];
        $_SESSION["user_role"] = $user["perfil"];
        $_SESSION["user_name"] = $user["nome"];
        return true;
    }
    return false;
}

function checkAuth($role = null) {
    if (!isset($_SESSION["user_id"])) {
        header("Location: /src/pages/login.php");
        exit;
    }

    if ($role && $_SESSION["user_role"] !== $role) {
        echo "<h2>Acesso negado!</h2>";
        exit;
    }
}

function logout() {
    $_SESSION = [];
    if (ini_get("session.use_cookies")) {
        $params = session_get_cookie_params();
        setcookie(session_name(), '', time() - 42000,
            $params["path"], $params["domain"],
            $params["secure"], $params["httponly"]
        );
    }
    session_destroy();
    header("Location: /src/pages/login.php");
    exit;
}