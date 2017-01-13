/*-
 * #%L
 * vertx-pojo-mapper-json
 * %%
 * Copyright (C) 2017 Braintags GmbH
 * %%
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * #L%
 */

package de.braintags.io.vertx.pojomapper.impl;

import de.braintags.io.vertx.pojomapper.mapping.IMapper;
import de.braintags.io.vertx.pojomapper.mapping.datastore.ITableInfo;
import de.braintags.io.vertx.pojomapper.mapping.datastore.impl.DefaultTableGenerator;

/**
 * 
 * 
 * @author Michael Remme
 * 
 */

public class DummyTableGenerator extends DefaultTableGenerator {

  /**
   * 
   */
  public DummyTableGenerator() {
    String test = "test";
  }

  @Override
  public ITableInfo createTableInfo(IMapper mapper) {
    return new DummyTableInfo(mapper);
  }

}
