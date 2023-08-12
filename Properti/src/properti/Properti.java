/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package properti;

import javax.swing.*;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.table.DefaultTableModel;
/**
 *
 * @author This PC
 */
public class Properti extends JFrame {

    private Connection conn;
    private Statement stmt;
    private JTable propertyTable;
    private DefaultTableModel tableModel;

    public Properti() {
        try {
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/manajemenprop", "root", "");
            stmt = conn.createStatement();

            // GUI
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setTitle("Manajemen Properti");
            setLayout(new BorderLayout());

            tableModel = new DefaultTableModel(new String[]{"ID", "Nama Properti", "Harga"}, 0);
            propertyTable = new JTable(tableModel);
            JScrollPane scrollPane = new JScrollPane(propertyTable);
            add(scrollPane, BorderLayout.CENTER);

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

            JButton addButton = new JButton("Tambah");
            addButton.addActionListener(e -> addProperty());
            buttonPanel.add(addButton);

            JButton editButton = new JButton("Edit");
            editButton.addActionListener(e -> editProperty());
            buttonPanel.add(editButton);

            JButton deleteButton = new JButton("Hapus");
            deleteButton.addActionListener(e -> deleteProperty());
            buttonPanel.add(deleteButton);

            add(buttonPanel, BorderLayout.SOUTH);

            refreshPropertyTable();

            setSize(800, 600);
            setLocationRelativeTo(null);
            setVisible(true);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void refreshPropertyTable() {
        tableModel.setRowCount(0);
        try {
            ResultSet resultSet = stmt.executeQuery("SELECT id, nama_properti, harga FROM properti");
            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String namaProperti = resultSet.getString("nama_properti");
                double harga = resultSet.getDouble("harga");
                tableModel.addRow(new Object[]{id, namaProperti, harga});
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void addProperty() {
        AddPropertyDialog addDialog = new AddPropertyDialog(this);
        addDialog.setVisible(true);
        refreshPropertyTable();
    }

    private void editProperty() {
        int selectedRow = propertyTable.getSelectedRow();
        if (selectedRow != -1) {
            int propertyID = (int) propertyTable.getValueAt(selectedRow, 0);
            EditPropertyDialog editDialog = new EditPropertyDialog(this, propertyID);
            editDialog.setVisible(true);
            refreshPropertyTable();
        } else {
            JOptionPane.showMessageDialog(this, "Pilih properti terlebih dahulu.");
        }
    }

    private void deleteProperty() {
        int selectedRow = propertyTable.getSelectedRow();
        if (selectedRow != -1) {
            int propertyID = (int) propertyTable.getValueAt(selectedRow, 0);
            int confirmResult = JOptionPane.showConfirmDialog(this, "Apakah Anda yakin ingin menghapus properti ini?", "Konfirmasi Hapus", JOptionPane.YES_NO_OPTION);
            if (confirmResult == JOptionPane.YES_OPTION) {
                deletePropertyFromDatabase(propertyID);
                refreshPropertyTable();
            }
        } else {
            JOptionPane.showMessageDialog(this, "Pilih properti terlebih dahulu.");
        }
    }

    private void deletePropertyFromDatabase(int propertyID) {
        try {
            String query = "DELETE FROM properti WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setInt(1, propertyID);
                pstmt.executeUpdate();
            }
            JOptionPane.showMessageDialog(this, "Properti berhasil dihapus.");
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Terjadi kesalahan saat menghapus properti.");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | UnsupportedLookAndFeelException e) {
                e.printStackTrace();
            }
            Properti properti = new Properti();
        });
    }
}

class AddPropertyDialog extends JDialog {
    private Connection conn;
    private final JTextField namaField;
    private final JTextField pemilikField;
    private final JTextField kontakField;
    private final JTextField alamatField;
    private final JTextField hargaField;
    private final JButton browseButton;
    private final JLabel selectedImageLabel;
    private File selectedImageFile;

