# Requirenments
Java 1.8, to install it use the best way and easiest is through:
- SDKMAN! : https://sdkman.io/install
    - sdk list java (to check all the javas available)
    - sdk install java 18.0.2-oracle
 
- jenv : https://www.jenv.be will set you jvm
    - ls /Users/ramon/.sdkman/candidates/java/
    - jenv add $HOME/.sdkman/candidates/java/18.0.2-oracle
    - jenv versions
    - jenv local oracle64-18.0.2
    
- gradle :
    - install using sdk:  sdk install gradle 8.1
    - check installation: gradle -v

Finally check your java version with the command:
```bash
java -version
```
An output similiar to this one should appear:
```bash
java version "18.0.2" 2022-07-19
Java(TM) SE Runtime Environment (build 18.0.2+9-61)
Java HotSpot(TM) 64-Bit Server VM (build 18.0.2+9-61, mixed mode, sharing)
```

# Pull directory
- clone directory:
    - git clone https://github.com/jr-marquez/DataContracts.git
    - do the rest of the commands inside DataContracts folder
    
    
# Modify Properties Files

change the *confluent.properites* file with your Confluent Cloud information inside:
```bash
/src/main/java/resources
```
populate the variables.env file and then execute:
```bash
source variables.env
```
### New objects to create in Confluent Cloud
Create two kafka Topics:
- transactions
- transactions-alert

Create one tag in Confluent Cloud:
- PCI

# Demo Objective
The idea of the demo is to remove null values from the type_of_customer field (put unknown), Mask PCI information (IBAN) and send to the transactions-investigate topic those trx that are bigger than 100000 for risk analysis 

# Schema Registry commands

Add Schema with rules:

```bash
curl -u $basic_auth_user_info \
--request POST --url $schema_registry_url'/subjects/transactions-value/versions'   \
--header 'content-type: application/octet-stream' \
  --data '{
            "schemaType": "AVRO",
            "schema": "{\"fields\": [{\"name\": \"id_trx\",\"type\": [\"null\",\"int\"]},{\"name\":\"id_customer\",\"type\": [\"null\",\"int\"]},{\"name\": \"IBAN\",\"type\": [\"null\",\"string\"],\"confluent:tags\":[\"PCI\"]},{\"name\": \"amount\",\"type\": [\"null\",\"int\"]},{\"name\": \"concept\",\"type\": [\"null\",\"string\"]},{\"name\": \"type_of_customer\",\"type\": [\"null\",\"string\"]}],\"name\": \"TransactionData\",\"namespace\": \"io.confluent.se.avro_schemas\",\"type\": \"record\"}",
            "metadata": {
            "properties": {
            "owner": "Confluent SE",
            "email": "confluentes@confluent.io"
            }
        },
        "ruleSet": {
        "domainRules": [
            {
            "name": "transformCustomerNull",
            "kind": "TRANSFORM",
            "type": "CEL_FIELD",
            "mode": "WRITE",
            "expr": "name == \"type_of_customer\" ; value == \"\" ? \"unknown\" : value"
            },
            {
            "name": "maskPCI",
            "kind": "TRANSFORM",
            "mode": "WRITE",
            "type": "CEL_FIELD",
            "tags": ["PCI"],
            "expr": "\"ESXXXXXXXXXXXXXXXXXXXXXX\""
             },
            {
            "name": "checkAmount",
            "kind": "CONDITION",
            "type": "CEL",
            "mode": "WRITE",
            "expr": "message.amount < 100000",
            "onFailure": "DLQ"
            }
            ]
        }
    }' 
```
Check the recently added schema:

```bash
curl -u $basic_auth_user_info \
--request GET \
  --url $schema_registry_url'/subjects/transactions-value/versions/latest' \
   | jq
```
# Compile and Run the Producer App
Compile
```bash
./gradlew shadowJar
```

Then run the producer jar file:
```bash
java -jar build/libs/kafka-producer-data-contracts-1.0-SNAPSHOT.jar
```
You will see some errores due to rule executions (this is expected) but the producer will still run forever :
```bash
at io.confluent.ProducerApp.ProduceEvents(ProducerApp.java:44)
        ... 1 more
Caused by: io.confluent.kafka.schemaregistry.rules.RuleException: Expr 'message.amount < 100000' failed
        at io.confluent.kafka.schemaregistry.rules.cel.CelExecutor.transform(CelExecutor.java:70)
        at io.confluent.kafka.serializers.AbstractKafkaSchemaSerDe.executeRules(AbstractKafkaSchemaSerDe.java:618)
        ... 7 more
```

Wait 20 seconds and check both Topics : transactions & transactions-alert

THE END
