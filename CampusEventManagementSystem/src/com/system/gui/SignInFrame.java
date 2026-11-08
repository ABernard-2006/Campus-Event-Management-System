/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package com.system.gui;

/**
 *
 * @author Dhustine
 */
import classModel.UserAccount;
import com.system.service.EventManager;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Arrays;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

public class SignInFrame extends javax.swing.JFrame {
    
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(SignInFrame.class.getName());

    private final EventManager manager;
    private final LoginFrame loginFrame;       // the window to go back to (may be null when run on its own)

    // Extra controls that are built in code (they are not in SignInFrame.form)
    private JLabel lblRegisterAs;
    private JComboBox<String> cmbRegisterAs;
    private JLabel lblConfirmPass;
    private JPasswordField passConfirmPass;
    private JLabel lblInviteKey;
    private JTextField txtInviteKey;
    private JLabel lblInviteHint;

    // What to show on the login screen once this window closes
    private String returnIdentifier = null;
    private String returnRole = null;

    /**
     * Creates new form SignInFrame
     */
    public SignInFrame() {
        this(new EventManager(), null);
    }

    public SignInFrame(EventManager manager, LoginFrame loginFrame) {
        this.manager = manager;
        this.loginFrame = loginFrame;
        initComponents();
        setUpSignUp();
    }

