# Just Gash Loot 2.1.1 — estado do Transfer Calculator

Na 2.1.0, o depósito automático reconhecia um Transfer Calculator adjacente
como qualquer outro nó SCC. Isso podia mandar loot ao storage mesmo quando
ele estava configurado apenas para enviar recursos à máquina.

A 2.1.1 consulta o estado do Transfer Calculator a cada tentativa de depósito:

- Ele deve apontar para o Loot Generator.
- No modo automático dos lados, `Import` e `Both` permitem receber os itens;
  `Export` impede esse caminho de depósito.
- Configurações manuais dos lados dos itens prevalecem: deve haver pelo menos
  um lado configurado para receber da máquina.
- A regra cobre o bloco e o painel instalado no cabo, inclusive itens recém
  gerados e o reenvio do estoque existente.
- Mudar o modo passa a valer sem reiniciar, sem botão e sem nova configuração
  no Loot Generator. O fornecimento de FE e lava e as capacidades de automação
  continuam disponíveis.

Uma conexão direta por outro cabo ou nó SCC continua sendo um caminho
independente para depósito. Os filtros, prioridades e regras de descarte dos
storages continuam sob controle do SCC.

Não há mudança no formato de salvamento. Os estoques e as configurações de
mundos anteriores são preservados. O mod continua funcionando sem SCC; a
integração é opcional nas versões NeoForge 1.21.1 e 26.1.2.

Relatórios: [servidores e SCC](../verification/results/scc-transfer-2.1.1.json)
e [auditoria dos JARs](../verification/results/artifact-audit.json).

Foram aprovadas 749 verificações em servidores reais, incluindo fornecimento
de FE pelo Transfer Calculator, bloqueio dos dois caminhos de depósito,
troca imediata de modo, configuração manual de lados, orientação do bloco,
painel montado no cabo e regressões do armazenamento e da geração.

| Minecraft | Loader | SCC | Verificações |
| --- | --- | --- | --- |
| 1.20.1 | Forge 47.4.10 | Ausente | 179 |
| 1.21.1 | NeoForge 21.1.248 | 1.4.7 | 180 |
| 26.1.2 | NeoForge 26.1.2.109 | 1.4.7 | 195 |
| 26.1.2 | NeoForge 26.1.2.109 | 1.5.4 | 195 |

Desenvolvido na branch `fix/scc-transfer-direction`, com validação antes do
merge em `main`. O JAR da 26.1.2 fica disponível em `artifacts/` para
instalação manual no FTB Evolution 2.
