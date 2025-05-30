package com.compilador.Execptions;


public class ErroLexico {

    public static void erroLexico(String lexema, int linha, int coluna) throws ExcecaoCompilador {
        throw new ExcecaoCompilador(mensagemErroLexico(lexema, linha, coluna));
    }


    // MÉTODOS AUXILIARES (mensagens)

    private static String mensagemErroLexico(String lexema, int linha, int coluna) {
        return "Erro Léxico: token não reconhecido '" + lexema + "' na linha " + linha + ", coluna " + coluna + ".";
    }

 
}
