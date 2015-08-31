/*
 * Copyright 2015 Braintags GmbH
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution.
 * 
 * The Eclipse Public License is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * You may elect to redistribute this code under this licenses.
 */

package de.braintags.io.vertx.pojomapper.dataaccess.query.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

/**
 * All elements of a query, which contain this interface will be called to fill a {@link IQueryRambler}
 * 
 * @author Michael Remme
 * 
 */

public interface IRamblerSource {

  /**
   * apply the current instance to the {@link IQueryRambler}
   * 
   * @param rambler
   */
  void applyTo(IQueryRambler ramblerHandler, Handler<AsyncResult<Void>> resultHandler);
}
