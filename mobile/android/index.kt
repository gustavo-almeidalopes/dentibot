package com.example.dentibot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Cores baseadas no CSS do site
val GreenMain = Color(0xFF10B981)
val YellowMain = Color(0xFFFBBF24)
val OrangeMain = Color(0xFFF97316)
val DarkBg = Color(0xFF111827)
val GrayBg = Color(0xFFF3F4F6)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                DentiBotApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DentiBotApp() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("D", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("DentiBot", fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    OutlinedButton(
                        onClick = { /* Ação de Login */ },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Login", color = DarkBg)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* Abrir WhatsApp */ },
                containerColor = GreenMain,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Email, contentDescription = "WhatsApp") // Usando ícone genérico como placeholder pro zap
            }
        },
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
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

@Composable
fun HeroSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            color = GrayBg,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Text(
                text = "🏢 Software B2B · Dentistas & Clínicas",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Text(
            text = buildAnnotatedString {
                append("Gerencie seu consultório como um negócio.\n")
                withStyle(style = SpanStyle(background = YellowMain.copy(alpha = 0.3f))) {
                    append("Porque ele é.")
                }
            },
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            lineHeight = 40.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "Para dentistas autônomos e clínicas de pequeno e médio porte. Agendamento, prontuário, cobrança e LGPD em uma plataforma só.",
            fontSize = 16.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Button(
            onClick = { /* Ação Testar */ },
            colors = ButtonDefaults.buttonColors(containerColor = GreenMain),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Testar 30 dias grátis →", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = { /* Ação Ver Funciona */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Ver como funciona", color = DarkBg, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Stats Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem("8h", "Economizadas\n/semana")
            StatItem("↓35%", "Faltas em\nmédia")
            StatItem("500+", "Consultórios\nativos")
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Mock Cards
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("📊", fontSize = 24.sp, modifier = Modifier.padding(end = 12.dp))
                Column {
                    Text("Faturamento / Mês", fontWeight = FontWeight.Bold)
                    Text("R$ 18.400 · +12% vs mês ant.", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 24.sp, fontWeight = FontWeight.Black, color = GreenMain)
        Text(label, fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)
    }
}

@Composable
fun SectionHeader(tag: String, title: String, desc: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Text(tag, color = GreenMain, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(title, fontSize = 28.sp, fontWeight = FontWeight.Bold, lineHeight = 34.sp, modifier = Modifier.padding(vertical = 8.dp))
        Text(desc, color = Color.Gray, fontSize = 16.sp)
    }
}

@Composable
fun ParaQuemSection() {
    Column(modifier = Modifier.background(GrayBg).padding(vertical = 32.dp)) {
        SectionHeader(
            "Para Quem",
            "Desenvolvido para quem trabalha com odontologia",
            "Duas realidades, uma plataforma. Escolha o perfil que mais se parece com o seu."
        )

        AudienceCard("Perfil 1", "Dentista Autônomo", "O DentiBot elimina o caos administrativo com agendamento automático...", GreenMain)
        AudienceCard("Perfil 2", "Clínica Pequena ou Média", "Controle de agenda por profissional, relatórios de desempenho...", YellowMain)
    }
}

@Composable
fun AudienceCard(eyebrow: String, title: String, desc: String, accentColor: Color) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(6.dp).background(accentColor))
            Column(modifier = Modifier.padding(24.dp)) {
                Text(eyebrow, fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                Text(desc, fontSize = 14.sp, color = Color.DarkGray, lineHeight = 20.sp)
            }
        }
    }
}

@Composable
fun FeaturesSection() {
    Column(modifier = Modifier.padding(vertical = 32.dp)) {
        SectionHeader(
            "Funcionalidades",
            "O sistema que paga por si mesmo",
            "Cada módulo foi desenhado para gerar eficiência real e mais receita para o seu consultório."
        )

        FeatureItem("📅", "Agenda Sem Faltas", "Lembretes automáticos via WhatsApp reduzem no-shows em até 35%.", GreenMain)
        FeatureItem("📋", "Prontuário na Nuvem", "Histórico clínico completo acessível de qualquer dispositivo.", YellowMain)
        FeatureItem("💰", "Cobrança Automatizada", "Gere cobranças via Pix, boleto e cartão.", GreenMain)
        FeatureItem("📦", "Estoque Inteligente", "Alerta automático de reposição de materiais.", OrangeMain)
    }
}

