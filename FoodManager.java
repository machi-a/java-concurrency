import java.io.*;
import java.security.PublicKey;
import java.util.concurrent.*;
import javax.sound.sampled.SourceDataLine;
import javax.sql.rowset.spi.SyncResolver;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;

public class FoodManager {
    static int numHotdog, numBurger, numSlots, numHotdogMakers, numBurgerMakers, numHotdogPackers, numBurgerPackers;
    static String filename = "logfile.txt";
    static int hotdogWorkTime = 3;
    static int burgerWorkTime = 3;
    static int writeTime = 1;
    static int packTime = 2;
    static int takeFromQueueTime = 1;
    static volatile int hotdogIndex = 0;
    static volatile int burgerIndex = 0;
    static volatile int numHotdogPacked = 0;
    static volatile int numBurgerPacked = 0;
    static volatile boolean queueInUse = false;
    static volatile boolean createHotdog = false;
    static volatile boolean createBurger = false;
    static Object queueLock = new Object();
    static Object burgerLock = new Object();
    static Object hotdogLock = new Object();

    static void gowork(int n_seconds) {
//         try {
//             Thread.sleep(1000);
//         } catch (InterruptedException ex) {
//             Thread.currentThread().interrupt();
//         }
        for (int i = 0; i < n_seconds; i++) {
            long n = 300000000;
            while (n > 0) { n--; }
        }
    }

    static void cleanUpFiles(String text) {
        File targetFile = new File(text);
        targetFile.delete();
    }

