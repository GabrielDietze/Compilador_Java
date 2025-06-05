package com.compilador.Exceptions;

import com.compilador.Lexica.Token;

public class ErroSintatico {
      public static void erroSintatico(String esperado, Token encontrado) throws ExcecaoCompilador {
        throw new ExcecaoCompilador(mensagemErroSintatico(esperado, encontrado));
    }

    public static void erroSintaticoAtribuicao(Token encontrado) throws ExcecaoCompilador {
        throw new ExcecaoCompilador(mensagemErroSintaticoAtribuicao(encontrado));
    }
    
   private static String mensagemErroSintatico(String esperado, Token encontrado) {
        return "Erro Sintático: era esperado '" + esperado + "', mas encontrado '" +
                encontrado.getNome() + "' na linha " + encontrado.getLinha() + ", coluna " + encontrado.getColuna() + ".";
    }

    private static String mensagemErroSintaticoAtribuicao(Token encontrado) {
        return "Erro Sintático: era esperada uma atribuição de valor (de variável ou literal), mas encontrado '" +
                encontrado.getNome() + "' na linha " + encontrado.getLinha() + ", coluna " + encontrado.getColuna() + ".";
    }
}
