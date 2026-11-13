# Plano — Rosto ID

## Objetivo e fluxo

Aplicação web full-stack para operar câmeras disponíveis ao navegador, configurando cada dispositivo como **ENTRADA** ou **SAÍDA**, detectar e reconhecer localmente rostos previamente cadastrados, atribuir IDs genéricos sequenciais, registrar ocorrências com data e hora e permitir vincular ou atualizar os dados pessoais de uma pessoa depois. A captura só começa após ação explícita do operador. A persistência biométrica e o cadastro só ocorrem para participantes que tenham sido informados e consentido; um rosto desconhecido permanece apenas na memória transitória do navegador até que o operador confirme esse consentimento e escolha cadastrá-lo. Não se guardam fotografias nem trechos de vídeo.

O frontend calcula e compara descritores faciais no navegador; os descritores e os dados pessoais são cifrados no cliente antes de serem enviados. O backend guarda somente os textos cifrados, IDs, confirmação/horário do consentimento e eventos de reconhecimento. Um segredo `ROSTOID_SETUP_KEY`, configurado fora do repositório no ambiente protegido do servidor e digitado apenas na configuração inicial, impede que um visitante reivindique o cofre. A senha de cofre, digitada em cada sessão e nunca gravada no navegador, desbloqueia os dados e fornece autenticação à API. A senha perdida não pode ser recuperada: o app avisa disso antes da configuração. Esta arquitetura reduz exposição, mas não substitui avaliação jurídica, governança de acesso, aviso aos titulares nem validação de conformidade com a LGPD antes de uso real.

## Arquitetura e implementação

- **Frontend:** React, TypeScript e Vite. Interface de painel interno em português, responsiva e acessível. `getUserMedia` solicita câmera apenas após clique; a área de vídeo indica detecção e estado do modelo. `@vladmandic/face-api` com pesos versionados no projeto executa detecção e extração de descritores no navegador. Comparação por distância euclidiana acontece localmente contra descritores descriptografados dos participantes consentidos. Não há upload de pixels ou imagem.
- **Desconhecidos:** mostram estado “não cadastrado”. A ação para cadastrar exige confirmação expressa, por checkbox não pré-marcado, de que a pessoa foi informada e consentiu; a confirmação fica vinculada a um descritor congelado, é invalidada quando o rosto muda ou sai do enquadramento e o cadastro refaz a detecção antes de salvar. Só depois é gerado o próximo `RID-000001`, cifrado e salvo o descritor e criado o primeiro evento. A captura desconhecida não gera um rastro persistente sem consentimento.
- **Backend:** Spring Boot 3 / Java 21, REST JSON, Spring Data JPA, validação de entrada e conexão com `DATABASE_URL` MySQL gerenciada; H2 em memória como fallback apenas quando não há DSN local. Durante Preview, Vite escuta na porta 3000 e encaminha `/api` ao Spring Boot na porta 8080. Em produção, o Dockerfile compila Vite e serve os assets estáticos pelo mesmo Spring Boot na porta `PORT` (padrão 3000). Não expor DSNs, chaves ou senha do cofre ao bundle.
- **Modelo de dados:** `people` guarda PK sequencial, ID público, descritor facial cifrado e IV, perfil cifrado e IV opcionais, confirmação e instante de consentimento e criação. `camera_configurations` guarda dispositivo selecionado pelo navegador, nome e direção configurada, com a direção autoritativa no servidor. `recognition_events` guarda FK para pessoa, câmera opcional, direção e nome da câmera como snapshot, além do `Instant` UTC criado pelo servidor. Evento inicial é criado na matrícula consentida usando a câmera ativa; ocorrências subsequentes são deduplicadas por pessoa+câmera+sentido durante 30 segundos, permitindo que câmeras configuradas em sentidos diferentes registrem ambas as passagens e evitando que a mesma câmera suprima uma troca Entrada/Saída. Eventos antigos permanecem intactos, sem inferir direção retroativamente. A interface converte o UTC para o fuso local. Atualizar o perfil não altera ou apaga eventos.
- **Cofre do cliente:** Web Crypto PBKDF2 + AES-GCM, salts aleatórios persistidos pelo backend, IV aleatório por objeto e chave somente em memória. A autenticação do API usa token derivado localmente da senha, sem enviar a senha em texto claro. Configuração inicial protegida por segredo de provisionamento de 24 ou mais caracteres e senha de 12 ou mais caracteres; sem reset remoto. Perfil mínimo: nome completo e campos opcionais de e-mail e telefone. O backend não consegue pesquisar ou ler perfis/descritores.
- **Remoção:** ação explícita e confirmada para remover um cadastro e seus eventos associados, com aviso claro de que o histórico será apagado junto. A tela mantém acesso apenas por senha de cofre; a proteção é adequada para protótipo interno, não uma substituição de SSO, autorização por função, revisão de segurança ou avaliação de impacto para produção.

## Incremento solicitado — configuração de câmera e identificação no histórico

