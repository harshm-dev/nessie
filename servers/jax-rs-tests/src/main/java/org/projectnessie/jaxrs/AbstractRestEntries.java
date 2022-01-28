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
package org.projectnessie.jaxrs;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.projectnessie.error.BaseNessieClientServerException;
import org.projectnessie.model.Branch;
import org.projectnessie.model.CommitMeta;
import org.projectnessie.model.Content.Type;
import org.projectnessie.model.ContentKey;
import org.projectnessie.model.EntriesResponse.Entry;
import org.projectnessie.model.IcebergTable;
import org.projectnessie.model.ImmutableSqlView;
import org.projectnessie.model.Operation.Put;
import org.projectnessie.model.SqlView;
import org.projectnessie.model.SqlView.Dialect;

/** See {@link AbstractTestRest} for details about and reason for the inheritance model. */
public abstract class AbstractRestEntries extends AbstractRestDiff {
  @Test
  public void filterEntriesByType() throws BaseNessieClientServerException {
    Branch branch = createBranch("filterTypes");
    ContentKey a = ContentKey.of("a");
    ContentKey b = ContentKey.of("b");
    IcebergTable tam = IcebergTable.of("path1", 42, 42, 42, 42);
    SqlView tb =
        ImmutableSqlView.builder().sqlText("select * from table").dialect(Dialect.DREMIO).build();
    getApi()
        .commitMultipleOperations()
        .branch(branch)
        .operation(Put.of(a, tam))
        .commitMeta(CommitMeta.fromMessage("commit 1"))
        .commit();
    getApi()
        .commitMultipleOperations()
        .branch(branch)
        .operation(Put.of(b, tb))
        .commitMeta(CommitMeta.fromMessage("commit 2"))
        .commit();
    List<Entry> entries = getApi().getEntries().refName(branch.getName()).get().getEntries();
    List<Entry> expected =
        asList(
            Entry.builder().name(a).type(Type.ICEBERG_TABLE).build(),
            Entry.builder().name(b).type(Type.VIEW).build());
    assertThat(entries).containsExactlyInAnyOrderElementsOf(expected);

    entries =
        getApi()
            .getEntries()
            .refName(branch.getName())
            .filter("entry.contentType=='ICEBERG_TABLE'")
            .get()
            .getEntries();
    assertEquals(singletonList(expected.get(0)), entries);

    entries =
        getApi()
            .getEntries()
            .refName(branch.getName())
            .filter("entry.contentType=='VIEW'")
            .get()
            .getEntries();
    assertEquals(singletonList(expected.get(1)), entries);

    entries =
        getApi()
            .getEntries()
            .refName(branch.getName())
            .filter("entry.contentType in ['ICEBERG_TABLE', 'VIEW']")
            .get()
            .getEntries();
    assertThat(entries).containsExactlyInAnyOrderElementsOf(expected);
  }

  @Test
  public void filterEntriesByNamespace() throws BaseNessieClientServerException {
    Branch branch = createBranch("filterEntriesByNamespace");
    ContentKey first = ContentKey.of("a", "b", "c", "firstTable");
    ContentKey second = ContentKey.of("a", "b", "c", "secondTable");
    ContentKey third = ContentKey.of("a", "thirdTable");
    ContentKey fourth = ContentKey.of("a", "fourthTable");
    getApi()
        .commitMultipleOperations()
        .branch(branch)
        .operation(Put.of(first, IcebergTable.of("path1", 42, 42, 42, 42)))
        .commitMeta(CommitMeta.fromMessage("commit 1"))
        .commit();
    getApi()
        .commitMultipleOperations()
        .branch(branch)
        .operation(Put.of(second, IcebergTable.of("path2", 42, 42, 42, 42)))
        .commitMeta(CommitMeta.fromMessage("commit 2"))
        .commit();
    getApi()
        .commitMultipleOperations()
        .branch(branch)
        .operation(Put.of(third, IcebergTable.of("path3", 42, 42, 42, 42)))
        .commitMeta(CommitMeta.fromMessage("commit 3"))
        .commit();
    getApi()
        .commitMultipleOperations()
        .branch(branch)
        .operation(Put.of(fourth, IcebergTable.of("path4", 42, 42, 42, 42)))
        .commitMeta(CommitMeta.fromMessage("commit 4"))
        .commit();

    List<Entry> entries = getApi().getEntries().refName(branch.getName()).get().getEntries();
    assertThat(entries).isNotNull().hasSize(4);

    entries = getApi().getEntries().refName(branch.getName()).get().getEntries();
    assertThat(entries).isNotNull().hasSize(4);

    entries =
        getApi()
            .getEntries()
            .refName(branch.getName())
            .filter("entry.namespace.startsWith('a.b')")
            .get()
            .getEntries();
    assertThat(entries).hasSize(2);
    entries.forEach(e -> assertThat(e.getName().getNamespace().name()).startsWith("a.b"));

    entries =
        getApi()
            .getEntries()
            .refName(branch.getName())
            .filter("entry.namespace.startsWith('a')")
            .get()
            .getEntries();
    assertThat(entries).hasSize(4);
    entries.forEach(e -> assertThat(e.getName().getNamespace().name()).startsWith("a"));

    entries =
        getApi()
            .getEntries()
            .refName(branch.getName())
            .filter("entry.namespace.startsWith('a.b.c.firstTable')")
            .get()
            .getEntries();
    assertThat(entries).isEmpty();

    entries =
        getApi()
            .getEntries()
            .refName(branch.getName())
            .filter("entry.namespace.startsWith('a.fourthTable')")
            .get()
            .getEntries();
    assertThat(entries).isEmpty();

    getApi()
        .deleteBranch()
        .branchName(branch.getName())
        .hash(getApi().getReference().refName(branch.getName()).get().getHash())
        .delete();
  }

