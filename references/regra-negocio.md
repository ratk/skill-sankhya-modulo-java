# Regra de Negócio e Ação Agendada

## RegraNegocioJava — Tela "Regras de Negócio"

Registrada na tela **Regras de Negócio** do Sankhya. Executa lógica customizada no ciclo de vida
de documentos comerciais, com acesso ao resultado e à possibilidade de interagir com liberações de limite.

### Interface `RegraNegocioJava`

```java
import br.com.sankhya.extensions.regrasnegocio.RegraNegocioJava;
import br.com.sankhya.extensions.regrasnegocio.ContextoRegra;

public class NomeRegra implements RegraNegocioJava {

    @Override
    public void executa(ContextoRegra ctx) throws MGEModelException {
        // implementação
        ctx.setSucesso(true);
        ctx.setMensagem("");
        ctx.setCodUsuLib(0);
    }
}
```

### `ContextoRegra`

| Método | Descrição |
|---|---|
| `ctx.getNunota()` | Número da nota em processamento |
| `ctx.setSucesso(boolean)` | `true` = passou, `false` = falhou/pendente liberação |
| `ctx.setMensagem(String)` | Mensagem exibida ao usuário |
| `ctx.setCodUsuLib(int)` | Código do usuário liberador (0 = não exige liberação) |

---

## Padrão Completo — RegraNegocioJava com Liberação de Limite

```java
package br.com.sankhya.dstech.nomedemanda.regradenegocio;

import br.com.sankhya.dstech.helper.CabecalhoNotaHelper;
import br.com.sankhya.dstech.utils.MessageUtils;
import br.com.sankhya.extensions.regrasnegocio.ContextoRegra;
import br.com.sankhya.extensions.regrasnegocio.RegraNegocioJava;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.modelcore.MGEModelException;
import br.com.sankhya.modelcore.comercial.AtributosRegras;
import br.com.sankhya.modelcore.util.DynamicEntityNames;

import java.math.BigDecimal;
import java.util.Collection;

/**
 * Regra de negócio registrada na tela Regras de Negócio do Sankhya.
 *
 * Configuração no Sankhya:
 *   Classe Java : br.com.sankhya.dstech.nomedemanda.regradenegocio.NomeRegra
 *   Evento      : ID do evento cadastrado em Regras de Negócio
 */
public class NomeRegra implements RegraNegocioJava {

    Boolean sucesso;
    String msgError;

    @Override
    public void executa(ContextoRegra ctx) throws MGEModelException {

        sucesso  = false;
        msgError = "";

        processa(ctx);

        ctx.setSucesso(sucesso);
        if (!msgError.isEmpty()) {
            ctx.setMensagem(msgError);
            MessageUtils.showInfo(msgError);
        } else if (sucesso) {
            ctx.setMensagem("");
        } else {
            ctx.setMensagem(msgError);
            MessageUtils.showInfo(msgError);
        }
        ctx.setCodUsuLib(0);
    }

    private void processa(ContextoRegra ctx) throws MGEModelException {
        JapeSession.SessionHandle hnd = null;

        try {
            hnd = JapeSession.open();
            hnd.setFindersMaxRows(-1);

            // Validações de contexto — executar somente na confirmação
            if (isItem() || !ehConfirmacao() || ehDuplicacao() || ehMovFin()) {
                sucesso = true;
                return;
            }

            BigDecimal nuNota = ctx.getNunota();

            DynamicVO notaVO = CabecalhoNotaHelper.getVo(nuNota);
            String tipMov = notaVO.asString("TIPMOV");

            if ("V".equals(tipMov)) {
                // lógica específica de venda
                sucesso = true;
            } else {
                sucesso = false;
                msgError = "<br>Tipo de movimento não suportado por esta regra.";
            }

        } catch (MGEModelException e) {
            sucesso  = false;
            msgError = "Ocorreu um erro no processamento: " + e.getMessage();
            throw e;
        } catch (Exception e) {
            sucesso  = false;
            msgError = "Ocorreu um erro inesperado: " + e.getMessage();
            throw new MGEModelException(e.getMessage(), e.getCause());
        } finally {
            JapeSession.close(hnd);
        }
    }

    // Verificadores de contexto (usando AtributosRegras)
    public static boolean isItem() {
        return JapeSession.getPropertyAsBoolean("isItem", Boolean.FALSE);
    }

    public static boolean ehConfirmacao() {
        return JapeSession.getPropertyAsBoolean(AtributosRegras.CONFIRMANDO, Boolean.FALSE);
    }

    public static boolean ehMovFin() {
        return JapeSession.getPropertyAsBoolean(AtributosRegras.CENTRAL_FINANCEIRO, Boolean.FALSE);
    }

    public static boolean ehDuplicacao() {
        return JapeSession.getPropertyAsBoolean(AtributosRegras.NUNOTA_SENDO_DUPLICADA, Boolean.FALSE);
    }

    // Verificar liberação de limite existente
    public int existeLibLimite(BigDecimal nuNota, BigDecimal codEvento) throws MGEModelException {
        try {
            Collection<DynamicVO> libVO = JapeFactory.dao(DynamicEntityNames.LIBERACAO_LIMITE)
                    .find("NUCHAVE = ? AND EVENTO = ? AND TABELA = 'TGFCAB'",
                            new Object[]{nuNota, codEvento});
            return libVO == null ? 0 : libVO.size();
        } catch (Exception e) {
            MGEModelException.throwMe(e);
        }
        return 0;
    }

    // Verificar se a liberação está aprovada
    public boolean estaLiberado(BigDecimal nuNota, BigDecimal codEvento) throws MGEModelException {
        try {
            DynamicVO libVO = JapeFactory.dao(DynamicEntityNames.LIBERACAO_LIMITE)
                    .findOne("NUCHAVE = ? AND EVENTO = ? AND TABELA = 'TGFCAB'" +
                                    " AND VLRLIBERADO = VLRATUAL AND VLRLIBERADO > 0 AND DHLIB IS NOT NULL",
                            new Object[]{nuNota, codEvento});
            return libVO != null;
        } catch (Exception e) {
            MGEModelException.throwMe(e);
        }
        return false;
    }
}
```

