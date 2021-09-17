package org.example.xlsx2csv;

import java.io.File;
import java.util.stream.Stream;
import org.apache.poi.openxml4j.util.ZipSecureFile;

public class App {


  public static void main(String[] args) throws Exception {
    Xlsx2csv xlsx2csv = new Xlsx2csv();

    if (args[0].equals("mus")) {
      xlsx2csv.mus(true);
    }

    ZipSecureFile.setMinInflateRatio(0);
    for (String arg : args) {
      File f = new File(arg);
      if (!f.exists()) {
        continue;
      }

      File[] files = null;
      if (f.isDirectory()) {
        files = f.listFiles();
      } else {
        files = new File[]{f};
      }

      if (files != null) {
        Stream.of(files)
            .parallel()
            .filter(xlsx2csv::filter)
            .forEach(xlsx2csv::toCsv);
      }
    }
  }
}
