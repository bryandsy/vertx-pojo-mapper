/*
 * Copyright 2014 Red Hat, Inc.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 * 
 * The Eclipse Public License is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * The Apache License v2.0 is available at
 * http://www.opensource.org/licenses/apache2.0.php
 * 
 * You may elect to redistribute this code under either of these licenses.
 */

package de.braintags.io.vertx.pojomapper.dataaccess;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

/**
 * IWrite is responsible for all write actions into the connected datasource. It performs inserts and updates.
 * 
 * @author Michael Remme
 * 
 */

public interface IWrite<T> extends IDataAccessObject<T> {

  /**
   * Add an entity to be saved
   * 
   * @param mapper
   *          the mapper to be saved
   */
  public void add(final T mapper);

  /**
   * SAve the entities inside the current instance
   * 
   * @param resultHandler
   *          a handler, which will receive information about the save result
   */
  public void save(Handler<AsyncResult<IWriteResult>> resultHandler);

  interface IWriteResult {

  }
}