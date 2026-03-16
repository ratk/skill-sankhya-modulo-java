# EventoProgramavelJava — Eventos Programáveis

## Visão Geral

`EventoProgramavelJava` é a interface usada em módulos Java para reagir a eventos de persistência
(CRUD) em entidades do Sankhya. Diferente do `@Listener` do Addon Studio, o registro é **manual**:
via XML de metadados no Construtor de Telas ou pelo menu de Eventos Programáveis no sistema.

## Interface `EventoProgramavelJava`

Todos os 7 métodos devem ser implementados. Os não utilizados ficam com corpo vazio.

```java
import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;

public class NomeEvento implements EventoProgramavelJava {

    @Override
    public void beforeInsert(PersistenceEvent pEvent) throws Exception {}

    @Override
    public void afterInsert(PersistenceEvent pEvent) throws Exception {}

    @Override
    public void beforeUpdate(PersistenceEvent pEvent) throws Exception {}

    @Override
    public void afterUpdate(PersistenceEvent pEvent) throws Exception {}

    @Override
    public void beforeDelete(PersistenceEvent pEvent) throws Exception {}

    @Override
    public void afterDelete(PersistenceEvent pEvent) throws Exception {}

    @Override
    public void beforeCommit(TransactionContext tranCtx) throws Exception {}
}
```

## Objeto `PersistenceEvent`

| Método | Descrição |
|---|---|
| `pEvent.getVo()` | `DynamicVO` com o estado atual do registro |
| `pEvent.getOldVO()` | `DynamicVO` com o estado anterior (disponível em `beforeUpdate`, `afterUpdate`, `beforeDelete`, `afterDelete`) |
| `pEvent.getJdbcWrapper()` | `JdbcWrapper` — usar quando precisar de SQL dentro da mesma transação |

## Tipos de Evento

| Método | Quando dispara |
|---|---|
| `beforeInsert` | Antes de inserir — pode modificar `DynamicVO` antes de salvar |
| `afterInsert` | Após inserir — registro já persistido no banco |
| `beforeUpdate` | Antes de atualizar — pode modificar `DynamicVO` antes de salvar |
| `afterUpdate` | Após atualizar — alteração já persistida |
| `beforeDelete` | Antes de excluir — pode lançar exceção para bloquear exclusão |
| `afterDelete` | Após excluir |
| `beforeCommit` | Antes do commit da transação |

## Padrão: Calcular campo antes de salvar

Preenche um campo calculado antes de inserir/atualizar. Delegação para helper.

```java
package br.com.empresa.dctm.modulo.evento;

import br.com.sankhya.dstech.utils.DwfUtils;
import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.modelcore.util.DynamicEntityNames;
import com.sankhya.util.BigDecimalUtil;
import java.math.BigDecimal;
import java.util.Collection;

/**
 * Evento registrado em AD_NOMETABELA (Before Insert, Before Update).
 *
 * Calcula automaticamente o campo TOTAL somando os valores dos itens vinculados.
 *
 * Configuração no Sankhya:
 *   Entidade    : AD_NOMETABELA
 *   Tipo        : Before Insert, Before Update
 *   Classe Java : br.com.empresa.dctm.modulo.evento.CalcularTotalEvento
 */
public class CalcularTotalEvento implements EventoProgramavelJava {

    @Override
    public void beforeInsert(PersistenceEvent pEvent) throws Exception {
        calcular(pEvent);
    }

    @Override
    public void beforeUpdate(PersistenceEvent pEvent) throws Exception {
        calcular(pEvent);
    }

    @Override public void beforeDelete(PersistenceEvent pEvent) throws Exception {}
    @Override public void afterInsert(PersistenceEvent pEvent) throws Exception {}
    @Override public void afterUpdate(PersistenceEvent pEvent) throws Exception {}
    @Override public void afterDelete(PersistenceEvent pEvent) throws Exception {}
    @Override public void beforeCommit(TransactionContext tranCtx) throws Exception {}

    private void calcular(PersistenceEvent pEvent) throws Exception {
        DynamicVO vo = (DynamicVO) pEvent.getVo();

        BigDecimal idPai = vo.asBigDecimal("IDPAI");
        if (BigDecimalUtil.isNullOrZero(idPai)) return;

        Collection<DynamicVO> itens = DwfUtils.findEntitysAsVO(
                "AD_NOMETABELAITE",
                "this.IDPAI = ?",
                new Object[]{idPai});

        if (itens == null || itens.isEmpty()) return;

        BigDecimal total = BigDecimal.ZERO;
        for (DynamicVO item : itens) {
            BigDecimal vlr = item.asBigDecimal("VLRITEM");
            if (vlr != null) total = total.add(vlr);
        }

        vo.setProperty("TOTAL", total);
    }
}
```

## Padrão: Copiar campo de entidade relacionada

Preenche automaticamente um campo copiando de outra entidade ao inserir/alterar um item.

```java
private void preencherValor(PersistenceEvent pEvent) throws Exception {
    DynamicVO itemVO = (DynamicVO) pEvent.getVo();

    BigDecimal nuNota    = itemVO.asBigDecimal("NUNOTA");
    BigDecimal sequencia = itemVO.asBigDecimal("SEQUENCIA");

    if (BigDecimalUtil.isNullOrZero(nuNota) || BigDecimalUtil.isNullOrZero(sequencia)) return;

    DynamicVO itemOrigem = JapeFactory.dao(DynamicEntityNames.ITEM_NOTA)
            .findByPK(nuNota, sequencia);
    if (itemOrigem == null) return;

    BigDecimal vlrUnit = itemOrigem.asBigDecimal("VLRUNIT");
    if (vlrUnit == null || vlrUnit.compareTo(BigDecimal.ZERO) == 0) return;

    itemVO.setProperty("VLRUNIT", vlrUnit);

    BigDecimal qtde = itemVO.asBigDecimal("QTDE");
    if (qtde != null && qtde.compareTo(BigDecimal.ZERO) > 0) {
        itemVO.setProperty("TOTAL", vlrUnit.multiply(qtde));
    }
}
```

