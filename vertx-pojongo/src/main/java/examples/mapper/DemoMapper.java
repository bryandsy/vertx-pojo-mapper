/*
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
package examples.mapper;

import de.braintags.vertx.jomnigate.annotation.Entity;
import de.braintags.vertx.jomnigate.annotation.field.Embedded;
import de.braintags.vertx.jomnigate.annotation.field.Id;
import de.braintags.vertx.jomnigate.annotation.field.Referenced;
import de.braintags.vertx.jomnigate.dataaccess.query.IIndexedField;
import de.braintags.vertx.jomnigate.dataaccess.query.impl.IndexedField;

@Entity
public class DemoMapper {

  public static final IIndexedField NAME = new IndexedField("name");

  @Id
  public String id;
  private String name;
  @Embedded
  public DemoSubMapper subMapperEmbedded;
  @Referenced
  public DemoSubMapper subMapperReferenced;

  /**
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * @param name
   *          the name to set
   */
  public void setName(String name) {
    this.name = name;
  }

}
