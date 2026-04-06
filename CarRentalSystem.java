import java.io.*;
import java.util.*;

class CarUnavailableException extends Exception {
    public CarUnavailableException(String message) { super(message); }
}

class PaymentFailedException extends Exception {
    public PaymentFailedException(String message) { super(message); }
}

class Car {
    protected String carId, model;
    protected double pricePerDay;
    protected boolean available;

    public Car(String carId, String model, double pricePerDay) {
        this.carId = carId;
        this.model = model;
        this.pricePerDay = pricePerDay;
        this.available = true;
    }

    public String getCarId() { return carId; }
    public String getModel() { return model; }
    public double getPricePerDay() { return pricePerDay; }
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
    public String getType() { return "Car"; }

    public String toString() {
        return "[" + getType() + "] " + carId + " - " + model + " | Rs." + pricePerDay + "/day | " + (available ? "Available" : "Booked");
    }
}

class SUV extends Car {
    public SUV(String carId, String model, double pricePerDay) { super(carId, model, pricePerDay); }
    public String getType() { return "SUV"; }
}

class Sedan extends Car {
    public Sedan(String carId, String model, double pricePerDay) { super(carId, model, pricePerDay); }
    public String getType() { return "Sedan"; }
}

class RentalManager {

    private List<Car> inventory = new ArrayList<>();
    private Map<String, String> reservations = new HashMap<>();

    class PaymentProcessor {
        void processPayment(String userId, double amount) throws PaymentFailedException {
            if (amount <= 0) throw new PaymentFailedException("Invalid payment amount: " + amount);
            System.out.println("Payment of Rs." + amount + " processed for user: " + userId);
            logTransaction("PAYMENT | User: " + userId + " | Amount: Rs." + amount);
        }
    }

    public void addCar(Car car) { inventory.add(car); }

    public void showAvailableCars() {
        System.out.println("\n--- Available Cars ---");
        for (Car car : inventory)
            if (car.isAvailable()) System.out.println(car);
    }

    public synchronized void bookCar(String userId, String carId, int days)
            throws CarUnavailableException, PaymentFailedException {

        Car selectedCar = null;
        for (Car car : inventory)
            if (car.getCarId().equals(carId)) { selectedCar = car; break; }

        if (selectedCar == null || !selectedCar.isAvailable())
            throw new CarUnavailableException("Car " + carId + " is not available.");

        selectedCar.setAvailable(false);
        reservations.put(userId, carId);

        double total = selectedCar.getPricePerDay() * days;
        new PaymentProcessor().processPayment(userId, total);

        logTransaction("BOOKING | User: " + userId + " | Car: " + carId + " | Days: " + days + " | Total: Rs." + total);
        System.out.println("Booking confirmed for " + userId + " -> " + carId);
    }

    public synchronized void returnCar(String userId) {
        String carId = reservations.get(userId);
        if (carId == null) { System.out.println("No reservation found for: " + userId); return; }
        for (Car car : inventory)
            if (car.getCarId().equals(carId)) { car.setAvailable(true); break; }
        reservations.remove(userId);
        logTransaction("RETURN | User: " + userId + " | Car: " + carId);
        System.out.println("Car " + carId + " returned by " + userId);
    }

    private void logTransaction(String record) {
        try (FileWriter fw = new FileWriter("rental_log.txt", true)) {
            fw.write(record + "\n");
        } catch (IOException e) {
            System.out.println("File write error: " + e.getMessage());
        }
    }

    public void printLog() {
        System.out.println("\n--- Transaction Log ---");
        try (BufferedReader br = new BufferedReader(new FileReader("rental_log.txt"))) {
            String line;
            while ((line = br.readLine()) != null) System.out.println(line);
        } catch (IOException e) {
            System.out.println("Log file not found.");
        }
    }
}

public class CarRentalSystem {
    public static void main(String[] args) throws InterruptedException {

        RentalManager manager = new RentalManager();

        manager.addCar(new Sedan("S01", "Honda City", 1500));
        manager.addCar(new Sedan("S02", "Maruti Dzire", 1200));
        manager.addCar(new SUV("U01", "Toyota Fortuner", 3000));
        manager.addCar(new SUV("U02", "Mahindra XUV700", 2500));

        manager.showAvailableCars();

        Thread t1 = new Thread(() -> {
            try { manager.bookCar("Aryan", "S01", 3); }
            catch (CarUnavailableException | PaymentFailedException e) { System.out.println("Booking failed: " + e.getMessage()); }
        });

        Thread t2 = new Thread(() -> {
            try { manager.bookCar("Aayush", "S01", 2); }
            catch (CarUnavailableException | PaymentFailedException e) { System.out.println("Booking failed: " + e.getMessage()); }
        });

        Thread t3 = new Thread(() -> {
            try { manager.bookCar("Rushil", "U01", 5); }
            catch (CarUnavailableException | PaymentFailedException e) { System.out.println("Booking failed: " + e.getMessage()); }
        });

        t1.start(); t2.start(); t3.start();
        t1.join();  t2.join();  t3.join();

        manager.showAvailableCars();
        manager.returnCar("Aryan");
        manager.showAvailableCars();
        manager.printLog();
    }
}