## Padrão: Validação bloqueante (impedir operação)

Lançar exceção em eventos `before*` impede a operação e exibe a mensagem ao usuário.

```java
@Override
public void beforeUpdate(PersistenceEvent pEvent) throws Exception {
    DynamicVO vo = (DynamicVO) pEvent.getVo();

    String status = vo.asString("STATUS");
    if ("F".equals(status)) {
        throw new MGEModelException(
            "Não é possível alterar um registro com status Finalizado.");
    }
}
```

## Padrão: Reação a alteração de campo específico

Comparar com `getOldVO()` para reagir apenas quando um campo específico mudou.

```java
@Override
public void beforeUpdate(PersistenceEvent pEvent) throws Exception {
    DynamicVO newVO = (DynamicVO) pEvent.getVo();
    DynamicVO oldVO = (DynamicVO) pEvent.getOldVO();

    if (oldVO == null) return;

    String statusNovo   = newVO.asString("STATUS");
    String statusAntigo = oldVO.asString("STATUS");

    if (statusNovo == null || statusNovo.equals(statusAntigo)) return;

    // STATUS mudou — executar lógica
    if ("F".equals(statusNovo)) {
        BigDecimal id = newVO.asBigDecimal("ID");
        FinalizacaoHelper.processarFinalizacao(id);
    }
}
```

## Padrão: Evento afterInsert — criação de registros dependentes

```java
/**
 * Evento registrado em AD_NOMETABELA (After Insert).
 *
 * Ao inserir um novo registro, cria automaticamente os itens padrão.
 *
 * Configuração no Sankhya:
 *   Entidade    : AD_NOMETABELA
 *   Tipo        : After Insert
 *   Classe Java : br.com.empresa.dctm.modulo.evento.CriarItensPadraoEvento
 */
public class CriarItensPadraoEvento implements EventoProgramavelJava {

    @Override
    public void afterInsert(PersistenceEvent pEvent) throws Exception {
        DynamicVO vo = (DynamicVO) pEvent.getVo();
        BigDecimal id = vo.asBigDecimal("ID");

        // Usar JdbcWrapper do evento para mesma transação
        ItensPadraoHelper.criarItens(id, pEvent.getJdbcWrapper());
    }

    @Override public void beforeInsert(PersistenceEvent pEvent) throws Exception {}
    @Override public void beforeUpdate(PersistenceEvent pEvent) throws Exception {}
    @Override public void afterUpdate(PersistenceEvent pEvent) throws Exception {}
    @Override public void beforeDelete(PersistenceEvent pEvent) throws Exception {}
    @Override public void afterDelete(PersistenceEvent pEvent) throws Exception {}
    @Override public void beforeCommit(TransactionContext tranCtx) throws Exception {}
}
```

## Registro Manual no Sankhya

### Opção 1: Via XML de Metadados (recomendada — automática no import)

```xml
<events>
  <event type="RJ">
    <description><![CDATA[NOME DO EVENTO]]></description>
    <eventConfig>
      <javaCall className="br.com.empresa.dctm.modulo.evento.NomeEvento" />
    </eventConfig>
    <moduleName><![CDATA[NOME DO MÓDULO]]></moduleName>
    <moduleResourceID><![CDATA[br.com.empresa.dctm.nomeModulo]]></moduleResourceID>
  </event>
</events>
```

### Opção 2: Via Menu do Sankhya

```
Menu: Arquivo → Gerenciador de módulos → Eventos Programáveis

Campos obrigatórios:
  Entidade    : nome da instância JAPE (ex: AD_NOMETABELA, CabecalhoNota)
  Tipo        : beforeInsert | afterInsert | beforeUpdate | afterUpdate | beforeDelete | afterDelete
  Classe Java : pacote completo (ex: br.com.empresa.dctm.modulo.evento.NomeEvento)
  Módulo      : nome descritivo
```

## Boas Práticas

- **Sempre implementar todos os 7 métodos** — mesmo os não usados, com corpo vazio
- **Delegar para helpers** — nenhuma lógica de negócio extensa no evento
- **Não usar `JapeSession.open()`** dentro de eventos — a sessão já está aberta pela plataforma
- **Usar `pEvent.getJdbcWrapper()`** quando precisar de SQL dentro da mesma transação
- **Validar campos nulos antes de processar** — `BigDecimalUtil.isNullOrZero()` para decimais
- **Verificar existência** com `vo.containsProperty("CAMPO")` — não usar try-catch
- **Eventos `after*`** — o registro já foi persistido; alterações no VO não têm efeito na entidade atual (usar JapeFactory para atualizar)

## Antipadrões

- Lógica de negócio extensa diretamente no método do evento
- `JapeSession.open()` dentro de evento (sessão já existe)
- SQL direto sem `pEvent.getJdbcWrapper()`
- Try-catch vazio que engole exceções
- Tentar modificar o VO em eventos `after*` esperando que persista automaticamente
