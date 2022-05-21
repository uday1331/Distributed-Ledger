# Distributed-Ledger

Following is the workflow for adding client transactions to the distributed ledger:

<img width="600" alt="Screenshot 2022-05-22 at 12 25 32 AM" src="https://user-images.githubusercontent.com/47697048/169665558-ea918880-9ea0-4076-8bf0-5320d1f7b61e.png">


## Client-Node Communication
1. Client sends a transaction request to the Node using a remote procedure call.
2. Node immediately returns a transaction ID for the to-be included transaction.
3. Node sends another response when transaction is included on the ledger i.e. a block is created.

<img width="600" alt="Untitled" src="https://user-images.githubusercontent.com/47697048/169665582-f39ed2a9-2862-4450-8fb2-c790f18e3743.png">


## Technologies Used
1. Raft Implementation: [Hazelcast](https://hazelcast.com/)
2. Java JMS with PubSub: [Payara Server]((https://www.payara.fish/)) (Glassfish)
3. Java RMI 

## Setup procedure
1. Start a Payara server and then create a Topic Connection Factory using Payara Admin. Also create a JMS Topic using the admin page. 
2. Setup Hazelcast cluster the way you like. We suggest using the docker containers for Hazelcast. To do this, follow the next few steps to create a three node cluster.
3. `docker network create hazelcast-network`
4. `docker run -dit --network hazelcast-network --rm -e HZ_CLUSTERNAME=comp3358-cluster -p 5701:5701 hazelcast/hazelcast:5.0.3`
5. `docker run -dit --network hazelcast-network --rm -e HZ_CLUSTERNAME=comp3358-cluster -p 5702:5701 hazelcast/hazelcast:5.0.3`
6. `docker run -dit --network hazelcast-network --rm -e HZ_CLUSTERNAME=comp3358-cluster -p 5703:5701 hazelcast/hazelcast:5.0.3`
7. Now, start a number of Nodes by running the corresponding java file: `Node.java`.
8. Finally, start a number of clients using the client file: `Client.java`.

## Logs
In the following testing setup, we have two clients that are simultaneouly sending 30 transactions. Every time the client receives anything from the server, it prints the message (JMS or RMI response).

Below we see a snippet logs for one of the two clients. Most of the logs contains the transaction ID of the sent transactions and every now and then the published block received as a response is displayed. Note that the `BLOCKSIZE` is 10 here but the block publish logs are seen much sooner than that in some cases. This is because of parallel `sendTransaction` calls by the seconds client.
<img width="1398" alt="Screenshot 2022-05-22 at 12 32 19 AM" src="https://user-images.githubusercontent.com/47697048/169665742-94c7a1e5-fd1e-46de-a5f0-de8c63eb6aa7.png">

Next, we look at the server logs. Every time a block is published, the server logs it and prints out the entire chain till now. The chain is printed in as the block has of the latest block going back to the genesis and then null. In this case, since each client sends 30 transactions each, there are 60 lines in the final log printing the chain.
<img width="1339" alt="Screenshot 2022-05-22 at 12 36 31 AM" src="https://user-images.githubusercontent.com/47697048/169665956-0a5a2b8b-6eeb-4f8a-937c-5ca5cac52ca2.png">