    // ------------------------------------------------------------------
    //  Hand-written setup (kept outside initComponents so Design view can't overwrite it)
    // ------------------------------------------------------------------
    private void setUpSignUp() {
        // Closing this window goes back to the login window instead of quitting the program.
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                LoginFrame target = loginFrame != null ? loginFrame : new LoginFrame(manager);
                if (loginFrame == null) target.setLocationRelativeTo(null);
                target.returnFromSignUp(returnIdentifier, returnRole);
            }
        });

        lblCreatePass.setText("Password");
        lblRegisterAs = new JLabel("Register as");
        cmbRegisterAs = new JComboBox<>(new String[] { UserAccount.ROLE_PARTICIPANT, UserAccount.ROLE_ORGANIZER });
        lblConfirmPass = new JLabel("Confirm Password");
        passConfirmPass = new JPasswordField();
        lblInviteKey = new JLabel("Organizer Invitation Key");
        txtInviteKey = new JTextField();
        lblInviteHint = new JLabel("Ask an administrator for a key. Each key works once.");
        lblInviteHint.setFont(new java.awt.Font("Calibri", 0, 11));
        lblInviteHint.setForeground(java.awt.Color.GRAY);

        // Rebuild the panel so the new rows fit. The existing components are re-used, not replaced.
        jPanel1.removeAll();
        jPanel1.setLayout(new GridBagLayout());
        jPanel1.setBorder(BorderFactory.createEmptyBorder(26, 22, 20, 22));
        lblSignInToCMS.setPreferredSize(new Dimension(356, 28));
        jLabel1.setHorizontalAlignment(SwingConstants.CENTER);

        addRow(lblSignInToCMS, 0);
        addRow(jLabel1, 14);
        addRow(lblSigInUser, 0);
        addRow(txtSignInUser, 2);
        addRow(lblSignInEmail, 10);
        addRow(txtEmailAdd, 2);
        addRow(lblRegisterAs, 10);
        addRow(cmbRegisterAs, 2);
        addRow(lblInviteKey, 10);
        addRow(txtInviteKey, 2);
        addRow(lblInviteHint, 2);
        addRow(lblCreatePass, 10);
        addRow(passCreatePass, 2);
        addRow(lblConfirmPass, 10);
        addRow(passConfirmPass, 2);
        addRow(btnSignIn, 16);
        addRow(lblSignUp, 14);

        setInviteKeyVisible(false);
        cmbRegisterAs.addActionListener(e -> {
            setInviteKeyVisible(UserAccount.ROLE_ORGANIZER.equals(cmbRegisterAs.getSelectedItem()));
            pack();
        });

        // "Already have an account? Login" link
        lblSignUp.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblSignUp.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                dispose();
            }
        });

        getContentPane().removeAll();
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(jPanel1, BorderLayout.CENTER);
        getRootPane().setDefaultButton(btnSignIn);          // pressing Enter registers
        setTitle("Campus Event Management System - Register");
        setResizable(false);
        setPreferredSize(null);                             // let the window fit its content
        pack();
    }

    /** Adds one component as a full-width row with the given empty space above it. */
    private void addRow(java.awt.Component component, int spaceAbove) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(spaceAbove, 0, 0, 0);
        jPanel1.add(component, c);
    }

    private void setInviteKeyVisible(boolean visible) {
        lblInviteKey.setVisible(visible);
        txtInviteKey.setVisible(visible);
        lblInviteHint.setVisible(visible);
        if (!visible) txtInviteKey.setText("");
    }

    /** Closes this window and tells the login window which account to pre-fill. */
    private void finishAndReturn(String identifier, String role) {
        returnIdentifier = identifier;
        returnRole = role;
        dispose();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        lblSignInToCMS = new javax.swing.JLabel();
        txtSignInUser = new javax.swing.JTextField();
        passCreatePass = new javax.swing.JPasswordField();
        lblSigInUser = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        lblCreatePass = new javax.swing.JLabel();
        lblSignInEmail = new javax.swing.JLabel();
        txtEmailAdd = new javax.swing.JTextField();
        btnSignIn = new javax.swing.JButton();
        lblSignUp = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setPreferredSize(new java.awt.Dimension(400, 500));

        jPanel1.setBackground(new java.awt.Color(249, 249, 249));

        lblSignInToCMS.setFont(new java.awt.Font("Candara", 1, 18)); // NOI18N
        lblSignInToCMS.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblSignInToCMS.setText("Campus Event Management System");

        lblSigInUser.setText("Username");

        jLabel1.setFont(new java.awt.Font("Calibri", 0, 12)); // NOI18N
        jLabel1.setText("Please Enter your details to register.");

        lblCreatePass.setText("Create Password");

        lblSignInEmail.setText("Email Address");

        btnSignIn.setBackground(new java.awt.Color(51, 51, 51));
        btnSignIn.setForeground(new java.awt.Color(255, 255, 255));
        btnSignIn.setText("Register");
        btnSignIn.addActionListener(this::btnSignInActionPerformed);

        lblSignUp.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblSignUp.setText("<html>Already have an account? <font color='blue'><u>Login</u></font></html>");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(lblSignInToCMS, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(111, 111, 111)
                        .addComponent(jLabel1))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(18, 18, 18)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(lblSigInUser)
                            .addComponent(txtSignInUser)
                            .addComponent(txtEmailAdd)
                            .addComponent(lblSignInEmail)
                            .addComponent(lblCreatePass)
                            .addComponent(passCreatePass)
                            .addComponent(btnSignIn, javax.swing.GroupLayout.DEFAULT_SIZE, 360, Short.MAX_VALUE))))
                .addContainerGap(22, Short.MAX_VALUE))
            .addComponent(lblSignUp, javax.swing.GroupLayout.Alignment.TRAILING)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(34, 34, 34)
                .addComponent(lblSignInToCMS)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel1)
                .addGap(18, 18, 18)
                .addComponent(lblSigInUser)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtSignInUser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lblSignInEmail)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtEmailAdd, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(12, 12, 12)
                .addComponent(lblCreatePass)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(passCreatePass, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(btnSignIn)
                .addGap(18, 18, 18)
                .addComponent(lblSignUp, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(24, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(303, 303, 303))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnSignInActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSignInActionPerformed
        char[] password = passCreatePass.getPassword();
        char[] confirm = passConfirmPass.getPassword();
        String role = (String) cmbRegisterAs.getSelectedItem();
        try {
            // All the rules (no Administrator accounts, key checks, duplicates) live in EventManager.
            UserAccount created = manager.registerAccount(
                    txtSignInUser.getText(), txtEmailAdd.getText(),
                    new String(password), new String(confirm),
                    role, txtInviteKey.getText());
            JOptionPane.showMessageDialog(this,
                    "Your " + created.getRole() + " account \"" + created.getUsername() + "\" was created.\nYou can log in now.",
                    "Registration complete", JOptionPane.INFORMATION_MESSAGE);
            finishAndReturn(created.getUsername(), created.getRole());
        } catch (SecurityException | IllegalArgumentException | IllegalStateException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Registration", JOptionPane.WARNING_MESSAGE);
        } finally {
            Arrays.fill(password, '\0');
            Arrays.fill(confirm, '\0');
        }
    }//GEN-LAST:event_btnSignInActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> new SignInFrame().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnSignIn;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JLabel lblCreatePass;
    private javax.swing.JLabel lblSigInUser;
    private javax.swing.JLabel lblSignInEmail;
    private javax.swing.JLabel lblSignInToCMS;
    private javax.swing.JLabel lblSignUp;
    private javax.swing.JPasswordField passCreatePass;
    private javax.swing.JTextField txtEmailAdd;
    private javax.swing.JTextField txtSignInUser;
    // End of variables declaration//GEN-END:variables
}
