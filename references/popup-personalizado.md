# Popups Personalizados via PopUpBuilder

## Visão Geral

O `PopUpBuilder` permite criar popups personalizados ricos em Módulos Java Sankhya, injetando HTML e JavaScript que rodam no contexto Angular do Sankhya OM.

**Quando usar:**
- Confirmações complexas (mais que Sim/Não simples)
- Seleção de registros em Grid
- Formulários de entrada de dados
- Exibição de dados detalhados
- Interações que exigem interface rica indisponível no nativo

**Quando NÃO usar:**
- Confirmações simples (usar `MessageUtils.showAlert()`)
- Mensagens de informação (usar `MessageUtils.showInfo()`)
- Erros simples (usar `MGEModelException.throwMe()`)

---

## Estrutura de Arquivos

```
src/
├── main/
│   ├── java/
│   │   └── utils/
│   │       └── PopUpBuilder.java          ← Builder (já existe no modelo)
│   └── resources/
│       └── popup/
│           ├── PopUpConfirmacao.html       ← HTML do popup
│           ├── PopUpConfirmacao.js         ← JavaScript do popup
│           ├── PopUpSelecao.html
│           ├── PopUpSelecao.js
│           └── ...
```

**Convenção:** Nome do arquivo começa com `PopUp` + finalidade (ex: `PopUpConfirmacao.html`)

---

## PopUpBuilder API

### Classe: `utils.PopUpBuilder.Builder`

```java
import utils.PopUpBuilder;
import utils.MessageUtils;

String popup = new PopUpBuilder.Builder()
    .setTitle("Título do Popup")                          // Obrigatório
    .setHtmlFile(getClass().getResourceAsStream("/popup/MeuPopUp.html"))  // Obrigatório
    .setJsFile(getClass().getResourceAsStream("/popup/MeuPopUp.js"))       // Obrigatório
    .setWidth(800)                                         // Opcional, padrão: 800
    .setHeight(400)                                        // Opcional, padrão: 400
    .setCssFile(getClass().getResourceAsStream("/popup/MeuPopUp.css"))   // Opcional
    .addVariable("codProj", codProj)                      // Passa variável Java para JS
    .addVariable("nome", "texto")                         // String é inserida com aspas
    .addVariable("ativo", true)                           // Boolean/Number sem aspas
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

## Tipos de Popup

### 1. Popup de Confirmação Sim/Não

**Cenário:** Confirmar ação com mensagem detalhada antes de executar.

**Java:**

```java
public void confirmarAcao(BigDecimal idRegistro) throws Exception {
    String popup = new PopUpBuilder.Builder()
        .setTitle("Confirmação de Exclusão")
        .setHtmlFile(getClass().getResourceAsStream("/popup/PopUpConfirmacao.html"))
        .setJsFile(getClass().getResourceAsStream("/popup/PopUpConfirmacao.js"))
        .setHeight(200)
        .addVariable("idRegistro", idRegistro)
        .build();
    
    MessageUtils.showInfo(popup);
}
```

**HTML (`/popup/PopUpConfirmacao.html`):**

```html
<sk-vbox gap="16" style="padding: 16px">
    <div style="text-align: center">
        <sk-icon font-icon="warning" style="font-size: 48px; color: #f39c12"></sk-icon>
        <h3 style="margin-top: 16px">Deseja realmente excluir este registro?</h3>
        <p style="color: #666">Esta ação não pode ser desfeita.</p>
    </div>
    
    <div sk-display="flex" style="justify-content: center; gap: 16px">
        <button class="btn" ng-click="confirmar()" brand>
            <sk-hbox gap="8" style="align-items: center">
                <sk-icon font-icon="check"></sk-icon>
                <span>Sim, excluir</span>
            </sk-hbox>
        </button>
        <button class="btn" ng-click="$dismiss()" danger>
            <sk-hbox gap="8" style="align-items: center">
                <sk-icon font-icon="times"></sk-icon>
                <span>Cancelar</span>
            </sk-hbox>
        </button>
    </div>
</sk-vbox>
```

**JavaScript (`/popup/PopUpConfirmacao.js`):**

```javascript
scope.confirmar = confirmar;

function confirmar() {
    ServiceProxy.callService('meumodulo@MeuServicoSP.excluir', {
        params: { P_ID: idRegistro }
    }, {}).then(response => {
        if (response.responseBody.success) {
            MessageUtils.showInfo(MessageUtils.TITLE_INFORMATION, "Registro excluído!");
        } else {
            MessageUtils.showAlert(MessageUtils.TITLE_ERROR, response.responseBody.message);
        }
        scope.$dismiss();
    });
}
```

---

### 2. Popup de Seleção em Grid

**Cenário:** Usuário precisa selecionar um registro (Tipo de Operação, Parceiro, Produto) antes de prosseguir.

**Java:**

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

**HTML (`/popup/PopUpSelecao.html`):**

```html
<sk-dataset entity-name="DhViewTopTipCont" sk-dataset-created="onDatasetCreated(dataset)">
    <sk-field pattern="CODTIPOPER,DESCROPER,ORIGEM"></sk-field>
