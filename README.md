# Creating a Food Manager with Java Concurrency

## Problem Statement
Consider a factory with machines making two types of food: hotdogs and burgers, as well as machines packing these food. The hotdogs and burgers will be first made by the making machines and then sent to a common pool. The packing machines will take these food from the pool for packing. The manager of the factory specifies the number of hotdogs and number of burgers to be made. There is a log file containing records for logging when a hotdog / burger is made and packed. When all the food is produced, the manager will append a summary to the log file.

## Requirements

Each making machine is modeled with a making thread (created by the main thread)
- The making machine makes one hotdog or one burger at a time
- The making machine takes 3 seconds to make a hotdog and 8 seconds to make a burger
- After a hotdog / burger is made, the making machine takes 1 second to send the food together with its machine id to the pool
- After making the food, the making machine writes a record to the log file in the form of “ &lt; i > puts &lt; type > &lt; id >, where i is a serial number of the makingmachine, type is either “hotdog” or “burger”, id is serial number of the food 
- Once a making machine finishes sending the food to the pool, it can start to make the next food item
- When the required number of hotdogs and burgers are made, the making machine stops

Each packing machine is modeled with a packing thread (created by the main thread).
- There are two types of packers – hotdog packers and burger packers
- A hotdog packer must put TWO hotdogs into one pack, a burger packer must put ONE burger into one pack
- A hotdog packer is blocked if the head of the buffer is a burger (even though there are some hotdogs in the buffer). Similarly a burger packer is blocked if the head of the buffer is a hotdog
- A hotdog packer must take the first two items from the buffer if both are hotdogs. However, if the second item is a burger, it will take the first hotdog and then will be blocked. The hotdog packer then wakes up a burger packer
- A hotdog packer that has taken one hotdog has a higher priority than those with no hotdogs. This would ensure those with hotdogs would be waken up first.
- The packing machine takes 1 second to take a hotdog / a burger from the pool,regardless if it is a hotdog or burger packer.
- After taking the food from the pool, the packing machine takes 2 seconds to pack the food, regardless if it is hotdog or burger packer
- After packing two hotdogs, a double packer writes a record of the form “&lt; i > gets hotdog &lt; id1 > from &lt; j > and &lt; id2 > from &lt; k >”, where id1, id2 are the hotdog serial numbers, i is the packing machine id and j, k are the making machine id
- On the other hand, after packing a burger, a burger packer writes a record to the log file in the form of “&lt; i > gets burger &lt; id > from &lt; j >”, where id is the burger serial number , i is the packing machine id and j is the making machine id
- Once the packing machine finishes writing the record, it can start packing the next food item
- When all the hotdogs and burgers are packed, the packing machines stop

The manager is the main thread that manages the production and packing of food o The manager specifies the number of hotdogs and burgers to make
- The manager initiates hotdog making machines,burger making machines, hotdog packers and burger packers
- The manager has to wait until all hotdogs and burgers are made and packed, as well as all records are logged
- After all machines stop, the manager reads the log file and appends a summary to the log file. The summary provides statistics about the number of hotdogs and burgers made and packed by each machine

The common pool is a circular queue.
- The queue has K slots, where K < M, K < N
- An arrival food, either a burger or a hotdog, is inserted to the end of the queue
- The food items are packed in the first-come-first-serve manner
- The pool will refuse accepting new arrival food when there is no empty slot
- The pool will not deliver food to packing machine when all slots are empty

The main thread should takes in parameters N, M, K, W, X, Y, Z where N is the number of hotdogs to make, M is the number of burgers to make, K is the number of slots in the common pool, W is the number of hotdog makers, X is the number of burger makers, Y is the number of hotdog packers, Z is the number of burger packers.

Constraints: 1 < N, M < 1000, 1 < K < 100, 1 < W, X, Y, Z < 50. N is an even number.
