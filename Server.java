package org.broker_server;

import java.io.IOException; // Импортируем необходимые библиотеки для работы с вводом/выводом и сетевыми соединениями
import java.net.ServerSocket; // Импорт для создания серверного сокета
import java.net.Socket; // Импорт для работы с клиентскими сокетами
import java.io.*; // Импортируем все классы для работы с вводом/выводом
import java.util.*; // Импортируем классы из стандартной библиотеки, такие как HashMap и Queue

// Основной класс сервера
public class Server {

    // Константа, определяющая порт, на котором будет слушать сервер
    public static final int PORT = 1234;

    // Хранит все именованные очереди, ключ - имя очереди, значение - очередь сообщений
    static Map<String, Queue<String>> namedQueues = new HashMap<>();

    // Переменная для хранения выбранной очереди
    static Queue<String> retrievedQueue;

    // Переменная для аргументов команд
    public static String arguments = null;

    // Главный метод, который запускает сервер
    public static void main(String[] args) {

        // Создаем таймер для периодической очистки очередей
        Timer timer = new Timer(true); // true делает таймер демоном, чтобы он завершал работу при закрытии программы
        timer.scheduleAtFixedRate(new TimerTask() { // Запланируем задачу на выполнение с фиксированным интервалом
            @Override
            public void run() {
                cleanUpQueues(); // Вызов метода очистки очередей
            }
        }, 0, 30 * 1000); // Запускать каждую 30 секунд

        try (ServerSocket serverSocket = new ServerSocket(PORT)) { // Создаем серверный сокет, который будет слушать указанный порт
            System.out.println("Сервер подключен!!!"); // Уведомляем о запуске сервера
            while (true) { // Бесконечный цикл для принятия подключений
                new StartServer(serverSocket.accept()).start(); // Принимаем новое подключение и создаем новый поток для обработки
            }
        } catch (IOException e) {
            throw new RuntimeException(e); // Обработка исключений при создании сервера
        }
    }

    // Метод для очистки пустых очередей
    public static void cleanUpQueues() {
        // Удаляем очереди, которые пустые
        namedQueues.entrySet().removeIf(entry -> entry.getValue().isEmpty()); // Удаляем пустые очереди из карты
        System.out.println("Очереди очищены от пустых очередей."); // Сообщение о очистке
    }

    // Вложенный класс, представляющий обработчик для каждого клиента
    public static class StartServer extends Thread {

        private final Socket socket; // Сокет, который будет использоваться для связи с клиентом
        private BufferedReader in; // Поток для чтения данных от клиента
        private static PrintWriter out; // Поток для отправки данных клиенту

        // Конструктор для инициализации сокета
        public StartServer(Socket socket) {
            this.socket = socket; // Присваиваем сокет
        }

        // Метод, выполняемый в потоке
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream())); // Инициализация потока для чтения
                out = new PrintWriter(socket.getOutputStream(), true); // Инициализация потока для записи

                String commandClient; // Переменная для хранения команды от клиента
                while ((commandClient = in.readLine()) != null) { // Читаем команды от клиента
                    cmd(commandClient); // Обработка полученной команды
                }

            } catch (IOException e) {
                System.out.println("Клиент вышел из сети!!!"); // Сообщение о том, что клиент отключился
            } finally {
                try {
                    socket.close(); // Закрываем сокет после завершения работы
                } catch (IOException e) {
                    throw new RuntimeException(e); // Обработка исключений при закрытии сокета
                }
            }
        }

        // Метод для обработки команд от клиента
        public static void cmd(String cmd) {
            String[] clientMessage = cmd.split(" ", 2); // Разделяем команду и аргументы
            String command = clientMessage[0]; // Первое слово - это команда
            switch (command) {
                case "send": // Команда для создания новой очереди
                    arguments = clientMessage.length > 1 ? clientMessage[1] : ""; // Получаем имя очереди
                    if (!queueExists(arguments)) { // Проверяем, существует ли очередь с таким именем
                        namedQueues.put(arguments, new LinkedList<>()); // Создаем новую очередь
                        out.println("Очередь с именем '" + arguments + "' создана, вводите сообщение."); // Сообщение об успехе
                    } else {
                        out.println("Очередь с именем '" + arguments + "' уже существует."); // Сообщение о существующей очереди
                    }
                    break;

                case "receive": // Команда для получения сообщения из очереди
                    arguments = clientMessage.length > 1 ? clientMessage[1] : ""; // Получаем имя очереди
                    if (!queueExists(arguments)) { // Проверяем, существует ли очередь
                        out.println("Очередь с именем '" + arguments + "' не существует, зайдите позже или создайте её сами.");
                        arguments = null; // Сброс переменной
                    } else {
                        retrievedQueue = namedQueues.get(arguments); // Получаем выбранную очередь
                        if (retrievedQueue.isEmpty()) { // Проверка на пустоту конкретной очереди
                            out.println("Очередь с именем '" + arguments + "' пустая."); // Сообщение о пустой очереди
                        } else {
                            out.println("Получено сообщение: " + retrievedQueue.poll()); // Извлекаем и возвращаем сообщение
                        }
                        arguments = null; // Сброс переменной
                    }
                    break;

                case "message": // Команда для отправки сообщения в очередь
                    String arguments_message = clientMessage.length > 1 ? clientMessage[1] : ""; // Получаем сообщение
                    if (arguments != null) { // Проверяем, выбрана ли очередь
                        retrievedQueue = namedQueues.get(arguments); // Получаем выбранную очередь
                        if (retrievedQueue != null) { // Проверяем, была ли выбрана очередь
                            retrievedQueue.add(arguments_message); // Добавляем сообщение в очередь
                            out.println("Сообщение " + arguments_message + " отправлено."); // Сообщение об успехе
                        } else {
                            out.println("Ошибка: очередь не выбрана."); // Сообщение об ошибке
                        }
                    } else {
                        out.println("Ошибка: вы не выбрали очередь для отправки сообщения."); // Сообщение об ошибке
                    }
                    break;

                default: // Если команда не распознана
                    out.println("Такой команды нет!!!"); // Сообщение о неверной команде
                    break;
            }
        }

        // Метод для проверки существования очереди
        public static boolean queueExists(String queueName) {
            return namedQueues.containsKey(queueName); // Проверяем наличие очереди в карте
        }
    }
}
