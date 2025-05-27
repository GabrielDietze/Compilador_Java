package com.compilador.Utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;


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

    
    
}
