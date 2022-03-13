import java.io.*;
import java.util.*;

public class FoodManager {
    static int numHotdog, numBurger, numSlots, numHotdogMakers, numBurgerMakers, numHotdogPackers, numBurgerPackers;
    static String filename = "logfile.txt";
    static int hotdogWorkTime = 3; // time taken to produce a hotdog
    static int burgerWorkTime = 3; // time taken to produce a burger
    static int writeTime = 1; // time taken to send or retrieve food items from queue
    static int packTime = 2; // time taken to pack
    static int recordedMadeBurgers = 0;
    static int recordedMadeHotdogs = 0;
    static int recordedPackedBurgers = 0;
    static int recordedPackedHotdogs = 0;
    static volatile int hotdogIndex = 0; // number of hotdog produced
    static volatile int burgerIndex = 0; // number of burgers produced
    static volatile int numHotdogPacked = 0; // number of hotdog packed
    static volatile int numBurgerPacked = 0; // number of burgers packed
    static volatile int numHotdogsTaken = 0;

    static Object queueLock = new Object();
    static Object burgerLock = new Object();
    static Object hotdogLock = new Object();

    static void gowork(int n_seconds) {
         try {
             Thread.sleep(100);
         } catch (InterruptedException ex) {
             Thread.currentThread().interrupt();
         }
//        for (int i = 0; i < n_seconds; i++) {
//            long n = 300000000;
//            while (n > 0) { n--; }
//        }
    }
    // to delete files
    static void cleanUpFiles(String text) {
        File targetFile = new File(text);
        targetFile.delete();
    }

    // to append to a file
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

    // to crete a file
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
        Buffer buffer = new Buffer(numSlots); // create buffer
        cleanUpFiles(filename); // delete any old logfile
        createFile(filename); // create logfile
        Hashtable<String, Integer> summary = new Hashtable<String, Integer>(); // create summary table

        // create runnables for each type of thread

