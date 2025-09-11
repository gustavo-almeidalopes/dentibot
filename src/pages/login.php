<?php
require_once "../../config/auth.php";

// Inicia a sessão para usar a variável $_SESSION para o token CSRF
if (session_status() === PHP_SESSION_NONE) {
    session_start();
}

// Gera um token CSRF se não existir um na sessão
if (empty($_SESSION['csrf_token'])) {
    $_SESSION['csrf_token'] = bin2hex(random_bytes(32));
}

$erro = "";

if ($_SERVER["REQUEST_METHOD"] === "POST") {
    // 1. Validação do Token CSRF
    if (!isset($_POST['csrf_token']) || !hash_equals($_SESSION['csrf_token'], $_POST['csrf_token'])) {
        $erro = "Erro de validação. Por favor, tente novamente.";
    } else {
        // 2. Validação básica de entrada
        $email = filter_input(INPUT_POST, "email", FILTER_VALIDATE_EMAIL);
        $password = $_POST["password"] ?? "";

        if (!$email) {
            $erro = "Formato de e-mail inválido!";
        } else {
            // Assumindo que a função login() está no auth.php
            // Recomenda-se fortemente que a função login() chame session_regenerate_id(true); após sucesso.
            if (login($email, $password)) {
                // Remove o token antigo após o uso
                unset($_SESSION['csrf_token']);
                
                switch ($_SESSION["user_role"]) {
                    case "recepcionista":
                        header("Location: recepcionista.php");
                        break;
                    case "dentista":
                        header("Location: dentista.php");
                        break;
                    case "coordenador":
                        header("Location: coordenador.php");
                        break;
                    default:
                        header("Location: cliente.php");
                }
                exit;
            } else {
                $erro = "E-mail ou senha inválidos!";
            }
        }
    }
}

// Inclui o header após toda a lógica de processamento
include "../partials/header.php"; 
?>

<!DOCTYPE html>
<html lang="pt-BR">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <meta name="description" content="Acesse ou crie sua conta no DentiBot, o sistema de gestão para sua clínica odontológica.">
  
  <title>DentiBot — Cadastro e Login</title>

  <link href="https://cdn.jsdelivr.net/npm/tailwindcss@2.2.19/dist/tailwind.min.css" rel="stylesheet">
  
  <link rel="stylesheet" href="css/style.css">
  <link rel="stylesheet" href="css/login.css">

  <script src="https://accounts.google.com/gsi/client" async defer></script>
</head>
<body class="bg-gray-50 flex items-center justify-center min-h-screen">
  <main class="w-full max-w-md mx-auto p-6 md:p-8">

    <section id="loginCard" class="login-card bg-white p-8 rounded-lg shadow-md">
      <header class="text-center mb-6">
        <div class="mx-auto w-16 h-16 rounded-lg flex items-center justify-center mb-3" style="background-color:var(--primary);">
          <span class="text-white font-bold text-2xl">D</span>
        </div>
        <h1 class="login-title">Entrar no DentiBot</h1>
        <p class="login-subtitle mt-2">Acesse sua conta para gerenciar sua clínica.</p>
      </header>
      
      <?php if (!empty($erro)): ?>
        <div class="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded relative mb-4" role="alert">
          <span class="block sm:inline"><?php echo htmlspecialchars($erro); ?></span>
        </div>
      <?php endif; ?>
      
      <div id="buttonDiv" class="mb-4"></div>
      
      <div class="separator">ou</div>
      
      <form class="space-y-4" action="login.php" method="POST">
        <input type="hidden" name="csrf_token" value="<?php echo htmlspecialchars($_SESSION['csrf_token']); ?>">

        <div>
          <label for="email" class="block text-sm font-medium mb-1">E-mail</label>
          <input id="email" name="email" type="email" autocomplete="email" required class="w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-primary" placeholder="seu@email.com">
        </div>
        <div>
          <label for="password" class="block text-sm font-medium mb-1">Senha</label>
          <input id="password" name="password" type="password" autocomplete="current-password" required class="w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-primary" placeholder="••••••••">
        </div>
        <div class="flex items-center justify-between text-sm">
          <div class="flex items-center">
            <input id="remember-me" name="remember-me" type="checkbox" class="h-4 w-4 rounded border-gray-300 text-primary focus:ring-primary">
            <label for="remember-me" class="ml-2 block text-sm text-gray-900">Lembrar-me</label>
          </div>
          <a href="#" class="font-medium text-sm nav-link">Esqueceu a senha?</a>
        </div>
        <div class="pt-3">
          <button type="submit" class="w-full button button-primary py-2.5">Entrar</button>
        </div>
        <div class="text-center text-sm text-foreground-muted">
          <p>Não tem conta? <a href="#" id="showRegister" class="text-primary font-semibold hover:underline">Criar conta</a></p>
        </div>
      </form>
    </section>

    <section id="cadastroCard" class="login-card bg-white p-8 rounded-lg shadow-md hidden">
        </section>

  </main>
  
  <script src="../js/jwt-decode.min.js"></script>
  <script src="../js/login.js"></script>
  <script src="https://cdnjs.cloudflare.com/ajax/libs/animejs/3.2.1/anime.min.js"></script>
</body>
</html>