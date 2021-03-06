/*
 * #%L
 * vertx-pojo-mapper-mysql
 * %%
 * Copyright (C) 2017 Braintags GmbH
 * %%
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * #L%
 */

package de.braintags.vertx.jomnigate.mysql.dataaccess;

import de.braintags.vertx.jomnigate.dataaccess.query.impl.AbstractQueryResult;
import de.braintags.vertx.jomnigate.mapping.IMapper;
import de.braintags.vertx.jomnigate.mysql.MySqlDataStore;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;

/**
 * The {@link SqlQueryResult} contains the {@link ResultSet} from a query, by which the entities are created
 * 
 * @param <T>
 *          the type of the mapper, which is handled here
 * @author Michael Remme
 * 
 */
public class SqlQueryResult<T> extends AbstractQueryResult<T> {
  private ResultSet resultSet;

  /**
   * Creates a lazy loading instance
   * 
   * @param resultSet
   *          The {@link ResultSet} of an executed query
   * @param store
   *          the store which was used to execute the query
   * @param mapper
   *          the underlaying mapper
   * @param query
   *          the {@link SqlQueryRambler}
   */
  public SqlQueryResult(ResultSet resultSet, MySqlDataStore store, IMapper mapper, SqlExpression expression) {
    super(store, mapper, resultSet.getNumRows(), expression);
    this.resultSet = resultSet;
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.vertx.jomnigate.dataaccess.query.impl.AbstractQueryResult#generatePojo(int,
   * io.vertx.core.Handler)
   */
  @Override
  protected void generatePojo(int i, Handler<AsyncResult<T>> handler) {
    JsonObject sourceObject = resultSet.getRows().get(i);
    SqlStoreObjectFactory sf = (SqlStoreObjectFactory) getDataStore().getStoreObjectFactory();
    sf.createStoreObject(sourceObject, getMapper(), result -> {
      if (result.failed()) {
        handler.handle(Future.failedFuture(result.cause()));
      } else {
        @SuppressWarnings("unchecked")
        T pojo = result.result().getEntity();
        handler.handle(Future.succeededFuture(pojo));
      }
    });
  }

  /**
   * @return the result set of this query result
   */
  public ResultSet getResultSet() {
    return resultSet;
  }
}