        Runnable HotdogMakerRunnable = new Runnable() {
            @Override
            public void run() {
                String thread_name = Thread.currentThread().getName(); // store thread name
                while(hotdogIndex != numHotdog){ // run until all hotdogs have been produced
                    Food newFood; // initialise Food Object
                    synchronized (hotdogLock){
                        if (hotdogIndex != numHotdog) // second check if there are still hotdogs not produced
                            newFood = new Food('H', hotdogIndex++, thread_name); // create a hotdog
                        else break; // if there are not more hotdogs to produce, the thread can end
                    }
                    gowork(hotdogWorkTime); // spend time to make a hotdog
                    writeFile(thread_name + " puts hotdog id:" + Integer.toString(newFood.getId())); // write to logfile
                    buffer.put(newFood); // add hotdog to queue
                    gowork(writeTime); // spend time to send to queue
                    summary.replace(thread_name, summary.get(thread_name) + 1); // record the production by this machine
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
                        if (burgerIndex != numBurger) // second check if there are still burgers to produce
                            newFood = new Food('B', burgerIndex++, thread_name); // create a burger
                        else break; // if there are not more burgers to produce, the thread can end
                    }
                    gowork(burgerWorkTime); // spend time required to make a burger
                    writeFile(thread_name + " puts burger id:" + Integer.toString(newFood.getId())); // write to logfile
                    buffer.put(newFood); // add burger to queue
                    gowork(writeTime); // spend time to send to queue
                    summary.replace(thread_name, summary.get(thread_name) + 1); // record the production by this machine
                }
            }
        };
        Runnable HotdogPackerRunnable = new Runnable() {
            @Override
            public void run() {
                String thread_name = Thread.currentThread().getName();
                Thread.currentThread().setPriority(Thread.MIN_PRIORITY); // set default priority of thread to lowest
                int hotdogsContained = 0; // record total number of hotdogs in packers at each instance
                Food[] store = new Food[2]; // create two slots for 2 hotdogs required to pack
                boolean check = false;
                while (numHotdogPacked != numHotdog){ // run until all hotdogs have been packed
                    Food newFood = new Food(); // initialise Food object
                    if (buffer.checkType() == 'H'){ // check if next in queue is a hotdog
                        synchronized (  queueLock){ // lock the queue
                            if (numHotdogPacked != numHotdog && buffer.checkType() == 'H'){ // double check if there is still a need to pack hotdogs AND if next in queue is a hotdog
                                    if (hotdogsContained == 0) { // if the packer has no hotdogs on hand
                                        if (numHotdogsTaken < hotdogIndex - numHotdogPacked) { // if there is less hotdogs left that hotdogs with all packers, let packers with hotdogs take from queue first
                                            store[0] = buffer.get(); // take from queue
                                            numHotdogPacked++;
                                            gowork(writeTime); // time to taken to take from queue
                                            check = true; // indicate that a hotdog was retrieved
                                            Thread.currentThread().setPriority(Thread.MAX_PRIORITY); // set to highest priority to retreive a hotdog
                                            hotdogsContained += 1;
                                            numHotdogsTaken += 1;
                                        }
                                    } else if (hotdogsContained == 1) {
                                        store[1] = buffer.get(); // take from queue
                                        numHotdogPacked++;
                                        gowork(writeTime); // time to taken to take from queue
                                        check = true; // indicate that a hotdog was retrieved
                                        Thread.currentThread().setPriority(Thread.MIN_PRIORITY); // set to lowest priority to let packers with one hotdog retreive a hotdog
                                        hotdogsContained += 1;
                                        numHotdogsTaken += 1;
                                    }

                            }
                        } 
                    }              
                    if (check){ // if a hotdog was taken
                        if (hotdogsContained == 2){ // if the packer has two hotdogs
                            summary.replace(thread_name, summary.get(thread_name) + 2); // record that it has packed two hotdogs
                            hotdogsContained = 0; // rest the counter to 0
                            writeFile(thread_name + " gets hotdog id:" + Integer.toString(store[0].getId()) + " from " + store[0].getMachineId() + " and id: " + Integer.toString(store[1].getId()) + " from " + store[1].getMachineId()); // write to file
                            gowork(packTime); // time to take to pack
                            numHotdogsTaken -= 2; // reduce the number of hotdogs with packers by 2
                        }
                        check = false;
                    }

                }
            }
        };
        Runnable BurgerPackerRunnable = new Runnable() {
            @Override
            public void run() {
                String thread_name = Thread.currentThread().getName();
                boolean check = false;
                while (numBurgerPacked != numBurger){
                    Food newFood = new Food();
                    if (buffer.checkType() == 'B'){
                        synchronized (queueLock){
                            if (numBurgerPacked != numBurger && buffer.checkType() == 'B'){
                                newFood = buffer.get(); // take from queue
                                numBurgerPacked++;
                                gowork(writeTime); // time to taken to take from queue
                                summary.replace(thread_name, summary.get(thread_name) + 1);
                                check = true;
                            }
                        }
                    }
                    
                    if (check){
                        gowork(packTime); // time to take to pack
                        writeFile(thread_name + " gets burger id:" + Integer.toString(newFood.getId()) + " from " + newFood.getMachineId()); // write to file
                        check = false;
                    }

                }
            }
        };


        // initialise threads
        Thread[] HotdogMakerThreads = new Thread[numHotdogMakers];
        for (int i = 0; i < HotdogMakerThreads.length; i++) {
            String name = "hm";
            HotdogMakerThreads[i] = new Thread(HotdogMakerRunnable, name.concat(Integer.toString(i)));
            summary.put(name.concat(Integer.toString(i)), 0);
            HotdogMakerThreads[i].start();
        }
        Thread[] BurgerMakerThreads = new Thread[numBurgerMakers];
        for (int i = 0; i < BurgerMakerThreads.length; i++) {
            String name = "bm";
            BurgerMakerThreads[i] = new Thread(BurgerMakerRunnable, name.concat(Integer.toString(i)));
            summary.put(name.concat(Integer.toString(i)), 0);
            BurgerMakerThreads[i].start();
        }
        Thread[] HotdogPackerThreads = new Thread[numHotdogPackers];
        for (int i = 0; i < HotdogPackerThreads.length; i++) {
            String name = "hp";
            HotdogPackerThreads[i] = new Thread(HotdogPackerRunnable, name.concat(Integer.toString(i)));
            summary.put(name.concat(Integer.toString(i)), 0);
            HotdogPackerThreads[i].start();
        }
        Thread[] BurgerPackerThreads = new Thread[numBurgerPackers];
        for (int i = 0; i < BurgerPackerThreads.length; i++) {
            String name = "bp";
            BurgerPackerThreads[i] = new Thread(BurgerPackerRunnable, name.concat(Integer.toString(i)));
            summary.put(name.concat(Integer.toString(i)), 0);
            BurgerPackerThreads[i].start();
        }

        try {
            Thread.sleep(2000); // delay for all threads to start running
            while(Thread.activeCount() != 1); // wait until all threads except for main have ceased

            // conver hashtable to tree map in order to sort
            TreeMap<String, Integer> test = new TreeMap<String, Integer>(summary);
            Set<String> keys = test.keySet();
            Iterator<String> itr = keys.iterator();
            while (itr.hasNext()){ // iterate through the tree map
                String s = itr.next();
                if (s.charAt(1) == 'p'){
                    writeFile(s + " packs " + test.get(s)); // record packers contribution
                    if (s.charAt(0) == 'h'){ recordedPackedHotdogs += test.get(s); }
                    else if (s.charAt(0) == 'b'){ recordedPackedBurgers += test.get(s);}
                } else if (s.charAt(1) == 'm'){
                    writeFile(s + " makes " + test.get(s)); // record makers contribution
                    if (s.charAt(0) == 'b'){recordedMadeBurgers +=test.get(s);}
                    else if (s.charAt(0) == 'h'){recordedMadeHotdogs +=test.get(s);}
                }
            }
        } catch (InterruptedException e) { e.printStackTrace();}

        // check if parameters provided matches those produced and packed
        System.out.println(recordedMadeBurgers + " burgers made, test case passed: " + (recordedMadeBurgers == numBurger) );
        System.out.println(recordedMadeHotdogs + " hotdogs made, test case passed: " + (recordedMadeHotdogs == numHotdog));
        System.out.println(recordedPackedHotdogs + " hotdogs packed, test case passed: " + (recordedPackedHotdogs == numHotdog));
        System.out.println(recordedPackedBurgers + " burgers packed, test case passed: " + (recordedPackedBurgers == numBurger));


    }
}

class Food {
    char type; // either 'H' or 'B' (hotdog or burger) 
    int id; // unqiue for each type
    String machineId; // machine that produced it
    public Food(){};
    public Food(char type, int id, String machineId) {
        this.type = type;
        this.id = id;
        this.machineId = machineId;
    }
    public int getId() { return id; }

    public char getType() { return type; }

    public String getMachineId() { return machineId; }
}

class Buffer {

    private final Food[] buffer;
    private int front = 0, back = 0;
    public int item_count = 0;

    Buffer(int size) { buffer = new Food[size]; }

    public synchronized void put(Food food) {

        while (item_count == buffer.length) {
            try {
                this.wait();
            } catch (InterruptedException e) {}
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
            } catch (InterruptedException e) {}
        }
        Food food = buffer[front];
        front = (front + 1) % buffer.length;
        item_count--;
        this.notifyAll();

        return food;
    }

    public synchronized char checkType() {
        if (!checkEmpty()) {
            return buffer[front].getType();
        } else {
            return 'z';
        }
    }

    public synchronized boolean checkEmpty() {
        return item_count == 0;
    }
}