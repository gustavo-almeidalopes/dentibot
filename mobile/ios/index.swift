import SwiftUI

// Cores baseadas no CSS do site
let greenMain = Color(hex: 0x10B981)
let yellowMain = Color(hex: 0xFBBF24)
let orangeMain = Color(hex: 0xF97316)
let darkBg = Color(hex: 0x111827)
let grayBg = Color(hex: 0xF3F4F6)

struct ContentView: View {
    var body: some View {
        ZStack(alignment: .bottomTrailing) {
            VStack(spacing: 0) {
                TopBar()
                
                ScrollView {
                    VStack(spacing: 0) {
                        HeroSection()
                        ParaQuemSection()
                        FeaturesSection()
                        BenefitsSection()
                        PricingSection()
                        TestimonialsSection()
                        FooterSection()
                    }
                }
            }
            
            // Floating Action Button (WhatsApp)
            Button(action: {
                // Ação para abrir WhatsApp
            }) {
                Image(systemName: "envelope.fill") // Utilizando ícone genérico como substituto para o WhatsApp
                    .font(.title2)
                    .foregroundColor(.white)
                    .frame(width: 56, height: 56)
                    .background(greenMain)
                    .clipShape(Circle())
                    .shadow(radius: 4)
            }
            .padding(16)
        }
        .background(Color.white.ignoresSafeArea())
    }
}

struct TopBar: View {
    var body: some View {
        HStack {
            HStack {
                ZStack {
                    RoundedRectangle(cornerRadius: 8)
                        .fill(darkBg)
                        .frame(width: 32, height: 32)
                    Text("D")
                        .foregroundColor(.white)
                        .bold()
                }
                Text("DentiBot")
                    .fontWeight(.bold)
            }
            
            Spacer()
            
            Button(action: {
                // Ação de Início de Sessão
            }) {
                Text("Login")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(darkBg)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 8)
                    .overlay(
                        RoundedRectangle(cornerRadius: 8)
                            .stroke(darkBg, lineWidth: 1)
                    )
            }
        }
        .padding()
        .background(Color.white)
        .shadow(color: Color.black.opacity(0.05), radius: 5, y: 5)
    }
}

struct HeroSection: View {
    var body: some View {
        VStack(spacing: 16) {
            Text("🏢 Software B2B · Dentistas & Clínicas")
                .font(.system(size: 12, weight: .medium))
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(grayBg)
                .cornerRadius(16)
                .padding(.top, 24)
            
            VStack(spacing: 4) {
                Text("Gerencie o seu consultório como um negócio.")
                    .font(.system(size: 28, weight: .black))
                    .multilineTextAlignment(.center)
                
                Text("Porque ele é.")
                    .font(.system(size: 28, weight: .black))
                    .multilineTextAlignment(.center)
                    .background(yellowMain.opacity(0.3))
            }
            
            Text("Para dentistas autônomos e clínicas de pequeno e médio porte. Agendamento, prontuário, cobrança e LGPD numa plataforma só.")
                .font(.system(size: 16))
                .foregroundColor(.gray)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 24)
                .padding(.bottom, 8)
            
            Button(action: {
                // Ação Testar
            }) {
                Text("Testar 30 dias grátis →")
                    .font(.system(size: 16, weight: .bold))
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: 56)
                    .background(greenMain)
                    .cornerRadius(8)
            }
            .padding(.horizontal, 24)
            
            Button(action: {
                // Ação Ver Funciona
            }) {
                Text("Ver como funciona")
                    .font(.system(size: 16, weight: .bold))
                    .foregroundColor(darkBg)
                    .frame(maxWidth: .infinity)
                    .frame(height: 56)
                    .overlay(
                        RoundedRectangle(cornerRadius: 8)
                            .stroke(darkBg, lineWidth: 1)
                    )
            }
            .padding(.horizontal, 24)
            
            HStack(spacing: 0) {
                StatItem(value: "8h", label: "Economizadas\n/semana")
                Spacer()
                StatItem(value: "↓35%", label: "Faltas em\nmédia")
                Spacer()
                StatItem(value: "500+", label: "Consultórios\nativos")
            }
            .padding(24)
            
