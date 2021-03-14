# A service to monitor IoT client disconnects
If a client disconnects for more than n seconds the service will do something.
## Flow
When a dis/connect message arrives a lambda writes it to a DynamoDB table. When a disconnect message arrives it's written to a queue with a delay. A lambda listens to the queue. When this lambda receives a message it checks the number of messages for that client in the DynamoDB table. If it's odd (i.e. number of connects != number of disconnects) it does something (writes to log or switches the client to polling).
## Components
1. IoT rules for connect and disconnect rules.
1. Lambda to write dis/connect messages to DynamoDB.
1. SQS queue for delayed disconnect messages.
1. Lambda to handle disconnect messages from the queue.


