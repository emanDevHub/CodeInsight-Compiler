import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.Files;
import java.util.*;

public class SimpleScannerGUI extends JFrame implements ActionListener {

    private JButton openFileBtn, clearBtn, saveBtn, scanBtn, parseBtn, sampleBtn;
    private JTable tokenTable;
    private DefaultTableModel tableModel;
    private JTextArea codeArea;
    private final ArrayList<Token> tokenList = new ArrayList<>();
    private JLabel fileLabel, statusLabel;
    private JLabel totalLbl, keywordLbl, idLbl, numLbl, opLbl, invalidLbl;

    private static final Set<String> GRAMMAR_KEYWORDS = new HashSet<>(Arrays.asList(
        "public", "private", "static", "class",
        "byte", "short", "int", "long", "void",
        "if", "return", "this"
    ));

    public SimpleScannerGUI() {
        setTitle("Java Scanner & Parser");
        setSize(1200, 750);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(21, 101, 192));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));

        JLabel titleLabel = new JLabel("Scanner & Parser Project");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));

        JLabel subTitle = new JLabel("Lexical Analysis + Syntax Validation (Assignment Grammar)");
        subTitle.setForeground(new Color(227, 242, 253));
        subTitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        JPanel titlePanel = new JPanel(new GridLayout(2, 1));
        titlePanel.setOpaque(false);
        titlePanel.add(titleLabel);
        titlePanel.add(subTitle);
        headerPanel.add(titlePanel, BorderLayout.WEST);
        add(headerPanel, BorderLayout.NORTH);

        JPanel controlPanel = new JPanel(new BorderLayout());
        controlPanel.setBackground(new Color(245, 245, 245));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        buttonPanel.setBackground(new Color(245, 245, 245));

        openFileBtn = new JButton("Open File");
        sampleBtn = new JButton("Load Sample");
        scanBtn = new JButton("Scan");
        parseBtn = new JButton("Parse");
        clearBtn = new JButton("Clear");
        saveBtn = new JButton("Save Tokens");

        styleButton(openFileBtn, new Color(46, 125, 50));
        styleButton(sampleBtn, new Color(0, 121, 107));
        styleButton(scanBtn, new Color(2, 136, 209));
        styleButton(parseBtn, new Color(156, 39, 176));
        styleButton(clearBtn, new Color(230, 81, 0));
        styleButton(saveBtn, new Color(123, 31, 162));

        openFileBtn.addActionListener(this);
        sampleBtn.addActionListener(this);
        scanBtn.addActionListener(this);
        parseBtn.addActionListener(this);
        clearBtn.addActionListener(this);
        saveBtn.addActionListener(this);

        fileLabel = new JLabel("No file selected");
        fileLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));

        buttonPanel.add(openFileBtn);
        buttonPanel.add(sampleBtn);
        buttonPanel.add(scanBtn);
        buttonPanel.add(parseBtn);
        buttonPanel.add(clearBtn);
        buttonPanel.add(saveBtn);
        buttonPanel.add(Box.createHorizontalStrut(16));
        buttonPanel.add(fileLabel);
        controlPanel.add(buttonPanel, BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(520);

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(BorderFactory.createTitledBorder("Source Code"));
        codeArea = new JTextArea();
        codeArea.setFont(new Font("Consolas", Font.PLAIN, 15));
        codeArea.setTabSize(4);
        codeArea.setBackground(new Color(250, 250, 250));
        codeArea.setMargin(new Insets(10, 10, 10, 10));
        leftPanel.add(new JScrollPane(codeArea), BorderLayout.CENTER);

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createTitledBorder("Token Stream"));

        tableModel = new DefaultTableModel(new String[]{"#", "Token", "Token Type"}, 0);
        tokenTable = new JTable(tableModel);
        tokenTable.setRowHeight(24);
        tokenTable.getColumnModel().getColumn(0).setPreferredWidth(40);
        tokenTable.getColumnModel().getColumn(1).setPreferredWidth(180);
        tokenTable.getColumnModel().getColumn(2).setPreferredWidth(140);

        tokenTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected, boolean hasFocus,
                int row, int col
            ) {
                Component c = super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, col
                );
                String type = (String) table.getValueAt(row, 2);
                if ("Invalid Token".equals(type)) {
                    c.setForeground(new Color(198, 40, 40));
                } else if ("Keyword".equals(type)) {
                    c.setForeground(new Color(21, 101, 192));
                } else if ("Identifier".equals(type)) {
                    c.setForeground(new Color(46, 125, 50));
                } else if ("Number".equals(type)) {
                    c.setForeground(new Color(230, 81, 0));
                } else {
                    c.setForeground(Color.BLACK);
                }
                return c;
            }
        });

        rightPanel.add(new JScrollPane(tokenTable), BorderLayout.CENTER);
        splitPane.setLeftComponent(leftPanel);
        splitPane.setRightComponent(rightPanel);

        JPanel statsPanel = new JPanel(new GridLayout(2, 3, 8, 4));
        statsPanel.setBorder(BorderFactory.createTitledBorder("Statistics"));
        totalLbl = new JLabel("Total: 0");
        keywordLbl = new JLabel("Keywords: 0");
        idLbl = new JLabel("Identifiers: 0");
        numLbl = new JLabel("Numbers: 0");
        opLbl = new JLabel("Operators: 0");
        invalidLbl = new JLabel("Invalid: 0");
        statsPanel.add(totalLbl);
        statsPanel.add(keywordLbl);
        statsPanel.add(idLbl);
        statsPanel.add(numLbl);
        statsPanel.add(opLbl);
        statsPanel.add(invalidLbl);

        statusLabel = new JLabel(" Ready — Scan first, then Parse");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        statusLabel.setOpaque(true);
        statusLabel.setBackground(new Color(238, 238, 238));
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(controlPanel, BorderLayout.NORTH);
        bottomPanel.add(statsPanel, BorderLayout.CENTER);
        bottomPanel.add(statusLabel, BorderLayout.SOUTH);

        add(splitPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
        setVisible(true);
    }

    private void styleButton(JButton button, Color color) {
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == openFileBtn) {
            openFile();
        } else if (e.getSource() == sampleBtn) {
            loadSample();
        } else if (e.getSource() == scanBtn) {
            scan();
        } else if (e.getSource() == parseBtn) {
            parseCode();
        } else if (e.getSource() == clearBtn) {
            clearAll();
        } else if (e.getSource() == saveBtn) {
            saveOutput();
        }
    }

    private void openFile() {
        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File f = fc.getSelectedFile();
                fileLabel.setText("File: " + f.getName());
                codeArea.setText(new String(Files.readAllBytes(f.toPath())));
                statusLabel.setText(" File loaded. Click Scan.");
                statusLabel.setBackground(new Color(238, 238, 238));
            } catch (Exception ex) {
                statusLabel.setText(" Failed to open file");
            }
        }
    }

    private void loadSample() {
        codeArea.setText(
            "public class Demo {\n"
            + "    private static int count;\n"
            + "    public Demo(int x) {\n"
            + "        count = x;\n"
            + "    }\n"
            + "    public void show() {\n"
            + "        if(count < 10) {\n"
            + "            count = count + 1;\n"
            + "        }\n"
            + "        return;\n"
            + "    }\n"
            + "}\n"
        );
        fileLabel.setText("Built-in sample");
        statusLabel.setText(" Sample loaded. Click Scan, then Parse.");
        statusLabel.setBackground(new Color(238, 238, 238));
    }

    private void scan() {
        tableModel.setRowCount(0);
        tokenList.clear();

        int total = 0, kw = 0, id = 0, num = 0, op = 0, punct = 0, invalid = 0;
        String code = codeArea.getText();
        int pos = 0;

        while (pos < code.length()) {
            char c = code.charAt(pos);

            if (Character.isWhitespace(c)) {
                pos++;
                continue;
            }

            if (pos + 1 < code.length()) {
                String two = code.substring(pos, pos + 2);
                if (two.equals("<=") || two.equals(">=") || two.equals("==") || two.equals("!=")) {
                    addTokenRow(++total, two, "Operator");
                    op++;
                    pos += 2;
                    continue;
                }
            }

            if ("{}();,.".indexOf(c) >= 0) {
                addTokenRow(++total, String.valueOf(c), "Punctuation");
                punct++;
                pos++;
                continue;
            }

            if ("+-*/%=<>".indexOf(c) >= 0) {
                addTokenRow(++total, String.valueOf(c), "Operator");
                op++;
                pos++;
                continue;
            }

            if (Character.isDigit(c)) {
                int start = pos;
                while (pos < code.length() && Character.isDigit(code.charAt(pos))) {
                    pos++;
                }
                addTokenRow(++total, code.substring(start, pos), "Number");
                num++;
                continue;
            }

            if (Character.isLetter(c)) {
                int start = pos;
                while (pos < code.length() && Character.isLetterOrDigit(code.charAt(pos))) {
                    pos++;
                }
                String word = code.substring(start, pos);
                if (GRAMMAR_KEYWORDS.contains(word)) {
                    addTokenRow(++total, word, "Keyword");
                    kw++;
                } else {
                    addTokenRow(++total, word, "Identifier");
                    id++;
                }
                continue;
            }

            addTokenRow(++total, String.valueOf(c), "Invalid Token");
            invalid++;
            pos++;
        }

        totalLbl.setText("Total: " + total);
        keywordLbl.setText("Keywords: " + kw);
        idLbl.setText("Identifiers: " + id);
        numLbl.setText("Numbers: " + num);
        opLbl.setText("Operators: " + (op + punct));
        invalidLbl.setText("Invalid: " + invalid);

        if (invalid > 0) {
            statusLabel.setText(" Scan done with " + invalid + " invalid token(s)");
            statusLabel.setBackground(new Color(255, 235, 238));
        } else {
            statusLabel.setText(" Scan completed (" + total + " tokens). Now click Parse.");
            statusLabel.setBackground(new Color(232, 245, 233));
        }
    }

    private void addTokenRow(int index, String value, String type) {
        tokenList.add(new Token(type, value));
        tableModel.addRow(new Object[]{index, value, type});
    }

    private void parseCode() {
        if (tokenList.isEmpty()) {
            scan();
        }
        if (tokenList.isEmpty()) {
            statusLabel.setText(" Nothing to parse. Enter code first.");
            statusLabel.setBackground(new Color(255, 243, 224));
            return;
        }

        Parser parser = new Parser(tokenList);
        parser.parse();
        ArrayList<String> errors = parser.getErrors();

        if (errors.isEmpty()) {
            statusLabel.setText(" Syntax Valid");
            statusLabel.setBackground(new Color(232, 245, 233));
            JOptionPane.showMessageDialog(
                this,
                "Syntax is valid according to assignment grammar.",
                "Parse Success",
                JOptionPane.INFORMATION_MESSAGE
            );
        } else {
            statusLabel.setText(" " + errors.size() + " error(s) found");
            statusLabel.setBackground(new Color(255, 235, 238));

            // Build numbered error list
            StringBuilder sb = new StringBuilder();
            sb.append("Found ").append(errors.size()).append(" error(s):\n\n");
            for (int i = 0; i < errors.size(); i++) {
                sb.append("  ").append(i + 1).append(". ").append(errors.get(i)).append("\n");
            }

            // Use JTextArea inside scroll pane for long error lists
            JTextArea ta = new JTextArea(sb.toString());
            ta.setEditable(false);
            ta.setFont(new Font("Consolas", Font.PLAIN, 13));
            ta.setMargin(new Insets(8, 8, 8, 8));
            JScrollPane sp = new JScrollPane(ta);
            sp.setPreferredSize(new Dimension(520, Math.min(60 + errors.size() * 22, 400)));

            JOptionPane.showMessageDialog(
                this,
                sp,
                "Parse Errors",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void clearAll() {
        tableModel.setRowCount(0);
        tokenList.clear();
        codeArea.setText("");
        fileLabel.setText("No file selected");
        totalLbl.setText("Total: 0");
        keywordLbl.setText("Keywords: 0");
        idLbl.setText("Identifiers: 0");
        numLbl.setText("Numbers: 0");
        opLbl.setText("Operators: 0");
        invalidLbl.setText("Invalid: 0");
        statusLabel.setText(" Ready");
        statusLabel.setBackground(new Color(238, 238, 238));
    }

    private void saveOutput() {
        try {
            JFileChooser fc = new JFileChooser();
            if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                PrintWriter pw = new PrintWriter(fc.getSelectedFile());
                for (int i = 0; i < tableModel.getRowCount(); i++) {
                    pw.println(
                        tableModel.getValueAt(i, 1) + " -> " + tableModel.getValueAt(i, 2)
                    );
                }
                pw.close();
                statusLabel.setText(" Tokens saved");
                statusLabel.setBackground(new Color(232, 245, 233));
            }
        } catch (Exception ignored) {
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SimpleScannerGUI::new);
    }
}
