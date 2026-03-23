---
name: sankhya-modulo-java
description: >
  Esta skill deve ser utilizada quando o usuário mencionar módulo Java Sankhya,
  EventoProgramavelJava, AcaoRotinaJava, RegraNegocioJava, ScheduledAction, JapeFactory,
  DwfUtils, MGEModelException, DynamicEntityNames, AdicionalEntityNames, CustomModuleLoader,
  Construtor de Telas, JapeSession, EntityFacade, helpers dstech, ou qualquer desenvolvimento
  Java para Sankhya OM que não utiliza as anotações @ActionButton/@Listener/@Job do Addon Studio.
  Também acionar para: "módulo java", "evento programável", "botão de ação manual",
  "tela personalizada", "metadados sankhya", "importar módulo", "criar evento",
  "criar trigger", "registrar botão ação", "construtor de telas",
  "JAR sankhya", "deploy módulo java", "módulo complementar sankhya".
version: 0.2.0
---

# Módulos Java Sankhya OM

Especialidade: desenvolvimento de módulos Java complementares para Sankhya OM sem Addon Studio.
Domínio: API interna Sankhya, eventos programáveis, botões de ação manuais, regras de negócio,
jobs agendados, helpers reutilizáveis e criação de telas via Construtor de Telas (XML).

**Idioma:** Respostas em Português do Brasil. Nomenclatura técnica no código permanece em inglês.

**Regra de ouro:** Completar o fluxo de raciocínio antes de gerar qualquer código.

---

## Pacote Padrão

Pacote raiz de todos os módulos: `br.com.sankhya.dstech`. O segmento `nomedemanda` é
substituído pelo nome real do módulo (ex: `ordemcoleta`, `registroamostra`, `contratos`).

```
br.com.sankhya.dstech.
  nomedemanda/
    botaoacao/               ← AcaoRotinaJava
    botaoacao/external/      ← AcaoRotinaJava proxy CustomModuleLoader
    eventos/                 ← EventoProgramavelJava  (plural: "eventos", não "evento")
    eventos/external/        ← EventoProgramavelJava proxy CustomModuleLoader
    acoesagendadas/          ← ScheduledAction (org.cuckoo.core)
    acoesagendadas/external/ ← ScheduledAction proxy CustomModuleLoader
    regradenegocio/          ← RegraNegocioJava / Regra
    regradenegocio/external/ ← RegraNegocioJava proxy CustomModuleLoader
  helper/                    ← Helpers transversais (br.com.sankhya.dstech.helper)
  utils/                     ← DwfUtils, MessageUtils
  enums/                     ← AdicionalEntityNames, StatusXxx, TipoXxx
```

---

## Módulo Java vs. Addon Studio

| Aspecto | Módulo Java | Addon Studio |
|---|---|---|
| **Pacote raiz** | `br.com.sankhya.dstech` | `br.com.empresa.addon` |
| **Registro de artefatos** | Manual via UI | Automático via anotações |
| **Eventos** | `EventoProgramavelJava` (manual) | `@Listener` (automático) |
| **Botões de ação** | `AcaoRotinaJava` (manual) | `@ActionButton` (automático) |
| **Jobs agendados** | `ScheduledAction` org.cuckoo.core | `@Job` (automático) |
| **Regras de negócio** | `RegraNegocioJava` / `Regra` (manual) | `@BusinessRule` (automático) |
| **Frontend** | **Popups via PopUpBuilder** — HTML/JS injetado, grids, formulários | `vc/` com xhtml5, SankhyaJS, Design System |
| **Telas/Tabelas** | XML Construtor de Telas (importação manual) | `datadictionary/` (deploy automático) |
| **Build** | Gradle → JAR → importação manual | Gradle → deploy automático pelo Studio |
| **Prefixo de tabelas** | `AD_` (convencional) | Prefixo do addon (nunca `AD_`) |
| **Helpers** | `br.com.sankhya.dstech.helper.*` (modelo disponível) | Classes ad-hoc por addon |

> **NUNCA** usar anotações `@ActionButton`, `@Listener`, `@Job`, `@Service`, `@BusinessRule`.
> **NUNCA** sugerir sankhya-js, Design System, xhtml5 ou qualquer artefato de frontend nativo do Addon Studio.
>
> **Exceção de UI — PopUpBuilder** (HTML/JS injetado via `MessageUtils.showInfo()`). Usar nos dois cenários abaixo:
>
> 1. **Botão de ação + seleção de registros:** parâmetros nativos (`ctx.getParam()`) só suportam inputs simples (texto, data, decimal). Quando o usuário precisa escolher de uma lista ou grid de registros, PopUpBuilder é a única alternativa.
> 2. **Eventos e Regras de Negócio + confirmação/pergunta:** `ctx.confirmarSimNao()` existe apenas em `AcaoRotinaJava`. Em `EventoProgramavelJava` e `RegraNegocioJava`, PopUpBuilder via `MessageUtils.showInfo()` é a forma de apresentar uma pergunta ou coletar uma escolha do usuário durante o fluxo.

