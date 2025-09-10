<?php
require_once "../../config/auth.php";
checkAuth("recepcionista");
?>
<?php include "../partials/header.php"; ?>
<body>
    <h1>Painel da Recepção</h1>
    <p>Bem-vindo, <?php echo htmlspecialchars($_SESSION["user_name"]); ?>!</p>
    <a href="../../config/logout.php">Sair</a>
</body>
<?php include "../partials/footer.php"; ?>

<!DOCTYPE html>
<html lang="pt-BR">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>DentiBot - Painel da Recepcionista</title>
    <link href="https://cdn.jsdelivr.net/npm/tailwindcss@2.2.19/dist/tailwind.min.css" rel="stylesheet">
    <script src="https://unpkg.com/lucide@latest"></script>
    <style>
        :root {
            --background: #f8fafc;
            --foreground: #475569;
            --card: #ffffff;
            --card-foreground: #475569;
            --primary: #059669;
            --primary-foreground: #ffffff;
            --secondary: #f1f5f9;
            --secondary-foreground: #475569;
            --muted: #f0fdf4;
            --muted-foreground: #374151;
            --accent: #10b981;
            --border: #e2e8f0;
            --input: #e2e8f0;
            --ring: #059669;
        }

        body {
            background-color: var(--background);
            color: var(--foreground);
            font-family: sans-serif;
        }

        html {
            scroll-behavior: smooth;
        }
        
        .btn {
            display: inline-flex; align-items: center; justify-content: center;
            padding: 0.5rem 1rem; border-radius: 0.5rem; font-weight: 600;
            transition: all 0.2s; border: 1px solid transparent;
        }
        .btn-primary {
            background-color: var(--primary); color: var(--primary-foreground);
            border-color: var(--primary);
        }
        .btn-primary:hover { background-color: #047857; border-color: #047857; }
        .btn-secondary {
            background-color: var(--secondary); color: var(--secondary-foreground);
            border-color: var(--border);
        }
        .btn-secondary:hover { background-color: #e2e8f0; }
        
        .form-input, .form-textarea, .form-select {
            width: 100%; padding: 0.75rem; border-radius: 0.5rem;
            border: 1px solid var(--border); background-color: var(--background);
            transition: all 0.2s;
        }
        .form-input:focus, .form-textarea:focus, .form-select:focus {
            outline: none; border-color: var(--ring);
            box-shadow: 0 0 0 2px rgba(5, 150, 105, 0.2);
        }

        .sidebar-link.active {
            background-color: var(--muted); color: var(--primary); font-weight: 600;
        }
        .sidebar-link.active i { color: var(--primary); }

        .info-box {
            background-color: var(--muted); border-left: 4px solid var(--accent);
            padding: 0.75rem 1rem; margin-top: 1rem; border-radius: 0.25rem;
            color: var(--muted-foreground);
        }
    </style>
</head>
<body class="min-h-screen">

    <div class="flex h-screen">
        <aside id="sidebar" class="fixed inset-y-0 left-0 bg-white w-64 p-4 border-r border-gray-200 transform -translate-x-full md:relative md:translate-x-0 transition-transform duration-200 ease-in-out z-30">
            <div class="flex items-center space-x-2 pb-4 border-b">
                <div class="h-8 w-8 rounded-lg flex items-center justify-center bg-primary">
                    <span class="text-lg font-bold text-primary-foreground">D</span>
                </div>
                <span class="text-xl font-bold">DentiBot</span>
            </div>
            <nav class="mt-6">
                <a href="#dashboard" class="sidebar-link flex items-center space-x-3 p-2 rounded-lg text-gray-600 hover:bg-gray-100">
                    <i data-lucide="layout-dashboard"></i><span>Dashboard</span>
                </a>
                <a href="#agendamento" class="sidebar-link flex items-center space-x-3 p-2 rounded-lg text-gray-600 hover:bg-gray-100 mt-2">
                    <i data-lucide="calendar-plus"></i><span>Agendamento</span>
                </a>
                <a href="#confirmacao" class="sidebar-link flex items-center space-x-3 p-2 rounded-lg text-gray-600 hover:bg-gray-100 mt-2">
                    <i data-lucide="mail-check"></i><span>Confirmações</span>
                </a>
                <a href="#reagendamento" class="sidebar-link flex items-center space-x-3 p-2 rounded-lg text-gray-600 hover:bg-gray-100 mt-2">
                    <i data-lucide="calendar-clock"></i><span>Reagendamento</span>
                </a>
                <a href="#observacoes" class="sidebar-link flex items-center space-x-3 p-2 rounded-lg text-gray-600 hover:bg-gray-100 mt-2">
                    <i data-lucide="notebook-pen"></i><span>Observações</span>
                </a>
                <a href="#convenios" class="sidebar-link flex items-center space-x-3 p-2 rounded-lg text-gray-600 hover:bg-gray-100 mt-2">
                    <i data-lucide="shield-plus"></i><span>Convênios</span>
                </a>
            </nav>
        </aside>

        <div class="flex-1 flex flex-col overflow-y-auto">
            <header class="sticky top-0 bg-white/80 backdrop-blur-lg shadow-sm z-20">
                <div class="container mx-auto px-4 sm:px-6 lg:px-8">
                    <div class="flex h-16 items-center justify-between">
                        <button id="menu-open-btn" class="md:hidden text-gray-600"><i data-lucide="menu" class="h-6 w-6"></i></button>
                        <h1 class="text-lg font-semibold text-gray-800">Painel da Recepção</h1>
                        <div class="flex items-center space-x-2">
                            <span class="text-sm hidden sm:block">Ana Paula</span>
                            <i data-lucide="user-circle-2" class="h-8 w-8 text-gray-500"></i>
                        </div>
                    </div>
                </div>
            </header>

            <main class="flex-1 p-4 sm:p-6 lg:p-8 space-y-8">
                
                <section id="dashboard">
                    <h2 class="text-2xl font-bold mb-4">Visão Geral de Hoje</h2>
                    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
                        <div class="bg-card p-5 rounded-xl shadow flex items-center space-x-4">
                            <div class="bg-blue-100 p-3 rounded-full"><i data-lucide="calendar-days" class="h-6 w-6 text-blue-600"></i></div>
                            <div>
                                <p class="text-3xl font-bold">12</p>
                                <p class="text-muted-foreground">Consultas para Hoje</p>
                            </div>
                        </div>
                        <div class="bg-card p-5 rounded-xl shadow flex items-center space-x-4">
                            <div class="bg-orange-100 p-3 rounded-full"><i data-lucide="mail-warning" class="h-6 w-6 text-orange-600"></i></div>
                            <div>
                                <p class="text-3xl font-bold">3</p>
                                <p class="text-muted-foreground">Confirmações Pendentes</p>
                            </div>
                        </div>
                        <div class="bg-card p-5 rounded-xl shadow flex items-center space-x-4">
                             <div class="bg-green-100 p-3 rounded-full"><i data-lucide="user-plus" class="h-6 w-6 text-green-600"></i></div>
                            <div>
                                <p class="text-3xl font-bold">2</p>
                                <p class="text-muted-foreground">Novos Pacientes</p>
                            </div>
                        </div>
                    </div>
                </section>

                <section id="agendamento" class="bg-card rounded-xl shadow-md p-6">
                    <h2 class="text-2xl font-bold mb-2">Agendamento Inteligente</h2>
                    <p class="text-muted-foreground mb-6">Insira novas consultas no calendário digital de forma rápida e eficiente.</p>
                    <form>
                        <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
                            <div>
                                <label for="pacienteNome" class="font-medium">Nome do Paciente:</label>
                                <input type="text" id="pacienteNome" name="pacienteNome" class="form-input mt-2" placeholder="Nome Completo" required>
                            </div>
                            <div>
                                <label for="dentistaResponsavel" class="font-medium">Dentista Responsável:</label>
                                <select id="dentistaResponsavel" name="dentistaResponsavel" class="form-select mt-2" required>
                                    <option value="">-- Selecione o Dentista --</option>
                                    <option value="drSilva">Dr. João Silva</option>
                                    <option value="draSouza">Dra. Maria Souza</option>
                                </select>
                            </div>
                            <div>
                                <label for="dataConsulta" class="font-medium">Data da Consulta:</label>
                                <input type="date" id="dataConsulta" name="dataConsulta" class="form-input mt-2" required>
                            </div>
                            <div>
                                <label for="horaConsulta" class="font-medium">Hora da Consulta:</label>
                                <input type="time" id="horaConsulta" name="horaConsulta" class="form-input mt-2" required>
                            </div>
                        </div>
                        <div class="mt-6">
                            <label for="tipoProcedimento" class="font-medium">Tipo de Procedimento:</label>
                            <select id="tipoProcedimento" name="tipoProcedimento" class="form-select mt-2" required>
                                <option value="">-- Selecione o Procedimento --</option>
                                <option value="avaliacao">Avaliação</option>
                                <option value="limpeza">Profilaxia/Limpeza</option>
                                <option value="emergencia">Emergência</option>
                            </select>
                        </div>
                        <button type="submit" class="btn btn-primary mt-6">
                            <i data-lucide="calendar-plus" class="mr-2 h-4 w-4"></i>Agendar Consulta
                        </button>
                        <div class="info-box"><p>O sistema ajustará a agenda do dentista e iniciará o fluxo de confirmação automaticamente.</p></div>
                    </form>
                </section>

                <section id="confirmacao" class="bg-card rounded-xl shadow-md p-6">
                    <h2 class="text-2xl font-bold mb-2">Confirmação e Gerenciamento de Presença</h2>
                    <p class="text-muted-foreground mb-6">Monitore o status das confirmações e atualize manualmente se necessário.</p>
                    <div class="overflow-x-auto">
                        <table class="w-full text-left">
                            <thead class="bg-gray-50">
                                <tr>
                                    <th class="p-3 font-semibold text-sm">Paciente</th>
                                    <th class="p-3 font-semibold text-sm">Data/Hora</th>
                                    <th class="p-3 font-semibold text-sm">Status</th>
                                    <th class="p-3 font-semibold text-sm">Ação</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr class="border-t"><td class="p-3">Carlos Santos</td><td class="p-3">25/11/2025 10:00</td><td class="p-3"><span class="bg-orange-100 text-orange-700 text-xs font-medium px-2 py-1 rounded-full">Aguardando</span></td><td class="p-3"><button class="btn btn-secondary text-xs">Ver Detalhes</button></td></tr>
                                <tr class="border-t"><td class="p-3">Fernanda Lima</td><td class="p-3">26/11/2025 14:30</td><td class="p-3"><span class="bg-green-100 text-green-700 text-xs font-medium px-2 py-1 rounded-full">Confirmado</span></td><td class="p-3"><button class="btn btn-secondary text-xs">Ver Detalhes</button></td></tr>
                                <tr class="border-t"><td class="p-3">Roberto Costa</td><td class="p-3">27/11/2025 09:00</td><td class="p-3"><span class="bg-orange-100 text-orange-700 text-xs font-medium px-2 py-1 rounded-full">Aguardando</span></td><td class="p-3"><button class="btn btn-secondary text-xs">Ver Detalhes</button></td></tr>
                            </tbody>
                        </table>
                    </div>
                </section>
                
                <section id="reagendamento" class="bg-card rounded-xl shadow-md p-6">
                    <h2 class="text-2xl font-bold mb-2">Reagendamento Simplificado</h2>
                    <p class="text-muted-foreground mb-6">Ajuste consultas no calendário e notifique os envolvidos com um clique.</p>
                    <form>
                        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                            <div class="lg:col-span-3">
                                <label for="consultaReagendar" class="font-medium">Consulta a Reagendar:</label>
                                <select id="consultaReagendar" name="consultaReagendar" class="form-select mt-2" required>
                                    <option value="">-- Selecione uma consulta --</option>
                                    <option value="carlos">Carlos Santos - 25/11/2025 10:00</option>
                                    <option value="ana">Ana Paula Silva - 28/11/2025 11:30</option>
                                </select>
                            </div>
                            <div>
                                <label for="novaDataReagendamento" class="font-medium">Nova Data:</label>
                                <input type="date" id="novaDataReagendamento" name="novaDataReagendamento" class="form-input mt-2" required>
                            </div>
                            <div>
                                <label for="novaHoraReagendamento" class="font-medium">Nova Hora:</label>
                                <input type="time" id="novaHoraReagendamento" name="novaHoraReagendamento" class="form-input mt-2" required>
                            </div>
                        </div>
                        <button type="submit" class="btn btn-primary mt-6">
                             <i data-lucide="send" class="mr-2 h-4 w-4"></i>Reagendar e Notificar
                        </button>
                        <div class="info-box"><p>Notificações automáticas serão enviadas ao paciente e ao dentista.</p></div>
                    </form>
                </section>
                
                <section id="observacoes" class="bg-card rounded-xl shadow-md p-6">
                    <h2 class="text-2xl font-bold mb-2">Registro de Observações</h2>
                     <p class="text-muted-foreground mb-6">Adicione informações importantes para um atendimento personalizado.</p>
                    <form>
                        <div>
                             <label for="pacienteObservacao" class="font-medium">Selecione o Paciente:</label>
                            <select id="pacienteObservacao" name="pacienteObservacao" class="form-select mt-2" required>
                                <option value="">-- Selecione um paciente --</option>
                                <option value="ana">Ana Paula Silva</option>
                                <option value="carlos">Carlos Eduardo Santos</option>
                            </select>
                        </div>
                        <div class="mt-6">
                            <label for="observacoesEspeciais" class="font-medium">Observações Especiais:</label>
                            <textarea id="observacoesEspeciais" name="observacoesEspeciais" rows="4" class="form-textarea mt-2" placeholder="Ex: Paciente cadeirante, necessita de rampa. Alergia a Penicilina."></textarea>
                        </div>
                        <button type="submit" class="btn btn-primary mt-6">Salvar Observação</button>
                    </form>
                </section>
                
                <section id="convenios" class="bg-card rounded-xl shadow-md p-6">
                    <h2 class="text-2xl font-bold mb-2">Integração com Convênios (TISS/TUSS)</h2>
                     <p class="text-muted-foreground mb-6">Solicite e verifique autorizações de procedimentos de forma integrada.</p>
                    <form>
                        <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
                             <div>
                                <label for="pacienteConvenio" class="font-medium">Selecione o Paciente:</label>
                                <select id="pacienteConvenio" name="pacienteConvenio" class="form-select mt-2" required>
                                    <option value="">-- Selecione um paciente --</option>
                                    <option value="ana">Ana Paula Silva</option>
                                </select>
                            </div>
                            <div>
                                <label for="numeroConvenio" class="font-medium">Nº da Carteirinha:</label>
                                <input type="text" id="numeroConvenio" name="numeroConvenio" class="form-input mt-2" placeholder="Ex: 1234567890">
                            </div>
                            <div class="md:col-span-2">
                                <label for="procedimentoConvenio" class="font-medium">Procedimento para Autorização:</label>
                                <input type="text" id="procedimentoConvenio" name="procedimentoConvenio" class="form-input mt-2" placeholder="Ex: Restauração Amálgama Dente 36">
                            </div>
                        </div>
                        <div class="mt-6 space-y-2 sm:space-y-0 sm:space-x-4">
                            <button type="submit" class="btn btn-primary w-full sm:w-auto">Solicitar Autorização</button>
                            <button type="button" class="btn btn-secondary w-full sm:w-auto">Checar Status</button>
                        </div>
                         <div class="info-box"><p>O sistema pode retornar a autorização ou glosa e enviar códigos TUSS para auditoria.</p></div>
                    </form>
                </section>

            </main>

            <footer class="text-center p-4 text-sm text-gray-500 border-t">
                <p>&copy; 2025 DentiBot - Projeto de Engenharia de Software da UNICID.</p>
            </footer>
        </div>
    </div>
    
    <div id="sidebar-overlay" class="fixed inset-0 bg-black bg-opacity-50 z-20 hidden md:hidden"></div>

    <script>
        lucide.createIcons();

        document.addEventListener('DOMContentLoaded', () => {
            // Lógica da Sidebar
            const sidebar = document.getElementById('sidebar');
            const menuOpenBtn = document.getElementById('menu-open-btn');
            const sidebarOverlay = document.getElementById('sidebar-overlay');
            const openSidebar = () => { sidebar.classList.remove('-translate-x-full'); sidebarOverlay.classList.remove('hidden'); };
            const closeSidebar = () => { sidebar.classList.add('-translate-x-full'); sidebarOverlay.classList.add('hidden'); };
            menuOpenBtn.addEventListener('click', openSidebar);
            sidebarOverlay.addEventListener('click', closeSidebar);
            document.querySelectorAll('.sidebar-link').forEach(link => { link.addEventListener('click', () => { if (window.innerWidth < 768) { closeSidebar(); } }); });

            // Lógica da Sidebar Ativa com Scroll
            const sections = document.querySelectorAll('main section');
            const navLinks = document.querySelectorAll('.sidebar-link');
            const observerOptions = { root: null, rootMargin: '0px', threshold: 0.3 };
            const sectionObserver = new IntersectionObserver((entries) => {
                let activeId = '';
                entries.forEach(entry => {
                    if (entry.isIntersecting) {
                        if (entry.intersectionRatio > 0) {
                           activeId = entry.target.getAttribute('id');
                        }
                    }
                });
                 navLinks.forEach(link => {
                    link.classList.remove('active');
                    if (link.getAttribute('href') === `#${activeId}`) {
                        link.classList.add('active');
                    }
                });
            }, observerOptions);
            sections.forEach(section => { sectionObserver.observe(section); });
        });
    </script>
</body>
</html>
