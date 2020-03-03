# netty-agents

Easy to use, message based and synchronous communication library based on Netty (https://netty.io/).

## Project Objectives
- Providing easy and ready-to-use communication agents based on Netty library
- Eliminating repeating and boilerplate code when using Netty as a communication library
- Supporting and providing design and usage of a strongly-typed and well-defined communication interface
- Providing synchronous (request-response based) model for communication requiremens similar to REST 
- Providing reliablity
- Providing security

## Sample Use Case

Sample server code like below:

```
...

ServerAgent serverAgent = new ServerAgent();
serverAgent.setMessageHandler(SampleMessage.class, message -> handleMessage(message));
serverAgent.setRequestHandler(SampleRequest.class, request -> handleRequest(request));
serverAgent.startup();

private static void handleMessage(SampleMessage message) {
    ...
    business logic
    ...
}

private static SampleResponse1 handleRequest(SampleRequest request) {
    SampleResponse1 response = new SampleResponse();
    ...
    business logic
    ...
    return response;
}
```

Sample client code like below:

```
ClientAgent clientAgent = new ClientAgent();
serverAgent.startup();
clientAgent.sendMessage(new SampleMessage()); // Sends a message to server without waiting a response
SampleResponse response = clientAgent.doRequest(new SampleRequest()); // Sends a request to server by waiting until receiving a response or timeout
```