    public AddPropertyDialog(JFrame parent) {
        super(parent, "Tambah Properti", true);

        try {
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/manajemenprop", "root", "");
            conn.createStatement();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        setLayout(new GridLayout(7, 2));
        setSize(300, 350);

        add(new JLabel("Nama Properti: "));
        namaField = new JTextField(20);
        add(namaField);

        add(new JLabel("Pemilik: "));
        pemilikField = new JTextField(20);
        add(pemilikField);

        add(new JLabel("Kontak: "));
        kontakField = new JTextField(20);
        add(kontakField);

        add(new JLabel("Alamat: "));
        alamatField = new JTextField(20);
        add(alamatField);

        add(new JLabel("Harga: "));
        hargaField = new JTextField(20);
        add(hargaField);

        add(new JLabel("Gambar: "));
        JPanel imagePanel = new JPanel();
        selectedImageLabel = new JLabel();
        browseButton = new JButton("Upload Gambar");
        browseButton.addActionListener(e -> selectImage());
        imagePanel.add(selectedImageLabel);
        imagePanel.add(browseButton);
        add(imagePanel);

        JButton saveButton = new JButton("Simpan");
        saveButton.addActionListener(e -> {
            try {
                saveProperty();
            } catch (FileNotFoundException ex) {
                Logger.getLogger(AddPropertyDialog.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        add(saveButton);
    }

    private void selectImage() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            selectedImageFile = fileChooser.getSelectedFile();
            ImageIcon imageIcon = new ImageIcon(selectedImageFile.getAbsolutePath());
            selectedImageLabel.setIcon(imageIcon);
        }
    }

    private void saveProperty() throws FileNotFoundException {
        String nama = namaField.getText();
        String pemilik = pemilikField.getText();
        String kontak = kontakField.getText();
        String alamat = alamatField.getText();
        double harga = Double.parseDouble(hargaField.getText());

        try {
            String query = "INSERT INTO properti (nama_properti, pemilik, kontak, alamat, harga, gambar) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, nama);
                pstmt.setString(2, pemilik);
                pstmt.setString(3, kontak);
                pstmt.setString(4, alamat);
                pstmt.setDouble(5, harga);

                if (selectedImageFile != null) {
                    FileInputStream imageStream = new FileInputStream(selectedImageFile);
                    pstmt.setBinaryStream(6, imageStream, (int) selectedImageFile.length());
                } else {
                    pstmt.setNull(6, Types.LONGVARBINARY);
                }

                pstmt.executeUpdate();
            }

            JOptionPane.showMessageDialog(this, "Properti berhasil ditambahkan.");
            dispose();
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Terjadi kesalahan saat menambahkan properti.");
        }
    }
}

class EditPropertyDialog extends JDialog {
    private Connection conn;
    private Statement stmt;
    private int propertyID;
    private final JTextField namaField;
    private final JTextField pemilikField;
    private final JTextField kontakField;
    private final JTextField alamatField;
    private final JTextField hargaField;
    private final JLabel imageLabel;
    private File selectedImageFile;

    public EditPropertyDialog(JFrame parent, int propertyID) {
        super(parent, "Edit Properti", true);
        this.propertyID = propertyID;

        try {
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/manajemenprop", "root", "");
            stmt = conn.createStatement();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        setLayout(new GridLayout(7, 2));
        setSize(300, 350);

        add(new JLabel("Nama Properti: "));
        namaField = new JTextField(20);
        add(namaField);

        add(new JLabel("Pemilik: "));
        pemilikField = new JTextField(20);
        add(pemilikField);

        add(new JLabel("Kontak: "));
        kontakField = new JTextField(20);
        add(kontakField);

        add(new JLabel("Alamat: "));
        alamatField = new JTextField(20);
        add(alamatField);

        add(new JLabel("Harga: "));
        hargaField = new JTextField(20);
        add(hargaField);

        JButton saveButton = new JButton("Simpan");
        saveButton.addActionListener(e -> saveProperty());
        add(saveButton);

        imageLabel = new JLabel();
        add(new JLabel("Gambar:"));
        add(imageLabel);

        loadPropertyData();
    }

    private void loadPropertyData() {
        try {
            String query = "SELECT * FROM properti WHERE id = " + propertyID;
            ResultSet resultSet = stmt.executeQuery(query);
            if (resultSet.next()) {
            namaField.setText(resultSet.getString("nama_properti"));
            pemilikField.setText(resultSet.getString("pemilik"));
            kontakField.setText(resultSet.getString("kontak"));
            alamatField.setText(resultSet.getString("alamat"));
            hargaField.setText(Double.toString(resultSet.getDouble("harga")));

            // Menampilkan gambar jika ada
            byte[] imageData = resultSet.getBytes("gambar");
            if (imageData != null) {
                ImageIcon imageIcon = new ImageIcon(imageData);
                Image image = imageIcon.getImage();
                Image scaledImage = image.getScaledInstance(150, 150, Image.SCALE_SMOOTH);
                imageIcon = new ImageIcon(scaledImage);
                imageLabel.setIcon(imageIcon);
            }
        }
    } catch (SQLException e) {
        e.printStackTrace();
        }
    }

    private void saveProperty() {
        String nama = namaField.getText();
        String pemilik = pemilikField.getText();
        String kontak = kontakField.getText();
        String alamat = alamatField.getText();
        double harga = Double.parseDouble(hargaField.getText());

        try {
        String query;
        if (selectedImageFile != null) {
            query = "UPDATE properti SET nama_properti=?, pemilik=?, kontak=?, alamat=?, harga=?, gambar=? WHERE id=?";
        } else {
            query = "UPDATE properti SET nama_properti=?, pemilik=?, kontak=?, alamat=?, harga=? WHERE id=?";
        }
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, nama);
            pstmt.setString(2, pemilik);
            pstmt.setString(3, kontak);
            pstmt.setString(4, alamat);
            pstmt.setDouble(5, harga);

            if (selectedImageFile != null) {
                FileInputStream imageStream = new FileInputStream(selectedImageFile);
                pstmt.setBinaryStream(6, imageStream, (int) selectedImageFile.length());
                pstmt.setInt(7, propertyID);
            } else {
                pstmt.setInt(6, propertyID);
            }

            pstmt.executeUpdate();
            JOptionPane.showMessageDialog(this, "Properti berhasil diubah.");
            dispose();
        }
    } catch (SQLException | FileNotFoundException e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(this, "Terjadi kesalahan saat mengubah properti.");
    }
}
}