# netty-agents

Easy to use, message based, synchronous communication library based on Netty (https://netty.io/).

Project Wiki: https://gitlab.com/opentoolset/netty-agents/-/wikis/home

Project Web site: https://opentoolset.org/

## Features
* Support for bi-directional communication with continuous channels,
* Performance, customization and flexibility using nice features of Netty library (socket level communication, channel pipeline context, etc.)
* Ability to design and use of a strongly-typed (with POJO objects) and well-defined protocol in applications
* Support for blocking, request-response based communication model as well as supporting asynchronous pattern
* Easily implementation of communication and security requirements in any Java project by writing lower code using a simplified library interface based on agent context.
* Secure communication with TLS and mutual certificate authentication

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
For detailed usage examples please read and try JUnit tests below:
* MTNettyAgents
* MTMultiClient

