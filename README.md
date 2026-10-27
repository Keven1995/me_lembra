# Me_lembra 🔔

**Sua agenda com lembretes inteligentes no celular.** PWA pessoal derivado da arquitetura do projeto [Agenda Instituto Dor](https://github.com/Keven1995/agenda-instituto-dor), com frontend React/TypeScript, API Spring Boot e PostgreSQL.

> Status: MVP de desenvolvimento. O build do frontend foi validado. A API ainda exige testes de integração, HTTPS, banco, Google OAuth e chaves Web Push configurados antes do uso real. Não foi implantado.

## O que faz

- Login com Google e compromissos privados por usuário.
- Calendário mensal, compromissos do dia e próximos eventos.
- Criar, editar, reagendar e cancelar compromissos.
- Categorias: saúde, finanças, trabalho, pessoal e outros; local e observações opcionais.
- Web Push com mensagens contextualizadas pelo evento, horário e categoria, até mesmo com o app fechado em dispositivos compatíveis.
- Enriquecimento do aviso de **1h antes** com previsão do tempo da Open-Meteo e tempo estimado de carro pela HERE Routing, incluindo atraso de trânsito quando disponível.
- Pesquisa e seleção de endereços para os destinos (HERE Geocoding), GPS do celular com consentimento e endereço de origem de reserva salvo.
- Horários calculados em `America/Sao_Paulo` por padrão:
  1. **Véspera às 18h**;
  2. **Dia do compromisso às 9h**, apenas se ainda faltar pelo menos 1h;
  3. **3h antes** do início;
  4. **1h antes** do início.
- Horários que já passaram ao cadastrar ou reagendar não geram alertas retroativos. Alertas cancelados ou desatualizados são removidos. Cada etapa possui seu próprio registro de envio, evitando o problema de um único flag para todas as mensagens.

Exemplo de aviso: **🩺 Está quase na hora!** — “Falta 1 hora para Consulta no dentista (14:00). Hora de se preparar! Local: Clínica.”

## Estrutura

```text
me_lembra/
├── agenda-web/  React 18 + TypeScript + Vite + Service Worker
├── agenda-api/  Java 17 + Spring Boot 3.4, OAuth, PostgreSQL e Web Push
└── docker-compose.yml
```

O frontend usa o contrato REST da aplicação de referência (`/api/appointments`), com novos campos `title`, `category`, `location`, `notes`, `destinationLatitude` e `destinationLongitude`. A migração Flyway `V3` cria `reminder_jobs`; a `V4` adiciona destinos geocodificados e preferências de localização por usuário. O agendador busca lembretes vencidos a cada 60s.

### Rodar localmente

Requisitos: Node 20+, Java 17+, Maven 3.9+, Docker e projeto OAuth configurado no Google Cloud.

1. `docker compose up -d db`
2. Copie `agenda-api/.env.example` para `agenda-api/.env` e configure as credenciais. **Exporte essas variáveis no terminal/processo** antes de iniciar a API (`.env` não é carregado automaticamente pelo Spring Boot). Em Bash, na pasta `agenda-api`: `set -a; source .env; set +a`. No PowerShell, configure-as no ambiente do processo.
3. Na pasta `agenda-api`, execute `mvn spring-boot:run` no mesmo terminal com as variáveis carregadas.
4. Na pasta `agenda-web`, execute `npm ci` e `npm run dev`.
5. Abra `http://localhost:5173`, entre com Google, crie um compromisso e ative as notificações.

### Previsão do tempo e trânsito no lembrete de 1h

1. Crie uma chave de API no [HERE Platform](https://platform.here.com/) com acesso a **Geocoding & Search** e **Routing API v8**. Confira o plano, cotas, condições e cobrança da conta HERE antes de usar; os limites gratuitos podem mudar. Configure `HERE_API_KEY` no `agenda-api/.env` ou no ambiente da hospedagem. **A chave só fica no backend**, nunca no frontend.
2. No aplicativo, em **Origem para o trânsito**, escolha **Usar minha localização atual** e aceite a permissão do navegador. Também recomendamos cadastrar um **Endereço de reserva**: pesquise e selecione um resultado da HERE.
3. Ao cadastrar ou editar um compromisso, preencha **Onde?**, clique em **Buscar** e **selecione um endereço**. Só assim o evento terá coordenadas suficientes para consultar previsão e rota; texto de local digitado sem selecionar funciona apenas como descrição.
4. A 1h do início, o servidor consulta a previsão no destino via [Open-Meteo Forecast API](https://open-meteo.com/en/docs) e a duração/atraso de trânsito de carro via [HERE Routing v8](https://www.here.com/docs/bundle/routing-api-v8-api-reference/page/index.html). A rota é calculada **na hora do aviso**, não continuamente. O texto também sugere horário para sair, com 10 minutos de margem operacional (configuração inicial).

**Prioridade de origem:** GPS enviado com consentimento há no máximo **30 minutos** → endereço de reserva salvo → sem rota se nenhuma origem estiver disponível. Ajuste `GPS_MAX_AGE_MINUTES` se necessário. O GPS **só é capturado com o PWA aberto**: ao tocar no botão, e durante o uso quando já há permissão concedida e GPS ativado no app. Navegadores não permitem consultar a localização atual de um celular com o PWA fechado via Service Worker. Mesmo se a localização do momento não estiver acessível, o endereço salvo mantém o cálculo de trânsito possível.

**Falhas parciais não bloqueiam os lembretes:** se a HERE estiver indisponível/sem chave, o aviso ainda sai com a previsão quando disponível; se o Open-Meteo estiver fora do ar, a rota pode entrar sozinha; se não houver destino validado ou ambos falharem, segue o lembrete padrão. Open-Meteo sem chave é adequado ao uso pessoal/não comercial, sujeito aos termos e limites vigentes. Previsão, trânsito e sugestão de saída são **estimativas**.

**Privacidade:** coordenadas do GPS e endereço de reserva são armazenados na conta do usuário para montar os lembretes. Ao desativar o GPS, a última coordenada é apagada do servidor; é possível excluir o endereço salvo separadamente. Os dados de origem/destino necessários à consulta são transmitidos aos fornecedores de clima/rota. Use HTTPS na produção para acessar a geolocalização.

O redirect URI do cliente OAuth de desenvolvimento é `http://localhost:8080/login/oauth2/code/google`. O projeto original sincroniza eventos com o Google Calendar e essa integração foi mantida neste MVP; portanto, para criar/alterar/cancelar eventos é necessário autorizar o escopo Calendar também. No futuro ela poderá ser tornada opcional.

Para hospedagem externa, aponte `VITE_API_URL` para a API do Me_lembra e configure o `FRONTEND_URL` no backend. O `vercel.json` inclui apenas fallback de SPA, sem nenhuma URL do Instituto Dor. Antes da produção, resolva o compartilhamento de sessão/cookies entre os domínios (preferencialmente frontend e API sob a mesma origem/site) e configure os redirecionamentos OAuth correspondentes.

### Web Push

Gere VAPID keys para o novo aplicativo (não reutilize as de outro app):

```bash
npx web-push generate-vapid-keys
```

Configure `VAPID_PUBLIC_KEY`, `VAPID_PRIVATE_KEY` e `VAPID_SUBJECT` no backend. No menu do app, toque em **Ativar avisos**. As inscrições são registradas por dispositivo, associadas ao usuário autenticado.

**Importante:** o backend precisa ficar rodando continuamente para disparar alertas no horário. Hospedagens que suspendem a instância ociosa podem atrasar ou impedir lembretes. O celular precisa estar online, com notificações permitidas; Web Push não oferece garantia absoluta do horário de exibição pelo sistema operacional. O projeto tenta reentregar falhas de envio com intervalo de 5 minutos, respeitando um limite de atraso de 20 minutos.

No iPhone, é necessário instalar pela opção **Adicionar à Tela de Início** e abrir o app instalado para solicitar a permissão de Web Push. Em produção, use HTTPS.

## Segurança e produção

- Não versione `.env`, chaves VAPID privadas ou credenciais OAuth.
- Não exponha `HERE_API_KEY` no bundle JavaScript, nos logs ou no repositório. Acompanhe o consumo da sua franquia HERE.
- Use PostgreSQL em um banco separado do Agenda Instituto Dor.
- Cookies de sessão seguros e CORS restrito ao frontend.
- Mutations exigem token CSRF emitido pela API e enviado no cabeçalho `X-XSRF-TOKEN`. **Antes de publicar:** revisar cookies/CORS para domínios finais, testar instalação e push reais em Android/iOS, adicionar testes de integração do scheduler com concorrência e tratamento de subscriptions inválidas (HTTP 404/410). Para múltiplas instâncias, validar estratégia de locking/fila.
- Não use PWA/Web Push como o único meio de aviso para atividades críticas, como emergências.

## Checks

```bash
cd agenda-web
npm run build
npm run lint

cd ../agenda-api
mvn test
```

O código foi desenvolvido como uma cópia independente: nada é alterado no repositório original.
