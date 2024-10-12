package org.broker;

import java.io.*; // Импортируем классы для работы с вводом и выводом
import java.net.Socket; // Импортируем класс для работы с клиентскими сокетами
import java.net.UnknownHostException; // Импортируем класс для обработки неизвестного хоста
import java.util.Scanner; // Импортируем класс Scanner для чтения ввода пользователя

// Основной класс клиента
public class Client {

    // Константа для определения порта, к которому будет подключаться клиент
    public static int PORT = 1234;

    // Переменные для работы с потоками ввода/вывода
    private PrintWriter out; // Поток для отправки данных на сервер
    private BufferedReader in; // Поток для чтения данных от сервера
    private Scanner scanner; // Объект для чтения команд с консоли

    // Точка входа в программу
    public static void main(String[] args) {
        new Client().startClient(); // Создаем экземпляр клиента и запускаем его
    }

    // Метод для инициализации и запуска клиента
    public void startClient() {
        try (Socket socket = new Socket("localhost", PORT)) { // Создаем сокет для подключения к серверу
            // Инициализация Scanner для чтения команд с консоли
            scanner = new Scanner(System.in);
            out = new PrintWriter(socket.getOutputStream(), true); // Инициализация потока для отправки данных на сервер
            in = new BufferedReader(new InputStreamReader(socket.getInputStream())); // Инициализация потока для чтения данных от сервера

            System.out.print("Вводите команду: "); // Подсказка пользователю о вводе команды

            // Создание потока для чтения ответов от сервера с использованием лямбда-выражения
            new Thread(() -> {
                String response; // Переменная для хранения ответа от сервера
                try {
                    // Бесконечный цикл для получения ответов от сервера
                    while ((response = in.readLine()) != null) {
                        // Печатаем ответ от сервера
                        System.out.println("Сервер: " + response);
                        // После получения ответа запрашиваем команду
                        System.out.print("Вводите команду: ");
                    }
                } catch (IOException e) {
                    System.out.println("Вы вышли из сети!!!"); // Обработка исключения при выходе из сети
                }
            }).start(); // Запуск потока для чтения ответов от сервера

            // Основной цикл для ввода команд
            while (true) {
                // Считывание команды
                String command = scanner.nextLine(); // Считывание всей строки с консоли
                // Проверка на пустую команду
                if (command.trim().isEmpty()) {
                    System.out.println("Команда не должна быть пустой."); // Сообщение о пустой команде
                    System.out.print("Вводите команду: "); // Запрос на ввод команды
                    continue; // Пропустить текущую итерацию
                }

                // Проверка на команду выхода
                if ("exit".equalsIgnoreCase(command.trim())) {
                    System.out.println("Выход из клиента."); // Сообщение о выходе
                    break; // Завершить выполнение
                }

                // Проверка на команду помощи
                if ("help".equalsIgnoreCase(command.trim())) {
                    System.out.println("Доступные команды:"); // Сообщение о доступных командах
                    System.out.println("send <queue> - подключиться как отправитель к очереди"); // Подсказка по команде send
                    System.out.println("receive <queue> - подключиться как получатель к очереди"); // Подсказка по команде receive
                    System.out.println("message <message> - отправить сообщение в текущую очередь"); // Подсказка по команде message
                    System.out.println("exit - выход из клиента"); // Подсказка по команде exit
                    System.out.print("Вводите команду: "); // Запрос на ввод команды
                    continue; // Пропустить текущую итерацию
                }

                // Проверка на наличие параметра в команде
                if (command.split(" ").length == 1) {
                    System.out.println("Команда, должна состоять из самой команды и параметра."); // Сообщение о необходимости параметра
                    System.out.print("Вводите команду: "); // Запрос на ввод команды
                    continue; // Пропустить текущую итерацию
                }

                // Отправка команды на сервер
                out.println(command); // Отправка команды на сервер
            }

        } catch (UnknownHostException e) {
            // Обработка ошибки при неизвестном хосте
            System.err.println("Не удалось установить соединение с сервером: " + e.getMessage());
        } catch (IOException e) {
            // Обработка ошибок ввода-вывода
            System.err.println("Ошибка ввода-вывода: " + e.getMessage());
        } finally {
            // Закрытие ресурсов
            if (scanner != null) {
                scanner.close(); // Закрытие сканера
            }
        }
    }
}