</sk-dataset>

<sk-vbox gap="8" style="padding: 8px">
    <sk-vbox flex style="height: 280px">
        <sk-datagrid
            sk-dataset="dataset"
            sk-allow-multiple-selection="false"
            sk-editable="false"
            sk-on-dbl-click="selecionar()">
            <sk-datagrid-custom-column sk-field-name="CODTIPOPER" width="100"/>
            <sk-datagrid-custom-column sk-field-name="DESCROPER" width="300"/>
            <sk-datagrid-custom-column sk-field-name="ORIGEM" width="80"/>
        </sk-datagrid>
    </sk-vbox>
    
    <div sk-display="flex" style="justify-content: center; gap: 16px">
        <button class="btn" ng-click="selecionar()" brand>
            <sk-hbox gap="8" style="align-items: center">
                <sk-icon font-icon="check"></sk-icon>
                <span>Selecionar</span>
            </sk-hbox>
        </button>
        <button class="btn" ng-click="$dismiss()" danger>Cancelar</button>
    </div>
</sk-vbox>
```

**JavaScript (`/popup/PopUpSelecao.js`):**

```javascript
scope.selecionar = selecionar;
scope.onDatasetCreated = onDatasetCreated;
scope.dataset;

function onDatasetCreated(dataset) {
    scope.dataset = dataset;
    if (tipoMov === 'R') {
        dataset.addCriteriaProvider(new CriteriaProvider(Criteria("this.REQPRJ = 'S'")));
    } else if (tipoMov === 'C') {
        dataset.addCriteriaProvider(new CriteriaProvider(Criteria("this.COMPRJ = 'S'")));
    }
    dataset.initAndRefresh();
}

function selecionar() {
    if (scope.dataset.isEmpty()) {
        MessageUtils.showAlert(MessageUtils.TITLE_WARNING, "Selecione um registro!");
        return;
    }
    
    var topSelecionada = scope.dataset.getCurrentRowAsObject().CODTIPOPER;
    
    ServiceProxy.callService('meuModulo@MeuServicoSP.processar', {
        params: {
            P_NUNOTA: nuNota,
            P_CODTIPOPER: topSelecionada
        }
    }, {}).then(response => {
        if (response.responseBody.success) {
            MessageUtils.showInfo(MessageUtils.TITLE_INFORMATION, "Processado com sucesso!");
        }
        scope.$dismiss();
    });
}
```

---

### 3. Popup de Formulário de Entrada

**Cenário:** Coletar dados do usuário antes de executar uma ação.

**Java:**

```java
public void coletarDados(BigDecimal codProj) throws Exception {
    String popup = new PopUpBuilder.Builder()
        .setTitle("Informações Adicionais")
        .setHtmlFile(getClass().getResourceAsStream("/popup/PopUpFormulario.html"))
        .setJsFile(getClass().getResourceAsStream("/popup/PopUpFormulario.js"))
        .setWidth(500)
        .setHeight(350)
        .addVariable("codProj", codProj)
        .addVariable("dataAtual", new SimpleDateFormat("dd/MM/yyyy").format(new Date()))
        .build();
    
    MessageUtils.showInfo(popup);
}
```

**HTML (`/popup/PopUpFormulario.html`):**

```html
<sk-vbox gap="16" style="padding: 16px">
    <div class="form-group">
        <label>Observação</label>
        <textarea ng-model="modelo.observacao" rows="3" class="form-control"></textarea>
    </div>
    
    <div class="form-group">
        <label>Prioridade</label>
        <select ng-model="modelo.prioridade" class="form-control">
            <option value="A">Alta</option>
            <option value="M">Média</option>
            <option value="B">Baixa</option>
        </select>
    </div>
    
    <div class="form-group">
        <label>Data Prevista: {{dataAtual}}</label>
        <input type="date" ng-model="modelo.dataPrevista" class="form-control">
    </div>
    
    <div sk-display="flex" style="justify-content: flex-end; gap: 8px">
        <button class="btn" ng-click="$dismiss()">Cancelar</button>
        <button class="btn" ng-click="salvar()" brand>Salvar</button>
    </div>
</sk-vbox>
```

**JavaScript (`/popup/PopUpFormulario.js`):**

```javascript
scope.modelo = { prioridade: 'M' };
scope.salvar = salvar;

