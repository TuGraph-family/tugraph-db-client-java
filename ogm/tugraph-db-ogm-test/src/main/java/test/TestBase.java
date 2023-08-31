/*
 * Copyright 2022 "Ant Group"
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package test;

import com.alibaba.fastjson.JSONObject;
import com.antgroup.tugraph.TuGraphDbRpcException;
import com.antgroup.tugraph.ogm.driver.Driver;
import entity.Actor;
import entity.Movie;
import com.antgroup.tugraph.ogm.cypher.ComparisonOperator;
import com.antgroup.tugraph.ogm.model.QueryStatistics;
import com.antgroup.tugraph.ogm.model.Result;
import com.antgroup.tugraph.ogm.session.Session;
import com.antgroup.tugraph.ogm.session.SessionFactory;
import com.antgroup.tugraph.ogm.cypher.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.*;


public class TestBase extends Client {
    private static final Logger log = LoggerFactory.getLogger(TestBase.class);
    private static SessionFactory sessionFactory;
    private static Session session;

    private static String host;

    public static void main(String[] args) throws Exception {
        if (args.length != 3 && args.length != 0) {
            log.info("java -jar target/tugraph-ogm-test-x.x.x.jar [host:port] [user] [password]");
            log.info("java -jar target/tugraph-ogm-test-x.x.x.jar");
            return;
        }
        if (args.length == 0) {
            haClientTest();
        } else {
            sessionFactory = new SessionFactory(getDriver(args), "entity");
            session = sessionFactory.openSession();
            testCreate();
            testQuery();
            testUpdate();
            testDelete();
        }
    }

    //--------------------------------    test base    ------------------------------------------------
    private static void testDelete() {
        log.info("----------------testDelete--------------------");
        // Test1  CREATE -> DELETE
        Actor a1 = new Actor();
        Actor a2 = new Actor();
        a1.setName("ado");
        a2.setName("abo");
        session.save(a1);
        session.save(a2);
        Collection<Actor> savedActors = session.loadAll(Actor.class);
        assertThat(savedActors).hasSize(4);

        List<Object> actorList = new ArrayList<>();
        actorList.add(a1);
        actorList.add(a2);
        session.delete(actorList);
        Collection<Actor> delActors = session.loadAll(Actor.class);
        assertThat(delActors).hasSize(2);

        // Test2  MATCH -> DELETE
        Result result = session.query("MATCH (n)-[r]->(n2) DELETE r", emptyMap());
        QueryStatistics statistics = result.queryStatistics();
        log.info("deleted " + statistics.getRelationshipsDeleted() + " edges");
        assertThat(statistics.getRelationshipsDeleted()).isEqualTo(3);

        // Test3  CREATE -> MATCH -> DELETE
        Movie movie = new Movie("Speed", 2019);
        Actor alice = new Actor("Alice Neeves");
        alice.actsIn(movie);
        session.save(movie);
        Movie m1 = session.load(Movie.class, movie.getId());
        session.delete(m1);
        session.delete(alice);
        Collection<Movie> ms = session.loadAll(Movie.class);
        assertThat(ms).hasSize(3);

        // Test4  DELETE -> LOADALL
        session.deleteAll(Actor.class);
        Collection<Actor> actors = session.loadAll(Actor.class);
        assertThat(actors).hasSize(0);

        // Test5  DELETE
        session.purgeDatabase();
        Collection<Movie> movies = session.loadAll(Movie.class);
        assertThat(movies).hasSize(0);
    }

    private static void testQuery() {
        log.info("----------------testQuery--------------------");
        // Test1  LOADALL
        Collection<Movie> movies = session.loadAll(Movie.class);
        assertThat(movies).hasSize(3);
        List<Movie> moviesList = new ArrayList<>(movies);
        for (int i = 0; i < moviesList.size(); i++) {
            log.info("Movie" + i + ": " + moviesList.get(i).getTitle()
                + ", released in " + moviesList.get(i).getReleased());
        }

        //Test2  LOADALL(Filter)
        Collection<Movie> moviesFilter = session.loadAll(Movie.class, new Filter("released", ComparisonOperator.LESS_THAN, 1995));
        assertThat(moviesFilter).hasSize(2);

        //Test3  MATCH
        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("title", "The Matrix");
        Iterable<Movie> actual = session
            .query(Movie.class, "MATCH (m:Movie{title: $title}) RETURN m", parameters);
        assertThat(actual.iterator().next().getTitle()).isEqualTo("The Matrix");

        // Test4
        HashMap<String, Object> parameters1 = new HashMap<>();
        parameters1.put("title", "The Matrix");
        Integer counts = session
            .queryForObject(Integer.class, "MATCH (n:Movie{title: $title})-[r]-(m:Actor) RETURN COUNT(m) AS counts", parameters1);
        assertThat(counts).isEqualTo(2);

        // Test5
        Result result6 = session.query("MATCH p = (m)-[rel]-(n) return p", Collections.emptyMap());
        assertThat(result6.queryResults()).hasSize(6);
    }

    private static void testCreate() {
        log.info("----------------testCreate--------------------");
        // Test1  CREATE -> LOAD
        session.query("CALL db.createVertexLabel('Movie', 'title', 'title', STRING, false, 'released', INT32, true)", emptyMap());
        session.query("CALL db.createVertexLabel('Actor', 'name', 'name', STRING, false, 'works', STRING, true)", emptyMap());
        session.query("CALL db.createEdgeLabel('ACTS_IN', '[]')", emptyMap());
        session.query("CALL db.createVertexLabel('Director', 'name', 'name', STRING, false, 'age', INT16, true)", emptyMap());
        session.query("CALL db.createEdgeLabel('DIRECT', '[]')", emptyMap());
        Movie movie1 = new Movie("Jokes", 1990);
        session.save(movie1);
        Movie m1 = session.load(Movie.class, movie1.getId());
        assertThat(movie1.getId()).isEqualTo(m1.getId());
        assertThat(movie1.getTitle()).isEqualTo(m1.getTitle());

        // Test2  CREATE -> LOAD
        Movie movie = new Movie("The Matrix", 1999);
        Actor keanu = new Actor("Keanu Reeves");
        keanu.setWorks("[{\"name\":\"The Matrix\",\"type\":\"movie\"},{\"name\":\"Phantom of the Opera\",\"type\":\"opera\"}]");
        keanu.actsIn(movie);
        Actor carrie = new Actor("Carrie-Ann Moss");
        carrie.actsIn(movie);
        session.save(movie);
        Movie matrix = session.load(Movie.class, movie.getId());
        for(Actor actor : matrix.getActors()) {
            log.info("Actor: " + actor.getName());
        }
        assertThat(matrix.getActors()).hasSize(2);

        // Test3  CREATE -> MATCH
        Result createResult = session.query("CREATE (n:Movie{title:\"The Shawshank Redemption\", released:1994})<-[r:DIRECT]-(n2:Director{name:\"Frank Darabont\", age:63})", emptyMap());
        QueryStatistics statistics = createResult.queryStatistics();
        assertThat(statistics.getNodesCreated()).isEqualTo(2);
        assertThat(statistics.getRelationshipsCreated()).isEqualTo(1);
        Result result = session.query("MATCH (m)-[r:DIRECT]->(n) return m,r,n", Collections.emptyMap());
        JSONObject r = (JSONObject) result.queryResults().iterator().next().get("r");
        for (String key : r.keySet()) {
            log.info(key + ": " + r.get(key));
        }
        assertThat(r).hasSize(7);
    }

    private static void testUpdate() {
        log.info("----------------testUpdate--------------------");
        // Test1: MATCH -> UPDATE -> LOAD
        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("name", "Keanu Reeves");
        Actor actor = session.queryForObject(Actor.class,
            "MATCH (actor:Actor{name:$name}) RETURN actor", parameters);
        actor.setName("NOBU Reeves");
        session.save(actor);
        Actor newactor = session.load(Actor.class, actor.getId());
        assertThat(newactor.getName()).isEqualTo("NOBU Reeves");
    }

    //--------------------------------    test base end   ------------------------------------------------

    //--------------------------------    test ha   ------------------------------------------------
    public static void executive(String stmt) {
        Runtime runtime = Runtime.getRuntime();

        try {
            String[] command = {"/bin/sh", "-c", stmt};

            Process process = runtime.exec(command);
            String inStr = consumeInputStream(process.getInputStream());
            String errStr = consumeInputStream(process.getErrorStream());

            int proc = process.waitFor();
            if (proc == 0) {
                log.info("succ");
                log.info(inStr);
            } else {
                log.info("fail");
                log.info(errStr);
            }
        } catch (Exception e) {
            log.info(e.getMessage());
        }
    }

    public static String consumeInputStream(InputStream is) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is, "GBK"));
        String s;
        StringBuilder sb = new StringBuilder();
        while ((s = br.readLine()) != null) {
            log.info(s);
            sb.append(s);
        }
        return sb.toString();
    }

    public static String executiveWithValue(String stmt) {
        Runtime runtime = Runtime.getRuntime();
        String inStr = "";

        try {
            String[] command = {"/bin/sh", "-c", stmt};

            Process process = runtime.exec(command);

            inStr = consumeInputStream(process.getInputStream());
            // String errStr = consumeInputStream(process.getErrorStream());

            int proc = process.waitFor();
            if (proc == 0) {
                log.info("succ");
            } else {
                log.info("fail");
            }
        } catch (Exception e) {
            log.info(e.getMessage());
        }
        return inStr;
    }

    public static Driver startHaClient(String port) {
        log.info("----------------startClient--------------------");
        String[] args = {
                host + ":" + port,
                "admin",
                "73@TuGraph"
        };
        Driver driver = getDriver(args);
        sessionFactory = new SessionFactory(driver, "entity");
        session = sessionFactory.openSession();
        return driver;
    }

    public static Driver startHaClient(List<String> ports) {
        log.info("----------------startClient--------------------");
        List<String> urls = new ArrayList<>();
        for (String port: ports) {
            urls.add(host + ":" + port);
        }
        String user = "admin";
        String password = "73@TuGraph";
        Driver driver = getDriverWithHA(urls, user, password);
        sessionFactory = new SessionFactory(driver, "entity");
        session = sessionFactory.openSession();
        return driver;
    }

    public static void haClientTest() throws Exception {
        host = executiveWithValue("hostname -I").trim();
        int len = host.indexOf(' ');
        if (len != -1) {
            host = host.substring(0, len);
        }

        // start HA group
        executive("mkdir ha1 && cp -r ../../src/server/lgraph_ha.json ./lgraph_server ./resource ha1 && cd ha1 && ./lgraph_server --host " + host + " --port 27072 --enable_rpc true --enable_ha true --ha_node_offline_ms 5000 --ha_node_remove_ms 10000 --rpc_port 29092 --directory ./db --log_dir ./log  --ha_conf " + host + ":29092," + host + ":29093," + host + ":29094 -c lgraph_ha.json -d start");
        Thread.sleep(3000);

        executive("mkdir ha2 && cp -r ../../src/server/lgraph_ha.json ./lgraph_server ./resource ha2 && cd ha2 && ./lgraph_server --host " + host + " --port 27073 --enable_rpc true --enable_ha true --ha_node_offline_ms 5000 --ha_node_remove_ms 10000 --rpc_port 29093 --directory ./db --log_dir ./log  --ha_conf " + host + ":29092," + host + ":29093," + host + ":29094 -c lgraph_ha.json -d start");
        Thread.sleep(3000);

        executive("mkdir ha3 && cp -r ../../src/server/lgraph_ha.json ./lgraph_server ./resource ha3 && cd ha3 && ./lgraph_server --host " + host + " --port 27074 --enable_rpc true --enable_ha true --ha_node_offline_ms 5000 --ha_node_remove_ms 10000 --rpc_port 29094 --directory ./db --log_dir ./log  --ha_conf " + host + ":29092," + host + ":29093," + host + ":29094 -c lgraph_ha.json -d start");
        Thread.sleep(3000);

        Driver driver = startHaClient("29092");
        try {

            log.info("---------client start success!--------");
            Thread.sleep(5000);

            testCreate();

            // test urlTable
            List<String> urls = new ArrayList<>();
            urls.add("29092");
            urls.add("29093");
            urls.add("29094");
            Driver urlDriver = startHaClient(urls);

            testQuery();
            urlDriver.close();

            // stop follower
            log.info("-------------------------stopping follower-------------------------");
            driver.close();
            Thread.sleep(1000 * 7);
            executive("kill -2 $(ps -ef | grep 27073 | grep -v grep | awk '{print $2}')");
            Thread.sleep(1000 * 13);
            driver = startHaClient("29092");
            Thread.sleep(1000 * 7);

            log.info("-------------------------stop follower successfully-------------------------");
            testQuery();
            // restart follower
            log.info("-------------------------starting follower-------------------------");
            driver.close();
            Thread.sleep(1000 * 7);
            executive("cd ha2 && ./lgraph_server --host " + host + " --port 27073 --enable_rpc true --enable_ha true --ha_node_offline_ms 5000 --ha_node_remove_ms 10000 --rpc_port 29093 --directory ./db --log_dir ./log  --ha_conf " + host + ":29092," + host + ":29093," + host + ":29094 -c lgraph_ha.json -d start");
            Thread.sleep(1000 * 13);
            driver = startHaClient("29092");
            Thread.sleep(1000 * 7);

            log.info("-------------------------start follower successfully-------------------------");
            testQuery();

            // stop leader
            log.info("-------------------------stopping leader-------------------------");
            driver.close();
            Thread.sleep(1000 * 7);
            executive("kill -2 $(ps -ef | grep 27072 | grep -v grep | awk '{print $2}')");
            Thread.sleep(1000 * 13);
            driver = startHaClient("29093");
            Thread.sleep(1000 * 7);

            log.info("-------------------------stop leader successfully-------------------------");
            testQuery();
            // restart leader
            log.info("-------------------------starting leader-------------------------");
            driver.close();
            Thread.sleep(1000 * 7);
            executive("cd ha1 && ./lgraph_server --host " + host + " --port 27072 --enable_rpc true --enable_ha true --ha_node_offline_ms 5000 --ha_node_remove_ms 10000 --rpc_port 29092 --directory ./db --log_dir ./log  --ha_conf " + host + ":29092," + host + ":29093," + host + ":29094 -c lgraph_ha.json -d start");
            Thread.sleep(1000 * 13);
            driver = startHaClient("29093");
            Thread.sleep(1000 * 7);

            log.info("-------------------------start leader successfully-------------------------");
            testQuery();
        } catch (TuGraphDbRpcException e) {
            log.info("Exception at " + e.GetErrorMethod() + " with errorCodeName: " + e.GetErrorCodeName() + " and error: " + e.GetError());
            log.info(e.getMessage());
        } catch (Exception e2) {
            log.info(e2.getMessage());
        } finally {
            // stop leader and follower
            driver.close();
            for (int i = 27072; i <= 27074; i++) {
                executive("kill -2 $(ps -ef | grep " + i + " | grep -v grep | awk '{print $2}')");
            }
            for (int i = 1; i <= 3; i++) {
                executive("rm -rf ha" + i);
            }
        }
    }
    //--------------------------------    test ha end  ------------------------------------------------
}
