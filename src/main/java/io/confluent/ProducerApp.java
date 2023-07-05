package io.confluent;
import org.apache.kafka.clients.producer.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.confluent.se.avro_schemas.TransactionData;
import io.confluent.se.avro_schemas.TransactionData.Builder;


import java.io.IOException;
import java.io.InputStream;

import java.util.*;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;



public class ProducerApp {
    private static final Logger LOG = LoggerFactory.getLogger(ProducerApp.class);
    private final Random random = new Random();
    private final List<String> iban = Arrays.asList("ES6621000418401234567891", "ES6000491500051234567892", "ES9420805801101234567891","ES9000246912501234567891","ES7100302053091234567895","ES1000492352082414205416","ES1720852066623456789011");
    private final List<String> concept = Arrays.asList("transfer", "bizum", "purchases","payments","receipts","sales","cash");
    private final List<String> typeCust = Arrays.asList("Gold", "Platinum", "Silver","");

    public void ProduceEvents() {

        Properties properties = loadProperties();
        try (final Producer<String, TransactionData> producer = new KafkaProducer<>(properties)){
            Builder trxBuilder = TransactionData.newBuilder();
            int i = 0;
            while (true) {
                i++;
                Integer trx = random.nextInt();
                TransactionData event = getTrxBuilder(trxBuilder, ((trx<  0) ? trx*-1 : trx) ,  random.nextInt(10),iban.get(random.nextInt(iban.size())),concept.get(random.nextInt(concept.size())),random.nextInt(50000),typeCust.get(random.nextInt(typeCust.size())));
                if (i % 10 == 0 ) {
                    event = getTrxBuilder(trxBuilder,((trx<  0) ? trx*-1 : trx) ,  random.nextInt(10),iban.get(random.nextInt(iban.size())),concept.get(random.nextInt(concept.size())),random.nextInt(10000)+100000,"");
                }
                ProducerRecord<String, TransactionData> producerRecord = new ProducerRecord<>((String) properties.get("topic"), String.valueOf(event.getIdTrx()), event);
                try {
                producer.send(producerRecord, new Callback() {
                    public void onCompletion(RecordMetadata recordMetadata, Exception e) {
                        // executes every time a record is successfully sent or an exception is thrown
                        if (e == null) {
                            // the record was successfully sent
                            LOG.info("Received new metadata. \n" +
                                    "Topic:" + recordMetadata.topic() + "\n" +
                                    "Key:" + producerRecord.key() + "\n" +
                                    "Partition: " + recordMetadata.partition() + "\n" +
                                    "Offset: " + recordMetadata.offset() + "\n" +
                                    "Timestamp: " + recordMetadata.timestamp());
                        } else {
                            LOG.error("Error while producing", e);
                        }
                    }
                });
                    try {
                        Thread.sleep(random.nextInt(3000));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                catch (Exception e) {
                    LOG.error(e.getMessage(), e.fillInStackTrace());
                } finally {
                    // Close the producer
                    producer.flush();
                }

            }
        }


    }

    TransactionData getTrxBuilder(Builder trxBuilder, Integer idtrx,Integer idcust, String iban,String concept,Integer amount,String typeCust) {
        trxBuilder.setIdTrx(idtrx);
        trxBuilder.setIdCustomer(idcust);
        trxBuilder.setIBAN(iban);
        trxBuilder.setConcept(concept);
        trxBuilder.setAmount(amount);
        trxBuilder.setTypeOfCustomer(typeCust);
        return trxBuilder.build();
    }



    Properties loadProperties() {
        try (InputStream inputStream = this.getClass()
                .getClassLoader()
                .getResourceAsStream("confluent.properties")) {
            Properties props = new Properties();
            props.load(inputStream);
            return props;
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    public static void main(String[] args) {
        ProducerApp producerApp = new ProducerApp();
        producerApp.ProduceEvents();
    }
}