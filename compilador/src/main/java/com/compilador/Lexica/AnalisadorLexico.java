package com.compilador.Lexica;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.compilador.Execptions.ErroLexico;
import com.compilador.Execptions.ExcecaoCompilador;
import com.compilador.Table.TabelaSimbolos;
// Se você tiver uma classe Token, importe-a aqui. Ex:
// import com.compilador.Table.Token; // ou o caminho correto para sua classe Token

public class AnalisadorLexico {

    private final TabelaSimbolos tabela;

    private final Pattern padraoNumeros = Pattern.compile("\\d+");
    private final Pattern padraoHexa = Pattern.compile("0h[a-fA-F0-9]{4}");
    private final Pattern padraoIdentificadores = Pattern.compile("[a-zA-Z_]\\w*");
    private final Pattern padraoBooleanos = Pattern.compile("true|false");
    private final Pattern padraoOperadores = Pattern.compile("==|<>|<=|>=|<|>|[+\\-*/=]");
    private final Pattern padraoDelimitadores = Pattern.compile("[,;()]");
    private final Pattern padraoComentarios = Pattern.compile("/\\*(.|\\R)*?\\*/|\\{[^}]*\\}|//.*");
    private final Pattern padraoStrings = Pattern.compile("\"([^\"\\\\]|\\\\.)*\"");
    private final Pattern padraoEspacos = Pattern.compile("\\s+");

    public AnalisadorLexico(TabelaSimbolos tabela) {
        this.tabela = tabela;
    }

    public void executarAnalise(String linhaCodigo, int linha) throws ExcecaoCompilador {
        int coluna = 1;
        // linhaCodigo = linhaCodigo.stripLeading(); // stripLeading no início da linha pode ser suficiente

        System.out.println("Analisando linha " + linha + ": " + linhaCodigo);

        String codigoRestante = linhaCodigo.stripLeading(); // Processa a linha a partir daqui

        while (!codigoRestante.isEmpty()) {
            Matcher m;
            boolean houveReconhecimento = false;
            int colunaInicioToken = linhaCodigo.length() - codigoRestante.length() + 1;


            m = ignorarEspacosOuComentarios(codigoRestante);
            if (m != null) {
                System.out.println("Ignorando espaços ou comentário: '" + codigoRestante.substring(0, m.end()) + "'");
                codigoRestante = codigoRestante.substring(m.end()).stripLeading();
                continue;
            }

            if ((m = padraoBooleanos.matcher(codigoRestante)).lookingAt()) {
                String lexemaOriginal = m.group();
                String valorHex = valorBooleano(lexemaOriginal);
                System.out.println("Token booleano encontrado: " + lexemaOriginal + " -> " + valorHex);
                // Assumindo construtor Token(nome, tipo, classificacao, linha, coluna)
                tabela.addToken(new Token(valorHex, "CONST", "boolean", linha, colunaInicioToken));
                houveReconhecimento = true;
            } else if ((m = padraoStrings.matcher(codigoRestante)).lookingAt()) {
                String lexema = m.group();
                System.out.println("Token string encontrado: " + lexema);
                tabela.addToken(new Token(lexema, "CONST", "string", linha, colunaInicioToken));
                houveReconhecimento = true;
            } else if ((m = padraoHexa.matcher(codigoRestante)).lookingAt()) {
                String lexema = m.group();
                System.out.println("Token hexadecimal encontrado: " + lexema);
                tabela.addToken(new Token(lexema, "CONST", "byte", linha, colunaInicioToken));
                houveReconhecimento = true;
            } else if ((m = padraoNumeros.matcher(codigoRestante)).lookingAt()) {
                String lexema = m.group();
                System.out.println("Token número encontrado: " + lexema);
                tabela.addToken(new Token(lexema, "CONST", "int", linha, colunaInicioToken));
                houveReconhecimento = true;
            } else if ((m = verificarIdentificadorOuSimbolo(codigoRestante)) != null) {
                String lexema = m.group();
                String categoria = tabela.palavraReservada(lexema) ? "RESERVADA" : "ID";
                System.out.println("Token identificado: " + lexema + " Categoria: " + categoria);
                // LINHA CORRIGIDA:
                tabela.addToken(new Token(lexema, categoria, categoria, linha, colunaInicioToken));
                houveReconhecimento = true;
            } else {
                // Calcula a coluna do erro com base no código original da linha
                ErroLexico.erroLexico(String.valueOf(codigoRestante.charAt(0)), linha, colunaInicioToken);
            }

            if (houveReconhecimento) {
                int tamConsumido = m.end();
                codigoRestante = codigoRestante.substring(tamConsumido).stripLeading();
            } else if (codigoRestante.isEmpty()) { // Evita loop infinito se nada for reconhecido mas a linha não estiver vazia (deve ser tratado pelo erro lexico)
                break; 
            }
        }
    }

    // Ignora espaços e comentários no código
    private Matcher ignorarEspacosOuComentarios(String codigo) {
        Matcher m = padraoEspacos.matcher(codigo);
        if (m.lookingAt()) {
            return m;
        }
        m = padraoComentarios.matcher(codigo);
        return m.lookingAt() ? m : null;
    }

    // Identifica identificadores, operadores ou delimitadores
    private Matcher verificarIdentificadorOuSimbolo(String codigo) {
        Matcher m = padraoIdentificadores.matcher(codigo);
        if (m.lookingAt()) {
            return m;
        }
        m = padraoOperadores.matcher(codigo);
        if (m.lookingAt()) {
            return m;
        }
        m = padraoDelimitadores.matcher(codigo);
        return m.lookingAt() ? m : null;
    }

    // Converte "true" e "false" para seus equivalentes hexadecimais
    private String valorBooleano(String valor) {
        return valor.equals("true") ? "0hFFFF" : "0h0000";
    }
}