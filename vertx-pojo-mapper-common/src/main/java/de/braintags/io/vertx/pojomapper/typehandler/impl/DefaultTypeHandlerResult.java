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

package de.braintags.io.vertx.pojomapper.typehandler.impl;

import de.braintags.io.vertx.pojomapper.typehandler.ITypeHandlerResult;

/**
 * Default implementation
 * 
 * @author Michael Remme
 * 
 */

public class DefaultTypeHandlerResult implements ITypeHandlerResult {
  private Object thResult;

  /**
   * 
   */
  public DefaultTypeHandlerResult() {
  }

  /**
   * 
   */
  public DefaultTypeHandlerResult(Object result) {
    this.thResult = result;
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.io.vertx.pojomapper.typehandler.ITypeHandlerResult#setResult(java.lang.Object)
   */
  @Override
  public void setResult(Object result) {
    this.thResult = result;
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.io.vertx.pojomapper.typehandler.ITypeHandlerResult#getResult()
   */
  @Override
  public Object getResult() {
    return thResult;
  }

}
