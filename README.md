# 🎯 Sprintt

<div align="center">

<img src="docs/assets/icon_round.png" alt="Sprintt App Icon" width="110" height="110" style="border-radius: 26px; box-shadow: 0 10px 25px rgba(2, 132, 199, 0.45);" />

<br/><br/>

![Android](https://img.shields.io/badge/Android-14%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9%2B-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![TLS / HTTPS](https://img.shields.io/badge/Security-TLS%20HTTPS-0284c7?style=for-the-badge&logo=letsencrypt&logoColor=white)
![Zeroconf](https://img.shields.io/badge/Discovery-mDNS%20Zeroconf-34d399?style=for-the-badge)
![GitHub Pages](https://img.shields.io/badge/Docs-GitHub%20Pages-222222?style=for-the-badge&logo=github)

<br/>

**Servidor ágil embarcado em Android para gestão autônoma de projetos locais.**  
*Quadro Kanban dinâmico, planejamento de sprints, telemetria em tempo real, relatórios analíticos e acesso seguro via mDNS na rede local.*

[Documentação Online](https://matheuscruzsouza.github.io/sprintt/) • [Histórico de Alterações](docs/changelog.html) • [Baixar Certificado SSL](docs/rootCA.pem)

</div>

---

## 📌 Visão Geral

O **Sprintt** transforma qualquer dispositivo Android em um servidor ágil e autônomo (estilo Jira/Linear) acessível por qualquer computador ou celular na mesma rede Wi-Fi através do endereço seguro:

```
https://sprintt.local:8443
```

Sem necessidade de conexão com a nuvem, banco de dados externo ou containers Docker. Todo o ecossistema roda localmente dentro do Android através do **NanoHTTPD** e **SQLite (Room)**.

---

## ✨ Principais Recursos

- **📱 Aplicativo Android Nativo**:
  - **Dashboard**: Visão executiva com status do servidor HTTP e atalhos rápidos.
  - **Gestão de Projetos & Equipe**: Criação e acompanhamento de squads, usuários e projetos segregados.
  - **Painel Analítico**: Taxa de conclusão de sprints, burndown de story points, distribuição por tipo (Story, Bug, Task, Subtask) e carga de trabalho do time.
- **🌐 Interface Web Ágil (HTMX + JMustache)**:
  - **Quadro Kanban Configurável por Projeto**: Adição, reordenação e exclusão de colunas com limites de WIP (Work in Progress), categorias de fluxo (`TO_DO`, `IN_PROGRESS`, `DONE`) e drag-and-drop dinâmico.
  - **Modal de Detalhes Expandido (2 Colunas)**: Layout horizontal estilo Jira/Linear com coluna principal para descrição detalhada, sub-tarefas com progresso e branch Git, e sidebar para metadados e propriedades.
  - **Product Backlog & Planejamento**: Criação rápida, priorização e organização de tickets em sprints futuras ou no backlog do produto.
  - **Telemetria de Bateria em Tempo Real (SSE)**: Conexão Server-Sent Events (`/api/v1/server/battery-sse`) mantida aberta com keep-alive que notifica imediatamente no navegador qualquer mudança de carga ou plug do celular servidor.
  - **Layout Otimizado & Barra Lateral Retrátil**: Interface responsiva e fluida para laptops compactos (1024×493 px) com barra lateral recolhível.
- **🔒 Segurança & Descoberta Zeroconf**:
  - Anúncio automático de rede via **JmDNS** (`sprintt.local`).
  - Suporte completo a **HTTPS / TLS** com suporte a múltiplos domínios (SANs) e autoridade certificadora própria (CA).
  - Autenticação e primeiro acesso seguro via código alfanumérico de 6 dígitos.
- **📡 API REST Completa & OpenAPI Swagger**:
  - Endpoints padronizados para gerenciamento de projetos, colunas do quadro, issues, sub-tarefas e sprints.
  - Documentação interativa em `/swagger-ui`.

---

## 🏗️ Arquitetura do Sistema

```text
┌────────────────────────────────────────────────────────────────────────┐
│                        DISPOSITIVO ANDROID                             │
│                                                                        │
│  ┌───────────────────────┐         ┌────────────────────────────────┐  │
│  │   UI Nativa (Kotlin)  │         │     Sprintt Background Svc     │  │
│  │  - Dashboard          │         │  - NanoSpring 2.1.0 (8443 TLS) │  │
│  │  - Projetos & Sprints │◄───────►│  - JmDNS ("sprintt.local")     │  │
│  │  - Equipe             │ SQLite/ │  - SseEmitter (Battery Stream) │  │
│  │  - Analíticos         │ JSON    │  - OpenAPI Swagger UI          │  │
│  └───────────────────────┘         └───────────────┬────────────────┘  │
└────────────────────────────────────────────────────┼───────────────────┘
                                                     │ HTTPS / SSE
                                                     ▼
                                      ┌───────────────────────────────┐
                                      │     Clientes Web da Rede      │
                                      │  - https://sprintt.local:8443 │
                                      │  - HTMX / JMustache UI        │
                                      │  - SSE Battery Events         │
                                      │  - Kanban Dinâmico (WIP)      │
                                      └───────────────────────────────┘
```

---

## 🚀 Como Executar o Projeto

### Pré-requisitos
- **Android Studio** Hedgehog / Iguana / Jellyfish ou superior.
- **JDK 17** instalado e configurado.
- Dispositivo Android com depuração USB ativada ou emulador Android.

### Compilação e Instalação

```bash
# Clonar o repositório
git clone https://github.com/matheuscruzsouza/sprintt.git
cd sprintt

# Compilar o APK de depuração
./gradlew assembleDebug

# Instalar no dispositivo conectado via ADB
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Iniciar o aplicativo
adb shell am start -n io.github.matheuscruzsouza.sprintt.debug/io.github.matheuscruzsouza.sprintt.ui.MainActivity
```

---

## 🔒 Configuração do Certificado SSL (Eliminar "Não Seguro")

Para navegar com cadeado verde em `https://sprintt.local:8443`:

1. Baixe o certificado raiz: [`docs/rootCA.pem`](docs/rootCA.pem) (ou acesse `https://sprintt.local:8443/cert/rootCA.pem`).
2. **Google Chrome / Chromium**:
   - Acesse `chrome://settings/certificates` &rarr; aba **Autoridades** &rarr; **Importar** &rarr; selecione `rootCA.pem` &rarr; marque *"Confiar neste certificado para identificar sites"*.
3. **Mozilla Firefox**:
   - Acesse `about:preferences#privacy` &rarr; **Ver Certificados** &rarr; aba **Autoridades** &rarr; **Importar** &rarr; marque *"Confiar nesta autoridade"*.
4. **Windows / macOS**:
   - Dê dois cliques no arquivo e adicione ao repositório de *"Autoridades de Certificação Raiz Confiáveis"* (ou Chaveiro do Sistema no macOS).

---

## 📡 Referência Rápida da API REST
 
| Método | Endpoint | Descrição |
|---|---|---|
| `GET` | `/api/v1/server/battery-sse` | Stream em tempo real de eventos da bateria (Server-Sent Events) |
| `GET` | `/api/v1/server/battery` | Telemetria energética instantânea (JSON ou HTML badge) |
| `GET` | `/api/v1/projects` | Lista todos os projetos cadastrados |
| `POST` | `/api/v1/projects` | Cria um novo projeto segregado |
| `GET` | `/api/v1/projects/:key/columns` | Lista as colunas configuradas para o quadro do projeto |
| `POST` | `/api/v1/projects/:key/columns` | Adiciona uma coluna ao quadro com título, categoria e WIP |
| `PUT` | `/api/v1/projects/:key/columns/:id` | Atualiza o título e limite WIP da coluna |
| `DELETE` | `/api/v1/projects/:key/columns/:id` | Remove a coluna e migra tickets para outra coluna |
| `POST` | `/api/v1/projects/:key/columns-reorder` | Reordena as colunas do quadro por lista de IDs |
| `GET` | `/api/v1/issues?sprintId={id}` | Filtra tickets da sprint especificada |
| `POST` | `/api/v1/issues` | Criação de novos tickets e histórias |
| `PUT` | `/api/v1/issues/:key/status` | Move o ticket entre status ou colunas (`columnId`) |
| `GET` | `/swagger-ui` | Documentação OpenAPI interativa Swagger |
| `GET` | `/cert/rootCA.pem` | Download direto do certificado raiz da CA |

---

## 📖 Documentação & GitHub Pages

A documentação interativa completa está estruturada na pasta [`docs/`](docs/).

Para ativar o **GitHub Pages**:
1. No repositório do GitHub, acesse **Settings** &rarr; **Pages**.
2. Em **Build and deployment > Source**, escolha **Deploy from a branch**.
3. Selecione a branch `main` e a pasta `/docs`.
4. Salve as alterações. O site estará disponível em instantes!

---

## 📄 Licença

Este projeto é desenvolvido para fins educacionais e de produtividade ágil autônoma. Sinta-se à vontade para contribuir!
