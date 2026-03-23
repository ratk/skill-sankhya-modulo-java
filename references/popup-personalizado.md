# Popups Personalizados via PopUpBuilder

## Visão Geral

O `PopUpBuilder` permite criar popups personalizados ricos em Módulos Java Sankhya, injetando HTML e JavaScript que rodam no contexto Angular do Sankhya OM.

**Quando usar — dois cenários principais:**

**1. Botão de ação com seleção de registros (`AcaoRotinaJava`)**
Os parâmetros nativos (`ctx.getParam()`) suportam apenas inputs simples: texto, data, decimal.
Quando o usuário precisa escolher de uma lista ou grid de registros, PopUpBuilder é a única alternativa.
Exemplos: selecionar tipo de operação, escolher produto de uma lista filtrada, marcar campos para exportação.

**2. Eventos e Regras de Negócio com confirmação/pergunta (`EventoProgramavelJava`, `RegraNegocioJava`)**
`ctx.confirmarSimNao()` existe apenas em `AcaoRotinaJava` — não está disponível em eventos nem em regras.
PopUpBuilder via `MessageUtils.showInfo()` é a forma de apresentar uma pergunta, pedir confirmação
ou coletar uma escolha do usuário dentro desses fluxos.
Exemplos: confirmar ação antes de gerar nota, perguntar se deve imprimir após evento, escolher destino de roteamento.

**Quando NÃO usar:**
- Confirmação simples em botão de ação → usar `ctx.confirmarSimNao()`
- Mensagem de erro → usar `MGEModelException`
- Mensagem informativa pós-operação → usar `MessageUtils.showInfo()` direto (sem popup HTML)

---

## Estrutura de Arquivos

```
Java/src/br/com/sankhya/dstech/
└── utils/
    └── PopUpBuilder.java               ← Builder CUSTOMIZADO — criar manualmente (ver examples/PopUpBuilder.java)

Java/resources/br/com/sankhya/dstech/
└── {modulo}/
    └── popup/
        ├── PopUpConfirmacao.html        ← HTML do popup
        ├── PopUpConfirmacao.js          ← JavaScript do popup
        ├── PopUpSelecao.html
        ├── PopUpSelecao.js
        └── ...
```

> **ATENÇÃO — PopUpBuilder NÃO é nativo do Sankhya SDK.**
> É uma classe customizada que deve ser criada em `br.com.sankhya.dstech.utils.PopUpBuilder`.
> A implementação completa está em `examples/PopUpBuilder.java`.
>
> **Pacote correto:** `br.com.sankhya.dstech.utils` (NÃO `utils.PopUpBuilder`)
>
> **Import correto nos seus artefatos:**
> ```java
> import br.com.sankhya.dstech.utils.PopUpBuilder;
> import br.com.sankhya.dstech.utils.MessageUtils;
> ```
>
> **Por que resources em `br/com/sankhya/dstech/{modulo}/popup/`?**
> O `build.gradle` do projeto usa `include "br/com/sankhya/dstech/${moduleName}/**"` no JAR task.
> Colocar os resources neste caminho garante que sejam incluídos sem alterar o build.gradle.
> Acesse via: `getClass().getResourceAsStream("/br/com/sankhya/dstech/{modulo}/popup/PopUp.html")`

**Convenção:** Nome do arquivo começa com `PopUp` + finalidade (ex: `PopUpConfirmacao.html`)

---

## PopUpBuilder API

### Classe: `br.com.sankhya.dstech.utils.PopUpBuilder.Builder`

> Implementação completa em `examples/PopUpBuilder.java` — copiar para `br.com.sankhya.dstech.utils.PopUpBuilder`.

