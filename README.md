# netty-agents

Easy to use, message based, synchronous communication library based on Netty (https://netty.io/).

Project Web site: https://www.opentoolset.org/

## Features
* Easy and ready-to-use communication agents based on Netty library
* Eliminates repeated and boilerplate code when using Netty as a communication library
* Strongly-typed, well-defined, message based communication interfaces
* Suppors synchronous (request-response based) communication through stateful channels
* Provides security with mutual TLS authentication with certificates
* Supports bi-directional, multi-client communication

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

