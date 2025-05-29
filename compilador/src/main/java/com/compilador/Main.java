package com.compilador;

import com.compilador.Lexica.AnalisadorLexico;
import com.compilador.Semantica.AnalisadorSemantico;
import com.compilador.Sintatica.AnalisadorSintatico;
import com.compilador.Table.TabelaSimbolos;
import com.compilador.Utils.LerLCCode;

public class Main {
    
    public static void main(String[] args) {
        // Caminho fixo do arquivo para teste
        String caminhoArquivo = "C:\\Users\\Gabri\\Desktop\\COMPILADOR_JAVA\\compilador\\src\\main\\java\\com\\compilador\\Codigos\\Main.lc";
        System.out.println("Abrindo arquivo: " + caminhoArquivo);

        // // Extrai nome do arquivo (sem extensão) para uso posterior
        // String nomeArquivo = caminhoArquivo.substring(caminhoArquivo.lastIndexOf("\\") + 1);
        // nomeArquivo = nomeArquivo.substring(0, nomeArquivo.lastIndexOf("."));
        
        // Instancia tabela de símbolos
        TabelaSimbolos tabela = new TabelaSimbolos();

        try {
            // Lê o arquivo e executa a análise léxica
            LerLCCode  leitor = new LerLCCode();
            AnalisadorLexico lexer  =  new AnalisadorLexico(tabela);
            System.out.println("==> Iniciando análise léxica...");
            leitor.AnalisarArquivo(caminhoArquivo, lexer);
            System.out.println("Análise léxica finalizada.");

            // Realiza análise sintática
            AnalisadorSintatico parser = new AnalisadorSintatico(tabela);
            System.out.println("==> Iniciando análise sintática...");
            parser.analisarPrograma();
            System.out.println("Análise sintática finalizada.");

            // Realiza análise semântica
            AnalisadorSemantico semantico = new AnalisadorSemantico(tabela);
            System.out.println("==> Iniciando análise semântica...");
            semantico.executarVerificacaoSemanticaCompleta();
            System.out.println("Análise semântica finalizada.");

            // // Gera código assembly
            // GeradorAssembly gerador = new GeradorAssembly(tabela, nomeArquivo);
            // System.out.println("==> Gerando código assembly...");
            // gerador.gerar();
            // System.out.println("Código assembly gerado com sucesso!");

        } catch (Exception e) {
            System.err.println("Erro durante a compilação: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Opcional: mostrar tabela de símbolos no final
        System.out.println("\nTabela de símbolos final:");
        tabela.printSimbolos();
    }
}
