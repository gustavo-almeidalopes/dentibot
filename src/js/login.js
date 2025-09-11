document.addEventListener("DOMContentLoaded", () => {
    // --- Seletores dos Elementos ---
    const loginCard = document.getElementById("loginCard");
    const cadastroCard = document.getElementById("cadastroCard");
    const recoveryCard = document.getElementById("recoveryCard");
    const cadastroForm = document.getElementById("cadastroForm");

    // Links de navegação
    const showRegisterLink = document.getElementById("showRegister");
    const showLoginLink = document.getElementById("showLogin");
    const forgotPasswordLink = document.getElementById("forgotPasswordLink");
    const backToLoginLinks = document.querySelectorAll(".back-to-login-link");

    // Elementos do card de recuperação
    const recoveryFormContainer = document.getElementById("recoveryFormContainer");
    const recoverySuccessMessage = document.getElementById("recoverySuccessMessage");
    const recoveryForm = document.getElementById("recoveryForm");

    // --- LOGIN ---
    const loginForm = document.getElementById("loginForm");

    loginForm?.addEventListener("submit", (e) => {
        e.preventDefault();

        const emailInput = loginForm.querySelector('[name="email"]').value.trim();
        const senhaInput = loginForm.querySelector('[name="senha"]').value.trim();

        // Usuário de teste fixo (para demo)
        if (emailInput === "teste" && senhaInput === "12345") {
            const usuarioTeste = {
                name: "Usuário Teste",
                email: "teste@demo.com", // email fictício, pode deixar assim
                role: "cliente",
                status: "ativo",
            };
            alert("✅ Bem-vindo, Usuário Teste!");
            redirecionarUsuario(usuarioTeste);
            return;
        }


        // Caso não seja o usuário fixo, verifica usuários do localStorage
        const usuarios = JSON.parse(localStorage.getItem("usuarios")) || [];
        const usuario = usuarios.find(
            (u) => u.email === emailInput && u.status === "ativo"
        );

        if (usuario && senhaInput) {
            alert("✅ Login bem-sucedido, " + usuario.name);
            redirecionarUsuario(usuario);
        } else {
            alert("❌ Usuário ou senha inválidos!");
        }
    });

    // --- Função para trocar cards com animação ---
    const switchCard = (cardToShow) => {
        const currentCard = document.querySelector(".login-card:not(.hidden)");

        if (currentCard && currentCard !== cardToShow) {
            anime({
                targets: currentCard,
                opacity: [1, 0],
                translateY: [0, -20],
                duration: 300,
                easing: 'easeInQuad',
                complete: () => {
                    currentCard.classList.add('hidden');
                    cardToShow.classList.remove('hidden');
                    cardToShow.style.opacity = '0';
                    anime({
                        targets: cardToShow,
                        opacity: [0, 1],
                        translateY: [20, 0],
                        duration: 300,
                        easing: 'easeOutQuad'
                    });
                }
            });
        } else if (!currentCard && cardToShow) {
            cardToShow.classList.remove('hidden');
            anime({
                targets: cardToShow,
                opacity: [0, 1],
                translateY: [20, 0],
                duration: 300,
                easing: 'easeOutQuad'
            });
        }
    };

    // --- Navegação entre cards ---
    showRegisterLink?.addEventListener('click', (e) => {
        e.preventDefault();
        switchCard(cadastroCard);
    });

    showLoginLink?.addEventListener('click', (e) => {
        e.preventDefault();
        switchCard(loginCard);
    });

    forgotPasswordLink?.addEventListener('click', (e) => {
        e.preventDefault();
        switchCard(recoveryCard);
    });

    backToLoginLinks.forEach(link => {
        link.addEventListener('click', (e) => {
            e.preventDefault();
            recoveryFormContainer.style.opacity = 1;
            recoveryFormContainer.classList.remove('hidden');
            recoverySuccessMessage.classList.add('hidden');
            recoveryForm.reset();
            switchCard(loginCard);
        });
    });

    // --- Recuperação de senha ---
    recoveryForm?.addEventListener('submit', (e) => {
        e.preventDefault();
        const email = document.getElementById("recovery-email").value;
        const submitButton = recoveryForm.querySelector('button[type="submit"]');

        submitButton.textContent = 'Enviando...';
        submitButton.disabled = true;

        console.log(`FRONTEND: Solicitando recuperação para o e-mail: ${email}`);

        setTimeout(() => {
            anime({
                targets: recoveryFormContainer,
                opacity: 0,
                duration: 300,
                easing: 'easeOutQuad',
                complete: () => {
                    recoveryFormContainer.classList.add('hidden');
                    recoverySuccessMessage.classList.remove('hidden');
                    anime({
                        targets: recoverySuccessMessage,
                        opacity: [0, 1],
                        duration: 300
                    });
                    submitButton.textContent = 'Enviar Link';
                    submitButton.disabled = false;
                }
            });
        }, 1500);
    });

    // --- Redirecionamento por tipo de usuário ---
    function redirecionarUsuario(usuario) {
        switch (usuario.role) {
            case "coordenador": window.location.href = "coordenador.html"; break;
            case "dentista": window.location.href = "dentista.html"; break;
            case "recepcionista": window.location.href = "recepcionista.html"; break;
            case "almoxarifado": window.location.href = "almoxarifado.html"; break;
            default: window.location.href = "cliente.html";
        }
    }

    // --- Login com Google ---
    window.handleCredentialResponse = function (response) {
        if (!response || !response.credential) {
            console.error("Resposta inválida do Google.");
            alert("ERRO: Resposta inválida do Google.");
            return;
        }

        const data = jwt_decode(response.credential);
        const email = data.email;
        const usuarios = JSON.parse(localStorage.getItem("usuarios")) || [];
        const usuario = usuarios.find((u) => u.email === email);

        if (usuario && usuario.status === "ativo") {
            alert("Bem-vindo de volta, " + usuario.name);
            redirecionarUsuario(usuario);
        } else {
            switchCard(cadastroCard);
            const nomeInput = cadastroForm.querySelector('[name="nome"]');
            if (nomeInput) nomeInput.value = data.name;
            cadastroForm.dataset.email = email;
        }
    };

    try {
        google.accounts.id.initialize({
            client_id: "COLOQUE_SEU_CLIENT_ID_REAL_AQUI",
            callback: handleCredentialResponse,
        });
        google.accounts.id.renderButton(document.getElementById("buttonDiv"), {
            type: "standard", shape: "pill", theme: "outline", text: "continue_with",
            size: "large", locale: "pt-BR", logo_alignment: "left", width: "340",
        });
    } catch (error) {
        console.error("Falha ao inicializar o Google Sign-In:", error);
    }

    // --- Cadastro ---
    cadastroForm?.addEventListener("submit", (e) => {
        e.preventDefault();

        const nome = cadastroForm.querySelector('[name="nome"]').value.trim();
        const nomeSocial = cadastroForm.querySelector('[name="nome_social"]').value.trim();
        const cpf = cadastroForm.querySelector('[name="cpf"]').value.trim();
        const nascimento = cadastroForm.querySelector('[name="nascimento"]').value.trim();
        const cep = cadastroForm.querySelector('[name="cep"]').value.trim();
        const logradouro = cadastroForm.querySelector('[name="logradouro"]').value.trim();
        const numero = cadastroForm.querySelector('[name="numero"]').value.trim();
        const complemento = cadastroForm.querySelector('[name="complemento"]').value.trim();
        const bairro = cadastroForm.querySelector('[name="bairro"]').value.trim();
        const cidade = cadastroForm.querySelector('[name="cidade"]').value.trim();
        const estado = cadastroForm.querySelector('[name="estado"]').value.trim();
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
            endereco: `${logradouro}, ${numero} - ${complemento} - ${bairro}, ${cidade} - ${estado}, CEP: ${cep}`,
            prioridade,
        };

        usuarios.push(novoUsuario);
        localStorage.setItem("usuarios", JSON.stringify(usuarios));

        alert("Cadastro concluído! Bem-vindo, " + novoUsuario.name);
        redirecionarUsuario(novoUsuario);
    });

    // --- Validação de força de senha ---
    const inputSenha = document.getElementById('senha');
    const criteriaList = {
        upper: document.getElementById('upper'),
        lower: document.getElementById('lower'),
        number: document.getElementById('number'),
        special: document.getElementById('special'),
        length: document.getElementById('length')
    };
    const regex = {
        upper: /[A-Z]/,
        lower: /[a-z]/,
        number: /[0-9]/,
        special: /[!#@$%&]/
    };
});