            // Mock Card
            VStack(alignment: .leading) {
                HStack(spacing: 12) {
                    Text("📊")
                        .font(.system(size: 24))
                    VStack(alignment: .leading) {
                        Text("Faturação / Mês")
                            .fontWeight(.bold)
                        Text("€ 18.400 · +12% vs mês ant.")
                            .font(.system(size: 12))
                            .foregroundColor(.gray)
                    }
                }
            }
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color.white)
            .cornerRadius(12)
            .shadow(color: .black.opacity(0.1), radius: 10, x: 0, y: 5)
            .padding(.horizontal, 24)
            .padding(.bottom, 32)
        }
    }
}

struct StatItem: View {
    let value: String
    let label: String
    
    var body: some View {
        VStack(spacing: 4) {
            Text(value)
                .font(.system(size: 24, weight: .black))
                .foregroundColor(greenMain)
            Text(label)
                .font(.system(size: 12))
                .foregroundColor(.gray)
                .multilineTextAlignment(.center)
        }
    }
}

struct SectionHeader: View {
    let tag: String
    let title: String
    let desc: String
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(tag)
                .font(.system(size: 14, weight: .bold))
                .foregroundColor(greenMain)
            Text(title)
                .font(.system(size: 28, weight: .bold))
                .lineSpacing(4)
            Text(desc)
                .font(.system(size: 16))
                .foregroundColor(.gray)
        }
        .padding(.horizontal, 24)
        .padding(.vertical, 16)
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

struct ParaQuemSection: View {
    var body: some View {
        VStack(spacing: 16) {
            SectionHeader(
                tag: "Para Quem",
                title: "Desenvolvido para quem trabalha com medicina dentária",
                desc: "Duas realidades, uma plataforma. Escolha o perfil que mais se parece com o seu."
            )
            
            AudienceCard(eyebrow: "Perfil 1", title: "Dentista Autônomo", desc: "O DentiBot elimina o caos administrativo com agendamento automático...", accentColor: greenMain)
            AudienceCard(eyebrow: "Perfil 2", title: "Clínica Pequena ou Média", desc: "Controlo de agenda por profissional, relatórios de desempenho...", accentColor: yellowMain)
        }
        .padding(.vertical, 32)
        .background(grayBg)
    }
}

struct AudienceCard: View {
    let eyebrow: String
    let title: String
    let desc: String
    let accentColor: Color
    
    var body: some View {
        VStack(spacing: 0) {
            Rectangle()
                .fill(accentColor)
                .frame(height: 6)
            
            VStack(alignment: .leading, spacing: 8) {
                Text(eyebrow)
                    .font(.system(size: 12, weight: .bold))
                    .foregroundColor(.gray)
                Text(title)
                    .font(.system(size: 20, weight: .bold))
                Text(desc)
                    .font(.system(size: 14))
                    .foregroundColor(.primary.opacity(0.8))
                    .lineSpacing(4)
            }
            .padding(24)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .background(Color.white)
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.05), radius: 5, y: 2)
        .padding(.horizontal, 24)
    }
}

struct FeaturesSection: View {
    var body: some View {
        VStack(spacing: 16) {
            SectionHeader(
                tag: "Funcionalidades",
                title: "O sistema que paga por si mesmo",
                desc: "Cada módulo foi desenhado para gerar eficiência real e mais receita para o seu consultório."
            )
            
            FeatureItem(icon: "📅", title: "Agenda Sem Faltas", desc: "Lembretes automáticos via WhatsApp reduzem no-shows em até 35%.", color: greenMain)
            FeatureItem(icon: "📋", title: "Prontuário na Nuvem", desc: "Histórico clínico completo acessível de qualquer dispositivo.", color: yellowMain)
            FeatureItem(icon: "💰", title: "Cobrança Automatizada", desc: "Gere cobranças via MBWay, referência Multibanco e cartão.", color: greenMain)
            FeatureItem(icon: "📦", title: "Estoque Inteligente", desc: "Alerta automático de reposição de materiais.", color: orangeMain)
        }
        .padding(.vertical, 32)
    }
}

