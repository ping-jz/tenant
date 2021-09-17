package org.example.xlsx2csv;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

public class Xlsx2csv {

  private boolean mus;

  public boolean ismus() {
    return mus;
  }

  public Xlsx2csv mus(boolean mus) {
    this.mus = mus;
    return this;
  }

  public boolean filter(File f) {
    return f != null && f.exists() && f.getName().endsWith(".xlsx");
  }

  public void toCsv(File f) {
    String fileName = getFileName(f);

    long start = System.currentTimeMillis();
    try (Workbook workbook = WorkbookFactory.create(f)) {
      workbook.getNumberOfSheets();
      for (Sheet sheet : workbook) {
        handleSheet(f, fileName, sheet);
        //only handle the first sheet
        if (ismus()) {
          break;
        }
      }
      System.out.format("转换%s, 耗时约:%s\n", fileName,
          System.currentTimeMillis() - start);
    } catch (Exception e) {
      System.out.format("%s  \n", fileName);
      e.printStackTrace();
    }
  }

  private void handleSheet(File f, String fileName, Sheet sheet) throws IOException {
    int maxCell = 0;
    for (Row r : sheet) {
      maxCell = Math.max(maxCell, r.getLastCellNum());
    }

    int rowNum = 0, cellNum = 0;
    DataFormatter dataFormat = new DataFormatter(true);
    String csvFileName = f.getParent() + File.separator + getCsvFileName(fileName, sheet) + ".csv";
    try (CSVPrinter printer = new CSVPrinter(
        new OutputStreamWriter(new FileOutputStream(csvFileName), StandardCharsets.UTF_8),
        CSVFormat.EXCEL)) {
      Iterator<Row> rowIterator = sheet.iterator();
      while (rowIterator.hasNext()) {
        Row r = rowIterator.next();
        rowNum = r.getRowNum();
        for (int c = 0; c < maxCell; c++) {
          cellNum = c;
          Cell cell = r.getCell(c);
          if (cell != null) {
            switch (cell.getCellType()) {
              case NUMERIC: {
                double doubleValue = cell.getNumericCellValue();
                int intValue = (int) doubleValue;
                final boolean hasDecimal = doubleValue - intValue != 0;
                if (hasDecimal) {
                  printer.print(doubleValue);
                } else {
                  printer.print(intValue);
                }
              }
              break;
              case STRING: {
                String s = cell.getStringCellValue();
                printer.print(StringUtils.isNoneBlank(s) ? s : null);
              }
              break;
              case FORMULA:{
                cell.getCachedFormulaResultType();
              }
              default: {
                printer.print(cell);
              }
              break;
            }

          } else {
            printer.print(null);
          }
        }

        if (rowIterator.hasNext()) {
          printer.println();
        }
      }
      printer.flush();
    } catch (Exception e) {
      System.out.format("%s_%s, 第%s行第%s列 转换错误\n", fileName, sheet.getSheetName(), rowNum, cellNum);
      e.printStackTrace();
    }
  }

  private String getCsvFileName(String fileName, Sheet sheet) {
    if (ismus()) {
      int idx = fileName.indexOf("-");
      return fileName.substring(0, idx).toLowerCase();
    } else {
      return (fileName + sheet.getSheetName()).toLowerCase();
    }
  }


  private String getFileName(File f) {
    String fileName = f.getName();
    fileName = fileName.substring(0, fileName.lastIndexOf("."));
    return fileName;
  }

}
