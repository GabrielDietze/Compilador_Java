package com.compilador.Execptions;

public class ExcecaoCompilador extends Exception {
    private static final long serialVersionUID = 1L;

    public ExcecaoCompilador(String mensagem) {
        super(mensagem);
    }

    public ExcecaoCompilador(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }

    public ExcecaoCompilador(Throwable causa) {
        super(causa);
    }
    
}
