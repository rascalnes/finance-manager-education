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
            System.err.println("Критическая ошибка в приложении: " + e.getMessage());
            e.printStackTrace();
            System.out.println("\nДля получения помощи:");
            System.out.println("  Проверьте, что все файлы скомпилированы правильно");
            System.out.println("  Убедитесь, что есть права на запись в директорию data/");
            System.out.println("  Попробуйте удалить поврежденные файлы в директории data/");
        }
    }
}