---

## Interface `Regra` — Alternativa para Regras via Preferência

Registrada como preferência no sistema (ex: `10@br.com.sankhya.dstech.nomedemanda.regradenegocio`).
Usada quando a regra deve ser configurável por preferência de módulo Java.

```java
package br.com.sankhya.dstech.nomedemanda.regradenegocio;

import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.modelcore.MGEModelException;
import br.com.sankhya.modelcore.comercial.AtributosRegras;
import br.com.sankhya.modelcore.comercial.ContextoRegra;
import br.com.sankhya.modelcore.comercial.Regra;
import com.sankhya.util.BigDecimalUtil;
import java.math.BigDecimal;

/**
 * Regra registrada via preferência: NOME_PREFERENCIA = "10@br.com.sankhya.dstech..."
 *
 * Configuração no Sankhya:
 *   Preferência : NOME_PREFERENCIA
 *   Valor       : {codModulo}@br.com.sankhya.dstech.nomedemanda.regradenegocio.NomeRegraPreferencia
 */
public class NomeRegraPreferencia implements Regra {

    @Override
    public void afterUpdate(ContextoRegra ctx) throws Exception {
        try {
            if (!JapeSession.getPropertyAsBoolean(AtributosRegras.CONFIRMANDO, Boolean.FALSE)) {
                return; // só na confirmação
            }

            String tipMov = ctx.getPrePersistEntityState().getNewVO().asString("TIPMOV");
            BigDecimal nuNota = BigDecimalUtil.getValueOrZero(
                    ctx.getPrePersistEntityState().getNewVO().asBigDecimal("NUNOTA"));

            // lógica aqui
        } catch (Exception ex) {
            throw MGEModelException.prettyMsg("Erro na regra: <br>" + ex.getMessage(), ex);
        }
    }

    @Override public void afterDelete(ContextoRegra ctx) throws Exception {}
    @Override public void afterInsert(ContextoRegra ctx) throws Exception {}
    @Override public void beforeDelete(ContextoRegra ctx) throws Exception {}
    @Override public void beforeInsert(ContextoRegra ctx) throws Exception {}
    @Override public void beforeUpdate(ContextoRegra ctx) throws Exception {}
}
```

