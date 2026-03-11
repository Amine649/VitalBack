package com.veterinaire.formulaireveterinaire;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FormulaireVeterinaireApplication {

    public static void main(String[] args) {
        SpringApplication.run(FormulaireVeterinaireApplication.class, args);
        System.out.println("Application démarrée avec succès !");

    }

}
