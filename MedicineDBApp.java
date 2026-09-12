import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;

public class MedicineDBApp {
    
    private static final String DB_URL = "";
    private static final String DB_USER = "";
    private static final String DB_PASSWORD = "";
    private static final String SCHEMA = "";
    
    private Connection conn;
    private Scanner scanner;
    
    public MedicineDBApp() {
        scanner = new Scanner(System.in);
    }
    
 
    // Įkraunamas JDBC driver'is
   
    public static void loadDriver() {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException cnfe) {
            System.out.println("Couldn't find driver class!");
            cnfe.printStackTrace();
            System.exit(1);
        }
    }
    
   
    // Prisijungimas prie duomenų bazės
   
    public Connection getConnection() {
        Connection postGresConn = null;
        try {
            postGresConn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            
            // Nustatome schema
            Statement stmt = null;
            try {
                stmt = postGresConn.createStatement();
                stmt.execute("SET search_path TO " + SCHEMA + ", public");
            } finally {
                if (stmt != null) {
                    stmt.close();
                }
            }
            
            System.out.println("Successfully connected to PostgreSQL Database");
        } catch (SQLException sqle) {
            System.out.println("Couldn't connect to database!");
            sqle.printStackTrace();
            return null;
        }
        
        return postGresConn;
    }
    
  
    // PAIEŠKA 
    public void ieskotiPacientu() {
        if (conn == null) {
            System.out.println("No database connection!");
            return;
        }
        
        System.out.println("\n=== Pacientų paieška ===");
        
        System.out.print("Įveskite paciento vardą (arba Enter jei praleisti): ");
        String vardas = scanner.nextLine().trim();
        
        System.out.print("Įveskite paciento pavardę (arba Enter jei praleisti): ");
        String pavarde = scanner.nextLine().trim();
        
        System.out.print("Įveskite gimimo metus (pvz., 1990) (arba Enter jei praleisti): ");
        String metaiStr = scanner.nextLine().trim();
        
        // Tikrinam ar bent vienas kriterijus įvestas
        if (vardas.isEmpty() && pavarde.isEmpty() && metaiStr.isEmpty()) {
            System.out.println("Būtina įvesti bent vieną paieškos kriterijų!");
            return;
        }
        
        
        StringBuilder sql = new StringBuilder(
            "SELECT ID, AK, Vardas, Pavarde, Gimimo_data, El_pastas, Tel_nr " +
            "FROM Pacientas WHERE 1=1"
        );
        
        if (!vardas.isEmpty()) {
            sql.append(" AND Vardas ILIKE ?");
        }
        if (!pavarde.isEmpty()) {
            sql.append(" AND Pavarde ILIKE ?");
        }
        if (!metaiStr.isEmpty()) {
            sql.append(" AND EXTRACT(YEAR FROM Gimimo_data) = ?");
        }
        
        sql.append(" ORDER BY Vardas, Pavarde, Gimimo_data");
        
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            pstmt = conn.prepareStatement(sql.toString());
            
            // Nustatom parametrus
            int paramIndex = 1;
            if (!vardas.isEmpty()) {
                pstmt.setString(paramIndex++, "%" + vardas + "%");
            }
            if (!pavarde.isEmpty()) {
                pstmt.setString(paramIndex++, "%" + pavarde + "%");
            }
            if (!metaiStr.isEmpty()) {
                pstmt.setInt(paramIndex++, Integer.parseInt(metaiStr));
            }
            
            rs = pstmt.executeQuery();
            
            System.out.println("\n=== Rasti pacientai ===");
            System.out.printf("%-5s %-12s %-15s %-15s %-12s %-25s %-15s\n",
                    "ID", "AK", "Vardas", "Pavardė", "Gimimo data", "El. paštas", "Tel. nr.");
            System.out.println("------------------------------------------------------------------------------------");
            
            boolean found = false;
            while (rs.next()) {
                found = true;
                System.out.printf("%-5d %-12s %-15s %-15s %-12s %-25s %-15s\n",
                        rs.getInt("ID"),
                        rs.getString("AK"),
                        rs.getString("Vardas"),
                        rs.getString("Pavarde"),
                        rs.getDate("Gimimo_data"),
                        rs.getString("El_pastas") != null ? rs.getString("El_pastas") : "---",
                        rs.getString("Tel_nr"));
            }
            
            if (!found) {
                System.out.println("Pacientų pagal nurodytus kriterijus nerasta.");
            }
            
        } catch (NumberFormatException e) {
            System.out.println("Neteisingas metų formatas!");
        } catch (SQLException e) {
            System.out.println("SQL Error!");
            System.out.println("Klaida: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
            } catch (SQLException e) {
                System.out.println("Error closing resources!");
                e.printStackTrace();
            }
        }
    }
    
    // PAIEŠKA 
    public void ieskotiGydytojoVizitu() {
        if (conn == null) {
            System.out.println("No database connection!");
            return;
        }
        
        System.out.print("\nĮveskite gydytojo spaudą (pvz., G001): ");
        String spaudas = scanner.nextLine();
        
        String sql = "SELECT v.Vizito_ID, v.Vizito_data, p.Vardas, p.Pavarde, " +
                     "g.Vardas as Gyd_vardas, g.Pavarde as Gyd_pavarde " +
                     "FROM Vizitas v " +
                     "JOIN Pacientas p ON v.Paciento_ID = p.ID " +
                     "JOIN Gydytojas g ON v.Gydytojo_Spaudas = g.Spaudo_nr " +
                     "WHERE v.Gydytojo_Spaudas = ? " +
                     "ORDER BY v.Vizito_data DESC";
        
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, spaudas);
            rs = pstmt.executeQuery();
            
            System.out.println("\n=== Gydytojo vizitai ===");
            boolean found = false;
            while (rs.next()) {
                if (!found) {
                    System.out.println("Gydytojas: " + rs.getString("Gyd_vardas") + 
                                     " " + rs.getString("Gyd_pavarde"));
                    System.out.println("----------------------------");
                    found = true;
                }
                System.out.printf("Vizito ID: %d | Data: %s | Pacientas: %s %s\n",
                        rs.getInt("Vizito_ID"),
                        rs.getDate("Vizito_data"),
                        rs.getString("Vardas"),
                        rs.getString("Pavarde"));
            }
            
            if (!found) {
                System.out.println("Vizitų nerasta arba gydytojas neegzistuoja.");
            }
            
        } catch (SQLException e) {
            System.out.println("SQL Error!");
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
            } catch (SQLException e) {
                System.out.println("Error closing resources!");
                e.printStackTrace();
            }
        }
    }
    
  
    // ĮVEDIMAS 
   
    public void pridetiPacienta() {
        if (conn == null) {
            System.out.println("No database connection!");
            return;
        }
        
        try {
            System.out.println("\n=== Naujo paciento pridėjimas ===");
            
            System.out.print("Asmens kodas (11 skaitmenų): ");
            String ak = scanner.nextLine();
            
            System.out.print("Vardas: ");
            String vardas = scanner.nextLine();
            
            System.out.print("Pavardė: ");
            String pavarde = scanner.nextLine();
            
            System.out.print("Gimimo data (YYYY-MM-DD): ");
            String gimData = scanner.nextLine();
            
            System.out.print("El. paštas (arba Enter jei nėra): ");
            String elpastas = scanner.nextLine();
            if (elpastas.isEmpty()) elpastas = null;
            
            System.out.print("Telefono numeris (arba Enter jei 'nenurodytas'): ");
            String telNr = scanner.nextLine();
            if (telNr.isEmpty()) telNr = "nenurodytas";
            
            String sql = "INSERT INTO Pacientas (AK, Vardas, Pavarde, Gimimo_data, El_pastas, Tel_nr) " +
                         "VALUES (?, ?, ?, ?::date, ?, ?)";
            
            PreparedStatement pstmt = null;
            ResultSet rs = null;
            
            try {
                pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                pstmt.setString(1, ak);
                pstmt.setString(2, vardas);
                pstmt.setString(3, pavarde);
                pstmt.setString(4, gimData);
                pstmt.setString(5, elpastas);
                pstmt.setString(6, telNr);
                
                int affected = pstmt.executeUpdate();
                
                if (affected > 0) {
                    rs = pstmt.getGeneratedKeys();
                    if (rs.next()) {
                        System.out.println("\n✓ Pacientas sėkmingai pridėtas! ID: " + rs.getInt(1));
                    }
                }
            } finally {
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
            }
            
        } catch (SQLException e) {
            System.out.println("SQL Error!");
            e.printStackTrace();
        }
    }
    
    /********************************************************/
    // ĮVEDIMAS - naujo gydytojo pridėjimas
    /********************************************************/
    public void pridetiGydytoja() {
        if (conn == null) {
            System.out.println("No database connection!");
            return;
        }
        
        try {
            System.out.println("\n=== Naujo gydytojo pridėjimas ===");
            
            System.out.print("Spaudos numeris (pvz., G004): ");
            String spaudas = scanner.nextLine();
            
            System.out.print("Vardas: ");
            String vardas = scanner.nextLine();
            
            System.out.print("Pavardė: ");
            String pavarde = scanner.nextLine();
            
            System.out.print("Specializacija (arba Enter jei 'nenurodyta'): ");
            String spec = scanner.nextLine();
            if (spec.isEmpty()) spec = "nenurodyta";
            
            String sql = "INSERT INTO Gydytojas (Spaudo_nr, Vardas, Pavarde, Specializacija) " +
                         "VALUES (?, ?, ?, ?)";
            
            PreparedStatement pstmt = null;
            
            try {
                pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, spaudas);
                pstmt.setString(2, vardas);
                pstmt.setString(3, pavarde);
                pstmt.setString(4, spec);
                
                int affected = pstmt.executeUpdate();
                
                if (affected > 0) {
                    System.out.println("\n✓ Gydytojas sėkmingai pridėtas!");
                }
            } finally {
                if (pstmt != null) pstmt.close();
            }
            
        } catch (SQLException e) {
            System.out.println("SQL Error!");
            e.printStackTrace();
        }
    }
    
   
    // ATNAUJINIMAS 
    public void atnaujintiPacientoKontaktus() {
        if (conn == null) {
            System.out.println("No database connection!");
            return;
        }
        
        try {
            System.out.println("\n=== Paciento kontaktų atnaujinimas ===");
            
            
            rodytiVisusPacientus();
            
            System.out.print("\nĮveskite paciento ID: ");
            int id = Integer.parseInt(scanner.nextLine());
            
            System.out.print("Naujas el. paštas (arba Enter jei nekeisti): ");
            String elpastas = scanner.nextLine();
            
            System.out.print("Naujas telefono numeris (arba Enter jei nekeisti): ");
            String telNr = scanner.nextLine();
            
            if (elpastas.isEmpty() && telNr.isEmpty()) {
                System.out.println("Nieko nekeista.");
                return;
            }
            
            StringBuilder sql = new StringBuilder("UPDATE Pacientas SET ");
            boolean first = true;
            
            if (!elpastas.isEmpty()) {
                sql.append("El_pastas = ?");
                first = false;
            }
            
            if (!telNr.isEmpty()) {
                if (!first) sql.append(", ");
                sql.append("Tel_nr = ?");
            }
            
            sql.append(" WHERE ID = ?");
            
            PreparedStatement pstmt = null;
            
            try {
                pstmt = conn.prepareStatement(sql.toString());
                int paramIndex = 1;
                
                if (!elpastas.isEmpty()) {
                    pstmt.setString(paramIndex++, elpastas);
                }
                
                if (!telNr.isEmpty()) {
                    pstmt.setString(paramIndex++, telNr);
                }
                
                pstmt.setInt(paramIndex, id);
                
                int affected = pstmt.executeUpdate();
                
                if (affected > 0) {
                    System.out.println("\n✓ Paciento kontaktai sėkmingai atnaujinti!");
                } else {
                    System.out.println("Pacientas su tokiu ID nerastas.");
                }
            } finally {
                if (pstmt != null) pstmt.close();
            }
            
        } catch (NumberFormatException e) {
            System.out.println("Neteisingas ID formatas!");
        } catch (SQLException e) {
            System.out.println("SQL Error!");
            e.printStackTrace();
        }
    }
    
    
    private void rodytiVisusPacientus() {
        if (conn == null) return;
        
        String sql = "SELECT ID, Vardas, Pavarde, El_pastas, Tel_nr FROM Pacientas ORDER BY ID";
        
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
            
            System.out.println("\nEsami pacientai:");
            System.out.printf("%-5s %-15s %-15s %-25s %-15s\n", 
                            "ID", "Vardas", "Pavardė", "El. paštas", "Tel. nr.");
            System.out.println("--------------------------------------------------------------------------------");
            
            while (rs.next()) {
                System.out.printf("%-5d %-15s %-15s %-25s %-15s\n",
                        rs.getInt("ID"),
                        rs.getString("Vardas"),
                        rs.getString("Pavarde"),
                        rs.getString("El_pastas") != null ? rs.getString("El_pastas") : "---",
                        rs.getString("Tel_nr"));
            }
        } catch (SQLException e) {
            System.out.println("SQL Error!");
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
            } catch (SQLException e) {
                System.out.println("Error closing resources!");
                e.printStackTrace();
            }
        }
    }
    
  
 public void istrintiVizita() {
    if (conn == null) {
        System.out.println("No database connection!");
        return;
    }
    
    try {
        System.out.println("\n=== Vizito šalinimas ===");
        rodytiVisusVizitus();
        
        System.out.print("\nĮveskite vizito ID: ");
        int vizitoId = Integer.parseInt(scanner.nextLine());
        rodytiVizitoDetales(vizitoId);
        
        System.out.print("\nAr tikrai norite ištrinti šį vizitą? (taip/ne): ");
        String confirm = scanner.nextLine();
        
        if (!confirm.equalsIgnoreCase("taip")) {
            System.out.println("Trynimas atšauktas.");
            return;
        }
        
       
        conn.setAutoCommit(false);
        
        PreparedStatement pstmt1 = null;
        PreparedStatement pstmt2 = null;
        
        try {
            
            String sql1 = "DELETE FROM Vizito_diagnoze WHERE Vizito_ID = ?";
            pstmt1 = conn.prepareStatement(sql1);
            pstmt1.setInt(1, vizitoId);
            int diagnozesTrinta = pstmt1.executeUpdate();
            
            
            String sql2 = "DELETE FROM Vizitas WHERE Vizito_ID = ?";
            pstmt2 = conn.prepareStatement(sql2);
            pstmt2.setInt(1, vizitoId);
            int affected = pstmt2.executeUpdate();
            
            if (affected > 0) {
                conn.commit(); 
                System.out.println("\n✓ Vizitas sėkmingai ištrintas!");
                System.out.println("  - Ištrinta diagnozių: " + diagnozesTrinta);
                System.out.println("  - Ištrintas vizitas");
            } else {
                conn.rollback(); 
                System.out.println("Vizitas su tokiu ID nerastas.");
            }
            
        } catch (SQLException e) {
            conn.rollback(); 
            System.out.println("SQL Error! Pakeitimai atšaukti.");
            System.out.println("Klaida: " + e.getMessage());
            e.printStackTrace();
        } finally {
            conn.setAutoCommit(true); 
            if (pstmt1 != null) pstmt1.close();
            if (pstmt2 != null) pstmt2.close();
        }
        
    } catch (NumberFormatException e) {
        System.out.println("Neteisingas ID formatas!");
    } catch (SQLException e) {
        System.out.println("Duomenų bazės klaida: " + e.getMessage());
    }
}
    
    
    private void rodytiVisusVizitus() {
        if (conn == null) return;
        
        String sql = "SELECT v.Vizito_ID, v.Vizito_data, " +
                     "g.Vardas || ' ' || g.Pavarde as Gydytojas, " +
                     "p.Vardas || ' ' || p.Pavarde as Pacientas " +
                     "FROM Vizitas v " +
                     "JOIN Gydytojas g ON v.Gydytojo_Spaudas = g.Spaudo_nr " +
                     "JOIN Pacientas p ON v.Paciento_ID = p.ID " +
                     "ORDER BY v.Vizito_data DESC, v.Vizito_ID";
        
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
            
            System.out.println("\nEsami vizitai:");
            System.out.printf("%-10s %-12s %-25s %-25s\n", 
                            "Vizito ID", "Data", "Gydytojas", "Pacientas");
            System.out.println("--------------------------------------------------------------------------------");
            
            while (rs.next()) {
                System.out.printf("%-10d %-12s %-25s %-25s\n",
                        rs.getInt("Vizito_ID"),
                        rs.getDate("Vizito_data"),
                        rs.getString("Gydytojas"),
                        rs.getString("Pacientas"));
            }
        } catch (SQLException e) {
            System.out.println("SQL Error!");
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
            } catch (SQLException e) {
                System.out.println("Error closing resources!");
                e.printStackTrace();
            }
        }
    }
   
    private void rodytiVizitoDetales(int vizitoId) {
        if (conn == null) return;
        
        String sql = "SELECT v.Vizito_ID, v.Vizito_data, " +
                     "g.Vardas || ' ' || g.Pavarde as Gydytojas, " +
                     "p.Vardas || ' ' || p.Pavarde as Pacientas, " +
                     "STRING_AGG(d.Pavadinimas, ', ') as Diagnozes " +
                     "FROM Vizitas v " +
                     "JOIN Gydytojas g ON v.Gydytojo_Spaudas = g.Spaudo_nr " +
                     "JOIN Pacientas p ON v.Paciento_ID = p.ID " +
                     "LEFT JOIN Vizito_diagnoze vd ON v.Vizito_ID = vd.Vizito_ID " +
                     "LEFT JOIN Diagnoze d ON vd.TLK = d.TLK " +
                     "WHERE v.Vizito_ID = ? " +
                     "GROUP BY v.Vizito_ID, v.Vizito_data, g.Vardas, g.Pavarde, p.Vardas, p.Pavarde";
        
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, vizitoId);
            rs = pstmt.executeQuery();
            
            if (rs.next()) {
                System.out.println("\n--- Vizito detalės ---");
                System.out.println("Vizito ID: " + rs.getInt("Vizito_ID"));
                System.out.println("Data: " + rs.getDate("Vizito_data"));
                System.out.println("Gydytojas: " + rs.getString("Gydytojas"));
                System.out.println("Pacientas: " + rs.getString("Pacientas"));
                String diagnozes = rs.getString("Diagnozes");
                System.out.println("Diagnozės: " + (diagnozes != null ? diagnozes : "nėra"));
                System.out.println("---------------------");
            } else {
                System.out.println("Vizitas su ID " + vizitoId + " nerastas.");
            }
        } catch (SQLException e) {
            System.out.println("SQL Error!");
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
            } catch (SQLException e) {
                System.out.println("Error closing resources!");
                e.printStackTrace();
            }
        }
    }
    

    // ĮVEDIMAS 
    public void pridetiVizita() {
        if (conn == null) {
            System.out.println("No database connection!");
            return;
        }
        
        try {
            System.out.println("\n=== Naujo vizito pridėjimas ===");
            
            // Rodomos esamos reikšmės
            rodytiVisusGydytojus();
            System.out.print("\nĮveskite gydytojo spaudą: ");
            String spaudas = scanner.nextLine();
            
            rodytiVisusPacientus();
            System.out.print("\nĮveskite paciento ID: ");
            int pacientoId = Integer.parseInt(scanner.nextLine());
            
            System.out.print("Įveskite vizito ID: ");
            int vizitoId = Integer.parseInt(scanner.nextLine());
            
            System.out.print("Vizito data (YYYY-MM-DD): ");
            String data = scanner.nextLine();
            
            String sql = "INSERT INTO Vizitas (Vizito_ID, Vizito_data, Gydytojo_Spaudas, Paciento_ID) " +
                        "VALUES (?, ?::date, ?, ?)";
            
            PreparedStatement pstmt = null;
            
            try {
                pstmt = conn.prepareStatement(sql);
                pstmt.setInt(1, vizitoId);
                pstmt.setString(2, data);
                pstmt.setString(3, spaudas);
                pstmt.setInt(4, pacientoId);
                
                int affected = pstmt.executeUpdate();
                
                if (affected > 0) {
                    System.out.println("\n✓ Vizitas sėkmingai pridėtas!");
                }
            } finally {
                if (pstmt != null) pstmt.close();
            }
            
        } catch (NumberFormatException e) {
            System.out.println("Neteisingas skaičiaus formatas!");
        } catch (SQLException e) {
            System.out.println("SQL Error!");
            System.out.println("Klaida: " + e.getMessage());
            e.printStackTrace();
        }
    }
    

   
    
   
    private void rodytiVisusGydytojus() {
        if (conn == null) return;
        
        String sql = "SELECT Spaudo_nr, Vardas, Pavarde, Specializacija FROM Gydytojas ORDER BY Spaudo_nr";
        
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
            
            System.out.println("\nEsami gydytojai:");
            System.out.printf("%-10s %-15s %-15s %-30s\n", "Spaudas", "Vardas", "Pavardė", "Specializacija");
            System.out.println("--------------------------------------------------------------------------------");
            
            while (rs.next()) {
                System.out.printf("%-10s %-15s %-15s %-30s\n",
                        rs.getString("Spaudo_nr"),
                        rs.getString("Vardas"),
                        rs.getString("Pavarde"),
                        rs.getString("Specializacija"));
            }
        } catch (SQLException e) {
            System.out.println("SQL Error!");
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
            } catch (SQLException e) {
                System.out.println("Error closing resources!");
                e.printStackTrace();
            }
        }
    }
    
   
    public void displayMenu() {
        System.out.println("\n╔════════════════════════════════════════════════╗");
        System.out.println("║   MEDICININĖS SISTEMOS DUOMENŲ BAZĖ            ║");
        System.out.println("╚════════════════════════════════════════════════╝");
        System.out.println("1.  Ieškoti pacientų pagal vardą/pavardę/gimimo metus");
        System.out.println("2.  Ieškoti gydytojo vizitų");
        System.out.println("3.  Pridėti naują pacientą");
        System.out.println("4.  Pridėti naują gydytoją");
        System.out.println("5.  Atnaujinti paciento kontaktus");
        System.out.println("6.  Ištrinti vizitą");
        System.out.println("7.  Pridėti vizitą");
        System.out.println("0.  Išeiti");
        System.out.println("════════════════════════════════════════════════");
        System.out.print("Pasirinkite funkciją: ");
    }
    

    public void run() {
        conn = getConnection();
        
        if (conn == null) {
            System.out.println("Cannot continue without database connection!");
            return;
        }
        
        while (true) {
            displayMenu();
            String choice = scanner.nextLine();
            
            switch (choice) {
                case "1":
                    ieskotiPacientu();
                    break;
                case "2":
                    ieskotiGydytojoVizitu();
                    break;
                case "3":
                    pridetiPacienta();
                    break;
                case "4":
                    pridetiGydytoja();
                    break;
                case "5":
                    atnaujintiPacientoKontaktus();
                    break;
                case "6":
                    istrintiVizita();
                    break;
                case "7":
                    pridetiVizita();
                    break;
               
                case "0":
                    System.out.println("\nAčiū, kad naudojotės sistema. Viso gero!");
                    return;
                default:
                    System.out.println("Neteisingas pasirinkimas. Bandykite dar kartą.");
            }
        }
    }
    
    
    public static void main(String[] args) {
        System.out.println("Paleidžiama medicininės sistemos programa...\n");
        
        loadDriver();
        
        MedicineDBApp app = new MedicineDBApp();
        app.run();
        
        
        if (app.conn != null) {
            try {
                app.conn.close();
                System.out.println("Database connection closed.");
            } catch (SQLException e) {
                System.out.println("Cannot close connection!");
                e.printStackTrace();
            }
        }
        
        if (app.scanner != null) {
            app.scanner.close();
        }
    }
}