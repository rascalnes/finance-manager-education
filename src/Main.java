import nes.finance.cli.CLIApplication;

public class Main {
    public static void main(String[] args) {
        try {
            CLIApplication app = new CLIApplication();

            // Обработчик завершения работы (Ctrl+C)
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\nЗавершение работы приложения...");
            }));

            app.run();
        } catch (Exception e) {
            System.err.println("Критическая ошибка: " + e.getMessage());
            e.printStackTrace();
        }
    }
}