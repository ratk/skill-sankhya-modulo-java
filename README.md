# skill-sankhya-modulo-java

Skill para Claude Code especializada em desenvolvimento de **módulos Java complementares para o ERP Sankhya OM** — abordagem sem Addon Studio, com registro manual de artefatos.

---

## O que esta skill faz

Transforma o Claude em um especialista sênior em módulos Java Sankhya OM, capaz de:

- Criar **EventoProgramavelJava** (eventos CRUD automáticos em entidades)
- Criar **AcaoRotinaJava** (botões de ação manuais)
- Criar **RegraNegocioJava** / **Regra** (lógica no ciclo de confirmação/faturamento)
- Criar **ScheduledAction** (jobs agendados via Cuckoo)
- Implementar o padrão **External / CustomModuleLoader** (dois JARs, proxy + lógica)
- Gerar **XML do Construtor de Telas** (criação de tabelas e telas via metadados)
- Usar os **helpers do modelo dstech** (`CabecalhoNotaHelper`, `ParceiroHelper`, `EnviaEmailHelper`, etc.)
- Seguir os **antipadrões proibidos** e boas práticas da plataforma

---

## Estrutura da Skill

```
sankhya-modulo-java/
├── SKILL.md                        Instruções principais da skill
├── examples/                       Templates Java prontos para copiar
│   ├── Modelo_Evento.java
│   ├── Modelo_EventoExternal.java
│   ├── Modelo_BotaoAcao.java
│   ├── Modelo_BotaoAcaoExternal.java
│   ├── Modelo_AcaoAgendada.java
│   ├── Modelo_RegraNegocio.java
│   ├── Modelo_RegraPreferencia.java
│   └── Modelo_Helper.java
└── references/                     Documentação técnica detalhada
    ├── acesso-dados.md             JapeFactory, DwfUtils, EntityFacade, JdbcWrapper
    ├── boas-praticas.md            Erros, logging, transações, performance
    ├── botao-acao.md               AcaoRotinaJava, ContextoAcao, External
    ├── construtor-de-telas.md      XML de metadados, campos, relacionamentos
    ├── entidades-sistema.md        DynamicEntityNames, campos TGFCAB/TGFITE/TGFFIN
    ├── estrutura-modulo.md         Pacotes dstech, build.gradle, deploy
    ├── eventos-java.md             EventoProgramavelJava, padrões, External
    ├── helpers-dstech.md           Todos os helpers do modelo dstech
    └── regra-negocio.md            RegraNegocioJava, Regra, ScheduledAction, External
```

---

## Pacote Padrão

Todos os módulos seguem o pacote raiz `br.com.sankhya.dstech`:

```
br.com.sankhya.dstech.
  nomedemanda/
    botaoacao/               ← AcaoRotinaJava
    botaoacao/external/      ← Proxy CustomModuleLoader
    eventos/                 ← EventoProgramavelJava
    eventos/external/        ← Proxy CustomModuleLoader
    acoesagendadas/          ← ScheduledAction (org.cuckoo.core)
    acoesagendadas/external/ ← Proxy CustomModuleLoader
    regradenegocio/          ← RegraNegocioJava / Regra
    regradenegocio/external/ ← Proxy CustomModuleLoader
  helper/                    ← Helpers transversais reutilizáveis
  utils/                     ← DwfUtils, MessageUtils
  enums/                     ← AdicionalEntityNames, StatusXxx
```

---

## Helpers Disponíveis no Modelo DSTech

| Helper | Entidade | O que faz |
|---|---|---|
| `CabecalhoNotaHelper` | TGFCAB | Busca VO do cabeçalho da nota |
| `ItemNotaHelper` | TGFITE | Busca itens de nota (por nota, produto) |
| `ParceiroHelper` | TGFPAR | Busca parceiro, atualiza conta contábil |
| `ProdutoHelper` | TGFPRO | Busca produto, ativa/desativa |
| `ServicoHelper` | TGFPRO | Busca serviço (USOPROD='S') |
| `EmpresaHelper` | TSIEMP | Busca VO da empresa |
| `UsuarioHelper` | TGFUSU | Busca VO do usuário |
| `TipoOperacaoHelper` | TGFTOP | Busca tipo de operação |
| `ContratoArmazemHelper` | TCSCON | Busca contrato de armazenagem |
| `ConfirmarNotaHelper` | — | Confirma notas, detecta contexto (confirmando/faturando/duplicando) |
| `LancarTelaHelper` | — | Gera links HTML para abrir telas nativas com filtro |
| `LancarRelatorioHelper` | — | Cria SessionFile e gera link de download |
| `EnviaEmailHelper` | TMDFMG | Insere e-mail na fila de envio |
| `CotacaoMoedaHelper` | TGFMOE | Busca e verifica cotação de moeda |
| `ImpostoItemNotaHelper` | TGFIMP | Busca impostos de item de nota |
| `ItemComposicaoProdutoHelper` | TGFICP | Busca componentes de kit |
| `CompraVendaVariosPedidoHelper` | TGFVAR | Insere registros de variação de pedido |