  @Test
  public void filterEntriesByNamespaceAndPrefixDepth() throws BaseNessieClientServerException {
    Branch branch = createBranch("filterEntriesByNamespaceAndPrefixDepth");
    ContentKey first = ContentKey.of("a", "b", "c", "firstTable");
    ContentKey second = ContentKey.of("a", "b", "c", "secondTable");
    ContentKey third = ContentKey.of("a", "thirdTable");
    ContentKey fourth = ContentKey.of("a", "b", "fourthTable");
    ContentKey fifth = ContentKey.of("a", "boo", "fifthTable");
    List<ContentKey> keys = ImmutableList.of(first, second, third, fourth, fifth);
    for (int i = 0; i < 5; i++) {
      getApi()
          .commitMultipleOperations()
          .branch(branch)
          .operation(Put.of(keys.get(i), IcebergTable.of("path" + i, 42, 42, 42, 42)))
          .commitMeta(CommitMeta.fromMessage("commit " + i))
          .commit();
    }

    List<Entry> entries =
        getApi().getEntries().refName(branch.getName()).namespaceDepth(0).get().getEntries();
    assertThat(entries).isNotNull().hasSize(5);

    entries =
        getApi()
            .getEntries()
            .refName(branch.getName())
            .namespaceDepth(0)
            .filter("entry.namespace.matches('a(\\\\.|$)')")
            .get()
            .getEntries();
    assertThat(entries).isNotNull().hasSize(5);

    entries =
        getApi()
            .getEntries()
            .refName(branch.getName())
            .namespaceDepth(1)
            .filter("entry.namespace.matches('a(\\\\.|$)')")
            .get()
            .getEntries();
    assertThat(entries).hasSize(1);
    assertThat(entries.stream().map(e -> e.getName().toPathString()))
        .containsExactlyInAnyOrder("a");

    entries =
        getApi()
            .getEntries()
            .refName(branch.getName())
            .namespaceDepth(2)
            .filter("entry.namespace.matches('a(\\\\.|$)')")
            .get()
            .getEntries();
    assertThat(entries).hasSize(3);
    assertThat(entries.stream().map(e -> e.getName().toPathString()))
        .containsExactlyInAnyOrder("a.thirdTable", "a.b", "a.boo");

    entries =
        getApi()
            .getEntries()
            .refName(branch.getName())
            .namespaceDepth(3)
            .filter("entry.namespace.matches('a\\\\.b(\\\\.|$)')")
            .get()
            .getEntries();
    assertThat(entries).hasSize(2);
    assertThat(entries.stream().map(e -> e.getName().toPathString()))
        .containsExactlyInAnyOrder("a.b.c", "a.b.fourthTable");

    entries =
        getApi()
            .getEntries()
            .refName(branch.getName())
            .namespaceDepth(4)
            .filter("entry.namespace.matches('a\\\\.b\\\\.c(\\\\.|$)')")
            .get()
            .getEntries();
    assertThat(entries).hasSize(2);
    assertThat(entries.stream().map(e -> e.getName().toPathString()))
        .containsExactlyInAnyOrder("a.b.c.firstTable", "a.b.c.secondTable");

    entries =
        getApi()
            .getEntries()
            .refName(branch.getName())
            .namespaceDepth(5)
            .filter("entry.namespace.matches('(\\\\.|$)')")
            .get()
            .getEntries();
    assertThat(entries).isEmpty();

    entries =
        getApi()
            .getEntries()
            .refName(branch.getName())
            .namespaceDepth(3)
            .filter("entry.namespace.matches('(\\\\.|$)')")
            .get()
            .getEntries();
    assertThat(entries).hasSize(3);
    assertThat(entries.get(2))
        .matches(e -> e.getType().equals(Type.UNKNOWN))
        .matches(e -> e.getName().equals(ContentKey.of("a", "b", "c")));
    assertThat(entries.get(1))
        .matches(e -> e.getType().equals(Type.ICEBERG_TABLE))
        .matches(e -> e.getName().equals(ContentKey.of("a", "b", "fourthTable")));
    assertThat(entries.get(0))
        .matches(e -> e.getType().equals(Type.ICEBERG_TABLE))
        .matches(e -> e.getName().equals(ContentKey.of("a", "boo", "fifthTable")));
  }
}