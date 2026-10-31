# Melhorias de jogabilidade (Fabric 1.21.1)

## 1. Aldeões: botão "Atualizar ofertas"
Aparece embaixo da tela de negociação (só em aldeões). Sorteia as ofertas de novo na hora, sem fechar a tela
e sem quebrar a bancada. Profissão, nível, experiência, bancada e vínculo ficam iguais.
Por padrão só funciona enquanto o aldeão ainda não foi usado em nenhuma troca (mesma regra do jogo original para
trocar de profissão). Para permitir depois de negociar: `allowRefreshAfterTrade: true` no config
(nesse caso, aldeões acima do nível 1 recebem só as ofertas do nível atual).
Em servidor, o mod precisa estar no servidor e no cliente.

## 2. Busca inteligente
Inventário criativo e livro de receitas. Corrige cada palavra digitada que não existe em nenhum item:
letras trocadas, faltando, repetidas, sem acento e palavras incompletas. Usa os nomes no idioma do jogo (pt-BR ou
inglês) e os ids em inglês, inclusive de itens de outros mods. 100% local. Não mexe em comandos nem no chat.
O texto na caixa continua como você digitou; só a busca usa a versão corrigida.

## 3. Barcos
Só na água, só para quem pilota (funciona também em servidores sem o mod no servidor).

## 4. Pesca
Começa quando o anzol toca a água: barra "PESCANDO..." com suspense. Ao recolher com um peixe, a barra completa e revela o item (ícone + nome, efeitos maiores para itens raros). Some quando você pega o item.
Loot tables e probabilidades não são alteradas: o mod só observa o item pescado.
O item já está saindo da água enquanto a animação roda (é só visual).

## Configuração: config/melhorias.json
| Opção | Padrão | O que faz |
|---|---|---|
| allowRefreshAfterTrade | false | permite atualizar ofertas depois de negociar |
| fuzzySearch | true | liga/desliga a busca com correção |
| boatImprovements | true | liga/desliga os ajustes dos barcos |
| boatMaxSpeed | 1.15 | velocidade máxima (1.0 = original) |
| boatSteering | 1.0 | sensibilidade da direção |
| boatAcceleration | 1.3 | aceleração |
| boatDeceleration | 1.0 | quanto o barco freia ao soltar o acelerador |
| boatDrift | 0.35 | 0 = gruda no rumo, 1 = derrapa como o original |
| fishingUi | true | liga/desliga a animação da pesca |

Edite o arquivo com o jogo fechado.

## Compilar
Suba esta pasta num repositório do GitHub (o arquivo .github/workflows/build.yml compila sozinho)
e baixe o .jar na aba Actions, em Artifacts.
