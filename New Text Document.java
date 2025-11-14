import java.sql.*;
import java.util.Scanner;

/**
 * Hotel Reservation System (Console Application)
 * Implemented in Java with MySQL for database connectivity.
 * This class provides methods for connecting to the database,
 * making, viewing, editing, and deleting room reservations.
 */
public class HotelReservationSystem {

    // --- Database Configuration (IMPORTANT: Update these credentials!) ---
    private static final String DB_URL = "jdbc:mysql://localhost:3306/hotel_db";
    private static final String DB_USER = "your_mysql_username"; // Replace with your MySQL username
    private static final String DB_PASSWORD = "your_mysql_password"; // Replace with your MySQL password
    // ---------------------------------------------------------------------

    private static Connection getConnection() throws SQLException {
        try {
            // Load the MySQL JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        } catch (ClassNotFoundException e) {
            // This happens if the MySQL Connector/J JAR is missing
            System.err.println("Error: MySQL JDBC Driver not found. Ensure the MySQL Connector/J JAR is in your classpath.");
            throw new SQLException("JDBC Driver not found.", e);
        }
    }

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("🏨 Welcome to the Simple Hotel Reservation System! 🏨");

            while (true) {
                System.out.println("\n--- Main Menu ---");
                System.out.println("1. Reserve a Room");
                System.out.println("2. View All Reservations");
                System.out.println("3. Edit a Reservation");
                System.out.println("4. Delete a Reservation (Check-out)");
                System.out.println("5. Exit");
                System.out.print("Enter your choice (1-5): ");

                if (scanner.hasNextInt()) {
                    int choice = scanner.nextInt();
                    scanner.nextLine(); // Consume newline

                    switch (choice) {
                        case 1:
                            reserveRoom(scanner);
                            break;
                        case 2:
                            viewReservations();
                            break;
                        case 3:
                            editReservation(scanner);
                            break;
                        case 4:
                            deleteReservation(scanner);
                            break;
                        case 5:
                            System.out.println("Thank you for using the system. Goodbye!");
                            return;
                        default:
                            System.out.println("Invalid choice. Please enter a number between 1 and 5.");
                    }
                } else {
                    System.out.println("Invalid input. Please enter a number.");
                    scanner.nextLine(); // Consume invalid input
                }
            }
        } catch (SQLException e) {
            System.err.println("A database error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handles the process of making a new room reservation.
     * @param scanner The Scanner object for user input.
     */
    private static void reserveRoom(Scanner scanner) {
        System.out.println("\n--- New Room Reservation ---");
        try (Connection conn = getConnection()) {
            System.out.print("Enter Guest Name: ");
            String guestName = scanner.nextLine();
            
            System.out.print("Enter Room Number: ");
            int roomNumber = scanner.nextInt();
            scanner.nextLine(); 

            System.out.print("Enter Contact Number (e.g., 555-1234): ");
            String contactNumber = scanner.nextLine();
            
            // Using current date as reservation date for simplicity
            Date reservationDate = new Date(System.currentTimeMillis());

            String sql = "INSERT INTO reservations (guest_name, room_number, contact_number, reservation_date) VALUES (?, ?, ?, ?)";
            try (PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
                preparedStatement.setString(1, guestName);
                preparedStatement.setInt(2, roomNumber);
                preparedStatement.setString(3, contactNumber);
                preparedStatement.setDate(4, reservationDate);

                int rowsAffected = preparedStatement.executeUpdate();

                if (rowsAffected > 0) {
                    System.out.println("✅ Reservation successful for " + guestName + " in room " + roomNumber + ".");
                } else {
                    System.out.println("❌ Reservation failed. No rows affected.");
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Error reserving room: " + e.getMessage());
        } catch (java.util.InputMismatchException e) {
             System.err.println("❌ Invalid input for room number. Please restart the reservation process.");
             scanner.nextLine(); // Clear the buffer
        }
    }

    /**
     * Retrieves and displays all current reservations from the database.
     */
    private static void viewReservations() {
        System.out.println("\n--- All Current Reservations ---");
        try (Connection conn = getConnection()) {
            String sql = "SELECT * FROM reservations ORDER BY reservation_id DESC";
            try (Statement statement = conn.createStatement();
                 ResultSet resultSet = statement.executeQuery(sql)) {

                if (!resultSet.isBeforeFirst()) {
                    System.out.println("No reservations found.");
                    return;
                }

                System.out.println("+----------------+----------------------+-------------+--------------------+----------------+");
                System.out.println("| ID             | Guest Name           | Room Number | Contact Number     | Reservation Date |");
                System.out.println("+----------------+----------------------+-------------+--------------------+----------------+");

                while (resultSet.next()) {
                    int id = resultSet.getInt("reservation_id");
                    String name = resultSet.getString("guest_name");
                    int room = resultSet.getInt("room_number");
                    String contact = resultSet.getString("contact_number");
                    Date date = resultSet.getDate("reservation_date");

                    System.out.printf("| %-14d | %-20s | %-11d | %-18s | %-14s |\n",
                            id, name, room, contact, date.toString());
                }
                System.out.println("+----------------+----------------------+-------------+--------------------+----------------+");
            }
        } catch (SQLException e) {
            System.err.println("❌ Error viewing reservations: " + e.getMessage());
        }
    }

    /**
     * Allows modification of an existing reservation's details.
     * @param scanner The Scanner object for user input.
     */
    private static void editReservation(Scanner scanner) {
        System.out.println("\n--- Edit Reservation ---");
        viewReservations(); // Show current reservations first
        System.out.print("Enter the Reservation ID to edit: ");
        
        try (Connection conn = getConnection()) {
            int reservationId = scanner.nextInt();
            scanner.nextLine(); // Consume newline

            System.out.print("Enter NEW Guest Name (or press Enter to keep current): ");
            String newName = scanner.nextLine();
            
            System.out.print("Enter NEW Room Number (or enter 0 to keep current): ");
            String roomInput = scanner.nextLine();
            Integer newRoom = roomInput.isEmpty() || roomInput.equals("0") ? null : Integer.parseInt(roomInput);

            System.out.print("Enter NEW Contact Number (or press Enter to keep current): ");
            String newContact = scanner.nextLine();

            // Fetch current data to populate defaults
            String selectSql = "SELECT guest_name, room_number, contact_number FROM reservations WHERE reservation_id = ?";
            String currentName = null;
            Integer currentRoom = null;
            String currentContact = null;
            
            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                selectStmt.setInt(1, reservationId);
                ResultSet rs = selectStmt.executeQuery();
                if (rs.next()) {
                    currentName = rs.getString("guest_name");
                    currentRoom = rs.getInt("room_number");
                    currentContact = rs.getString("contact_number");
                } else {
                    System.out.println("❌ Reservation ID not found.");
                    return;
                }
            }

            // Use new values if provided, otherwise use current values
            String finalName = newName.isEmpty() ? currentName : newName;
            int finalRoom = newRoom == null ? currentRoom : newRoom.intValue();
            String finalContact = newContact.isEmpty() ? currentContact : newContact;


            String updateSql = "UPDATE reservations SET guest_name = ?, room_number = ?, contact_number = ? WHERE reservation_id = ?";
            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setString(1, finalName);
                updateStmt.setInt(2, finalRoom);
                updateStmt.setString(3, finalContact);
                updateStmt.setInt(4, reservationId);
                
                int rowsAffected = updateStmt.executeUpdate();

                if (rowsAffected > 0) {
                    System.out.println("✅ Reservation ID " + reservationId + " updated successfully.");
                } else {
                    System.out.println("❌ Update failed (perhaps no changes were made).");
                }
            }
        } catch (java.util.InputMismatchException | NumberFormatException e) {
            System.err.println("❌ Invalid input. Please enter a valid number for the ID or Room Number.");
            scanner.nextLine(); 
        } catch (SQLException e) {
            System.err.println("❌ Error editing reservation: " + e.getMessage());
        }
    }

    /**
     * Removes an existing reservation from the database.
     * @param scanner The Scanner object for user input.
     */
    private static void deleteReservation(Scanner scanner) {
        System.out.println("\n--- Delete Reservation (Check-out) ---");
        viewReservations(); // Show current reservations first
        System.out.print("Enter the Reservation ID to delete: ");

        try (Connection conn = getConnection()) {
            int reservationId = scanner.nextInt();
            scanner.nextLine(); // Consume newline

            String sql = "DELETE FROM reservations WHERE reservation_id = ?";
            try (PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
                preparedStatement.setInt(1, reservationId);

                int rowsAffected = preparedStatement.executeUpdate();

                if (rowsAffected > 0) {
                    System.out.println("✅ Reservation ID " + reservationId + " deleted (Checked-out) successfully.");
                } else {
                    System.out.println("❌ Deletion failed. Reservation ID " + reservationId + " not found.");
                }
            }
        } catch (java.util.InputMismatchException e) {
            System.err.println("❌ Invalid input. Please enter a valid Reservation ID.");
            scanner.nextLine(); 
        } catch (SQLException e) {
            System.err.println("❌ Error deleting reservation: " + e.getMessage());
        }
    }
}