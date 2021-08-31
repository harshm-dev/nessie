/*
 * Copyright (C) 2020 Dremio
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.projectnessie.versioned.persist.inmem;

import org.projectnessie.versioned.persist.adapter.DatabaseAdapter;
import org.projectnessie.versioned.persist.adapter.DatabaseAdapterConfig;
import org.projectnessie.versioned.persist.adapter.DatabaseAdapterFactory;
import org.projectnessie.versioned.persist.adapter.spi.ImmutableDefaultDatabaseAdapterConfig;

public class InmemoryDatabaseAdapterFactory
    implements DatabaseAdapterFactory<DatabaseAdapterConfig> {

  @Override
  public String getName() {
    return "In-Memory";
  }

  @Override
  public Builder<DatabaseAdapterConfig> newBuilder() {
    return new Builder<DatabaseAdapterConfig>() {
      @Override
      protected DatabaseAdapterConfig getDefaultConfig() {
        return ImmutableDefaultDatabaseAdapterConfig.builder().build();
      }

      @Override
      public DatabaseAdapter build() {
        return new InmemoryDatabaseAdapter(getConfig());
      }
    };
  }
}