struct FeatureItem: View {
    let icon: String
    let title: String
    let desc: String
    let color: Color
    
    var body: some View {
        HStack(alignment: .top, spacing: 16) {
            ZStack {
                RoundedRectangle(cornerRadius: 12)
                    .fill(color.opacity(0.2))
                    .frame(width: 48, height: 48)
                Text(icon)
                    .font(.system(size: 24))
            }
            
            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(.system(size: 18, weight: .bold))
                Text(desc)
                    .font(.system(size: 14))
                    .foregroundColor(.gray)
            }
            Spacer()
        }
        .padding(.horizontal, 24)
        .padding(.vertical, 8)
    }
}

struct BenefitsSection: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Benefícios")
                .font(.system(size: 14, weight: .bold))
                .foregroundColor(yellowMain)
            
            Text("ROI em semanas, não em anos")
                .font(.system(size: 28, weight: .bold))
                .foregroundColor(.white)
            
            Text("Cada euro investido no DentiBot paga-se em menos de 30 dias.")
                .font(.system(size: 16))
                .foregroundColor(.white.opacity(0.7))
                .padding(.bottom, 16)
            
            BenefitRow(icon: "📈", title: "+€ 2.000/mês", desc: "Faturam em média € 2.000 a mais por mês.")
            BenefitRow(icon: "⏱️", title: "8 Horas Economizadas", desc: "Eliminam chamadas manuais.")
            BenefitRow(icon: "🚀", title: "Implementação em 1 Dia", desc: "O seu consultório no ar em menos de 24h.")
        }
        .padding(24)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(darkBg)
    }
}

struct BenefitRow: View {
    let icon: String
    let title: String
    let desc: String
    
    var body: some View {
        HStack(alignment: .top, spacing: 16) {
            Text(icon)
                .font(.system(size: 24))
            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(.system(size: 18, weight: .bold))
                    .foregroundColor(.white)
                Text(desc)
                    .font(.system(size: 14))
                    .foregroundColor(.white.opacity(0.7))
            }
        }
        .padding(.vertical, 8)
    }
}

struct PricingSection: View {
    var body: some View {
        VStack(spacing: 16) {
            SectionHeader(
                tag: "Planos",
                title: "Preço justo para o tamanho do seu negócio",
                desc: "Sem fidelização. Cancele quando quiser. 30 dias grátis em qualquer plano."
            )
            
            PricingCard(title: "Solo", badge: "Dentista Autônomo", price: "97", isHighlight: false)
            PricingCard(title: "Clínica", badge: "Mais Popular", price: "197", isHighlight: true)
        }
        .padding(.vertical, 32)
    }
}

struct PricingCard: View {
    let title: String
    let badge: String
    let price: String
    let isHighlight: Bool
    
    var body: some View {
        let bgColor = isHighlight ? darkBg : .white
        let textColor = isHighlight ? Color.white : darkBg
        let btnColor = isHighlight ? yellowMain : greenMain
        
        VStack(alignment: .leading, spacing: 16) {
            Text(badge)
                .font(.system(size: 12, weight: .bold))
                .foregroundColor(isHighlight ? darkBg : greenMain)
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(isHighlight ? yellowMain : greenMain.opacity(0.2))
                .cornerRadius(16)
            
            Text(title)
                .font(.system(size: 24, weight: .bold))
                .foregroundColor(textColor)
                .padding(.top, 8)
            
            HStack(alignment: .bottom, spacing: 4) {
                Text("€")
                    .font(.system(size: 16))
                    .foregroundColor(textColor)
                    .padding(.bottom, 6)
                Text(price)
                    .font(.system(size: 48, weight: .black))
                    .foregroundColor(textColor)
            }
            
            Text("por mês")
                .font(.system(size: 14))
                .foregroundColor(isHighlight ? .white.opacity(0.7) : .gray)
            
            Divider()
                .background(isHighlight ? Color.white.opacity(0.2) : grayBg)
                .padding(.vertical, 8)
            
            VStack(alignment: .leading, spacing: 12) {
                PricingFeature(text: "Agendamento ilimitado", textColor: textColor)
                PricingFeature(text: "Prontuário digital", textColor: textColor)
                PricingFeature(text: "Lembretes WhatsApp", textColor: textColor)
            }
            
            Button(action: {
                // Iniciar
            }) {
                Text("Começar grátis →")
                    .font(.system(size: 16, weight: .bold))
                    .foregroundColor(isHighlight ? darkBg : .white)
                    .frame(maxWidth: .infinity)
                    .frame(height: 50)
                    .background(btnColor)
                    .cornerRadius(8)
            }
            .padding(.top, 16)
        }
        .padding(24)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(bgColor)
        .cornerRadius(16)
        .shadow(color: .black.opacity(0.1), radius: 10, y: 5)
        .padding(.horizontal, 24)
    }
}

