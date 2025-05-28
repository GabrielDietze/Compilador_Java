package com.compilador.Sintatica;

import java.util.LinkedList;
import java.util.Queue;

import com.compilador.Lexica.Token;
import com.compilador.Table.TabelaSimbolos;

public class AnalisadorSintatico {

    private final Queue<Token> filaTokens;
    private Token tokenAtual;

    public AnalisadorSintatico(TabelaSimbolos tabela) {
        this.filaTokens = new LinkedList<>();
        for (int i = 0; i < tabela.tamanho(); i++) {
            filaTokens.add(tabela.tokenAtual(i));
        }
        avancarToken();
    }

    private void avancarToken() {
        tokenAtual = filaTokens.poll();
    }

    private void erro(String mensagem) {
        throw new RuntimeException("Erro sintático: " + mensagem + " (token: " + tokenAtual + ")");
    }

    private boolean verificarToken(String lexemaEsperado) {
        return tokenAtual != null && tokenAtual.getNome().equals(lexemaEsperado);
    }

    private boolean verificarCategoria(String categoriaEsperada) {
        return tokenAtual != null && tokenAtual.getClassificacao().equals(categoriaEsperada);
    }

    private void consumirToken(String lexemaEsperado) {
        if (verificarToken(lexemaEsperado)) {
            avancarToken();
        } else {
            erro("Esperado '" + lexemaEsperado + "'");
        }
    }

    private void consumirCategoria(String categoriaEsperada) {
        if (verificarCategoria(categoriaEsperada)) {
            avancarToken();
        } else {
            erro("Esperado token da categoria '" + categoriaEsperada + "'");
        }
    }

    public void analisarPrograma() {
        // Começa com declaração de variáveis opcionais
        analisarDeclaracoes();
        // Depois bloco principal
        analisarBloco();
        if (tokenAtual != null) {
            erro("Fim do programa esperado, mas encontrou token extra.");
        }
        System.out.println("Análise sintática finalizada com sucesso.");
    }

    private void analisarDeclaracoes() {
        while (isTipoPrimitivo() || verificarToken("final")) {
            avancarToken();  // Consome tipo ou 'final'
            consumirCategoria("ID"); // espera identificador

            if (verificarToken("=")) {
                avancarToken();
                if (!isConstante() && !verificarCategoria("ID")) {
                    erro("Constante ou identificador esperado após '='");
                }
                avancarToken();
            }

            consumirToken(";"); // final da declaração
        }
    }

    private void analisarBloco() {
        consumirToken("begin");
        analisarComandos();
        consumirToken("end");
    }

    private void analisarComandos() {
        while (tokenAtual != null && !verificarToken("end")) {
            analisarComando();
        }
    }

    private void analisarComando() {
        if (tokenAtual == null) {
            erro("Comando esperado, mas fim dos tokens encontrado");
        }

        switch (tokenAtual.getNome()) {
            case "write":
            case "writeln":
                avancarToken();
                analisarConcat();
                consumirToken(";");
                break;
            case "readln":
                avancarToken();
                consumirToken(",");
                consumirCategoria("ID");
                consumirToken(";");
                break;
            case "while":
                avancarToken();
                analisarExpressao();
                analisarBloco();
                break;
            case "if":
                avancarToken();
                analisarExpressao();
                analisarBloco();
                break;
            case "else":
                avancarToken();
                analisarBloco();
                break;
            case "begin":
                analisarBloco();
                break;
            default:
                if (verificarCategoria("ID")) {
                    analisarAtribuicao();
                } else {
                    erro("Comando válido esperado");
                }
                break;
        }

    }

    private void analisarConcat() {
        consumirToken(",");
        if (!isConstante() && !verificarCategoria("ID")) {
            erro("Constante ou identificador esperado em concatenação");
        }
        avancarToken();

        if (verificarToken(",")) {
            analisarConcat();
        }
    }

    private void analisarAtribuicao() {
        avancarToken(); // consome ID
        consumirToken("=");
        analisarExpressao();
        consumirToken(";");
    }

    private void analisarExpressao() {
        analisarExpressaoLogica();
    }

    private void analisarExpressaoLogica() {
        if (verificarToken("not")) {
            avancarToken();
            analisarExpressaoLogica();
            return;
        }

        analisarExpressaoAritmetica();

        if (isOperadorLogico()) {
            avancarToken();
            analisarExpressaoLogica();
        }
    }

    private void analisarExpressaoAritmetica() {
        analisarTermo();
        analisarExpressaoAritmeticaTail();
    }

    private void analisarExpressaoAritmeticaTail() {
        if (verificarToken("+") || verificarToken("-")) {
            avancarToken();
            analisarTermo();
            analisarExpressaoAritmeticaTail();
        }
    }

    private void analisarTermo() {
        analisarFator();
        analisarTermoTail();
    }

    private void analisarTermoTail() {
        if (verificarToken("*") || verificarToken("/")) {
            avancarToken();
            analisarFator();
            analisarTermoTail();
        }
    }

    private void analisarFator() {
        if (isConstante() || verificarCategoria("ID") || verificarCategoria("BOOLEAN")) {
            avancarToken();
        } else if (verificarToken("(")) {
            avancarToken();
            analisarExpressao();
            consumirToken(")");
        } else {
            erro("Constante, identificador ou expressão entre parênteses esperados");
        }
    }

    // Helper methods
    private boolean isTipoPrimitivo() {
        return verificarToken("int") || verificarToken("string")
                || verificarToken("boolean") || verificarToken("byte");
    }

    private boolean isConstante() {
        return tokenAtual != null && tokenAtual.getTipo().equals("CONST");
    }

    private boolean isOperadorLogico() {
        if (tokenAtual == null) {
            return false;
        }

        switch (tokenAtual.getNome()) {
            case "==":
            case "<":
            case "<=":
            case ">":
            case ">=":
            case "<>":
            case "and":
            case "or":
                return true;
            default:
                return false;
        }
    }

}