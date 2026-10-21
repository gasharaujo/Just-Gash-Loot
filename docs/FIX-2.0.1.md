# Correções de receitas — 2.0.1

Verificado em **21 de outubro de 2026**.

## Forge 1.20.1

O ingrediente de encantamento era construído com uma lista vazia de valores.
Seu predicado reconhecia Fortune III e Looting III na bancada, mas a consulta
`Ingredient.isEmpty()` o identificava como vazio. O JEI deixava os quatro cantos
sem ingredientes.

A construção agora inclui um livro com o encantamento correspondente no nível III.
As receitas aparecem com os quatro livros, e a sincronização mantém o predicado.
Ferramentas danificadas e misturas de livros e ferramentas continuam aceitas.

## NeoForge 26.1.2 e FTB Evolution 2

O ingrediente anunciava todos os itens registrados como candidatos. Filtros de
receitas do KubeJS, como os de `kubejs/server_scripts/recipes/removals.js` do pack,
consideravam as receitas de Quantity/Quality como consumidoras de itens bloqueados
e as removiam.

Os candidatos agora são livros encantados e os itens associados ao encantamento
pelos dados do jogo. O predicado continua verificando encantamentos aplicados ou
armazenados no livro, exigindo exatamente nível III.

A regressão foi reproduzida em ambiente isolado com NeoForge **26.1.2.121**,
KubeJS **26.1.2-8.0.7**, Rhino **2101.2.8-build.91** e JEI **29.47.0.111**, usados
na instância FTB Evolution 2. O teste usa um filtro de entrada para maçã:

- Antes: 11 receitas removidas, incluindo os dez upgrades.
- Depois: somente a receita vanilla de maçã dourada removida; os dez upgrades preservados.

A fixture está em [recipe-filter.js](../verification/kubejs/recipe-filter.js).
Para reproduzir, copie-a para `kubejs/server_scripts/` do diretório isolado de
verificação e use `-PkubejsJar`, `-PrhinoJar` e `-PkubejsTooltipsJar` com os JARs
correspondentes. O teste valida que o filtro rodou e que as 24 receitas do mod existem.

## Resultados

| Ambiente | Servidor dedicado | Cliente com JEI |
| --- | --- | --- |
| Forge 1.20.1 / 47.4.10, JEI 15.74.0.228 | 180 verificações | 103 verificações |
| NeoForge 26.1.2 / 26.1.2.121, KubeJS do pack, JEI 29.47.0.111 | 183 verificações | 103 verificações |

Nos dois clientes, o índice real do JEI contém as dez receitas, cada uma com nove
entradas e quatro livros encantados. As telas de Quantity e Quality foram abertas
e conferidas. No servidor, as 24 receitas foram executadas com consumo dos ingredientes;
níveis II e IV são recusados, e ingredientes encantados preservam sua validação após
sincronização. Os testes incluem regressões de armazenamento, geração, Jade e Lootr.

Relatório: [recipe-fix-2.0.1.json](../verification/results/recipe-fix-2.0.1.json).
Auditoria dos JARs: [artifact-audit.json](../verification/results/artifact-audit.json).

| Versão | Quantity | Quality |
| --- | --- | --- |
| Forge 1.20.1 | [JEI](screenshots/forge-1.20.1-jei-quantity.png) | [JEI](screenshots/forge-1.20.1-jei-quality.png) |
| NeoForge 26.1.2 | [JEI](screenshots/neoforge-26.1.2-jei-quantity.png) | [JEI](screenshots/neoforge-26.1.2-jei-quality.png) |

Os três JARs usam a versão 2.0.1. O código das receitas na 1.21.1 permanece o da
versão anterior; sua numeração foi alinhada à distribuição desta atualização.

Os JARs 2.0.1 foram instalados nas instâncias `20.1`, `21`, `26.1.2` e
`FTB Evolution 2` (26.1.2). As cópias foram conferidas por SHA-256.
