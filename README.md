# BlockyGroups
BlockyGroups é um plugin de grupos projetado para o servidor BlockyCRAFT. Ele permite que os jogadores se organizem em grupos, gerenciem membros e cargos, e compitam em um ranking de riqueza. O plugin é profundamente integrado com o sistema de proteção de terras do BlockyClaim.

## Permissões
- `blockygroups.*` — Garante acesso a todos os comandos do plugin (padrão para OP).
- `blockygroups.grp` — Permite o uso do comando `/grp` e seus subcomandos (padrão para todos os jogadores).

## Funcionamento do Sistema
- Os jogadores podem criar, entrar e gerenciar suas próprias grupos.
- Sistema de hierarquia com cargos de Líder, Oficial, Tesoureiro e Membro, cada um com diferentes níveis de permissão.
- A tesouraria do grupo é gerenciada por um jogador designado como "Tesoureiro", para quem os recursos podem ser enviados.
- Um ranking de grupos é gerado com base no patrimônio total (`net_worth`), um valor que é atualizado por sistemas externos.
- Todas as informações do grupo, incluindo membros e configurações, são salvas em arquivos `.yml` individuais no servidor.

## Integração com BlockyClaim
O BlockyGroups ativa um sistema de **"Trust Automático"** no [BlockyClaim](https://github.com/andradecore/BlockyClaim). Jogadores que pertencem à mesma grupo automaticamente recebem permissão de construção e interação nos terrenos uns dos outros, como se tivessem usado `/trust`. Esta permissão é gerenciada pelo grupo e não pode ser removida individualmente com `/untrust`.

## Tutorial de Comandos

### Comandos Gerais
- `/grp ajuda [pagina]`
  Mostra a lista de comandos disponíveis, dividida em páginas.

- `/grp criar <tag> <nome>`
  Cria uma nova grupo, definindo você como o Líder.

- `/grp sair`
  Permite que você saia da sua grupo atual. Se você for o líder, a grupo será dissolvida.

- `/grp convidar <jogador>`
  Envia um convite para um jogador online se juntar à sua grupo (requer cargo de Líder ou Oficial).

- `/grp entrar <nome-da-grupo>`
  Aceita um convite pendente para entrar em uma grupo.

### Comandos Informativos
- `/grp list [nome-da-grupo]`
  Se usado sem argumentos, mostra informações detalhadas sobre a sua grupo. Se um nome for especificado, mostra informações públicas de outra grupo.

- `/grp rank`
  Exibe o top 10 dos grupo mais ricas do servidor, com base em seu patrimônio (`net_worth`).

### Comandos de Gerenciamento (Líder e Oficial)
- `/grp expulsar <jogador>`
  Remove um membro da sua grupo. Oficiais não podem expulsar outros oficiais.

### Comandos de Gerenciamento (Apenas Líder)
- `/grp promover <jogador> <oficial|membro>`
  Promove um membro a Oficial ou rebaixa um Oficial a Membro.

- `/grp lider <jogador>`
  Transfere a liderança do grupo para outro membro. O antigo líder se torna um membro comum.

- `/grp tesoureiro <jogador|nenhum>`
  Define um membro como o tesoureiro ("Tesoureiro") do grupo. Um jogador não pode ser Tesoureiro e Oficial ao mesmo tempo. Use `nenhum` para deixar o cargo vago.

- `/grp tag <nova-tag>`
  Altera a tag da sua grupo (máx. 5 caracteres, sem espaços ou símbolos).

- `/grp pvp <on|off>`
  Ativa ou desativa o combate entre membros da mesma grupo.

## Reportar bugs ou requisitar features
Reporte bugs ou sugira novas funcionalidades na seção [Issues](https://github.com/andradecore/BlockyGroups/issues) do projeto.

## Contato:
- Discord: https://discord.gg/tthPMHrP