---

## Exemplos de Código

### Evento Programável

```java
public class PesoEstimadoEvento implements EventoProgramavelJava {

    @Override
    public void beforeInsert(PersistenceEvent event) throws Exception { processar(event); }
    @Override
    public void beforeUpdate(PersistenceEvent event) throws Exception { processar(event); }
    @Override public void beforeDelete(PersistenceEvent event) throws Exception {}
    @Override public void afterInsert(PersistenceEvent event) throws Exception {}
    @Override public void afterUpdate(PersistenceEvent event) throws Exception {}
    @Override public void afterDelete(PersistenceEvent event) throws Exception {}
    @Override public void beforeCommit(TransactionContext tranCtx) throws Exception {}

    private void processar(PersistenceEvent event) throws Exception {
        DynamicVO vo = (DynamicVO) event.getVo();
        PesoEstimadoHelper.calcular(vo);
    }
}
```

### Botão de Ação

```java
public class CriarOrdemCargaAction implements AcaoRotinaJava {

    @Override
    public void doAction(ContextoAcao ctx) throws Exception {
        if (ctx.getLinhas().length == 0) {
            ctx.setMensagemRetorno("Selecione um registro!");
            return;
        }
        BigDecimal id = BigDecimalUtil.getBigDecimal(ctx.getLinhas()[0].getCampo("NUNOTA"));
        JapeSession.SessionHandle hnd = null;
        try {
            hnd = JapeSession.open();
            String link = OrdemCargaHelper.criar(id);
            ctx.setMensagemRetorno(link);
        } catch (Exception e) {
            throw MGEModelException.prettyMsg("Erro: <br>" + e.getMessage(), e);
        } finally {
            JapeSession.close(hnd);
        }
    }
}
```

### Padrão External (CustomModuleLoader)

```java
// JAR proxy — registrado no Sankhya
public class NomeActionExternal implements AcaoRotinaJava {
    @Override
    public void doAction(ContextoAcao contexto) throws Exception {
        String pref = "AD_MODACAOFINAL";
        int codMod = MGECoreParameter.getParameterAsInt(pref);
        EntityFacade ef = EntityFacadeFactory.getDWFFacade();
        BigDecimal moduleID = BigDecimalUtil.getValueOrZero(BigDecimal.valueOf(codMod));
        if (moduleID.compareTo(BigDecimal.ZERO) == 0)
            throw new MGEModelException("Parâmetro \"" + pref + "\" não configurado.");
        AcaoRotinaJava acao = (AcaoRotinaJava)
                CustomModuleLoader.getClass(ef, moduleID, "br.com.sankhya.dstech.nomedemanda.botaoacao.NomeAction")
                        .newInstance();
        acao.doAction(contexto);
    }
}
```

---

## Instalação

### Como skill standalone (fora de plugin)

```bash
# Clonar na pasta de skills do Claude Code
git clone https://github.com/ratk/skill-sankhya-modulo-java \
    ~/.claude/skills/sankhya-modulo-java
```

A skill será detectada automaticamente pelo Claude Code no próximo restart.

### Como parte de um plugin

Copiar o diretório `sankhya-modulo-java/` para dentro da pasta `skills/` do seu plugin:

```
meu-plugin/
└── skills/
    └── sankhya-modulo-java/
        ├── SKILL.md
        ├── examples/
        └── references/
```

---

## Ativação

A skill é ativada automaticamente quando o usuário menciona:

- Termos técnicos: `EventoProgramavelJava`, `AcaoRotinaJava`, `RegraNegocioJava`, `ScheduledAction`, `JapeFactory`, `DwfUtils`, `CustomModuleLoader`, `JapeSession`, `MGEModelException`
- Frases em português: `"módulo java"`, `"evento programável"`, `"botão de ação manual"`, `"construtor de telas"`, `"criar evento"`, `"registrar botão ação"`, `"deploy módulo java"`

Ou via comando direto:

```
/sankhya-modulo-java
```

---

## Diferenças: Módulo Java vs. Addon Studio

| Aspecto | Módulo Java | Addon Studio |
|---|---|---|
| Registro de artefatos | Manual via UI do Sankhya | Automático via anotações |
| Frontend | **Indisponível** | xhtml5, SankhyaJS, Design System |
| Telas/Tabelas | XML Construtor de Telas | `datadictionary/` no deploy |
| Build | Gradle → JAR → importação manual | Gradle → deploy automático |
| Pacote raiz | `br.com.sankhya.dstech` | `br.com.empresa.addon` |

---

## Projeto Modelo

Esta skill foi construída com base no projeto modelo `modelo-dstech-customizacoes`, mantido internamente pela DSTech. Os helpers documentados em `references/helpers-dstech.md` são os mesmos disponíveis neste projeto modelo.

---

## Licença

Uso interno — DSTech Soluções.
