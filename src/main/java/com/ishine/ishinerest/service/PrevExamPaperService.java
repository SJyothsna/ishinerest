package com.ishine.ishinerest.service;

import com.ishine.ishinerest.entity.PrevExamPaper;
import com.ishine.ishinerest.pojo.PrevExamPaperResponse;
import com.ishine.ishinerest.repository.PrevExamPaperRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PrevExamPaperService {

    private final PrevExamPaperRepository paperRepository;

    public PrevExamPaperService(PrevExamPaperRepository paperRepository) {
        this.paperRepository = paperRepository;
    }

    public PrevExamPaper savePaper(PrevExamPaper paper) {
        return paperRepository.save(paper);
    }

    public List<PrevExamPaper> getAllPapers() {
        return paperRepository.findAll();
    }

    public void processExcelUpload(MultipartFile file) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheet("Prev_Exam_Papers");
            if (sheet == null) {
                throw new IllegalArgumentException("Sheet 'Prev_Exam_Papers' not found.");
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) { // Skip header row
                Row row = sheet.getRow(i);
                if (row == null) continue;

                PrevExamPaper paper = new PrevExamPaper();
                paper.setExamId(getStringValue(row.getCell(0)));
                paper.setExamYear((int) getNumericValue(row.getCell(1)));
                paper.setQuestionPaper(getStringValue(row.getCell(2)));
                paper.setMarkingScheme(getStringValue(row.getCell(3)));
                paper.setPaperType(getStringValue(row.getCell(4)));
                paper.setSubjectId(getStringValue(row.getCell(5)));
                paper.setTitle(getStringValue(row.getCell(6)));

                paperRepository.save(paper);
            }
        }
    }
        private double getNumericValue (Cell cell){
            return cell != null && cell.getCellType() == CellType.NUMERIC
                    ? cell.getNumericCellValue()
                    : Double.parseDouble(cell.getStringCellValue());
        }

        private String getStringValue (Cell cell){
            return cell != null
                    ? (cell.getCellType() == CellType.STRING
                    ? cell.getStringCellValue()
                    : String.valueOf((int) cell.getNumericCellValue()))
                    : "";
        }
    public List<PrevExamPaperResponse> getPaperDetailsBySubjectId(String subjectId) {
        return paperRepository.findPaperDetailsBySubjectId(subjectId);
    }


}
