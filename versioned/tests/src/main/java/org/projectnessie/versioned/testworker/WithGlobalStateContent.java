/*
 * Copyright (C) 2022 Dremio
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
package org.projectnessie.versioned.testworker;

import java.util.UUID;
import org.immutables.value.Value;

/** Content with on-reference state and mandatory global state. */
@Value.Immutable
public interface WithGlobalStateContent extends BaseContent {

  static WithGlobalStateContent withGlobal(String global, String onRef, String contentId) {
    return ImmutableWithGlobalStateContent.builder()
        .onRef(onRef)
        .global(global)
        .id(contentId)
        .build();
  }

  static WithGlobalStateContent newWithGlobal(String global, String onRef) {
    return ImmutableWithGlobalStateContent.builder()
        .onRef(onRef)
        .global(global)
        .id(UUID.randomUUID().toString())
        .build();
  }

  String getOnRef();

  String getGlobal();
}