---

## AtributosRegras — Constantes de Contexto

```java
import br.com.sankhya.modelcore.comercial.AtributosRegras;

// Contextos disponíveis em JapeSession
AtributosRegras.CONFIRMANDO               // nota sendo confirmada
AtributosRegras.CENTRAL_FINANCEIRO        // contexto de movimentação financeira
AtributosRegras.NUNOTA_SENDO_DUPLICADA    // nota sendo duplicada
```

---

## ScheduledAction — Ação Agendada (Cuckoo)

Tarefa agendada registrada no Sankhya via menu **Ações Agendadas**. Interface `ScheduledAction`
da biblioteca `org.cuckoo.core`.

```java
package br.com.sankhya.dstech.nomedemanda.acoesagendadas;

import org.cuckoo.core.ScheduledAction;
import org.cuckoo.core.ScheduledActionContext;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.modelcore.MGEModelException;
import org.apache.log4j.Logger;

/**
 * Ação agendada — executada periodicamente pelo Sankhya.
 *
 * Configuração no Sankhya:
 *   Menu        : Gerenciamento → Ações Agendadas
 *   Classe Java : br.com.sankhya.dstech.nomedemanda.acoesagendadas.NomeAgendada
 *   Frequência  : configurada na própria tela (CRON ou intervalo)
 */
public class NomeAgendada implements ScheduledAction {

    private static final Logger logger = Logger.getLogger(NomeAgendada.class);

    @Override
    public void onTime(ScheduledActionContext arg0) {
        JapeSession.SessionHandle hnd = null;
        try {
            hnd = JapeSession.open();
            hnd.setFindersMaxRows(-1);

            logger.info("Iniciando processamento agendado...");

            NomeModuloHelper.processarLote();

            logger.info("Processamento agendado finalizado.");

        } catch (Exception e) {
            logger.error("Erro no processamento agendado", e);
        } finally {
            JapeSession.close(hnd);
        }
    }
}
```

### Diferenças do @Job (Addon Studio)

| Aspecto | ScheduledAction (Módulo Java) | @Job (Addon Studio) |
|---|---|---|
| Interface | `org.cuckoo.core.ScheduledAction` | `br.com.sankhya.studio.annotations.Job` |
| Método | `onTime(ScheduledActionContext)` | `onSchedule()` |
| Configuração | Manual — tela Ações Agendadas | Automática via anotação |
| CRON | Configurado na tela | Atributo `frequency` na anotação |

### Registro no Sankhya

```
Menu: Gerenciamento → Ações Agendadas (ou Agendador de Tarefas)

Campos:
  Descrição   : nome descritivo
  Classe Java : br.com.sankhya.dstech.nomedemanda.acoesagendadas.NomeAgendada
  Frequência  : CRON ou intervalo em milissegundos
```

---

## External / CustomModuleLoader — Delegar para Outro JAR

Padrão onde um JAR base ("proxy") é registrado no Sankhya e delega para um segundo JAR com a
lógica real. Benefícios: registra o módulo uma única vez, troca a implementação por preferência
sem re-registrar, e separa o contrato (proxy) da lógica (implementação).

**Estrutura de dois módulos:**
```
modulo-proxy.jar      ← registrado no Sankhya (evento, botão, regra, agendada)
  └── external/NomeXxxExternal.java   → lê preferência → carrega modulo-logica.jar

modulo-logica.jar     ← contém a implementação real
  └── NomeXxx.java    → lógica de negócio
```

O ID do módulo lógico vem de uma preferência do sistema (ex: `AD_MOD_NOMEMODULO = {ID}`).

---

### External de Evento