---

## Fluxo de Raciocínio (Obrigatório)

Percorrer mentalmente antes de gerar qualquer solução:

```
Problema
→ Artefato correto (Evento / Botão / Regra / Job / Helper / Enum)
→ Entidade(s) envolvida(s) — DynamicEntityNames ou AdicionalEntityNames
→ Dados e campos necessários
→ Regras de negócio
→ Camadas (artefato → helper → utilitários)
→ Tratamento de erros (MGEModelException)
→ Registro manual necessário (tipo, entidade, classe)
→ XML Construtor de Telas (se nova tabela/tela for necessária)
→ Código
```

Quando faltar informação crítica, perguntar antes de implementar:
entidade alvo, tipo de evento, campos envolvidos, regra de negócio esperada.

---

## Heurísticas de Escolha de Artefato

| Artefato | Interface | Quando usar |
|---|---|---|
| **EventoProgramavelJava** | `EventoProgramavelJava` | Lógica automática em CRUD de uma entidade (before/after insert/update/delete) |
| **AcaoRotinaJava** | `AcaoRotinaJava` | Ação manual via botão "Ações" em uma tela |
| **RegraNegocioJava** | `RegraNegocioJava` / `Regra` | Lógica no ciclo de confirmação/faturamento de nota |
| **ScheduledAction** | `ScheduledAction` (org.cuckoo.core) | Tarefa agendada — processamento periódico |
| **External (CustomModuleLoader)** | qualquer interface acima | Proxy para outro JAR — módulo base delega para módulo de lógica |
| **Helper estático** | — | Lógica reutilizável entre artefatos; métodos estáticos, construtor privado |
| **Enum** | — | Estados, tipos e valores fixos — substituir strings mágicas |
| **XML Construtor de Telas** | — | Criar/atualizar tabela e tela; empacotar JAR no ZIP de metadados |
| **PopUpBuilder** | — | Popup personalizado para interação rica: confirmação complexa, seleção em grid, formulário de dados |

---

## Arquitetura

### Responsabilidade por camada

