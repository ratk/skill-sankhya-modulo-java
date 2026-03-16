# Estrutura do Projeto — Módulo Java DSTech

## Visão Geral

Um módulo Java Sankhya é um projeto Gradle que gera um JAR implantado via Construtor de Telas.
O projeto modelo de referência é `modelo-dstech-customizacoes` em `/mnt/c/Projetos/modelo-dstech-customizacoes`.

**Pacote raiz:** `br.com.sankhya.dstech`

---

## Estrutura de Diretórios (Modelo DSTech)

```
projeto-dstech/
├── Java/
│   └── src/
│       └── br/com/sankhya/dstech/
│           ├── nomedemanda/               ← substituir pelo nome real do módulo
│           │   ├── botaoacao/
│           │   │   └── NomeAction.java          ← AcaoRotinaJava
│           │   ├── botaoacao/external/
│           │   │   └── NomeActionExternal.java  ← Proxy CustomModuleLoader
│           │   ├── eventos/
│           │   │   └── NomeEvento.java          ← EventoProgramavelJava
│           │   ├── eventos/external/
│           │   │   └── NomeEventoExternal.java  ← Proxy CustomModuleLoader
│           │   ├── acoesagendadas/
│           │   │   └── NomeAgendada.java        ← ScheduledAction
│           │   ├── acoesagendadas/external/
│           │   │   └── NomeAgendadaExternal.java ← Proxy CustomModuleLoader
│           │   ├── regradenegocio/
│           │   │   ├── NomeRegra.java           ← RegraNegocioJava
│           │   │   └── NomeRegraPreferencia.java ← Regra (via preferência)
│           │   └── regradenegocio/external/
│           │       └── NomeRegraExternal.java   ← Proxy CustomModuleLoader
│           ├── helper/                    ← Helpers transversais (todo o projeto)
│           │   ├── CabecalhoNotaHelper.java
│           │   ├── ItemNotaHelper.java
│           │   ├── ParceiroHelper.java
│           │   ├── ProdutoHelper.java
│           │   ├── EmpresaHelper.java
│           │   ├── UsuarioHelper.java
│           │   ├── TipoOperacaoHelper.java
│           │   ├── ContratoArmazemHelper.java
│           │   ├── ConfirmarNotaHelper.java
│           │   └── LancarTelaHelper.java
│           ├── utils/
│           │   ├── DwfUtils.java
│           │   └── MessageUtils.java
│           └── enums/
│               └── AdicionalEntityNames.java
├── Kotlin/                               ← utilitários Kotlin (MathUtils, ErrorHandle)
│   └── src/br/com/sankhya/dstech/utils/
├── build.gradle
└── Telas Adicionais/                     ← XMLs de metadados para importação
    └── nomedemanda/
        └── Metadados_AD_NOMETABELA.zip
```

---

## Convenções de Nomenclatura

### Pacotes

| Camada | Pacote | Substituir |
|---|---|---|
| Eventos do módulo | `br.com.sankhya.dstech.nomedemanda.eventos` | `nomedemanda` → nome real |
| Eventos externos | `br.com.sankhya.dstech.nomedemanda.eventos.external` | idem |
| Botões do módulo | `br.com.sankhya.dstech.nomedemanda.botaoacao` | idem |
| Botões externos | `br.com.sankhya.dstech.nomedemanda.botaoacao.external` | idem |
| Ações agendadas | `br.com.sankhya.dstech.nomedemanda.acoesagendadas` | idem |
| Regras de negócio | `br.com.sankhya.dstech.nomedemanda.regradenegocio` | idem |
| Helpers transversais | `br.com.sankhya.dstech.helper` | fixo |
| Utilitários | `br.com.sankhya.dstech.utils` | fixo |
| Enums | `br.com.sankhya.dstech.enums` | fixo |

> "eventos" é **plural** — seguir exatamente o padrão do modelo.

### Classes