```java
package br.com.sankhya.dstech.nomedemanda.eventos.external;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.modelcore.MGEModelException;
import br.com.sankhya.modelcore.custommodule.CustomModuleLoader;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.modelcore.util.MGECoreParameter;
import com.sankhya.util.BigDecimalUtil;
import java.math.BigDecimal;

/**
 * Classe registrada no evento — delega para outro JAR via CustomModuleLoader.
 * O JAR alvo é identificado por uma preferência do sistema.
 *
 * Configuração no Sankhya:
 *   Entidade    : AD_NOMETABELA
 *   Tipo        : Before Insert, Before Update
 *   Classe Java : br.com.sankhya.dstech.nomedemanda.eventos.external.NomeEventoExternal
 */
public class NomeEventoExternal implements EventoProgramavelJava {

    @Override
    public void beforeInsert(PersistenceEvent event) throws Exception {
        chamarExterno(event);
    }

    @Override
    public void beforeUpdate(PersistenceEvent event) throws Exception {
        chamarExterno(event);
    }

    @Override public void beforeDelete(PersistenceEvent event) throws Exception {}
    @Override public void afterInsert(PersistenceEvent event) throws Exception {}
    @Override public void afterUpdate(PersistenceEvent event) throws Exception {}
    @Override public void afterDelete(PersistenceEvent event) throws Exception {}
    @Override public void beforeCommit(TransactionContext tranCtx) throws Exception {}

    private void chamarExterno(PersistenceEvent pe) throws Exception {
        String nomePreferencia = "AD_MOD_NOMEEVENTO";
        int codModuloJava = MGECoreParameter.getParameterAsInt(nomePreferencia);
        String classeAlvo = "br.com.sankhya.dstech.nomedemanda.eventos.NomeEvento";

        EntityFacade ef = EntityFacadeFactory.getDWFFacade();
        BigDecimal moduleID = BigDecimalUtil.getValueOrZero(BigDecimal.valueOf(codModuloJava));

        if (moduleID.compareTo(BigDecimal.ZERO) == 0) {
            throw new MGEModelException(
                "Parâmetro \"" + nomePreferencia + "\" não configurado com o módulo Java.");
        }

        Object obj = CustomModuleLoader.getClass(ef, moduleID, classeAlvo).newInstance();
        obj.getClass().getMethod("executar", PersistenceEvent.class).invoke(obj, pe);
    }
}
```

### External de Botão de Ação

```java
package br.com.sankhya.dstech.nomedemanda.botaoacao.external;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.modelcore.MGEModelException;
import br.com.sankhya.modelcore.custommodule.CustomModuleLoader;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.modelcore.util.MGECoreParameter;
import com.sankhya.util.BigDecimalUtil;
import java.math.BigDecimal;

/**
 * Classe registrada no botão — delega para outro JAR via CustomModuleLoader.
 *
 * Configuração no Sankhya:
 *   Entidade : AD_NOMETABELA
 *   Classe   : br.com.sankhya.dstech.nomedemanda.botaoacao.external.NomeActionExternal
 */
public class NomeActionExternal implements AcaoRotinaJava {

    @Override
    public void doAction(ContextoAcao contexto) throws Exception {
        String nomePreferencia = "AD_MOD_NOMEBOTAO";
        int codModuloJava = MGECoreParameter.getParameterAsInt(nomePreferencia);
        String classeAlvo = "br.com.sankhya.dstech.nomedemanda.botaoacao.NomeAction";

        EntityFacade ef = EntityFacadeFactory.getDWFFacade();
        BigDecimal moduleID = BigDecimalUtil.getValueOrZero(BigDecimal.valueOf(codModuloJava));

        if (moduleID.compareTo(BigDecimal.ZERO) == 0) {
            throw new MGEModelException(
                "Parâmetro \"" + nomePreferencia + "\" não configurado com o módulo Java.");
        }

        AcaoRotinaJava acao = (AcaoRotinaJava)
                CustomModuleLoader.getClass(ef, moduleID, classeAlvo).newInstance();
        acao.doAction(contexto);
    }
}
```

### External de Regra de Negócio

