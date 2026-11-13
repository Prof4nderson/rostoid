# TODO — Rosto ID

## [x] Câmera, detecção e cadastro consentido
- O frontend React Vite captura vídeo contínuo da câmera apenas depois de ação explícita do operador e mostra visualmente quando um rosto está detectado.
- O reconhecimento facial identifica se o rosto já possui registro, comparando localmente somente rostos previamente cadastrados com consentimento.
- Para rosto novo, o operador pode confirmar que a pessoa foi informada e consentiu e então cadastrar; só após isso o sistema atribui um ID genérico sequencial. A confirmação fica vinculada ao descritor consentido, é invalidada quando o rosto muda ou sai do enquadramento, e uma nova detecção é feita antes de salvar. Nenhum frame, imagem ou descritor em claro é enviado ao servidor.

## [x] IDs e histórico de reconhecimentos
- Cada ocorrência é registrada com o ID correspondente, dia e horário; o horário UTC é criado pelo backend e exibido no fuso local.
- O primeiro evento fica associado à matrícula consentida; ocorrências repetidas evitam uma linha por frame, e o cliente só avança seu cooldown após resposta bem-sucedida do servidor.

## [x] Cadastro pessoal posterior e manutenção
- Permitir cadastrar posteriormente os dados pessoais da pessoa reconhecida e vincular esses dados ao rosto anteriormente registrado por ID genérico, a qualquer tempo.
- Permitir visualizar e atualizar os dados pessoais vinculados a cada rosto, preservando o histórico de reconhecimentos já existente.
- Cifrar o perfil e o descritor no frontend antes de persistir; o perfil inicial pede nome completo e deixa e-mail e telefone opcionais.

## [x] Backend, frontend e persistência
- Entregar API backend em Spring Boot para processamento das operações, validação e comunicação com o frontend React Vite.
- Persistir IDs sequenciais, referências faciais cifradas, dados pessoais cifrados e ocorrências de reconhecimento com seus horários no MySQL gerenciado em Preview/produção; usar H2 em memória somente como fallback local sem `DATABASE_URL`.
- Disponibilizar cadastro, atualização, histórico e remoção individual com aviso claro de que apagar um cadastro também apaga seus eventos relacionados.

## [x] Primeiro acesso e privacidade biométrica
- Proteger o primeiro setup com o segredo de ambiente `ROSTOID_SETUP_KEY`, fornecido por entrada protegida e exigido antes que o cofre possa ser criado; sem esse segredo, recusar a inicialização. Não salvar segredo de provisionamento, senha nem token em arquivos ou armazenamento persistente do navegador.
- Exigir uma senha de cofre com pelo menos 12 caracteres e confirmação no primeiro uso; exigir novamente a senha para desbloquear sessões futuras e avisar que não há recuperação.
- Exigir confirmação afirmativa, não pré-selecionada, de que a pessoa foi informada e consentiu antes de persistir sua biometria; registrar quando essa confirmação ocorreu.
- Não armazenar fotos nem vídeos; não enviar pixels ou descritores em claro ao backend; exibir aviso de que o protótipo exige avaliação de privacidade, segurança e conformidade com a LGPD antes de uso real.

## [x] Configuração, alternância e uso simultâneo das câmeras
- Depois de ação explícita e concessão de permissão, permitir listar os dispositivos de câmera disponíveis e configurar uma ou duas câmeras com sentido de passagem **Entrada** ou **Saída**.
- Quando houver duas câmeras configuradas, exigir sentidos distintos — uma Entrada e outra Saída — e permitir iniciar e usar ambas simultaneamente.
- Quando houver apenas uma câmera, permitir parar sua captura, alterar e salvar seu sentido entre Entrada e Saída e reiniciar; cada evento novo deve usar o sentido vigente salvo no backend.
- Persistir as configurações no backend; não iniciar leitura para registro de passagem em câmera sem configuração salva. O backend determina o sentido usando a configuração persistida, não um valor de sentido enviado pelo navegador.
- Persistir cada evento com o ID/nome da câmera e o sentido Entrada/Saída como snapshot, para que alteração ou remoção da configuração não reclassifique ocorrências anteriores.
- Aplicar cooldown de 30 segundos por pessoa+câmera+sentido: evitar repetição contínua na mesma câmera e sentido, mas permitir a passagem oposta em outra câmera ou após alternar o sentido da câmera única.
- Não permitir salvar a troca de sentido enquanto qualquer feed ou requisição de cadastro/evento estiver em andamento; isso mantém o primeiro evento associado à direção exibida na câmera no momento da matrícula.

## [x] Histórico com nome, sentido e câmera
- Para cada evento, exibir o nome se houver perfil cadastrado, descriptografando o perfil localmente; caso contrário, exibir o ID genérico. O histórico lista os eventos correspondentes com data e horário; nenhum nome pessoal em claro é enviado ao backend.
- Exibir o sentido Entrada/Saída gravado, a câmera quando disponível e a data/hora no fuso local; eventos anteriores sem esses metadados devem exibir **Não informado**.
- Preservar as ocorrências existentes ao cadastrar/atualizar o perfil ou reconfigurar/remover uma câmera; a configuração nova não altera retrospectivamente o sentido já gravado.

## [x] UX de monitoramento sem rolagem desnecessária
- Após a configuração inicial, a tela **Câmera** mantém o foco nos feeds, imagens e controles individuais; a configuração deixa de ocupar o topo permanentemente.
- Disponibilizar **Configurações** no menu lateral para alterar posteriormente dispositivos, rótulos e sentidos sem retirar a visualização dos feeds da tela principal.
