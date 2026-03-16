# Boas Práticas — Módulos Java Sankhya

## Tratamento de Erros

### Hierarquia de tratamento

```java
// 1. Erro de validação de negócio — mensagem exibida ao usuário
throw new MGEModelException("Não é possível realizar esta operação: motivo específico.");

// 2. Relançar exceção preservando stack trace (em helpers)
} catch (Exception e) {
    MGEModelException.throwMe(e);
}

// 3. Relançar com mensagem amigável (em botões de ação — suporta HTML)
} catch (Exception e) {
    throw MGEModelException.prettyMsg("Erro ao processar: <br>" + e.getMessage(), e);
}

// 4. No catch externo do botão de ação — logar e relançar
} catch (Exception e) {
    e.printStackTrace();  // log no servidor
    MGEModelException.throwMe(e);
}
```

### Nunca engolir exceção

```java
// RUIM
try {
    operacao();
} catch (Exception e) {
    // silêncio
}

// BOM
try {
    operacao();
} catch (Exception e) {
    logger.error("Erro em operacao()", e);
    MGEModelException.throwMe(e);
}
```

---

## Logging

```java
import org.apache.log4j.Logger;

// Declaração — sempre private static final
private static final Logger logger = Logger.getLogger(MinhaClasse.class);

// Níveis corretos
logger.debug("Detalhes de diagnóstico");          // apenas desenvolvimento
logger.info("Operação iniciada para ID: " + id);  // eventos significativos
logger.warn("Valor nulo, usando zero como fallback para campo PESO"); // situação anormal
logger.error("Erro ao processar registro " + id, e); // sempre com exceção

// Verificar isDebugEnabled antes de concatenações custosas
if (logger.isDebugEnabled()) {
    logger.debug("Processando VO: " + vo.toString());
}

// Em loops — logar resumo, não cada item
logger.info("Processando lote de " + itens.size() + " itens");
// for (DynamicVO item : itens) { logger.debug(...) }  // BOM com isDebugEnabled
```

---

## Verificação de Campos

```java
// CORRETO — verificar existência antes de acessar
if (vo.containsProperty("CAMPO_OPCIONAL")) {
    BigDecimal val = vo.asBigDecimal("CAMPO_OPCIONAL");
}

// ERRADO — nunca usar try-catch para verificar existência
try {
    BigDecimal val = vo.asBigDecimal("CAMPO_OPCIONAL"); // lança se não existir
} catch (Exception e) {
    // antipadrão
}

// Para campos BigDecimal — verificar nulo ou zero
if (!BigDecimalUtil.isNullOrZero(vo.asBigDecimal("CAMPO"))) {
    // processar
}
```

---

## JapeSession — Regras

```java
// CORRETO — sempre com finally
JapeSession.SessionHandle hnd = null;
try {
    hnd = JapeSession.open();
    // operações...
} finally {
    JapeSession.close(hnd); // seguro se hnd for null
}

// ERRADO — sem finally
hnd = JapeSession.open();
// operações...
JapeSession.close(hnd); // não executado se exceção for lançada

// ERRADO — usar dentro de evento (sessão já existe)
public void beforeInsert(PersistenceEvent pEvent) throws Exception {
    JapeSession.SessionHandle hnd = JapeSession.open(); // desnecessário e perigoso
    // ...
}
```

---

## Performance

### Nunca consultar dentro de loop

```java
// RUIM — N consultas ao banco
for (DynamicVO item : itens) {
    DynamicVO pedido = JapeFactory.dao(DynamicEntityNames.CABECALHO_NOTA)
            .findByPK(item.asBigDecimal("NUNOTA")); // N queries
}

// BOM — consultar fora do loop quando possível
BigDecimal nuNota = itens.iterator().next().asBigDecimal("NUNOTA");
DynamicVO pedido = JapeFactory.dao(DynamicEntityNames.CABECALHO_NOTA).findByPK(nuNota);
for (DynamicVO item : itens) {
    // usar pedido já carregado
}

// BOM — acumular resultado no loop sem consulta adicional
BigDecimal total = BigDecimal.ZERO;
for (DynamicVO item : itens) {
    BigDecimal valor = item.asBigDecimal("VLRITEM");
    if (valor != null) total = total.add(valor);
}
```

### Evitar transações longas

```java
// Transações longas bloqueiam registros no banco
// Para processamento em lote: dividir em lotes menores ou usar JdbcWrapper
// Para operações independentes: usar JapeSession.open()/close() por unidade
```

---

## Helpers — Design Correto

```java
// CORRETO — construtor privado, métodos estáticos
public class MeuHelper {
    private MeuHelper() {
        throw new UnsupportedOperationException("Não é permitido instanciar esta classe");
    }
    public static BigDecimal calcular(DynamicVO vo) throws Exception { ... }
}

// ERRADO — helper instanciável
public class MeuHelper {
    public BigDecimal calcular(DynamicVO vo) throws Exception { ... } // método de instância
}

// ERRADO — state em helper (não é thread-safe)
public class MeuHelper {
    private static BigDecimal ultimoValor; // compartilhado entre threads
}
```

---

## Enums — Boas Práticas

```java
// CORRETO — enum no lugar de String mágica
if (StatusAmostra.APROVADO.getCodigo().equals(vo.asString("STATUS"))) {
    // processar aprovado
}

// ERRADO — String mágica
if ("A".equals(vo.asString("STATUS"))) { // "A" o que significa?
    // processar aprovado
}

// CORRETO — null-safe ao comparar
String statusAtual = vo.asString("STATUS");
if (StatusAmostra.APROVADO.getCodigo().equals(statusAtual)) { // equals no enum, não no null
    // seguro
}

// ERRADO — NullPointerException se statusAtual for null
if (statusAtual.equals(StatusAmostra.APROVADO.getCodigo())) {
    // risco de NPE
}
```

---

## Comentário Javadoc de Configuração

Toda classe que precisa de registro manual no Sankhya deve ter o comentário padronizado:

```java
/**
 * Breve descrição do que a classe faz.
 *
 * Regras de negócio: descrever as validações e transformações aplicadas.
 *
 * Configuração no Sankhya:
 *   Entidade    : AD_NOMETABELA
 *   Tipo        : Before Insert, Before Update
 *   Classe Java : br.com.empresa.dctm.modulo.evento.NomeEvento
 */
public class NomeEvento implements EventoProgramavelJava { ... }
```

Isso facilita re-registro após atualizações de servidor.

---

## Checklist de Code Review

- [ ] Helper com construtor privado e métodos estáticos
- [ ] `JapeSession.open()` com `finally { JapeSession.close(hnd); }`
- [ ] `MGEModelException` para erros de negócio e relançamento
- [ ] `vo.containsProperty()` antes de campos opcionais
- [ ] `BigDecimalUtil.isNullOrZero()` antes de operar com decimais
- [ ] Sem consulta dentro de loop
- [ ] Logger `private static final` com `isDebugEnabled()` em loops/objetos complexos
- [ ] Enum no lugar de strings mágicas
- [ ] Javadoc de configuração em eventos e botões de ação
- [ ] Sem lógica de negócio extensa em eventos/botões — delegar para helpers
- [ ] Nenhuma referência a frontend, sankhya-js ou Design System