| Tipo | Sufixo | Exemplo |
|---|---|---|
| Evento | `Evento` ou `Modelo` | `PesoEstimadoOrdemColetaEvento` |
| Evento externo | `External` | `NomeEventoExternal` |
| Botão de ação | `Action` ou `Modelo` | `CriarOrdemCargaAction` |
| Botão externo | `External` | `NomeActionExternal` |
| Ação agendada | — | `ProcessarFinanceiroAgendado` |
| Regra de negócio | — | `ValidarNotaVendaRegra` |
| Helper | `Helper` | `CabecalhoNotaHelper` |
| Enum de entidade | `EntityNames` | `AdicionalEntityNames` |
| Enum de status | `Status` + domínio | `StatusAmostra` |

---

## Helper Estático — Estrutura Padrão

```java
package br.com.sankhya.dstech.helper; // ou .nomedemanda.helper se específico do módulo

import br.com.sankhya.modelcore.MGEModelException;
import org.apache.log4j.Logger;

public class NomeHelper {

    private static final Logger logger = Logger.getLogger(NomeHelper.class);

    private NomeHelper() {
        throw new UnsupportedOperationException("Não é permitido instanciar esta classe");
    }

    public static RetornoTipo metodoPublico(BigDecimal id) throws Exception {
        try {
            // implementação
        } catch (Exception e) {
            MGEModelException.throwMe(e);
        }
        return null;
    }
}
```

---

## AdicionalEntityNames — Enum de Entidades Customizadas

```java
package br.com.sankhya.dstech.enums;

public enum AdicionalEntityNames {
    MINHA_ENTIDADE("AD_MINHAENTIDADE"),
    MINHA_ENTIDADE_ITENS("AD_MINHAENTIDADE_ITE");

    private final String entityName;

    AdicionalEntityNames(String entityName) {
        this.entityName = entityName;
    }

    public String getEntityName() {
        return entityName;
    }

    public static AdicionalEntityNames getByEntityName(String entityname) {
        for (AdicionalEntityNames adicional : values()) {
            if (adicional.getEntityName().equals(entityname)) {
                return adicional;
            }
        }
        throw new IllegalArgumentException("Instância inválida: " + entityname);
    }
}
```

---

## Build e Deploy

### build.gradle típico

```groovy
plugins {
    id 'java'
}

group = 'br.com.sankhya'
version = '1.0.0'
sourceCompatibility = '1.8'
targetCompatibility = '1.8'

sourceSets {
    main {
        java { srcDirs = ['Java/src'] }
    }
}

repositories {
    flatDir { dirs 'libs' }
}

dependencies {
    implementation fileTree(dir: 'libs', include: ['*.jar'])
}

jar {
    archiveFileName = 'dstech-nomemodulo.jar'
}
```

### Processo de Deploy

1. **Build:** `gradle jar` → gera `build/libs/dstech-nomemodulo.jar`
2. **Empacotar ZIP:**
   ```
   Metadados_NOMETABELA.zip
   ├── metadata.xml
   └── dstech-nomemodulo.jar
   ```
3. **Importar:** Sankhya → Construtor de Telas → Importar Metadados → selecionar ZIP
4. **Registrar manualmente** (se não constar no XML):
   - Eventos: Gerenciamento → Eventos Programáveis
   - Botões: Gerenciamento → Botões de Ação
   - Regras: Gerenciamento → Regras de Negócio
   - Agendadas: Gerenciamento → Ações Agendadas

---

## Javadoc de Configuração (Padrão)

```java
/**
 * Breve descrição do que a classe faz.
 *
 * Regras aplicadas: descrever validações e transformações.
 *
 * Configuração no Sankhya:
 *   Entidade    : AD_NOMETABELA                                    (evento)
 *   Tipo        : Before Insert, Before Update                     (evento)
 *   Classe Java : br.com.sankhya.dstech.nomedemanda.eventos.NomeEvento
 *
 *   — ou —
 *
 *   Entidade : AD_NOMETABELA                                       (botão)
 *   Classe   : br.com.sankhya.dstech.nomedemanda.botaoacao.NomeAction
 */
```
