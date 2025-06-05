package com.compilador.Table;
import  java.util.ArrayList;
import java.util.Set;

import com.compilador.Lexica.Token;

public class TabelaSimbolos {
    
    private static final Set<String> palavrasReservadas = Set.of(
        "final", "int", "byte", "string", "while", "if", "else",
        "and", "or", "not", "==", "=", "(", ")",
        "<", ">", "<>", ">=", "<=", ",", "+",
        "-", "*", "/", ";", "begin", "end", "readln",
        "write", "writeln", "true", "false", "boolean");

    private final ArrayList<Token> tabelaSimbolos = new ArrayList<>(); // Lista de tokens


    public ArrayList<Token> getTabelaSimbolos() {
    return this.tabelaSimbolos;
}

    public Token tokenAtual(int index){
        return this.tabelaSimbolos.get(index);
    }

    public void addToken(Token token) {
        this.tabelaSimbolos.add(token);
    }

    public boolean palavraReservada(String palavra) {
        return palavrasReservadas.contains(palavra);
    }

    public int tamanho() {
        return this.tabelaSimbolos.size();
    }

    public void subistituirToken(int index, Token token) {
        if (index >= 0 && index < this.tabelaSimbolos.size()) {
            this.tabelaSimbolos.set(index, token);
        } else {
            throw new IndexOutOfBoundsException("Índice fora dos limites da tabela de símbolos.");
        }
    }


    public void printSimbolos() {
        for (Token token : this.tabelaSimbolos) {
            System.out.println(token.toString());
        }
    }
}