struct PricingFeature: View {
    let text: String
    let textColor: Color
    
    var body: some View {
        HStack(spacing: 8) {
            Image(systemName: "checkmark")
                .foregroundColor(greenMain)
                .font(.system(size: 14, weight: .bold))
            Text(text)
                .font(.system(size: 14))
                .foregroundColor(textColor)
        }
    }
}

struct TestimonialsSection: View {
    var body: some View {
        VStack(spacing: 16) {
            SectionHeader(
                tag: "Depoimentos",
                title: "Consultórios que já crescem com o DentiBot",
                desc: "Resultados reais de profissionais que fazem medicina dentária."
            )
            
            TestimonialCard(stars: "★★★★★", text: "\"Economizei quase 2 horas por dia só com o agendamento automático.\"", name: "Dr. João Silva", city: "Lisboa")
            TestimonialCard(stars: "★★★★★", text: "\"Tenho 5 dentistas na clínica. Antes era um caos de folhas de cálculo e WhatsApp. Tudo centralizado agora.\"", name: "Dra. Ana Mello", city: "Porto")
        }
        .padding(.vertical, 32)
        .background(grayBg)
    }
}

struct TestimonialCard: View {
    let stars: String
    let text: String
    let name: String
    let city: String
    
    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text(stars)
                .foregroundColor(yellowMain)
                .font(.system(size: 18))
                .tracking(2)
            
            Text(text)
                .font(.system(size: 16))
                .italic()
            
            HStack(spacing: 12) {
                ZStack {
                    Circle()
                        .fill(greenMain)
                        .frame(width: 40, height: 40)
                    Text(String(name.prefix(2)).uppercased())
                        .foregroundColor(.white)
                        .fontWeight(.bold)
                }
                
                VStack(alignment: .leading, spacing: 2) {
                    Text(name)
                        .fontWeight(.bold)
                    Text("Consultório · \(city)")
                        .font(.system(size: 12))
                        .foregroundColor(.gray)
                }
            }
        }
        .padding(24)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.white)
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.05), radius: 5, y: 2)
        .padding(.horizontal, 24)
    }
}

struct FooterSection: View {
    var body: some View {
        VStack(spacing: 16) {
            HStack {
                ZStack {
                    RoundedRectangle(cornerRadius: 6)
                        .fill(darkBg)
                        .frame(width: 24, height: 24)
                    Text("D")
                        .font(.system(size: 12, weight: .bold))
                        .foregroundColor(.white)
                }
                Text("DentiBot")
                    .fontWeight(.bold)
            }
            
            Text("© 2026 DentiBot. Todos os direitos reservados.")
                .font(.system(size: 12))
                .foregroundColor(.gray)
            
            Text("v1.2.0 · Compatível com RGPD")
                .font(.system(size: 12))
                .foregroundColor(.gray.opacity(0.7))
            
            Spacer().frame(height: 80) // Espaço para o Floating Action Button não cobrir o rodapé
        }
        .padding(24)
    }
}

// Extensão útil para utilizar cores Hex no SwiftUI
extension Color {
    init(hex: UInt, alpha: Double = 1) {
        self.init(
            .sRGB,
            red: Double((hex >> 16) & 0xff) / 255,
            green: Double((hex >> 08) & 0xff) / 255,
            blue: Double((hex >> 00) & 0xff) / 255,
            opacity: alpha
        )
    }
}

#Preview {
    ContentView()
}