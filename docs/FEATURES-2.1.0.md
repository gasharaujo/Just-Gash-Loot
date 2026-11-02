# Just Gash Loot 2.1.0 — Void Filter e SCC

Validação em **1 de novembro de 2026**.

## Void Filter

O texto **Void** fica no canto superior direito da GUI, oposto ao nome da máquina.
Abre uma tela para cadastrar IDs de itens ou tags de itens, com sugestões fornecidas
pelo servidor. Exemplos: `minecraft:rotten_flesh`, `#minecraft:planks` e `#c:ingots/iron`.
Buscas parciais, como `diamond` e `#planks`, também são aceitas pelo autocomplete.

Clique numa sugestão ou use Tab para completar. Enter ou **Adicionar** salva a regra;
clique numa regra cadastrada para removê-la. Cada máquina aceita até 64 regras,
com rolagem e paginação. IDs inexistentes são recusados pelo servidor.

As regras ficam salvas no bloco e são aplicadas antes do depósito na rede ou no
inventário local. Cada ID seleciona todas as variantes daquele item; tags usam os
dados carregados do mundo. Itens que já estavam guardados permanecem disponíveis.
Mundos anteriores começam com uma lista vazia.

O servidor valida o menu aberto, a distância até a máquina e o modo do jogador.
Pacotes de outro menu ou de uma máquina fora do alcance não alteram configurações.

## Depósitos opcionais no SCC

Disponível nas versões NeoForge **1.21.1** e **26.1.2**. O mod mantém o funcionamento
normal sem SCC e não inclui classes ou arquivos do SCC nos JARs de distribuição.

Conecte um cabo ou nó de rede SCC a uma face do Loot Generator. A integração registra
o bloco como endpoint no SCC e usa `NetworkView.insert` para os depósitos. Assim,
prioridades, filtros, modos de fluxo, disponibilidade da rede e políticas de descarte
do próprio SCC continuam valendo. O Loot Generator não serve de ponte entre redes.

Novos resultados passam pelo Void Filter e seguem diretamente para o SCC. O que a
rede recusar fica no inventário local; se o armazenamento local também não comportar
o restante, vale o descarte de excedentes já existente no mod.

Estoques guardados são reenviados a cada dez ticks, em lotes limitados a uma pilha
normal por slot. O filtro Void não é aplicado retroativamente a esses estoques.
Tentativas recusadas pelo SCC preservam os itens e não retransmitem contadores
inalterados da GUI. A integração não força o carregamento de chunks vizinhos.

Versões verificadas: SCC **1.4.7** na 1.21.1; SCC **1.4.7 e 1.5.4** na 26.1.2.
A ausência de uma API compatível deixa o armazenamento local disponível e registra
o diagnóstico da integração opcional.

## Verificação

- **19.228 verificações do núcleo**, incluindo normalização e limites das regras.
- Servidores reais nas três versões, com e sem a dependência opcional aplicável.
- Redes SCC reais: prioridades, filtros, modos sem depósito, capacidade cheia,
  descarte de excedentes, preservação de dados dos itens e desconexão.
- Ordem de processamento: Void antes de SCC; recuperação de estoques antigos sem
  descarte retroativo; tentativas sem alteração não invalidam a revisão do inventário.
- Clientes reais: abertura pelo texto Void, autocomplete de item e tag, adição e
  remoção, rejeição de IDs inválidos, persistência nativa e retorno à GUI da máquina.
- Telas conferidas nas escalas 2 e 3, incluindo clientes com SCC instalado.

Relatórios: [Void Filter](../verification/results/void-filter-2.1.0.json),
[SCC](../verification/results/scc-2.1.0.json) e
[auditoria dos JARs](../verification/results/artifact-audit-2.1.0.json).

| Versão | Tela do filtro |
| --- | --- |
| Forge 1.20.1 | [Captura](screenshots/forge-1.20.1-void.png) |
| NeoForge 1.21.1 | [Captura](screenshots/neoforge-1.21.1-void.png) |
| NeoForge 26.1.2 | [Captura](screenshots/neoforge-26.1.2-void.png) |

As mudanças foram desenvolvidas nas branches `feature/void-filter` e
`feature/scc-deposit`, com validação antes dos merges na `main`.

Os JARs foram conferidos por SHA-256 e instalados nas instâncias de teste
`20.1`, `21` e `26.1.2`. O JAR da 26.1.2 fica em `artifacts/` para a
instalação manual no FTB Evolution 2.