- Após permissão explícita de câmera, o operador lista os dispositivos disponíveis e configura até dois: se houver dois, um é Entrada e o outro Saída e ambos podem executar reconhecimento simultaneamente; não se permite dois sentidos iguais nessa configuração. Se houver apenas um, pode alternar entre Entrada e Saída, parando a captura antes de salvar a mudança e reiniciar.
- Não iniciar reconhecimento de câmera sem configuração salva; o cliente envia apenas os IDs das câmeras configuradas e o backend decide o sentido registrado. O evento guarda direção e nome da câmera como snapshots, para que alterações ou remoções de configuração não reclassifiquem ocorrências antigas. Registros anteriores sem configuração continuam com direção desconhecida; não recebem um sentido presumido.
- O histórico mostra o nome quando o perfil está cadastrado, descriptografando-o apenas no frontend; sem perfil mostra o ID genérico. Mostra também o sentido (Entrada/Saída ou Não informado), a câmera quando disponível e a data/hora local. Dados pessoais em claro não são adicionados à API nem persistidos em texto claro.
- A deduplicação de 30 segundos é escopada por pessoa, câmera e sentido: leituras repetidas na mesma câmera/sentido são deduplicadas, mas uma passagem por Entrada e outra por Saída são eventos distintos, inclusive quando ocorre a troca de sentido de uma câmera única.

## Estrutura do projeto

```text
rostoid/
├── frontend/                 # Aplicação React/Vite, câmera, cofre e interface
│   ├── public/models/        # Manifestos, pesos e licença do reconhecedor local
│   └── src/
│       ├── api/              # Cliente HTTP e tipos das respostas
│       ├── crypto/           # KDF e cifragem/decifragem Web Crypto
│       ├── components/
│       │   ├── SecurityGate.tsx
│       │   ├── Shell.tsx
│       │   └── pages/        # CameraPage, CameraScanner, HistoryPage e PeoplePage
│       ├── App.tsx           # Estado de sessão, eventos e perfis em claro só no cliente
│       └── styles.css        # Tokens e estilos responsivos
├── backend/                  # Serviço Spring Boot e configuração de dados
│   └── src/main/java/...     # REST, entidades, serviços, validação e segurança
├── Dockerfile                # Build Node + Maven e container de produção
├── package.json              # Scripts frontend, versão e package manager fixos
├── pnpm-lock.yaml            # Lockfile do frontend
├── pnpm-workspace.yaml       # Política revisada para scripts de dependências
├── frontend/public/manus-routes.json # Manifesto das rotas de páginas
├── plan.md                   # Este plano
└── TODO.md                   # Resultados e critérios funcionais
```

## Direção visual

Alternativas de cauda, registradas antes da escolha: **console suíço de instrumentação, azul-petróleo e verde-lima (0,07)**; **arquivo editorial brutalista, papel e laranja queimado (0,04)**; **minimalismo clínico luminoso, azul profundo e ciano (0,03)**. Escolha: console suíço de instrumentação.

- **Movimento:** suíço-internacional aplicado a instrumentação de segurança, com tipografia e alinhamento rigorosos, sem aparência de vigilância agressiva.
- **Princípios:** consentimento acima de automação; status legível de relance; densidade controlada; dados pessoais visualmente discretos.
- **Filosofia de cor:** carvão azulado para o plano operacional, branco quente para áreas de leitura e verde-lima/menta como sinal de operação consentida; âmbar indica pendência e vermelho fica reservado a ações destrutivas.
- **Layout:** console assimétrico com navegação lateral compacta; a área de câmera usa um feed ou duas prévias simultâneas conforme a configuração, enquanto o histórico permanece tabular e evita cartões repetidos.
- **Assinaturas:** moldura de foco em quatro cantos ao redor da área de captura; chips monoespaçados para IDs e horários; linha vertical colorida identificando cada etapa do fluxo.
- **Interação:** a câmera começa apenas por ação explícita; os estados têm texto e ícone além da cor; confirmação de consentimento nunca vem pré-selecionada; exclusão requer confirmação explícita.
- **Animação:** transições de 150–220 ms para mudança de estado, pulso discreto apenas ao detectar foco; sem varredura incessante, flash ou animação enquanto a câmera estiver parada; respeitar `prefers-reduced-motion`.
- **Tipografia:** `Inter` para conteúdo e `IBM Plex Mono` para IDs, timestamps e métricas; hierarquia curta com título de página, legenda operacional e rótulos auxiliares.
- **Essência e voz:** registro biométrico consentido para ambientes controlados; personalidade precisa, discreta e responsável. Tom direto: “Câmera pausada até você iniciar.” / “Rosto não cadastrado. Só salve após confirmar o consentimento.”
- **Marca:** wordmark “ROSTO / ID” em duas linhas, com símbolo geométrico de quatro cantos de foco aberto e centro vazio, evitando representar olhos ou um rosto real. Cor proprietária: verde sinal `#B7F36B`.

## Dependências e restrições relevantes

O projeto Webdev já declarou servidor e banco gerenciado; o banco MySQL do Preview e o de produção são compartilhados, então migrations devem ser aditivas e sem apagar dados. O ambiente Preview usa a porta principal 3000. O build Java é não padrão e depende do toolchain Maven/JDK no container descrito no Dockerfile; Preview e publicação serão tratados separadamente. Pesos de modelo são binários necessários à função, versionados no frontend e servidos localmente para não enviar vídeo/rosto a um terceiro. Nada deve ser publicado sem autorização específica.
