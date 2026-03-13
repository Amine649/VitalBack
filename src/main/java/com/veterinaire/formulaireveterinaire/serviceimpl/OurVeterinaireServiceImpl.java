package com.veterinaire.formulaireveterinaire.serviceimpl;

import com.veterinaire.formulaireveterinaire.DAO.OurVeterinaireRepository;
import com.veterinaire.formulaireveterinaire.entity.OurVeterinaire;
import com.veterinaire.formulaireveterinaire.service.OurVeterinaireService;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

@Service
public class OurVeterinaireServiceImpl implements OurVeterinaireService {
    private static final Logger logger = LoggerFactory.getLogger(OurVeterinaireServiceImpl.class);

    private final OurVeterinaireRepository ourVeterinaireRepository;

    public OurVeterinaireServiceImpl(OurVeterinaireRepository ourVeterinaireRepository) {
        this.ourVeterinaireRepository = ourVeterinaireRepository;
    }



    @Override
    public void uploadExcel(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            logger.error("Le fichier fourni est vide ou null.");
            throw new IllegalArgumentException("Le fichier Excel est vide ou non fourni.");
        }

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null || sheet.getPhysicalNumberOfRows() == 0) {
                logger.error("Le fichier Excel est vide ou la feuille n'existe pas.");
                throw new IllegalArgumentException("Le fichier Excel est vide ou la feuille n'existe pas.");
            }

            // Validate header row
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                logger.error("La première ligne (en-tête) est manquante.");
                throw new IllegalArgumentException("La première ligne (en-tête) est manquante.");
            }

            // Expected column names
            String[] expectedHeaders = {"nom", "matricule", "email"};
            boolean headersValid = validateHeaders(headerRow, expectedHeaders);

            if (!headersValid) {
                logger.error("Les colonnes de l'en-tête ne correspondent pas à 'nom', 'matricule'.");
                throw new IllegalArgumentException("Les colonnes de l'en-tête doivent être exactement 'nom', 'matricule'.");
            }

            // Process data rows
            for (Row row : sheet) {
                if (row == null || row.getRowNum() == 0) continue; // Skip header or null rows

                // Get cells with null check
                Cell nomCell = row.getCell(0, Row.MissingCellPolicy.RETURN_NULL_AND_BLANK);
                Cell matriculeCell = row.getCell(1, Row.MissingCellPolicy.RETURN_NULL_AND_BLANK);
                Cell emailCell = row.getCell(2, Row.MissingCellPolicy.RETURN_NULL_AND_BLANK);

                // Handle different cell types
                String nom = getCellValueAsString(nomCell).trim();
                String matricule = getCellValueAsString(matriculeCell).trim();
                String email = getCellValueAsString(emailCell).trim();

                // Check for null or empty values
                if (nomCell == null  || matriculeCell == null || emailCell == null
                        || nom.isEmpty()  || matricule.isEmpty() || email.isEmpty()) {

                    String errorMessage = String.format(
                            "Ligne %d est incomplète : tous les champs (nom, matricule, email) doivent être remplis.",
                            row.getRowNum() + 1
                    );
                    logger.error(errorMessage);
                    throw new InvalidExcelFileException(errorMessage);
                }

                Optional<OurVeterinaire> existing = ourVeterinaireRepository.findByMatricule(matricule);
                if (existing.isPresent()) {
                    // Update existing
                    OurVeterinaire vet = existing.get();
                    vet.setNom(nom);
                    vet.setEmail(email);
                    ourVeterinaireRepository.save(vet);
                    logger.info("Mise à jour de OurVeterinaire pour matricule: {} (Ligne {})", matricule, row.getRowNum() + 1);
                } else {
                    // Create new
                    OurVeterinaire vet = new OurVeterinaire();
                    vet.setNom(nom);
                    vet.setMatricule(matricule);
                    vet.setEmail(email);
                    ourVeterinaireRepository.save(vet);
                    logger.info("Création de OurVeterinaire pour matricule: {} (Ligne {})", matricule, row.getRowNum() + 1);
                }
            }
        } catch (IOException e) {
            logger.error("Erreur lors du traitement du fichier Excel: {}", e.getMessage(), e);
            String userMessage = determineUserFriendlyMessage(e);
            throw new InvalidExcelFileException(userMessage, e);
        }
    }

    // Helper method to validate headers
    private boolean validateHeaders(Row headerRow, String[] expectedHeaders) {
        if (headerRow.getPhysicalNumberOfCells() < expectedHeaders.length) {
            return false;
        }

        for (int i = 0; i < expectedHeaders.length; i++) {
            Cell cell = headerRow.getCell(i, Row.MissingCellPolicy.RETURN_NULL_AND_BLANK);
            String headerValue = getCellValueAsString(cell).trim().toLowerCase();
            if (!headerValue.equalsIgnoreCase(expectedHeaders[i])) {
                return false;
            }
        }
        return true;
    }

    // Helper method to convert cell value to string
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue() != null ? cell.getStringCellValue() : "";
            case NUMERIC:
                if (cell.getColumnIndex() == 2) { // Matricule column
                    return String.format("%.0f", cell.getNumericCellValue());
                }
                return String.valueOf(cell.getNumericCellValue());
            case BLANK:
                return "";
            default:
                return "";
        }
    }

    // Custom exception class
    public class InvalidExcelFileException extends Exception {
        public InvalidExcelFileException(String message) {
            super(message);
        }

        public InvalidExcelFileException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    // Determine user-friendly message for IOException
    private String determineUserFriendlyMessage(IOException e) {
        String message = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        if (message.contains("invalidformat")) {
            return "Le fichier fourni n'est pas un fichier Excel valide (format XLSX requis).";
        } else if (message.contains("stream closed")) {
            return "Erreur lors de la lecture du fichier : flux fermé.";
        } else {
            return "Erreur lors du traitement du fichier Excel. Veuillez vérifier le format et réessayer.";
        }
    }

    @Override
    public List<OurVeterinaire> getAllVeterinaires() {
        List<OurVeterinaire> veterinaires = ourVeterinaireRepository.findAll();
        logger.info("Retrieved {} veterinaires from database", veterinaires.size());

        // Throw if empty (uncomment if you prefer 404 on empty)
        if (veterinaires.isEmpty()) {
            throw new RuntimeException("Aucun vétérinaire trouvé.");
        }

        return veterinaires;
    }
}
