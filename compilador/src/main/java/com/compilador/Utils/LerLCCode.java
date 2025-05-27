package com.compilador.Utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import com.compilador.Lexica.Analisador;
import com.compilador.Execptions.ExcecaoCompilador; 


public class LerLCCode {

    public String lerArquivo(String caminho) {
        StringBuilder conteudo = new StringBuilder();

        try (BufferedReader br = new BufferedReader(new FileReader(caminho))) {
            String linha;
            while ((linha = br.readLine()) != null) {
                conteudo.append(linha).append(System.lineSeparator());
            }
        } catch (IOException e) {
            System.err.println("Erro ao ler o arquivo: " + e.getMessage());
        }
        return conteudo.toString();
    }

    public void AnalisarArquivo(String filePath, Analisador analisador) throws  ExcecaoCompilador {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            int lineNumber = 1;

            while ((line = reader.readLine()) != null) {
                // Remove espaços em branco à direita para evitar problemas de coluna
                line = line.stripTrailing();

                // Envia a linha para o analisador léxico
                analisador.analyze(line, lineNumber);

                // Incrementa o número da linha
                lineNumber++;
            }
        } catch (IOException e) {
            System.err.println("Erro ao ler o arquivo: " + e.getMessage());
        }
    }
    
    
}
