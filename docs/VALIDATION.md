# Validação — Just Gash Loot 2.0.0

Verificado em **20 de outubro de 2026**, em ambientes de desenvolvimento isolados.

| Minecraft / loader | Jade | Lootr | Servidor sem ambos | Servidor com ambos | Cliente com ambos |
| --- | --- | --- | --- | --- | --- |
| Forge 1.20.1 / 47.4.10 | 11.13.3 | 0.7.35.94 | 151 | 160 | Aprovado |
| NeoForge 1.21.1 / 21.1.248 | 15.10.6 | 1.11.38.127 | 151 | 164 | Aprovado |
| NeoForge 26.1.2 / 26.1.2.109 | 26.1.12 | 1.23.38.123 | 146 | 162 | Aprovado |

O núcleo passou por **19.214 verificações**: 15.018 de
armazenamento e 4.196 de geração. O teste suplementar do Forge
com Lootr e sem Jade passou por **157 verificações**.
Os relatórios completos estão em [verification/results](../verification/results/summary.json).

## Geração, armazenamento e migração

- Tempos de geração e gasto proporcional de FE/lava em todos os níveis de Speed.
- Economia do Energy em todas as combinações com Speed, preservando créditos entre ciclos.
- Conservação de frações de lava, alternância de fonte e pausa quando não há recursos.
- Sorteios reais de loot tables, Quantity com 2/3/5/6/8 execuções totais e Quality com luck 1/5/15/50/100.
- Progresso parcial e frações preservados pela serialização nativa do bloco.
- Leitura do formato anterior sem a seção de geração, preservando estoque acima de 2,1 bilhões, recursos e upgrades.
- Tabela indisponível pausa sem consumir; resultados que não cabem são descartados sem alterar itens existentes.
- Limite de `long`, dados diferentes em slots separados, concorrência entre menus, extração e rollback de transações.

## Receitas, tablet e Player Upgrade

- As 24 receitas carregaram e produziram os itens corretos.
- O menu de crafting vanilla consumiu uma unidade de cada ingrediente em cada receita.
- Livros exatamente III e ferramentas danificadas foram aceitos; níveis II e IV foram rejeitados.
- Captura por clique real em baú Lootr, preservando o recipiente e sem abrir sua GUI.
- Shift + direito, busca, paginação com 42 entradas, seleção e aplicação da tabela no bloco por pacotes reais.
- Coleções pessoais sem duplicação, isolamento por jogador e rejeição de seleção de tabela não capturada.
- Coleção e ativa restauradas do disco em outra execução do servidor dedicado.
- Coleção com 3.500 tabelas gravada em listas NBT estruturadas, ultrapassando 64 KB sem depender de uma única string.
- Player Upgrade associado por clique, Shift, tecla numérica e arraste; remoção elimina o vínculo.
- Jogador real conectado usado no cliente integrado e jogador simulado apenas com identidade usado offline.

## Integrações e regressões

Os testes percorreram os provedores de blocos e entidades registrados pelo Lootr
nas versões indicadas. Incluem os tipos disponíveis em cada versão: baús, barris,
shulkers, carrinhos, jarros, blocos arqueológicos, item frames e variantes de cobre.
Conteúdos fixos são recusados; não são convertidos artificialmente em tabelas.

Jade foi verificado com contagens exatas até `Long.MAX_VALUE`, tanque, bateria,
tabela configurada e identificação do instalador. As interfaces públicas e um
funil vanilla real foram usados para testar automação, rejeição de inserção e
extração sem duplicação. As capacidades de todas as faces foram verificadas.

Também passaram os testes de quebra em 300/301 pilhas, aviso e confirmação por
agachar, fake players, explosões reais, drops em pilhas normais e descarte de lava/FE.

No Forge 1.20.1 e NeoForge 1.21.1, `IItemHandler` limita consultas a
`2.147.483.647` por slot. Estoque real e Jade permanecem exatos; outros mods que
consultam apenas essa API podem mostrar o teto. No NeoForge 26.1.2, o handler
informa contagens completas em `long`. Não foi validada cada combinação de cabos,
addons ou modpacks existentes. Condições de outros mods podem distinguir fake players.

## Conferência visual e artefatos

GUIs verificadas nas escalas 2 e 3. O cabeçalho do tablet permanece legível na 1.21.1,
e os textos do Jade e da seleção usam os caracteres corretos. As cópias dos novos
sprites mantêm dimensões adequadas ao atlas; os originais não foram regravados.

As tabelas `justgashloot:verification/*` e a coleção de teste são fixtures exclusivas
do harness. Esses dados e os comandos de verificação são excluídos dos JARs de distribuição.

| Versão | Máquina | Jade | Tablet | Jade com contexto |
| --- | --- | --- | --- | --- |
| Forge 1.20.1 | [GUI](screenshots/forge-1.20.1-gui.png) | [Contagens](screenshots/forge-1.20.1-jade.png) | [Coleção](screenshots/forge-1.20.1-tablet.png) | [Tabela e jogador](screenshots/forge-1.20.1-jade-context.png) |
| NeoForge 1.21.1 | [GUI](screenshots/neoforge-1.21.1-gui.png) | [Contagens](screenshots/neoforge-1.21.1-jade.png) | [Coleção](screenshots/neoforge-1.21.1-tablet.png) | [Tabela e jogador](screenshots/neoforge-1.21.1-jade-context.png) |
| NeoForge 26.1.2 | [GUI](screenshots/neoforge-26.1.2-gui.png) | [Contagens](screenshots/neoforge-26.1.2-jade.png) | [Coleção](screenshots/neoforge-26.1.2-tablet.png) | [Tabela e jogador](screenshots/neoforge-26.1.2-jade-context.png) |

Os três JARs 2.0.0 em `artifacts/` foram instalados nas instâncias CurseForge
`20.1`, `21` e `26.1.2`; as cópias foram conferidas por SHA-256. Os arquivos de
distribuição foram auditados quanto à versão, licença, receitas, texturas,
ausência do harness e ausência de classes dos mods opcionais. A conferência está em
[artifact-audit-2.0.0.json](../verification/results/artifact-audit-2.0.0.json).