- **botaoacao/** — Recebe `ContextoAcao`, valida seleção, chama helpers. Sem lógica de negócio.
- **botaoacao/external/** — Proxy: lê preferência `MGECoreParameter` → `CustomModuleLoader` → cast `(AcaoRotinaJava)` → `doAction()`.
- **eventos/** — Recebe `PersistenceEvent`, extrai `DynamicVO`, chama helpers. Implementa 7 métodos obrigatórios.
- **eventos/external/** — Proxy: usa reflection para chamar `executar(PersistenceEvent)` no outro JAR.
- **acoesagendadas/** — `ScheduledAction.onTime()`, delega para helpers.
- **acoesagendadas/external/** — Proxy: cast direto `(ScheduledAction)` → `onTime()`.
- **regradenegocio/** — `RegraNegocioJava.executa()` ou `Regra.afterUpdate()`.
- **regradenegocio/external/** — Proxy: cast direto `(RegraNegocioJava)` → `executa()`.
- **helper/** — Toda a lógica: consultas, cálculos, validações, persistência. Métodos estáticos.
- **utils/** — `DwfUtils` (consultas genéricas), `MessageUtils` (feedback via ServiceContext).
- **enums/** — Enums de domínio, incluindo `AdicionalEntityNames`.

### Antipadrões proibidos

- Lógica de negócio direta em eventos ou botões — sempre delegar para helpers
- SQL ou consulta dentro de loop — usar `IN` ou processar fora do loop
- `JapeSession.open()` sem `finally { JapeSession.close(hnd); }`
- Capturar `Exception` sem relançar — nunca engolir silenciosamente
- `try-catch` para verificar campo em `DynamicVO` — usar `vo.containsProperty("CAMPO")`
- Instanciar helpers — construtor privado lançando `UnsupportedOperationException`
- Criar helper sem antes verificar `references/helpers-dstech.md`

---

## Checklist Antes de Responder

- [ ] Artefato correto escolhido com justificativa
- [ ] Pacote correto (`nomedemanda/eventos/`, `nomedemanda/botaoacao/`, etc.)
- [ ] Evento implementa todos os 7 métodos de `EventoProgramavelJava`
- [ ] `JapeSession.open()` com `finally { JapeSession.close(hnd); }` quando necessário
- [ ] Helper com construtor privado e métodos estáticos
- [ ] `MGEModelException` para erros de negócio e relançamento
- [ ] `vo.containsProperty()` antes de acessar campos opcionais (não try-catch)
- [ ] Enum no lugar de strings mágicas
- [ ] Sem SQL dentro de loop
- [ ] Javadoc com `Configuração no Sankhya` (entidade, tipo, classe)
- [ ] Logger `private static final`, `isDebugEnabled()` antes de concatenações custosas
- [ ] Instruções de registro manual no Sankhya incluídas na resposta
- [ ] Nenhuma referência a frontend, sankhya-js, Design System ou xhtml5

---

## Estrutura de Resposta

1. **Explicação** — O que será feito e por quê
2. **Artefato escolhido** — Com justificativa
3. **Arquivos a criar ou alterar** — Caminhos e pacotes
4. **Código completo** — Com Javadoc `Configuração no Sankhya`
5. **XML do Construtor de Telas** — Se nova tabela/tela for necessária
6. **Como registrar no Sankhya** — Passos pós-deploy do JAR
7. **Como testar** — Validação do funcionamento

---

## Exemplos de Código

Templates Java prontos para copiar e adaptar em `examples/`:

- **`examples/Modelo_Evento.java`** — EventoProgramavelJava com 7 métodos e delegate para helper
- **`examples/Modelo_EventoExternal.java`** — Proxy CustomModuleLoader para evento (usa reflection)
- **`examples/Modelo_BotaoAcao.java`** — AcaoRotinaJava com validação de seleção, confirmação e JapeSession
- **`examples/Modelo_BotaoAcaoExternal.java`** — Proxy CustomModuleLoader para botão (cast direto)
- **`examples/Modelo_AcaoAgendada.java`** — ScheduledAction com JapeSession e logger
- **`examples/Modelo_RegraNegocio.java`** — RegraNegocioJava completo: sucesso/msgError, isItem/ehConfirmacao, liberação de limite
- **`examples/Modelo_RegraPreferencia.java`** — Interface Regra via preferência MODREGCENTRAL
- **`examples/Modelo_Helper.java`** — Helper estático com exemplos de DwfUtils, JapeFactory, helpers dstech e execWithTx
- **`examples/Modelo_PopUpHelper.java`** — Helper para popups personalizados com PopUpBuilder

## Templates de Popup

Templates HTML/JS prontos para copiar em `assets/popup/`:

- **`assets/popup/PopUpConfirmacao.html` / `.js`** — Confirmação Sim/Não com ícone e botões
- **`assets/popup/PopUpSelecao.html` / `.js`** — Seleção em grid com filtro via CriteriaProvider
- **`assets/popup/PopUpFormulario.html` / `.js`** — Formulário com textarea, select e date
- **`assets/popup/PopUpDetalhes.html` / `.js`** — Exibição de dados somente-leitura em grid

---

## Referências

Carregar o arquivo correspondente ao aprofundar um tópico:

| Tópico | Arquivo |
|---|---|
| XML Construtor de Telas — instâncias, campos, relacionamentos, deploy com JAR | `references/construtor-de-telas.md` |
| EventoProgramavelJava — padrões completos, registro, External/CustomModuleLoader | `references/eventos-java.md` |
| AcaoRotinaJava — padrões, ContextoAcao, confirmação, External/CustomModuleLoader | `references/botao-acao.md` |
| RegraNegocioJava, Regra, ScheduledAction, External de regra e job | `references/regra-negocio.md` |
| JapeFactory, DwfUtils, EntityFacade, JdbcWrapper, NativeSql, JapeSession | `references/acesso-dados.md` |
| Estrutura do projeto dstech, diretórios, build.gradle, processo de deploy | `references/estrutura-modulo.md` |
| Helpers prontos — CabecalhoNotaHelper, ParceiroHelper, LancarTelaHelper, DwfUtils, etc. | `references/helpers-dstech.md` |
| Boas práticas — erros, logging, transações, performance, design de helpers | `references/boas-praticas.md` |
| Entidades nativas — DynamicEntityNames, campos TGFCAB/TGFITE/TGFFIN/TGFUSU | `references/entidades-sistema.md` |
| Popups personalizados — PopUpBuilder, HTML/JS, Angular, ServiceProxy | `references/popup-personalizado.md` |
