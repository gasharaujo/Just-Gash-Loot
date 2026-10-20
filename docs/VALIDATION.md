# ValidaÃ§Ã£o â€” Just Gash Loot 0.1.0

Verificado em **20 de outubro de 2026**.

| Minecraft / loader | Jade | Servidor sem Jade | Servidor com Jade | Cliente |
| --- | --- | --- | --- | --- |
| Forge 1.20.1 / 47.4.10 | 11.13.3 | 61 verificaÃ§Ãµes | 63 verificaÃ§Ãµes | GUI, cliques e HUD aprovados |
| NeoForge 1.21.1 / 21.1.248 | 15.10.6 | 61 verificaÃ§Ãµes | 63 verificaÃ§Ãµes | GUI, cliques e HUD aprovados |
| NeoForge 26.1.2 / 26.1.2.109 | 26.1.12 | 56 verificaÃ§Ãµes | 58 verificaÃ§Ãµes | GUI, cliques e HUD aprovados |

O nÃºcleo tambÃ©m passou por **15.018 verificaÃ§Ãµes**, incluindo conservaÃ§Ã£o de itens
em 10.000 operaÃ§Ãµes determinÃ­sticas, limite de `long`, restauraÃ§Ã£o de snapshots,
formataÃ§Ã£o e a fronteira de 300/301 pilhas. Os relatÃ³rios estruturados estÃ£o em
[verification/results](../verification/results/summary.json).

## Comportamentos cobertos

- PersistÃªncia de contagens acima de 2,1 bilhÃµes, dados personalizados dos itens, upgrades, lava e FE.
- RestriÃ§Ã£o de categorias, exclusividade do quinto slot Player e transferÃªncias de upgrades por Shift.
- SaÃ­das somente para retirada; clique esquerdo, direito, Shift, teclas numÃ©ricas e rejeiÃ§Ã£o de arraste/duplo clique.
- Dois menus consumindo o mesmo estoque, inventÃ¡rio cheio e serializaÃ§Ã£o de `Long.MAX_VALUE` pela rede.
- Retirada por um **funil vanilla real**, consulta e extraÃ§Ã£o pelas APIs pÃºblicas, simulaÃ§Ã£o sem alteraÃ§Ã£o de estoque e rejeiÃ§Ã£o de inserÃ§Ã£o de itens.
- AceitaÃ§Ã£o de lava, rejeiÃ§Ã£o de Ã¡gua, limites de capacidade e FE sem extraÃ§Ã£o.
- Commit e rollback de transaÃ§Ãµes de itens, fluidos e energia no NeoForge 26.1.
- Bloqueio de mineraÃ§Ã£o automÃ¡tica, aviso no chat para jogador real, confirmaÃ§Ã£o por agachar e explosÃµes reais dos dois lados do limite.
- Queda de uma entidade por pilha normal, descarte de lava/FE, drop do prÃ³prio bloco e ausÃªncia de duplicaÃ§Ã£o no callback repetido.
- Jade com contadores separados dos Ã­cones, preservando `Long.MAX_VALUE` no payload NBT e exibindo quantidades completas em linhas legÃ­veis.
- GUI nas escalas 2 e 3, cliques enviados pelo cliente e confirmaÃ§Ã£o das alteraÃ§Ãµes recebidas do servidor.
- Texturas de uso preparadas em dimensÃµes quadradas de potÃªncia de dois, sem alterar os PNGs originais.

## Alcance da integraÃ§Ã£o

Os testes de execuÃ§Ã£o usam ambientes isolados com o loader e o Jade. A automaÃ§Ã£o
foi verificada pelas interfaces pÃºblicas e por um funil real. Nas versÃµes antigas,
`IItemHandler.getStackInSlot` permite no mÃ¡ximo `Integer.MAX_VALUE` por consulta;
esse valor Ã© saturado para estoques maiores. A quantidade real continua em `long`,
as retiradas preservam o restante e a integraÃ§Ã£o prÃ³pria do Jade mostra o total.
Outros mods que utilizam somente a interface antiga podem apresentar esse teto.

No NeoForge 26.1.2, o handler padrÃ£o informa diretamente as quantidades completas
de 64 bits. A confirmaÃ§Ã£o de uma quebra grande permite a queda de todas as pilhas;
nÃ£o hÃ¡ descarte ou limitaÃ§Ã£o artificial dos itens para impedir lag.

## Capturas

| VersÃ£o | GUI | Jade |
| --- | --- | --- |
| Forge 1.20.1 | [GUI](screenshots/forge-1.20.1-gui.png) | [Contagens](screenshots/forge-1.20.1-jade.png) |
| NeoForge 1.21.1 | [GUI](screenshots/neoforge-1.21.1-gui.png) | [Contagens](screenshots/neoforge-1.21.1-jade.png) |
| NeoForge 26.1.2 | [GUI](screenshots/neoforge-26.1.2-gui.png) | [Contagens](screenshots/neoforge-26.1.2-jade.png) |

Os JARs em `artifacts/` tÃªm manifesto SHA-256 e foram copiados para as instÃ¢ncias
CurseForge `20.1`, `21` e `26.1.2`. As cÃ³pias instaladas foram conferidas pelo hash.
Os JARs de distribuiÃ§Ã£o nÃ£o contÃªm o harness nem os comandos de verificaÃ§Ã£o.
