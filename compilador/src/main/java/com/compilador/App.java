package com.compilador;
import com.compilador.Lexica.Token;
import com.compilador.Table.TabelaSimbolos;


public class App 
{
    public static void main( String[] args )
    {
        // Obtém o caminho do arquivo a partir dos argumentos
        String arquivo = "C:\\Users\\andre\\Documents\\GitHub\\compilador\\compilador\\src\\main\\java\\com\\compilador\\Codigos\\Main.lc";
        System.out.println("Lendo o arquivo: " + arquivo);
        
        TabelaSimbolos tabelaSimbolos = new TabelaSimbolos(); // Cria uma nova tabela de símbolos
        
        tabelaSimbolos.addToken(new Token("if", "palavra-chave", "condicional", 1, 5));
    }
}