    static boolean writeFile(String text) {
        try (FileWriter f = new FileWriter(filename, true);
             BufferedWriter b = new BufferedWriter(f);
             PrintWriter p = new PrintWriter(b)) {
            p.println(text);
            return true;
        } catch (IOException i) {
            i.printStackTrace();
            return false;
        }
    }
    static boolean createFile(String text) {
        try {
            File myObj = new File(text);
            if (myObj.createNewFile()) {
                System.out.println("File created: " + myObj.getName());
                return true;
            }
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
        return false;
    }
    public static void main(String[] args) {
        
        // store parameters
        numHotdog = Integer.parseInt(args[0]); // number of hotdogs to produce and pack
        numBurger = Integer.parseInt(args[1]); // number of burgers to produce and pack
        numSlots = Integer.parseInt(args[2]); // number of slots in the packing queue
        numHotdogMakers = Integer.parseInt(args[3]); // number of hotdog makers
        numBurgerMakers = Integer.parseInt(args[4]); // number of burger makers
        numHotdogPackers = Integer.parseInt(args[5]); // number of hotdog packers
        numBurgerPackers = Integer.parseInt(args[6]); // numebr of burger packers

        // print parameters
        System.out.println("hotdogs: " + numHotdog);
        System.out.println("burgers: " + numBurger);
        System.out.println("capacity: " + numSlots);
        System.out.println("hotdog makers: " + numHotdogMakers);
        System.out.println("burger makers: " + numBurgerMakers);
        System.out.println("hotdog packers: " + numHotdogPackers);
        System.out.println("burger packers: " + numBurgerPackers);

        // initialise neccessary variables
        Buffer buffer = new Buffer(numSlots);
        cleanUpFiles(filename);
        createFile(filename);

        // create runnables for each type of thread

        Runnable HotdogMakerRunnable = new Runnable() {
            @Override
            public void run() {
                String thread_name = Thread.currentThread().getName(); // store thread name
                while(hotdogIndex != numHotdog){ // run until all hotdogs have been produced
                    Food newFood; // initialise Food Object
                    synchronized (hotdogLock){
                        if (hotdogIndex != numHotdog)
                            newFood = new Food('H', hotdogIndex++, thread_name); // create a hotdog
                        else break;
                    }
                    gowork(hotdogWorkTime); // spend time required to make a hotdog
                    writeFile(thread_name + " puts hotdog id:" + Integer.toString(newFood.getId())); // write to logfile
                    buffer.put(newFood);
                    gowork(writeTime); // spend time to write to file
                }
            }
        };

        Runnable BurgerMakerRunnable = new Runnable() {
            @Override
            public void run() {
                String thread_name = Thread.currentThread().getName();
                while(burgerIndex != numBurger){ // run until all burgers have been produced
                    Food newFood; // initialise Food Object
                    synchronized (burgerLock){
                        if (burgerIndex != numBurger)
                            newFood = new Food('B', burgerIndex++, thread_name); // create a hotdog
                        else break;
                    }
                    gowork(burgerWorkTime); // spend time required to make a hotdog
                    writeFile(thread_name + " puts burger id:" + Integer.toString(newFood.getId())); // write to logfile
                    buffer.put(newFood);
                    gowork(writeTime); // spend time to write to file
                }
            }
        };
        Runnable HotdogPackerRunnable = new Runnable() {
            @Override
            public void run() {
                String thread_name = Thread.currentThread().getName();
            }
        };
        Runnable BurgerPackerRunnable = new Runnable() {
            @Override
            public void run() {
                String thread_name = Thread.currentThread().getName();
                boolean check = false;
                while (numBurgerPacked != numBurger){
                    Food newFood;
                    synchronized (queueLock){
                        if (numBurgerPacked != numBurger && buffer.checkType() == 'B'){
                            newFood = buffer.get();
                            numBurgerPacked++;
                            gowork(takeFromQueueTime);
                            check = true;
                        }
                    }
                    if (check){
                        gowork(packTime);
                        writeFile(thread_name + " gets burger id:" + Integer.toString(newFood.getId()) + " from " + newFood.getMachineId());


                    }

                }
            }
        };


        // initialise threads
        Thread[] HotdogMakerThreads = new Thread[numHotdogMakers];
        for (int i = 0; i < HotdogMakerThreads.length; i++) {
            String name = "hm";
            HotdogMakerThreads[i] = new Thread(HotdogMakerRunnable, name.concat(Integer.toString(i)));
            HotdogMakerThreads[i].start();
        }
        Thread[] BurgerMakerThreads = new Thread[numBurgerMakers];
        for (int i = 0; i < BurgerMakerThreads.length; i++) {
            String name = "bm";
            BurgerMakerThreads[i] = new Thread(BurgerMakerRunnable, name.concat(Integer.toString(i)));
            BurgerMakerThreads[i].start();
        }
        Thread[] HotdogPackerThreads = new Thread[numHotdogPackers];
        for (int i = 0; i < HotdogPackerThreads.length; i++) {
            String name = "hp";
            HotdogPackerThreads[i] = new Thread(HotdogPackerRunnable, name.concat(Integer.toString(i)));
            HotdogPackerThreads[i].start();
        }
        Thread[] BurgerPackerThreads = new Thread[numBurgerPackers];
        for (int i = 0; i < BurgerPackerThreads.length; i++) {
            String name = "bp";
            BurgerPackerThreads[i] = new Thread(BurgerPackerRunnable, name.concat(Integer.toString(i)));
            BurgerPackerThreads[i].start();
        }

        try {
            Thread.sleep(2000);
            while(Thread.activeCount() != 1);
            buffer.returnQueue();


        } catch (InterruptedException e) { e.printStackTrace();}
    }
}

class Food {
    char type; // either 'H' or 'B' (hotdog or burger) 
    int id; // unqiue for each type
    String machineId; // machine that produced it

    public Food(char type, int id, String machineId) {
        this.type = type;
        this.id = id;
        this.machineId = machineId;
    }
    public int getId() { return id; }

    public char getType() { return type; }

    public String getMachineId() {
        return machineId;
    }
}

class Buffer {

    private final Food[] buffer;
    private int front = 0, back = 0;
    public int item_count = 0;

    Buffer(int size) {
        buffer = new Food[size];
    }

    public synchronized void put(Food food) {

        while (item_count == buffer.length) {
            try {
                this.wait();
            } catch (InterruptedException e) {
            }
        }
        buffer[back] = food;
        back = (back + 1) % buffer.length;
        item_count++;
        this.notifyAll();

    }

    public synchronized Food get() {

        while (item_count == 0) {
            try {
                this.wait();
            } catch (InterruptedException e) {
            }
        }

        Food food = buffer[front];
        front = (front + 1) % buffer.length;
        item_count--;
        this.notifyAll();

        return food;
    }

    public synchronized char checkType() {
        if (item_count != 0) {
            return buffer[front].getType();
        } else {
            return 'z';
        }
    }

    public synchronized boolean checkEmpty() {
        return item_count == 0;
    }

    public void returnQueue() {
        String result = "";
        for (int i = 0; i < item_count; i++) {
            result = result + buffer[i].getType() + buffer[i].getId() + " ";
        }
        System.out.println(result);
    }
}