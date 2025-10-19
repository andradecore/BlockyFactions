# BlockyFactions
BlockyFactions é um plugin de facções projetado para o servidor BlockyCRAFT. Ele permite que os jogadores se organizem em grupos, gerenciem membros e cargos, e compitam em um ranking de riqueza. O plugin é profundamente integrado com o sistema de proteção de terras do BlockyClaim.

## Permissões
- `blockyfactions.*` — Garante acesso a todos os comandos do plugin (padrão para OP).
- `blockyfactions.fac` — Permite o uso do comando `/fac` e seus subcomandos (padrão para todos os jogadores).

## Funcionamento do Sistema
- Os jogadores podem criar, entrar e gerenciar suas próprias facções.
- Sistema de hierarquia com cargos de Líder, Oficial, Tesoureiro e Membro, cada um com diferentes níveis de permissão.
- A tesouraria da facção é gerenciada por um jogador designado como "Tesoureiro", para quem os recursos podem ser enviados.
- Um ranking de facções é gerado com base no patrimônio total (`net_worth`), um valor que é atualizado por sistemas externos.
- Todas as informações da facção, incluindo membros e configurações, são salvas em arquivos `.yml` individuais no servidor.

## Integração com BlockyClaim
O BlockyFactions ativa um sistema de **"Trust Automático"** no [BlockyClaim](https://github.com/andradecore/BlockyClaim). Jogadores que pertencem à mesma facção automaticamente recebem permissão de construção e interação nos terrenos uns dos outros, como se tivessem usado `/trust`. Esta permissão é gerenciada pela facção e não pode ser removida individualmente com `/untrust`.

## Tutorial de Comandos

### Comandos Gerais
- `/fac ajuda [pagina]`
  Mostra a lista de comandos disponíveis, dividida em páginas.

- `/fac criar <tag> <nome>`
  Cria uma nova facção, definindo você como o Líder.

- `/fac sair`
  Permite que você saia da sua facção atual. Se você for o líder, a facção será dissolvida.

- `/fac convidar <jogador>`
  Envia um convite para um jogador online se juntar à sua facção (requer cargo de Líder ou Oficial).

- `/fac entrar <nome-da-faccao>`
  Aceita um convite pendente para entrar em uma facção.

### Comandos Informativos
- `/fac list [nome-da-faccao]`
  Se usado sem argumentos, mostra informações detalhadas sobre a sua facção. Se um nome for especificado, mostra informações públicas de outra facção.

- `/fac rank`
  Exibe o top 10 das facções mais ricas do servidor, com base em seu patrimônio (`net_worth`).

### Comandos de Gerenciamento (Líder e Oficial)
- `/fac expulsar <jogador>`
  Remove um membro da sua facção. Oficiais não podem expulsar outros oficiais.

### Comandos de Gerenciamento (Apenas Líder)
- `/fac promover <jogador> <oficial|membro>`
  Promove um membro a Oficial ou rebaixa um Oficial a Membro.

- `/fac lider <jogador>`
  Transfere a liderança da facção para outro membro. O antigo líder se torna um membro comum.

- `/fac tesoureiro <jogador|nenhum>`
  Define um membro como o tesoureiro ("Tesoureiro") da facção. Um jogador não pode ser Tesoureiro e Oficial ao mesmo tempo. Use `nenhum` para deixar o cargo vago.

- `/fac tag <nova-tag>`
  Altera a tag da sua facção (máx. 5 caracteres, sem espaços ou símbolos).

- `/fac pvp <on|off>`
  Ativa ou desativa o combate entre membros da mesma facção.

## Reportar bugs ou requisitar features
Reporte bugs ou sugira novas funcionalidades na seção [Issues](https://github.com/andradecore/BlockyFactions/issues) do projeto.

## Contato:
- Discord: https://discord.gg/tthPMHrP