@Composable
fun FeatureItem(icon: String, title: String, desc: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(color.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 24.sp)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(desc, color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
fun BenefitsSection() {
    Column(modifier = Modifier.background(DarkBg).padding(vertical = 40.dp)) {
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Text("Benefícios", color = YellowMain, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("ROI em semanas, não em anos", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
            Text("Cada real investido no DentiBot se paga em menos de 30 dias.", color = Color.LightGray, fontSize = 16.sp)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            BenefitRow("📈", "+R$ 2.000/mês", "Faturam em média R$ 2.000 a mais por mês.")
            BenefitRow("⏱️", "8 Horas Economizadas", "Eliminam ligações manuais.")
            BenefitRow("🚀", "Implantação em 1 Dia", "Seu consultório no ar em menos de 24h.")
        }
    }
}

@Composable
fun BenefitRow(icon: String, title: String, desc: String) {
    Row(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(icon, fontSize = 24.sp, modifier = Modifier.padding(end = 16.dp))
        Column {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(desc, color = Color.LightGray, fontSize = 14.sp)
        }
    }
}

@Composable
fun PricingSection() {
    Column(modifier = Modifier.padding(vertical = 32.dp)) {
        SectionHeader(
            "Planos",
            "Preço justo para o tamanho do seu negócio",
            "Sem fidelidade. Cancele quando quiser. 30 dias grátis em qualquer plano."
        )

        PricingCard("Solo", "Dentista Autônomo", "97", false)
        PricingCard("Clínica", "Mais Popular", "197", true)
    }
}

@Composable
fun PricingCard(title: String, badge: String, price: String, isHighlight: Boolean) {
    val bgColor = if (isHighlight) DarkBg else Color.White
    val textColor = if (isHighlight) Color.White else DarkBg
    val btnColor = if (isHighlight) YellowMain else GreenMain

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = bgColor)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Surface(
                color = if(isHighlight) YellowMain else GreenMain.copy(alpha = 0.2f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(badge, color = if(isHighlight) DarkBg else GreenMain, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
            }
            Text(title, color = textColor, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text("R$", color = textColor, fontSize = 16.sp, modifier = Modifier.padding(bottom = 6.dp, end = 4.dp))
                Text(price, color = textColor, fontSize = 48.sp, fontWeight = FontWeight.Black)
            }
            Text("por mês", color = if(isHighlight) Color.LightGray else Color.Gray, fontSize = 14.sp)
            
            Spacer(modifier = Modifier.height(24.dp))
            Divider(color = if(isHighlight) Color.DarkGray else GrayBg)
            Spacer(modifier = Modifier.height(24.dp))
            
            PricingFeature("Agendamento ilimitado", textColor)
            PricingFeature("Prontuário digital", textColor)
            PricingFeature("Lembretes WhatsApp", textColor)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = { /* Iniciar */ },
                colors = ButtonDefaults.buttonColors(containerColor = btnColor),
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Começar grátis →", color = if(isHighlight) DarkBg else Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PricingFeature(text: String, textColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
        Icon(Icons.Default.Check, contentDescription = null, tint = GreenMain, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, color = textColor, fontSize = 14.sp)
    }
}

@Composable
fun TestimonialsSection() {
    Column(modifier = Modifier.background(GrayBg).padding(vertical = 32.dp)) {
        SectionHeader(
            "Depoimentos",
            "Consultórios que já crescem com o DentiBot",
            "Resultados reais de profissionais que fazem odontologia."
        )

        TestimonialCard("★★★★★", "\"Economizei quase 2 horas por dia só com o agendamento automático.\"", "Dr. João Silva", "São Paulo")
        TestimonialCard("★★★★★", "\"Tenho 5 dentistas na clínica. Antes era um caos de planilhas e WhatsApp. Tudo centralizado agora.\"", "Dra. Ana Mello", "Recife")
    }
}

@Composable
fun TestimonialCard(stars: String, text: String, name: String, city: String) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(stars, color = YellowMain, fontSize = 18.sp, letterSpacing = 2.sp)
            Text(text, fontSize = 16.sp, modifier = Modifier.padding(vertical = 16.dp), fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(GreenMain),
                    contentAlignment = Alignment.Center
                ) {
                    Text(name.take(2).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(name, fontWeight = FontWeight.Bold)
                    Text("Consultório · $city", color = Color.Gray, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun FooterSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(DarkBg),
                contentAlignment = Alignment.Center
            ) {
                Text("D", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("DentiBot", fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("© 2026 DentiBot. Todos os direitos reservados.", color = Color.Gray, fontSize = 12.sp)
        Text("v1.2.0 · LGPD Compliant", color = Color.LightGray, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(80.dp)) // Espaço para o Floating Action Button não cobrir
    }
}