```java
import br.com.sankhya.dstech.utils.PopUpBuilder;
import br.com.sankhya.dstech.utils.MessageUtils;

// Path do resource: absoluto a partir do classpath
// Colocar em Java/resources/br/com/sankhya/dstech/{modulo}/popup/
// para que o build.gradle inclua no JAR sem alterações.

String popup = new PopUpBuilder.Builder()
    .setTitle("Título do Popup")                          // Obrigatório
    .setHtmlFile(MinhaClasse.class.getResourceAsStream(
        "/br/com/sankhya/dstech/{modulo}/popup/MeuPopUp.html"))  // Obrigatório
    .setJsFile(MinhaClasse.class.getResourceAsStream(
        "/br/com/sankhya/dstech/{modulo}/popup/MeuPopUp.js"))    // Obrigatório
    .setWidth(800)                                         // Opcional, padrão: 800
    .setHeight(400)                                        // Opcional, padrão: 400
    .setCssFile(MinhaClasse.class.getResourceAsStream(
        "/br/com/sankhya/dstech/{modulo}/popup/MeuPopUp.css"))   // Opcional
    .addVariable("codProj", codProj)                      // Passa variável Java para JS
    .addVariable("nome", "texto")                         // String → var nome = 'texto';
    .addVariable("ativo", true)                           // Boolean/Number → var ativo = true;
    .addVariable("jsonStr", jsonString)                   // JSON String → var x = '[{...}]';
    .build();

MessageUtils.showInfo(popup);
```

### Métodos do Builder

| Método | Tipo | Descrição |
|---|---|---|
| `setTitle(String)` | String | Título exibido no header do popup |
| `setHtmlFile(InputStream)` | InputStream | Arquivo HTML com estrutura do popup |
| `setJsFile(InputStream)` | InputStream | Arquivo JS com lógica do popup |
| `setCssFile(InputStream)` | InputStream | CSS customizado (opcional) |
| `setWidth(int)` | int | Largura em pixels (padrão: 800) |
| `setHeight(int)` | int | Altura em pixels (padrão: 400) |
| `addVariable(String, Object)` | Object | Passa variável Java para JavaScript |
| `build()` | String | Retorna HTML completo para exibir |

---

## Serviços Angular Disponíveis

O PopUpBuilder injeta automaticamente estes serviços do Sankhya no escopo Angular:

```javascript
// Disponíveis automaticamente no popup
ObjectUtils,      // Operações de objeto
MessageUtils,     // Mensagens: showAlert, showInfo, TITLE_WARNING, TITLE_ERROR, etc.
AngularUtil,      // Utilitários Angular
DateUtils,        // Manipulação de datas
NumberUtils,      // Formatação de números
ServiceProxy,     // Chamadas de serviço backend
StringUtils,      // Manipulação de strings
SkApplicationInstance// Instância da aplicação
```

### Exemplo de uso no JavaScript:

```javascript
// Exibir mensagem
MessageUtils.showAlert(MessageUtils.TITLE_WARNING, "Selecione um registro!");

// Chamar serviço backend
ServiceProxy.callService('modulo@ServicoSP.minhaAcao', {params: {P_ID: id}}, {})
    .then(response => {
        if (response.responseBody.success) {
            MessageUtils.showInfo(MessageUtils.TITLE_INFORMATION, "Operação realizada!");
        }
        scope.$dismiss();  // Fechar popup
    });
```

---

## Componentes Sankhya Disponíveis

### sk-dataset (Dataset)

Carrega dados de uma entidade/view do Sankhya:

```html
<sk-dataset 
    entity-name="DhViewTopTipCont"
    sk-dataset-created="onDatasetCreated(dataset)">
    <sk-field pattern="CODTIPOPER,DESCROPER,ORIGEM"></sk-field>
</sk-dataset>
```

```javascript
scope.onDatasetCreated = function(dataset) {
    dataset.addCriteriaProvider(new CriteriaProvider(Criteria("this.CODTIPCONT = ?", codTipCont)));
    dataset.initAndRefresh();
};
```

### sk-datagrid (Grid)

Exibe dados com seleção:

```html
<sk-datagrid
    sk-dataset="dataset"
    sk-allow-multiple-selection="false"
    sk-editable="false"
    sk-on-dbl-click="confirmar()"
    sk-order-columms="orderColumns">
    <sk-datagrid-custom-column sk-field-name="CODTIPOPER" width="150"/>
    <sk-datagrid-custom-column sk-field-name="DESCROPER" width="300"/>
</sk-datagrid>
```

### sk-vbox, sk-hbox (Layout)

Containers de layout:

```html
<sk-vbox gap="8" flex layout="column">
    <!-- conteúdo -->
</sk-vbox>

<sk-hbox flex gap="8" style="align-items: center">
    <!-- conteúdo -->
</sk-hbox>
```

---

## Templates de Assets

Os tipos de popup têm templates prontos para copiar em `assets/popup/`. Usar como ponto de partida e adaptar entidade, campos e serviço:

| Tipo | HTML | JS |
|---|---|---|
| Confirmação | `assets/popup/PopUpConfirmacao.html` | `assets/popup/PopUpConfirmacao.js` |
| Seleção em Grid | `assets/popup/PopUpSelecao.html` | `assets/popup/PopUpSelecao.js` |
| Formulário | `assets/popup/PopUpFormulario.html` | `assets/popup/PopUpFormulario.js` |
| Exibição de Dados | `assets/popup/PopUpDetalhes.html` | `assets/popup/PopUpDetalhes.js` |
| **Wizard 2 Páginas** | — (construir inline) | ver padrão abaixo |

---

## Tipos de Popup

### 1. Popup de Confirmação Sim/Não

**Cenário:** Confirmar ação com mensagem detalhada antes de executar.
**Template:** `assets/popup/PopUpConfirmacao.html` + `PopUpConfirmacao.js`

```java
public void confirmarAcao(BigDecimal idRegistro) throws Exception {
    String popup = new PopUpBuilder.Builder()
        .setTitle("Confirmação de Exclusão")
        .setHtmlFile(getClass().getResourceAsStream("/popup/PopUpConfirmacao.html"))
        .setJsFile(getClass().getResourceAsStream("/popup/PopUpConfirmacao.js"))
        .setWidth(400)
        .setHeight(200)
        .addVariable("idRegistro", idRegistro)
        .addVariable("mensagem", "Deseja realmente excluir este registro?")
        .addVariable("detalhe", "Esta ação não pode ser desfeita.")
        .build();

    MessageUtils.showInfo(popup);
}
```

---

### 2. Popup de Seleção em Grid

**Cenário:** Usuário precisa selecionar um registro (Tipo de Operação, Parceiro, Produto) antes de prosseguir.
**Template:** `assets/popup/PopUpSelecao.html` + `PopUpSelecao.js`

```java
public void selecionarTipoOperacao(BigDecimal nuNota, String tipoMov) throws Exception {
    String popup = new PopUpBuilder.Builder()
        .setTitle("Selecione o Tipo de Operação")
        .setHtmlFile(getClass().getResourceAsStream("/popup/PopUpSelecao.html"))
        .setJsFile(getClass().getResourceAsStream("/popup/PopUpSelecao.js"))
        .setWidth(700)
        .setHeight(400)
        .addVariable("nuNota", nuNota)
        .addVariable("tipoMov", tipoMov)
        .build();

    MessageUtils.showInfo(popup);
}
```

No JS (`PopUpSelecao.js`), aplicar filtro no `onDatasetCreated` conforme a variável recebida:

```javascript
function onDatasetCreated(dataset) {
    scope.dataset = dataset;
    if (tipoMov === 'R') {
        dataset.addCriteriaProvider(new CriteriaProvider(Criteria("this.REQPRJ = 'S'")));
    } else if (tipoMov === 'C') {
        dataset.addCriteriaProvider(new CriteriaProvider(Criteria("this.COMPRJ = 'S'")));
    }
    dataset.initAndRefresh();
}
```

---

### 3. Popup de Formulário de Entrada

**Cenário:** Coletar dados do usuário antes de executar uma ação.
**Template:** `assets/popup/PopUpFormulario.html` + `PopUpFormulario.js`

