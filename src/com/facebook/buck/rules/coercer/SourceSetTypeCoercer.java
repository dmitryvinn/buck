/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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

package com.facebook.buck.rules.coercer;

import com.facebook.buck.core.cell.nameresolver.CellNameResolver;
import com.facebook.buck.core.model.TargetConfiguration;
import com.facebook.buck.core.path.ForwardRelativePath;
import com.facebook.buck.core.sourcepath.SourcePath;
import com.facebook.buck.core.sourcepath.UnconfiguredSourcePath;
import com.facebook.buck.io.filesystem.ProjectFilesystem;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.TypeToken;
import java.util.List;

/** Coerce to {@link com.facebook.buck.rules.coercer.SourceSet}. */
public class SourceSetTypeCoercer extends SourceSetConcatable
    implements TypeCoercer<Object, SourceSet> {
  private final TypeCoercer<ImmutableSet<UnconfiguredSourcePath>, ImmutableSet<SourcePath>>
      unnamedHeadersTypeCoercer;
  private final TypeCoercer<
          ImmutableMap<String, UnconfiguredSourcePath>, ImmutableMap<String, SourcePath>>
      namedHeadersTypeCoercer;

  SourceSetTypeCoercer(
      TypeCoercer<String, String> stringTypeCoercer,
      TypeCoercer<UnconfiguredSourcePath, SourcePath> sourcePathTypeCoercer) {
    this.unnamedHeadersTypeCoercer = new SetTypeCoercer<>(sourcePathTypeCoercer);
    this.namedHeadersTypeCoercer = new MapTypeCoercer<>(stringTypeCoercer, sourcePathTypeCoercer);
  }

  @Override
  public TypeToken<SourceSet> getOutputType() {
    return TypeToken.of(SourceSet.class);
  }

  @Override
  public TypeToken<Object> getUnconfiguredType() {
    return TypeToken.of(Object.class);
  }

  @Override
  public boolean hasElementClass(Class<?>... types) {
    return unnamedHeadersTypeCoercer.hasElementClass(types)
        || namedHeadersTypeCoercer.hasElementClass(types);
  }

  @Override
  public void traverse(CellNameResolver cellRoots, SourceSet object, Traversal traversal) {
    switch (object.getType()) {
      case UNNAMED:
        unnamedHeadersTypeCoercer.traverse(cellRoots, object.getUnnamedSources().get(), traversal);
        break;
      case NAMED:
        namedHeadersTypeCoercer.traverse(cellRoots, object.getNamedSources().get(), traversal);
        break;
    }
  }

  @Override
  public Object coerceToUnconfigured(
      CellNameResolver cellRoots,
      ProjectFilesystem filesystem,
      ForwardRelativePath pathRelativeToProjectRoot,
      Object object)
      throws CoerceFailedException {
    return object;
  }

  @Override
  public SourceSet coerce(
      CellNameResolver cellRoots,
      ProjectFilesystem filesystem,
      ForwardRelativePath pathRelativeToProjectRoot,
      TargetConfiguration targetConfiguration,
      TargetConfiguration hostConfiguration,
      Object object)
      throws CoerceFailedException {
    if (object instanceof List) {
      return SourceSet.ofUnnamedSources(
          unnamedHeadersTypeCoercer.coerceBoth(
              cellRoots,
              filesystem,
              pathRelativeToProjectRoot,
              targetConfiguration,
              hostConfiguration,
              object));
    } else {
      return SourceSet.ofNamedSources(
          namedHeadersTypeCoercer.coerceBoth(
              cellRoots,
              filesystem,
              pathRelativeToProjectRoot,
              targetConfiguration,
              hostConfiguration,
              object));
    }
  }
}
