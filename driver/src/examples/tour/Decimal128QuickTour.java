/*
 * Copyright 2016 MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package tour;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.types.Decimal128;

import java.math.BigDecimal;

public class Decimal128QuickTour {
    public static void main(String[] args) {
        MongoClient mongoClient;

        if (args.length == 0) {
            // connect to the local database server
            mongoClient = new MongoClient();
        } else {
            mongoClient = new MongoClient(new MongoClientURI(args[0]));
        }

        // get handle to "mydb" database
        MongoDatabase database = mongoClient.getDatabase("mydb");


        // get a handle to the "test" collection
        MongoCollection<Document> collection = database.getCollection("test");

        // drop all the data in it
        collection.drop();

        // make a document and insert it
        Document doc = new Document("name", "MongoDB")
                               .append("amount1", Decimal128.valueOf(".10"))
                               .append("amount2", Decimal128.valueOf(42L))
                               .append("amount3", Decimal128.valueOf(new BigDecimal(".200")));

        collection.insertOne(doc);


        Document first = collection.find().filter(Filters.eq("amount1", Decimal128.valueOf(new BigDecimal(".10")))).first();

        Decimal128 amount3 = (Decimal128) first.get("amount3");
        BigDecimal amount2AsBigDecimal = amount3.bigDecimalValue();

        System.out.println(amount3.toString());
        System.out.println(amount2AsBigDecimal.toString());

    }
}