```java
public void coletarDados(BigDecimal codRegistro) throws Exception {
    String popup = new PopUpBuilder.Builder()
        .setTitle("Informações Adicionais")
        .setHtmlFile(getClass().getResourceAsStream("/popup/PopUpFormulario.html"))
        .setJsFile(getClass().getResourceAsStream("/popup/PopUpFormulario.js"))
        .setWidth(500)
        .setHeight(350)
        .addVariable("codRegistro", codRegistro)
        .build();

    MessageUtils.showInfo(popup);
}
```

---

### 4. Popup de Exibição de Dados

**Cenário:** Exibir informações detalhadas em formato de grid somente-leitura.
**Template:** `assets/popup/PopUpDetalhes.html` + `PopUpDetalhes.js`

```java
public void exibirDetalhes(BigDecimal codRegistro, BigDecimal codEmp) throws Exception {
    String popup = new PopUpBuilder.Builder()
        .setTitle("Detalhes do Registro")
        .setHtmlFile(getClass().getResourceAsStream("/popup/PopUpDetalhes.html"))
        .setJsFile(getClass().getResourceAsStream("/popup/PopUpDetalhes.js"))
        .setWidth(800)
        .setHeight(400)
        .addVariable("codRegistro", codRegistro)
        .addVariable("codemp", codEmp)
        .build();

    MessageUtils.showInfo(popup);
}
```

---

## Integração com Artefatos

### Em Botão de Ação (AcaoRotinaJava)

```java
public class MeuBotaoAcao implements AcaoRotinaJava {
    
    @Override
    public void doAction(ContextoAcao ctx) throws Exception {
        BigDecimal[] selected = ctx.getSelectedRecordsPK("NUNOTA");
        if (selected.length == 0) {
            throw new MGEModelException("Selecione ao menos uma nota!");
        }
        
        // Exibir popup de seleção
        new PopUpHelper().selecionarTipoOperacao(selected[0], "R");
        
        // IMPORTANTE: Botão de ação não deve continuar após popup
        // O processamento continua no callback do JavaScript
    }
}
```

### Em Evento (EventoProgramavelJava) e Regra de Negócio (RegraNegocioJava)

`ctx.confirmarSimNao()` não existe nesses artefatos — PopUpBuilder é a forma de fazer perguntas ou coletar confirmação do usuário durante o fluxo.

```java
// beforeUpdate de uma RegraNegocioJava: pergunta antes de prosseguir
public void executa(DynamicVO vo, DynamicVO voAnt) throws Exception {

    String popup = new PopUpBuilder.Builder()
        .setTitle("Confirmação de Roteamento")
        .setHtmlFile(getClass().getResourceAsStream("/popup/PopUpConfirmacao.html"))
        .setJsFile(getClass().getResourceAsStream("/popup/PopUpConfirmacao.js"))
        .setWidth(400)
        .setHeight(200)
        .addVariable("mensagem", "Deseja gerar a nota de devolução automaticamente?")
        .addVariable("detalhe", "A operação criará um novo documento vinculado.")
        .build();

    MessageUtils.showInfo(popup);
    // O processamento continua no callback do JavaScript via ServiceProxy
}
```

**Padrão recomendado para eventos/regras:** o popup coleta a decisão do usuário e chama um `ServiceProxy` com a escolha. O Java da resposta executa a lógica correspondente. Não há retorno síncrono do popup para o método Java — o fluxo é sempre assíncrono via callback JavaScript.

---

## Boas Práticas

### Nomenclatura

| Arquivo | Convenção | Exemplo |
|---|---|---|
| Java | `PopUp[Nome]Helper.java` | `PopUpHelper.java` |
| HTML | `PopUp[Nome].html` | `PopUpConfirmacao.html` |
| JavaScript | `PopUp[Nome].js` | `PopUpConfirmacao.js` |

### Tamanho do Popup

