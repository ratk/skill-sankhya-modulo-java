package br.com.sankhya.dstech.nomedemanda.helper;

import utils.MessageUtils;
import utils.PopUpBuilder;

import java.math.BigDecimal;

/**
 * Helper para popups personalizados — encapsula PopUpBuilder para casos comuns.
 *
 * Configuração no Sankhya:
 *   - Nenhum registro necessário, apenas inclusão no JAR
 *   - Arquivos HTML/JS em src/main/resources/popup/
 *
 * Uso em Botão de Ação:
 *   PopUpHelper.confirmarExclusao(idRegistro);
 *   PopUpHelper.selecionarTipoOperacao(nuNota, "R");
 */
public class Modelo_PopUpHelper {

    private Modelo_PopUpHelper() {
        throw new UnsupportedOperationException("Não é permitido instanciar esta classe");
    }

    // -------------------------------------------------------------------------
    // Popup de Confirmação Simples
    // -------------------------------------------------------------------------

    public static void confirmarExclusao(BigDecimal idRegistro) throws Exception {
        String popup = new PopUpBuilder.Builder()
            .setTitle("Confirmação de Exclusão")
            .setHtmlFile(Modelo_PopUpHelper.class.getResourceAsStream("/popup/PopUpConfirmacao.html"))
            .setJsFile(Modelo_PopUpHelper.class.getResourceAsStream("/popup/PopUpConfirmacao.js"))
            .setWidth(400)
            .setHeight(200)
            .addVariable("idRegistro", idRegistro)
            .build();

        MessageUtils.showInfo(popup);
    }

    // -------------------------------------------------------------------------
    // Popup de Seleção em Grid
    // -------------------------------------------------------------------------

    public static void selecionarTipoOperacao(BigDecimal nuNota, String tipoMov) throws Exception {
        String popup = new PopUpBuilder.Builder()
            .setTitle("Selecione o Tipo de Operação")
            .setHtmlFile(Modelo_PopUpHelper.class.getResourceAsStream("/popup/PopUpSelecao.html"))
            .setJsFile(Modelo_PopUpHelper.class.getResourceAsStream("/popup/PopUpSelecao.js"))
            .setWidth(700)
            .setHeight(400)
            .addVariable("nuNota", nuNota)
            .addVariable("tipoMov", tipoMov)
            .build();

        MessageUtils.showInfo(popup);
    }

    // -------------------------------------------------------------------------
    // Popup de Formulário
    // -------------------------------------------------------------------------

    public static void coletarDadosAdicionais(BigDecimal codProj, BigDecimal codEmp) throws Exception {
        String popup = new PopUpBuilder.Builder()
            .setTitle("Informações Adicionais")
            .setHtmlFile(Modelo_PopUpHelper.class.getResourceAsStream("/popup/PopUpFormulario.html"))
            .setJsFile(Modelo_PopUpHelper.class.getResourceAsStream("/popup/PopUpFormulario.js"))
            .setWidth(500)
            .setHeight(350)
            .addVariable("codProj", codProj)
            .addVariable("codEmp", codEmp)
            .build();

        MessageUtils.showInfo(popup);
    }

    // -------------------------------------------------------------------------
    // Popup de Exibição de Dados
    // -------------------------------------------------------------------------

    public static void exibirDetalhes(BigDecimal codProd) throws Exception {
        String popup = new PopUpBuilder.Builder()
            .setTitle("Detalhes do Produto")
            .setHtmlFile(Modelo_PopUpHelper.class.getResourceAsStream("/popup/PopUpDetalhes.html"))
            .setJsFile(Modelo_PopUpHelper.class.getResourceAsStream("/popup/PopUpDetalhes.js"))
            .setWidth(800)
            .setHeight(400)
            .addVariable("codProd", codProd)
            .build();

        MessageUtils.showInfo(popup);
    }

    // -------------------------------------------------------------------------
    // Popup com CSS Customizado
    // -------------------------------------------------------------------------

    public static void popupComEstilo(BigDecimal id, String titulo) throws Exception {
        String popup = new PopUpBuilder.Builder()
            .setTitle(titulo)
            .setHtmlFile(Modelo_PopUpHelper.class.getResourceAsStream("/popup/PopUpCustom.html"))
            .setJsFile(Modelo_PopUpHelper.class.getResourceAsStream("/popup/PopUpCustom.js"))
            .setCssFile(Modelo_PopUpHelper.class.getResourceAsStream("/popup/PopUpCustom.css"))
            .setWidth(600)
            .setHeight(300)
            .addVariable("id", id)
            .build();

        MessageUtils.showInfo(popup);
    }
}