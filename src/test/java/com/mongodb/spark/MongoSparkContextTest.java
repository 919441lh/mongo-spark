/*
 * Copyright (c) 2008-2015 MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.mongodb.spark;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import org.apache.spark.SparkConf;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaRDD;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.Document;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static java.util.Collections.singletonList;

public class MongoSparkContextTest {
    private String master = "local";
    private String appName = "testApp";
    private String sparkHome = "path/to/spark";
    private String jarFile = "test.jar";

    private SparkConf sparkConf = new SparkConf().setMaster(master).setAppName(appName);
    private MongoSparkContext msc;
    private int partitions = 2;

    private String username = "test";
    private String password = "password";
    private String host = "localhost:27017";
    private String database = "test";
    private String collection = "test";
    private MongoClientURI uri =
            new MongoClientURI("mongodb://" + username + ":" + password + "@" + host + "/" + database + "." + collection);

    private MongoClient client = new MongoClient(uri);

    private String key = "a";
    private List<Document> documents = Arrays.asList(new Document(key, 0), new Document(key, 1), new Document(key, 2));
    private BsonDocument query = new BsonDocument(key, new BsonInt32(0));
    private List<BsonDocument> pipeline = singletonList(new BsonDocument("$project", new BsonDocument(key, new BsonInt32(1))));

    @Before
    public void setUp() {
        client.getDatabase(uri.getDatabase()).getCollection(uri.getCollection()).drop();
        client.getDatabase(uri.getDatabase()).getCollection(uri.getCollection()).insertMany(documents);
    }

    @After
    public void tearDown() {
        msc.stop();
        msc = null;
    }

    @Test
    public void shouldConstructMSCWithSparkConf() {
        msc = new MongoSparkContext(sparkConf, uri);

        JavaRDD<Document> rdd = msc.parallelize(Document.class);

        Assert.assertEquals(documents.size(), rdd.count());
    }

    @Test
    public void shouldConstructMSCWithSparkContext() {
        msc = new MongoSparkContext(new SparkContext(sparkConf), uri);

        JavaRDD<Document> rdd = msc.parallelize(Document.class);

        Assert.assertEquals(documents.size(), rdd.count());
    }

    @Test
    public void shouldConstructMSCWithMasterAppNameCredentialsHostsOptions() {
        msc = new MongoSparkContext(master, appName, uri);

        JavaRDD<Document> rdd = msc.parallelize(Document.class);

        Assert.assertEquals(documents.size(), rdd.count());
    }

    @Test
    public void shouldConstructMSCWithMasterAppNameConfCredentialsHostsOptions() {
        msc = new MongoSparkContext(master, appName, sparkConf, uri);

        JavaRDD<Document> rdd = msc.parallelize(Document.class);

        Assert.assertEquals(documents.size(), rdd.count());
    }

    @Test
    public void shouldConstructMSCWithMasterAppNameSparkHomeJarFileCredentialsHostsOptions() {
        msc = new MongoSparkContext(master, appName, sparkHome, jarFile, uri);

        JavaRDD<Document> rdd = msc.parallelize(Document.class);

        Assert.assertEquals(documents.size(), rdd.count());
    }

    @Test
    public void shouldConstructMSCWithMasterAppNameSparkHomeJarsCredentialsHostsOptions() {
        msc = new MongoSparkContext(master, appName, sparkHome, new String[] {jarFile}, uri);

        JavaRDD<Document> rdd = msc.parallelize(Document.class);

        Assert.assertEquals(documents.size(), rdd.count());
    }

    @Test
    public void shouldParallelizeWithPartitions() {
        msc = new MongoSparkContext(sparkConf, uri);
        JavaRDD<Document> rdd = msc.parallelize(Document.class, partitions);

        Assert.assertEquals(documents.size(), rdd.count());
        Assert.assertEquals(1, rdd.partitions().size()); // TODO: check actual num partitions once partitioning is implemented
    }

    @Test
    public void shouldParallelizeWithQuery() {
        msc = new MongoSparkContext(sparkConf, uri);

        JavaRDD<Document> rdd = msc.parallelize(Document.class, query);
        Assert.assertEquals(1, rdd.count());
        Assert.assertEquals(msc.sc().defaultParallelism(), rdd.partitions().size());
    }

    @Test
    public void shouldParallelizeWithPartitionsAndQuery() {
        msc = new MongoSparkContext(sparkConf, uri);

        JavaRDD<Document> rdd = msc.parallelize(Document.class, partitions, query);

        Assert.assertEquals(1, rdd.count());
        Assert.assertEquals(1, rdd.partitions().size()); // TODO: check actual num partitions once partitioning is implemented
    }

    @Test
    public void shouldParallelizeWithPipeline() {
        msc = new MongoSparkContext(sparkConf, uri);

        JavaRDD<Document> rdd = msc.parallelize(Document.class, pipeline);

        Assert.assertEquals(documents.size(), rdd.count());
        Assert.assertEquals(msc.sc().defaultParallelism(), rdd.partitions().size());
    }

    @Test
    public void shouldParallelizeWithPartitionsAndPipeline() {
        msc = new MongoSparkContext(sparkConf, uri);

        JavaRDD<Document> rdd = msc.parallelize(Document.class, partitions, pipeline);

        Assert.assertEquals(documents.size(), rdd.count());
        Assert.assertEquals(1, rdd.partitions().size()); // TODO: check actual num partitions once partitioning is implemented
    }

    @Test
    public void shouldParallelizeWithDefaultParallelism() {
        msc = new MongoSparkContext(sparkConf, uri);

        JavaRDD<Document> rdd = msc.parallelize(Document.class);

        Assert.assertEquals(documents.size(), rdd.count());
        Assert.assertEquals(msc.sc().defaultParallelism(), rdd.partitions().size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailNonnegativePartitions() {
        msc = new MongoSparkContext(sparkConf, uri);

        msc.parallelize(Document.class, 0);
    }
}