| Tipo | Largura | Altura |
|---|---|---|
| Confirmação simples | 400px | 200px |
| Seleção em Grid | 700-900px | 400-500px |
| Formulário | 500-600px | 350-450px |
| Exibição de dados | 800-1000px | variável |

### Tratamento de Erros

```javascript
// Sempre tratar erro em chamadas de serviço
ServiceProxy.callService('modulo@ServicoSP.acao', {params: params}, {})
    .then(response => {
        if (response.responseBody.success) {
            MessageUtils.showInfo(MessageUtils.TITLE_INFORMATION, "Sucesso!");
            scope.$dismiss();
        } else {
            MessageUtils.showAlert(MessageUtils.TITLE_ERROR, response.responseBody.message);
        }
    })
    .catch(error => {
        MessageUtils.showAlert(MessageUtils.TITLE_ERROR, "Erro inesperado: " + error);
    });
```

### Performance

- Filtrar dados no `CriteriaProvider` do dataset, não carregar tudo
- Usar `sk-standalone` para datasets independentes
- Evitar chamadas múltiplas de `initAndRefresh()`

---

## Estrutura de Diretórios Recomendada

```
Java/
├── src/br/com/sankhya/dstech/
│   ├── utils/
│   │   └── PopUpBuilder.java              ← OBRIGATÓRIO — copiar de examples/PopUpBuilder.java
│   └── {modulo}/
│       ├── botaoacao/
│       │   └── MeuBotaoAcao.java          ← Chama PopUpBuilder diretamente ou via helper
│       └── helper/
│           └── PopUpHelper.java           ← Opcional — encapsula PopUpBuilder
└── resources/br/com/sankhya/dstech/
    └── {modulo}/
        └── popup/
            ├── PopUpNome.html             ← Path capturado pelo build.gradle sem alterações
            └── PopUpNome.js
```

---

## 5. Popup Wizard (Multi-Etapas / State Machine)

**Cenário:** Fluxo em etapas dentro de um único popup — ex: selecionar uma entidade
na etapa 1 e configurar opções na etapa 2.

**Vantagem sobre dois popups sequenciais:** Sem problema de timing; estado mantido
no scope Angular entre as páginas sem chamadas adicionais ao servidor.

### HTML (duas divs com ng-show)

```html
<div ng-show="pagina === 1">
    <!-- etapa 1 -->
    <input type="text" ng-model="filtro" placeholder="Filtrar..."/>
    <table class="table table-hover">
        <tbody>
            <tr ng-repeat="item in itensFiltrados()"
                ng-click="selecionarItem(item)"
                ng-dblclick="irParaPagina2()"
                ng-class="{'info': itemSelecionado && itemSelecionado.id === item.id}">
                <td>{{item.nome}}</td>
            </tr>
        </tbody>
    </table>
    <button class="btn" ng-click="$dismiss()" danger>Cancelar</button>
    <button class="btn" ng-click="irParaPagina2()" ng-disabled="!itemSelecionado" brand>Próximo ></button>
</div>

<div ng-show="pagina === 2">
    <!-- etapa 2 -->
    <h4>{{itemSelecionado.nome}}</h4>
    <!-- conteúdo da etapa 2 -->
    <button class="btn" ng-click="voltar()">< Voltar</button>
    <button class="btn" ng-click="$dismiss()" danger>Cancelar</button>
    <button class="btn" ng-click="confirmar()" brand>Confirmar</button>
</div>
```

### JS (state machine)

