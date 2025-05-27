package com.compilador.Lexica;

import com.compilador.Execptions.ErroLexico;
import com.compilador.Execptions.ExcecaoCompilador;
import com.compilador.Table.TabelaSimbolos;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Analisador {

    private final TabelaSimbolos tabela;

    private final Pattern padraoNumeros = Pattern.compile("\\d+");
    private final Pattern padraoHexa = Pattern.compile("0h[a-fA-F0-9]{4}");
    private final Pattern padraoIdentificadores = Pattern.compile("[a-zA-Z_]\\w*");
    private final Pattern padraoBooleanos = Pattern.compile("true|false");
    private final Pattern padraoOperadores = Pattern.compile("==|<>|<=|>=|<|>|[+\\-*/=]");
    private final Pattern padraoDelimitadores = Pattern.compile("[,;()]");
    private final Pattern padraoComentarios = Pattern.compile("/\\*(.|\\R)*?\\*/|\\{[^}]*\\}");
    private final Pattern padraoStrings = Pattern.compile("\"([^\"\\\\]|\\\\.)*\"");
    private final Pattern padraoEspacos = Pattern.compile("\\s+");

    public Analisador(TabelaSimbolos tabela) {
        this.tabela = tabela;
    }

    public void executarAnalise(String linhaCodigo, int linha) throws ExcecaoCompilador {
        int coluna = 1;
        linhaCodigo = linhaCodigo.stripLeading();

        while (!linhaCodigo.isEmpty()) {
            Matcher m;
            boolean houveReconhecimento = false;

            m = ignorarEspacosOuComentarios(linhaCodigo);
            if (m != null) {
                coluna += m.end();
                linhaCodigo = linhaCodigo.substring(m.end()).stripLeading();
                continue;
            }

            if ((m = padraoBooleanos.matcher(linhaCodigo)).lookingAt()) {
                tabela.addToken(new Token(valorBooleano(m.group()), "CONST", "boolean", linha, coluna));
                houveReconhecimento = true;
            } else if ((m = padraoStrings.matcher(linhaCodigo)).lookingAt()) {
                tabela.addToken(new Token(m.group(), "CONST", "string", linha, coluna));
                houveReconhecimento = true;
            } else if ((m = padraoHexa.matcher(linhaCodigo)).lookingAt()) {
                tabela.addToken(new Token(m.group(), "CONST", "byte", linha, coluna));
                houveReconhecimento = true;
            } else if ((m = padraoNumeros.matcher(linhaCodigo)).lookingAt()) {
                tabela.addToken(new Token(m.group(), "CONST", "int", linha, coluna));
                houveReconhecimento = true;
            } else if ((m = verificarIdentificadorOuSimbolo(linhaCodigo)) != null) {
                String lexema = m.group();
                String categoria = tabela.isPalavraReservada(lexema) ? "RESERVADA" : "ID";

                tabela.addToken(new Token(lexema, categoria, "NULL", linha, coluna));
                houveReconhecimento = true;
            } else {
                ErroLexico.gerarErro(String.valueOf(linhaCodigo.charAt(0)), linha, coluna);
            }

            if (houveReconhecimento) {
                int tamConsumido = m.end();
                coluna += tamConsumido;
                linhaCodigo = linhaCodigo.substring(tamConsumido).stripLeading();
            }
        }
    }

    // Ignora espaços e comentários no código
    private Matcher ignorarEspacosOuComentarios(String codigo) {
        Matcher m = padraoEspacos.matcher(codigo);
        if (m.lookingAt()) return m;
        m = padraoComentarios.matcher(codigo);
        return m.lookingAt() ? m : null;
    }

    // Identifica identificadores, operadores ou delimitadores
    private Matcher verificarIdentificadorOuSimbolo(String codigo) {
        Matcher m = padraoIdentificadores.matcher(codigo);
        if (m.lookingAt()) return m;
        m = padraoOperadores.matcher(codigo);
        if (m.lookingAt()) return m;
        m = padraoDelimitadores.matcher(codigo);
        return m.lookingAt() ? m : null;
    }

    // Converte "true" e "false" para seus equivalentes hexadecimais
    private String valorBooleano(String valor) {
        return valor.equals("true") ? "0hFFFF" : "0h0000";
    }
}
