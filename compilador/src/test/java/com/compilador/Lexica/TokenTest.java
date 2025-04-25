package com.compilador.Lexica;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class TokenTest {

    @Test
    public void testTokenConstructorAndGetters() {
        Token token = new Token("if", "palavra-chave", "condicional", 1, 5);

        assertEquals("if", token.getNome());
        assertEquals("palavra-chave", token.getTipo());
        assertEquals("condicional", token.getClassificacao());
        assertEquals(1, token.getLinha());
        assertEquals(5, token.getColuna());
    }

    @Test
    public void testToString() {
        Token token = new Token("while", "palavra-chave", "repeticao", 2, 3);

        String esperado = "Nome: while, Classificacao: repeticao, Tipo: palavra-chave, Linha: 2, Coluna: 3]";
        assertEquals(esperado, token.toString());
    }
}
