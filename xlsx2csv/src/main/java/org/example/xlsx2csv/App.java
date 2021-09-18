package org.example.xlsx2csv;

import java.awt.Container;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.openxml4j.util.ZipSecureFile;

public class App implements ActionListener {

  JFrame frame = new JFrame("Xlsx2Csv-Example");// 框架布局
  JTabbedPane tabPane = new JTabbedPane();// 选项卡布局
  Container con = new Container();//
  JLabel sourceLabel = new JLabel("源目录");
  JLabel targetLabel2 = new JLabel("输出目录1");
  JLabel targetLabel3 = new JLabel("输出目录2");
  JTextField sourcePathText = new JTextField();// TextField 目录的路径
  JTextField targetPathText1 = new JTextField();// 文件的路径
  JTextField targetPathText2 = new JTextField();// 文件的路径
  JButton sourceButton1 = new JButton("...");// 选择
  JButton targetButton2 = new JButton("...");// 选择
  JButton targetButton3 = new JButton("...");// 选择
  JFileChooser jfc = new JFileChooser();// 文件选择器
  JButton action = new JButton("确定");//

  JTextArea textArea = new JTextArea();
  JScrollPane scrollPane = new JScrollPane(textArea);
  ExecutorService executorService = Executors.newWorkStealingPool();

  App() {
    jfc.setCurrentDirectory(null);// 文件选择器的初始目录定为d盘

    double lx = Toolkit.getDefaultToolkit().getScreenSize().getWidth();

    double ly = Toolkit.getDefaultToolkit().getScreenSize().getHeight();

    frame.setLocation(new Point((int) (lx / 2) - 150, (int) (ly / 2) - 150));// 设定窗口出现位置
    frame.setSize(630, 600);// 设定窗口大小
    frame.setContentPane(tabPane);// 设置布局
    frame.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        super.windowClosing(e);
        executorService.shutdown();
      }
    });
    sourceLabel.setBounds(10, 10, 70, 20);
    sourcePathText.setBounds(75, 10, 320, 20);
    sourceButton1.setBounds(400, 10, 50, 20);
    targetLabel2.setBounds(10, 35, 70, 20);
    targetLabel3.setBounds(10, 60, 70, 20);
    targetPathText1.setBounds(75, 35, 320, 20);
    targetPathText2.setBounds(75, 60, 320, 20);
    targetButton2.setBounds(400, 35, 50, 20);
    targetButton3.setBounds(400, 60, 50, 20);
    action.setBounds(30, 100, 60, 20);
    sourceButton1.addActionListener(this); // 添加事件处理
    targetButton2.addActionListener(this); // 添加事件处理
    targetButton3.addActionListener(this); // 添加事件处理
    action.addActionListener(this); // 添加事件处理

    textArea.setLineWrap(true);
    scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    scrollPane.setBounds(10, 130, 600, 300);

    con.add(scrollPane);
    con.add(sourceLabel);
    con.add(sourcePathText);
    con.add(sourceButton1);
    con.add(targetLabel2);
    con.add(targetPathText1);
    con.add(targetButton2);
    con.add(targetLabel3);
    con.add(targetPathText2);
    con.add(targetButton3);
    con.add(action);
    frame.setVisible(true);// 窗口可见
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);// 使能关闭窗口，结束程序
    tabPane.add("1面板", con);// 添加布局1
  }

  /**
   * 时间监听的方法
   */
  public void actionPerformed(ActionEvent e) {
    openFileChooser(e, sourceButton1, JFileChooser.FILES_AND_DIRECTORIES, sourcePathText);

    // 绑定到选择文件，先择文件事件
    openFileChooser(e, targetButton2, JFileChooser.DIRECTORIES_ONLY, targetPathText1);

    // 绑定到选择文件，先择文件事件
    openFileChooser(e, targetButton3, JFileChooser.DIRECTORIES_ONLY, targetPathText2);

    if (e.getSource().equals(action)) {
      textArea.selectAll();
      textArea.replaceSelection("");
      // 弹出对话框可以改变里面的参数具体得靠大家自己去看，时间很短
      try {
        xlsx2csv();
      } catch (Exception exception) {
        //暂时无视
      }
    }
  }

  private void openFileChooser(ActionEvent e, JButton sourceButton1, int filesAndDirectories,
      JTextField sourcePathText) {
    if (e.getSource().equals(sourceButton1)) {// 判断触发方法的按钮是哪个
      jfc.setFileSelectionMode(filesAndDirectories);
      int state = jfc.showOpenDialog(null);// 此句是打开文件选择器界面的触发语句
      if (state != 1) {

        File f = jfc.getSelectedFile();// f为选择到的目录
        sourcePathText.setText(f.getAbsolutePath());
      }
    }
  }

  public void xlsx2csv() throws Exception {
    Xlsx2csv xlsx2csv = new Xlsx2csv();

    xlsx2csv.mus(true);
    ZipSecureFile.setMinInflateRatio(0);

    File f = new File(sourcePathText.getText());
    if (!f.exists()) {
      return;
    }

    File[] files = null;
    if (f.isDirectory()) {
      files = f.listFiles();
    } else {
      files = new File[]{f};
    }

    List<File> targetDirectors = new ArrayList<>();
    for (String s : Arrays.asList(targetPathText1.getText(), targetPathText2.getText())) {
      if (StringUtils.isNoneBlank(s)) {
        targetDirectors.add(new File(s));
      }
    }

    if (files != null) {
      List<File> xlsxFile = Stream.of(files).filter(xlsx2csv::filter)
          .collect(Collectors.toList());
      AtomicInteger integer = new AtomicInteger(xlsxFile.size());
      for (File file : xlsxFile) {
        if (xlsx2csv.filter(file)) {
          executorService.execute(() -> {
            String result = xlsx2csv.toCsv(file, targetDirectors);
            int count = integer.decrementAndGet();
            synchronized (textArea) {
              textArea.append(result);
              if (count <= 0) {
                textArea.append("转换完成\n");
              }
            }
          });
        }
      }
    }
  }

  public static void main(String[] args) {
    new App();
  }

}