```javascript
// Variável injetada pelo Java: var itensJson = '[{...}]';
var itens = JSON.parse(itensJson);

scope.pagina = 1;
scope.itens  = itens;
scope.itemSelecionado = null;
scope.filtro = '';

scope.itensFiltrados  = itensFiltrados;
scope.selecionarItem  = selecionarItem;
scope.irParaPagina2   = irParaPagina2;
scope.voltar          = voltar;
scope.confirmar       = confirmar;

function itensFiltrados() {
    var f = (scope.filtro || '').toUpperCase();
    if (!f) return scope.itens;
    return scope.itens.filter(function(i) { return i.nome.toUpperCase().indexOf(f) !== -1; });
}

function selecionarItem(item)  { scope.itemSelecionado = item; }
function irParaPagina2() {
    if (!scope.itemSelecionado) {
        MessageUtils.showAlert(MessageUtils.TITLE_WARNING, 'Selecione um item!');
        return;
    }
    scope.pagina = 2;
}
function voltar() { scope.pagina = 1; }
function confirmar() { /* lógica final */ scope.$dismiss(); }
```

### Java (pre-carrega dados e injeta como JSON)

```java
// Serializar lista para JSON antes de abrir o popup
String itensJson = MinhaHelper.buscarItensJson();  // retorna "[{\"id\":1,\"nome\":\"...\"}]"

String popup = new PopUpBuilder.Builder()
    .setTitle("Selecionar Item")
    .setHtmlFile(MinhaClasse.class.getResourceAsStream("/br/.../popup/PopUpWizard.html"))
    .setJsFile(MinhaClasse.class.getResourceAsStream("/br/.../popup/PopUpWizard.js"))
    .setWidth(750).setHeight(500)
    .addVariable("itensJson", itensJson)   // String → var itensJson = '[{...}]';
    .build();

MessageUtils.showInfo(popup);
```

> **Quando pre-carregar vs. chamar serviço em runtime:**
> Pre-carregue tudo no `doAction` quando o volume for razoável (< ~200 registros).
> Para volumes grandes, use `ServiceProxy` no JS para carregar on-demand — mas isso
> requer um endpoint de backend acessível.

---

## 6. Download de Arquivo Client-Side (Blob URL)

**Cenário:** Gerar e baixar um arquivo (XML, CSV, JSON, TXT) a partir do conteúdo
construído no JavaScript, sem precisar de um endpoint de backend.

```javascript
function baixarArquivo(conteudo, nomeArquivo, tipoMime) {
    try {
        var blob = new Blob([conteudo], { type: tipoMime + ';charset=utf-8;' });
        var url  = URL.createObjectURL(blob);
        var link = document.createElement('a');
        link.setAttribute('href', url);
        link.setAttribute('download', nomeArquivo);
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        setTimeout(function() { URL.revokeObjectURL(url); }, 1500);
    } catch (e) {
        MessageUtils.showAlert(MessageUtils.TITLE_ERROR, 'Erro ao gerar arquivo: ' + e.message);
    }
}

// Exemplo — gerar XML e fechar popup
function gerarXml() {
    var xml = construirXml();   // string com o XML completo
    baixarArquivo(xml, 'metadata.xml', 'application/xml');
    scope.$dismiss();
    MessageUtils.showInfo(MessageUtils.TITLE_INFORMATION, 'Arquivo gerado com sucesso!');
}
```

**Tipos MIME comuns:**
- XML:  `application/xml`
- CSV:  `text/csv`
- JSON: `application/json`
- TXT:  `text/plain`

---

## Checklist

- [ ] `PopUpBuilder.java` existe em `br.com.sankhya.dstech.utils` (copiar de `examples/PopUpBuilder.java`)
- [ ] HTML e JS em `Java/resources/br/com/sankhya/dstech/{modulo}/popup/`
- [ ] `getResourceAsStream` usa path absoluto: `/br/com/sankhya/dstech/{modulo}/popup/...`
- [ ] Nome de arquivo segue convenção `PopUp[Nome].html`
- [ ] Variáveis passadas via `addVariable()` documentadas
- [ ] `MessageUtils` para feedback ao usuário
- [ ] `scope.$dismiss()` para fechar popup
- [ ] Tratamento de erro em `ServiceProxy.callService()`
- [ ] `CriteriaProvider` para filtros de dataset
- [ ] Tamanho adequado ao conteúdo
- [ ] Wizard: usar `ng-show="pagina === N"` (não `ng-if`, para não destruir o DOM)