function salvar() {
    if (!scope.modelo.observacao) {
        MessageUtils.showAlert(MessageUtils.TITLE_WARNING, "Preencha a observação!");
        return;
    }
    
    ServiceProxy.callService('meuModulo@ProjetoSP.atualizar', {
        params: {
            P_CODPROJ: codProj,
            P_OBS: scope.modelo.observacao,
            P_PRIORIDADE: scope.modelo.prioridade,
            P_DATA_PREV: scope.modelo.dataPrevista
        }
    }, {}).then(response => {
        if (response.responseBody.success) {
            MessageUtils.showInfo(MessageUtils.TITLE_INFORMATION, "Dados salvos!");
            scope.$dismiss();
        }
    });
}
```

---

### 4. Popup de Exibição de Dados

**Cenário:** Exibir informações detalhadas em formato de grid ou lista.

**Java:**

```java
public void exibirDetalhes(BigDecimal codProd, BigDecimal codEmp) throws Exception {
    String popup = new PopUpBuilder.Builder()
        .setTitle("Materiais-Primas do Produto")
        .setHtmlFile(getClass().getResourceAsStream("/popup/PopUpDetalhes.html"))
        .setJsFile(getClass().getResourceAsStream("/popup/PopUpDetalhes.js"))
        .setWidth(800)
        .setHeight(400)
        .addVariable("codProd", codProd)
        .addVariable("codEmp", codEmp)
        .build();
    
    MessageUtils.showInfo(popup);
}
```

**HTML (`/popup/PopUpDetalhes.html`):**

```html
<sk-dataset
    id="dsDetalhes"
    entity-name="DhViewMateriais"
    sk-dataset-created="onDatasetCreated(dataset)"
    sk-standalone>
    <sk-field pattern="CODPROD,NOMEPROD,QTDEST,QTDRES"></sk-field>
</sk-dataset>

<sk-vbox gap="8" style="padding: 8px">
    <label style="font-weight: bold; font-size: 14px">
        Materiais-Primas - Produto: {{codProd}}
    </label>
    
    <sk-datagrid
        sk-dataset="dsDetalhes"
        sk-allow-multiple-selection="false"
        sk-editable="false">
        <sk-datagrid-custom-column sk-field-name="CODPROD" width="100"/>
        <sk-datagrid-custom-column sk-field-name="NOMEPROD" width="250"/>
        <sk-datagrid-custom-column sk-field-name="QTDEST" width="100"/>
        <sk-datagrid-custom-column sk-field-name="QTDRES" width="100"/>
    </sk-datagrid>
    
    <div sk-display="flex" style="justify-content: flex-end">
        <button class="btn" ng-click="$dismiss()">Fechar</button>
    </div>
</sk-vbox>
```

**JavaScript (`/popup/PopUpDetalhes.js`):**

```javascript
scope.onDatasetCreated = onDatasetCreated;

function onDatasetCreated(dataset) {
    if (dataset.getEntityName() === "DhViewMateriais") {
        dataset.addCriteriaProvider(new CriteriaProvider(
            Criteria("this.CODPROD = ? AND this.CODEMP = ?", codProd, codEmp)
        ));
        dataset.initAndRefresh();
    }
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

### Em Evento (EventoProgramavelJava)

**Limitação:** Eventos não podem exibir popups interativos porque rodam no servidor.

```java
// O popup em eventos NÃO FUNCIONA para interação com usuário
// use apenas para mensagens informativas após commit
public void afterInsert(PersistenceEvent event) throws Exception {
    // Considere usar MessageUtils.showInfo() ou MGEModelException
    // Popups interativos não são adequados em eventos
}
```

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
src/main/
├── java/
│   └── br/com/sankhya/dstech/modulo/
│       ├── botaoacao/
│       │   └── MeuBotaoAcao.java        ← Chama PopUpHelper
│       └── helper/
│           └── PopUpHelper.java         ← Encapsula PopUpBuilder
└── resources/
    └── popup/
        ├── PopUpConfirmacao.html
        ├── PopUpConfirmacao.js
        ├── PopUpSelecao.html
        ├── PopUpSelecao.js
        ├── PopUpFormulario.html
        ├── PopUpFormulario.js
        ├── PopUpDetalhes.html
        └── PopUpDetalhes.js
```

---

## Checklist

- [ ] HTML e JS em `src/main/resources/popup/`
- [ ] Nome de arquivo segue convenção `PopUp[Nome].html`
- [ ] Variáveis passadas via `addVariable()` documentadas
- [ ] `MessageUtils` para feedback ao usuário
- [ ] `scope.$dismiss()` para fechar popup
- [ ] Tratamento de erro em `ServiceProxy.callService()`
- [ ] `CriteriaProvider` para filtros de dataset
- [ ] Tamanho adequado ao conteúdo