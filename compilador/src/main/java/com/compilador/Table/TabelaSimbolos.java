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

    public Token currentToken(int index){
        return this.tabelaSimbolos.get(index);
    }

    public void addToken(Token token) {
        this.tabelaSimbolos.add(token);
    }

    public boolean isPalavraReservada(String palavra) {
        return palavrasReservadas.contains(palavra);
    }

    public int getSize() {
        return this.tabelaSimbolos.size();
    }

    public void printSimbolos() {
        for (Token token : this.tabelaSimbolos) {
            System.out.println(token.toString());
        }
    }
}
