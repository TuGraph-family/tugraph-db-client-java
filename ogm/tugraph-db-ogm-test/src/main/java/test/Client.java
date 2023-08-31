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

import com.antgroup.tugraph.ogm.config.Configuration;
import com.antgroup.tugraph.ogm.driver.Driver;
import com.antgroup.tugraph.ogm.drivers.rpc.driver.RpcDriver;

import java.util.List;

public class Client {
    private static Configuration.Builder baseConfigurationBuilder;

    protected static Driver getDriver(String[] args) {
        Driver driver = new RpcDriver();
        baseConfigurationBuilder = new Configuration.Builder()
            .database("default")
            .uri(args[0])
            .verifyConnection(true)
            .credentials(args[1], args[2]);
        driver.configure(baseConfigurationBuilder.build());
        return driver;
    }

    protected static Driver getDriverWithHA(List<String> databaseUris, String username, String password) {
        Driver driver = new RpcDriver();
        String[] uris = databaseUris.toArray(new String[databaseUris.size()]);
        baseConfigurationBuilder = new Configuration.Builder()
                .database("default")
                .uris(uris)
                .verifyConnection(true)
                .credentials(username, password);
        driver.configure(baseConfigurationBuilder.build());
        return driver;
    }
}
