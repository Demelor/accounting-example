
# Accounting Web Service Example  
A simple REST API service for money transfers between accounts that meets the following requirements:

 - Simple structure.
 - No heavyweight web frameworks.
 - In-memory datastore should be enough.
 - Safe to be used by multiple clients simultaneously.
 - Developed with common sense :)

## Running  
  
Service runs on localhost:8080.

It could be executed by running maven task in project directory:
```sh
$ mvn exec:java
```  
Or just by running already packaged jar file:
```sh
java -jar accounting-service.jar
```
## REST API

#### All Accounts
```
GET /accounts
```
Returns all available accounts, example:
```json
{
    "accounts": [
        {
            "id": 1,
            "name": "Alice",
            "funds": "45.12"
        },
        {
            "id": 2,
            "name": "Bob",
            "funds": "100.00"
        }
    ]
}
```
#### Find Account
```
GET /accounts/<id>
```
Returns specific account with provided id:
```json
{
    "id": 1,
    "name": "Alice",
    "funds": "45.12"
}
```
If there is no account with such id, then it returns status 404 and error message:
```json
{
    "message": "Cannot find account by provided id"
}
```
#### Create Account
```
POST /accounts/create
{
    "name": "Bob",
    "initialFunds": "100"
}
```
Creates new account, returns its info with assigned id:
```json
{
    "id": 2,
    "name": "Bob",
    "funds": "100.00"
}
```
#### Transfer Funds
```
POST /transfer
{
    "sourceId": 2,
    "targetId": 1,
    "amount": "30.33"
}
```
Returns state of the source account after successful transfer:
```json
{
    "id": 2,
    "name": "Bob",
    "funds": "69.67"
}
```
## Error Handling
If error occured while processing request, server responds with error status code and error message in json form:
```json
{
    "message": "Transfer amount must be a positive value"
}
```
