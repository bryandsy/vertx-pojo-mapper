/*-
 * #%L
 * vertx-pojongo
 * %%
 * Copyright (C) 2017 Braintags GmbH
 * %%
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * #L%
 */
package de.braintags.vertx.jomnigate.mongo.typehandler;

import de.braintags.vertx.jomnigate.json.typehandler.JsonTypeHandlerFactory;
import de.braintags.vertx.jomnigate.json.typehandler.handler.GeoPointTypeHandlerJson;

/**
 * 
 * 
 * @author Michael Remme
 * 
 */
public class MongoTypeHandlerFactory extends JsonTypeHandlerFactory {

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.vertx.jomnigate.json.typehandler.JsonTypeHandlerFactory#init()
   */
  @Override
  protected void init() {
    super.init();
    remove(GeoPointTypeHandlerJson.class);
    add(new GeoPointTypeHandlerMongo(this));
  }

}
