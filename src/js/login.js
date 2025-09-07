document.addEventListener("DOMContentLoaded", () => {
  const loginCard = document.getElementById("loginCard");
  const cadastroCard = document.getElementById("cadastroCard");
  const cadastroForm = document.getElementById("cadastroForm");

  function redirecionarUsuario(usuario) {
    // Redireciona o usuário com base na sua função
    switch (usuario.role) {
      case "coordenador":
        window.location.href = "coordenador.html";
        break;
      case "dentista":
        window.location.href = "dentista.html";
        break;
      case "recepcionista":
        window.location.href = "recepcionista.html";
        break;
      case "almoxarifado":
        window.location.href = "almoxarifado.html";
        break;
      default:
        window.location.href = "cliente.html";
    }
  }

  // Login com Google
  window.handleCredentialResponse = function (response) {
    // Verifica se a resposta do Google contém a credencial
    if (!response || !response.credential) {
        console.error("Resposta inválida do Google. O Client ID pode estar incorreto.");
        alert("ERRO: Resposta inválida do Google.");
        return;
    }

    const data = jwt_decode(response.credential);
    const email = data.email;
    const usuarios = JSON.parse(localStorage.getItem("usuarios")) || [];

    const usuario = usuarios.find((u) => u.email === email);

    if (usuario && usuario.status === "ativo") {
      // Usuário já cadastrado → entra direto
      alert("Bem-vindo de volta, " + usuario.name);
      redirecionarUsuario(usuario);
    } else {
      // Usuário novo → mostrar formulário de cadastro
      loginCard.classList.add("hidden");
      cadastroCard.classList.remove("hidden");

      const nomeInput = cadastroForm.querySelector('[name="nome"]');
      if (nomeInput) {
        nomeInput.value = data.name;
      }
      cadastroForm.dataset.email = email;
    }
  };

  // Inicializa botão Google
  window.onload = function () {
    try {
      google.accounts.id.initialize({
        client_id: "USAR_ID_REAL_DO_CLIENTE", // Lembre-se de usar seu Client ID real
        callback: handleCredentialResponse,
      });
      google.accounts.id.renderButton(document.getElementById("buttonDiv"), {
        type: "standard",
        shape: "pill",
        theme: "outline",
        text: "continue_with",
        size: "large",
        locale: "pt-BR",
        logo_alignment: "left",
        width: "275",
      });
    } catch (error) {
        console.error("Falha crítica ao inicializar o Google Sign-In:", error);
        alert("Não foi possível carregar a opção de login com o Google.");
    }
  };

  // Cadastro para novos clientes
  cadastroForm.addEventListener("submit", (e) => {
    e.preventDefault();

    const nome = cadastroForm.querySelector('[name="nome"]').value.trim();
    const nomeSocial = cadastroForm.querySelector('[name="nome_social"]').value.trim();
    const cpf = cadastroForm.querySelector('[name="cpf"]').value.trim();
    const nascimento = cadastroForm.querySelector('[name="nascimento"]').value.trim();
    const endereco = cadastroForm.querySelector('[name="endereco"]').value.trim();
    const prioridade = cadastroForm.querySelector('[name="prioridade"]').value;

    const usuarios = JSON.parse(localStorage.getItem("usuarios")) || [];
    const novoUsuario = {
      name: nome,
      email: cadastroForm.dataset.email,
      role: "cliente",
      status: "ativo",
      nomeSocial,
      cpf,
      nascimento,
      endereco,
      prioridade,
    };

    usuarios.push(novoUsuario);
    localStorage.setItem("usuarios", JSON.stringify(usuarios));

    alert("Cadastro concluído! Bem-vindo, " + novoUsuario.name);
    redirecionarUsuario(novoUsuario);
  });

});
