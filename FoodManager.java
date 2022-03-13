public class FoodManager {
    static int numHotdog, numBurger, numSlots, numHotdogMakers, numBurgerMakers, numHotdogPackers, numBurgerPackers;

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

    public int getId() {
        return id;
    }

    public char getType() {
        return type;
    }

    public String getMachineId() {
        return machineId;
    }
}

class Buffer {

    private Food[] buffer;
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