```java
package br.com.sankhya.dstech.nomedemanda.regradenegocio.external;

import br.com.sankhya.extensions.rules.RegraNegocioJava;
import br.com.sankhya.extensions.rules.ContextoRegra;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.modelcore.MGEModelException;
import br.com.sankhya.modelcore.custommodule.CustomModuleLoader;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.modelcore.util.MGECoreParameter;
import com.sankhya.util.BigDecimalUtil;
import java.math.BigDecimal;

/**
 * Classe registrada na regra — delega para outro JAR via CustomModuleLoader.
 *
 * Configuração no Sankhya:
 *   Entidade    : AD_NOMETABELA
 *   Tipo        : Before Insert, Before Update (conforme necessário)
 *   Classe Java : br.com.sankhya.dstech.nomedemanda.regradenegocio.external.NomeRegraExternal
 */
public class NomeRegraExternal implements RegraNegocioJava {

    @Override
    public void executa(ContextoRegra ctx) throws Exception {
        String nomePreferencia = "AD_MOD_NOMEREGRA";
        int codModuloJava = MGECoreParameter.getParameterAsInt(nomePreferencia);
        String classeAlvo = "br.com.sankhya.dstech.nomedemanda.regradenegocio.NomeRegra";

        EntityFacade ef = EntityFacadeFactory.getDWFFacade();
        BigDecimal moduleID = BigDecimalUtil.getValueOrZero(BigDecimal.valueOf(codModuloJava));

        if (moduleID.compareTo(BigDecimal.ZERO) == 0) {
            throw new MGEModelException(
                "Parâmetro \"" + nomePreferencia + "\" não configurado com o módulo Java.");
        }

        RegraNegocioJava regra = (RegraNegocioJava)
                CustomModuleLoader.getClass(ef, moduleID, classeAlvo).newInstance();
        regra.executa(ctx);
    }
}
```

---

### External de Ação Agendada (Job)

```java
package br.com.sankhya.dstech.nomedemanda.acoesagendadas.external;

import org.cuckoo.core.ScheduledAction;
import org.cuckoo.core.ScheduledActionContext;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.modelcore.MGEModelException;
import br.com.sankhya.modelcore.custommodule.CustomModuleLoader;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.modelcore.util.MGECoreParameter;
import com.sankhya.util.BigDecimalUtil;
import java.math.BigDecimal;

/**
 * Classe registrada no agendador — delega para outro JAR via CustomModuleLoader.
 *
 * Configuração no Sankhya:
 *   Classe Java : br.com.sankhya.dstech.nomedemanda.acoesagendadas.external.NomeAgendadaExternal
 */
public class NomeAgendadaExternal implements ScheduledAction {

    @Override
    public void onTime(ScheduledActionContext arg0) throws Exception {
        String nomePreferencia = "AD_MOD_NOMEAGENDADA";
        int codModuloJava = MGECoreParameter.getParameterAsInt(nomePreferencia);
        String classeAlvo = "br.com.sankhya.dstech.nomedemanda.acoesagendadas.NomeAgendada";

        EntityFacade ef = EntityFacadeFactory.getDWFFacade();
        BigDecimal moduleID = BigDecimalUtil.getValueOrZero(BigDecimal.valueOf(codModuloJava));

        if (moduleID.compareTo(BigDecimal.ZERO) == 0) {
            throw new MGEModelException(
                "Parâmetro \"" + nomePreferencia + "\" não configurado com o módulo Java.");
        }

        ScheduledAction agendada = (ScheduledAction)
                CustomModuleLoader.getClass(ef, moduleID, classeAlvo).newInstance();
        agendada.onTime(arg0);
    }
}
```

---

### Como funciona

1. A classe External é registrada no Sankhya (evento, botão, regra ou agendador)
2. Ao executar, lê a preferência configurada → obtém o ID do módulo Java
3. `CustomModuleLoader.getClass(ef, moduleID, classe)` carrega a classe do outro JAR dinamicamente
4. Chama o método da classe alvo via casting direto (regra, botão, agendada) ou reflection (evento)

**Configuração da preferência:**
```
Menu: Preferências do Sistema
  Nome  : AD_MOD_NOMEEVENTO   (ou AD_MOD_NOMEBOTAO, AD_MOD_NOMEREGRA, AD_MOD_NOMEAGENDADA)
  Valor : {ID do módulo Java cadastrado no Sankhya}
```
