/*
* #%L
 * vertx-pojo-mapper-common
 * %%
 * Copyright (C) 2017 Braintags GmbH
 * %%
 * All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * #L%
*/
package de.braintags.vertx.jomnigate.mapping.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

import de.braintags.vertx.jomnigate.annotation.Entity;
import de.braintags.vertx.jomnigate.annotation.Index;
import de.braintags.vertx.jomnigate.annotation.Indexes;
import de.braintags.vertx.jomnigate.annotation.KeyGenerator;
import de.braintags.vertx.jomnigate.annotation.VersionInfo;
import de.braintags.vertx.jomnigate.annotation.field.Referenced;
import de.braintags.vertx.jomnigate.annotation.lifecycle.AfterDelete;
import de.braintags.vertx.jomnigate.annotation.lifecycle.AfterLoad;
import de.braintags.vertx.jomnigate.annotation.lifecycle.AfterSave;
import de.braintags.vertx.jomnigate.annotation.lifecycle.BeforeDelete;
import de.braintags.vertx.jomnigate.annotation.lifecycle.BeforeLoad;
import de.braintags.vertx.jomnigate.annotation.lifecycle.BeforeSave;
import de.braintags.vertx.jomnigate.dataaccess.query.IIndexedField;
import de.braintags.vertx.jomnigate.dataaccess.query.IdField;
import de.braintags.vertx.jomnigate.exception.MappingException;
import de.braintags.vertx.jomnigate.mapping.IIdInfo;
import de.braintags.vertx.jomnigate.mapping.IIndexDefinition;
import de.braintags.vertx.jomnigate.mapping.IKeyGenerator;
import de.braintags.vertx.jomnigate.mapping.IMapper;
import de.braintags.vertx.jomnigate.mapping.IMapperFactory;
import de.braintags.vertx.jomnigate.mapping.IMethodProxy;
import de.braintags.vertx.jomnigate.mapping.IProperty;
import de.braintags.vertx.jomnigate.mapping.datastore.IColumnHandler;
import de.braintags.vertx.jomnigate.mapping.datastore.ITableGenerator;
import de.braintags.vertx.jomnigate.mapping.datastore.ITableInfo;
import de.braintags.vertx.jomnigate.observer.IObserverHandler;
import de.braintags.vertx.jomnigate.observer.ObserverEventType;
import de.braintags.vertx.jomnigate.versioning.IMapperVersion;
import de.braintags.vertx.util.ClassUtil;
import de.braintags.vertx.util.exception.InitException;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;

/**
 * This implementation of {@link IMapper} is using the bean convention to define fields, which shall be mapped. It is
 * first reading all public, non transient fields, then the bean-methods ( public getter/setter ). The way of mapping
 * can be defined by adding several annotations to the field
 *
 * @author Michael Remme
 * @param <T>
 *          the class of the underlaying mapper
 */

public abstract class AbstractMapper<T> implements IMapper<T> {
  private static final io.vertx.core.logging.Logger LOGGER = io.vertx.core.logging.LoggerFactory
      .getLogger(AbstractMapper.class);

  /**
   * all annotations which shall be examined for the mapper class itself
   */
  protected static final List<Class<? extends Annotation>> LIFECYCLE_ANNOTATIONS = Arrays.asList(AfterDelete.class,
      AfterLoad.class, AfterSave.class, BeforeDelete.class, BeforeLoad.class, BeforeSave.class);
  /**
   * all annotations which shall be examined for the mapper class itself
   */
  protected static final List<Class<? extends Annotation>> CLASS_ANNOTATIONS = Arrays.asList(Indexes.class,
      KeyGenerator.class);

  private final Map<String, IProperty> mappedProperties = new HashMap<>();
  private final Map<Class<? extends Annotation>, IProperty[]> propertyCache = new HashMap<>();
  private final Class<T> mapperClass;
  private final IMapperFactory mapperFactory;
  private IKeyGenerator keyGenerator;
  private IIdInfo idInfo;
  private Entity entity;
  private VersionInfo versionInfo;
  private ImmutableSet<IIndexDefinition> indexes;
  private ITableInfo tableInfo;
  private boolean syncNeeded = true;
  private boolean hasReferencedFields = false;
  private IObserverHandler observerHandler;

  /**
   * Class annotations which were found inside the current definition
   */
  private final Map<Class<? extends Annotation>, Annotation> existingClassAnnotations = new HashMap<>();

  /**
   * Methods which are life-cycle events. Per event there can be several methods defined
   */
  private final Map<Class<? extends Annotation>, List<IMethodProxy>> lifecycleMethods = new HashMap<>();

  public AbstractMapper(final Class<T> mapperClass, final IMapperFactory mapperFactory) {
    this.mapperFactory = mapperFactory;
    this.mapperClass = mapperClass;
    init();
  }

  /**
   * Initialize the mapping process
   */
  protected void init() {
    if (LOGGER.isDebugEnabled())
      LOGGER.debug("examining " + getMapperClass().getName());
    computePersistentFields();
    computeLifeCycleAnnotations();
    computeClassAnnotations();
    computeEntity();
    computeVersionInfo();
    computeKeyGenerator();
    generateTableInfo();
    computeIndexes();
    checkReferencedFields();
    observerHandler = IObserverHandler.createInstance(this);
    internalValidate();
  }

  /**
   * Validations, which are not overwritable
   */
  private final void internalValidate() {
    if (idInfo == null)
      throw new MappingException("No id-field specified in mapper " + getMapperClass().getName());
    if (getVersionInfo() != null && !IMapperVersion.class.isAssignableFrom(getMapperClass())) {
      throw new MappingException(
          "Mapper, where the property Entity.version is set must implement the interface IMapperVersion");
    }
    if (getVersionInfo() != null && !getVersionInfo().eventType().equals(ObserverEventType.AFTER_LOAD)
        && !getVersionInfo().eventType().equals(ObserverEventType.BEFORE_UPDATE)) {
      throw new MappingException("VersionConverter can only be handled at phase AFTER_LOAD or BEFORE_UPDATE; mapper: "
          + getMapperClass().getName());
    }
    validate();
  }

  /**
   * Validation for required properties etc
   */
  protected abstract void validate();

  /**
   * Compute all fields, which shall be persisted
   */
  protected abstract void computePersistentFields();

  /**
   * Checks wether fields of the mapper or of children of the mapper are annotated with {@link Referenced}
   */
  protected void checkReferencedFields() {
    hasReferencedFields = false;
    for (IProperty field : getMappedProperties().values()) {
      if (field.hasAnnotation(Referenced.class)) {
        hasReferencedFields = true;
        return;
      }
    }
  }

  protected void generateTableInfo() {
    if (getMapperFactory().getDataStore() != null) {
      ITableGenerator tg = getMapperFactory().getDataStore().getTableGenerator();
      this.tableInfo = tg.createTableInfo(this);
      for (String fn : getFieldNames()) {
        IProperty field = getField(fn);
        IColumnHandler ch = tg.getColumnHandler(field);
        this.tableInfo.createColumnInfo(field, ch);
      }
    }
  }

  protected void computeVersionInfo() {
    if (mapperClass.isAnnotationPresent(VersionInfo.class)) {
      versionInfo = mapperClass.getAnnotation(VersionInfo.class);
    }
  }

  protected void computeEntity() {
    if (mapperClass.isAnnotationPresent(Entity.class)) {
      entity = mapperClass.getAnnotation(Entity.class);
    }
  }

  protected void computeIndexes() {
    Map<String, IIndexDefinition> definitions = new HashMap<>();
    if (getMapperClass().isAnnotationPresent(Indexes.class)) {
      Indexes tmpIndexes = getMapperClass().getAnnotation(Indexes.class);
      for (Index index : tmpIndexes.value()) {
        IndexDefinition indexDefinition = new IndexDefinition(index);
        IIndexDefinition old = definitions.put(indexDefinition.getIdentifier(), indexDefinition);
        if (old != null) {
          throw new IllegalStateException("duplicate index definition:" + indexDefinition);
        }
      }
    }
    computeIndexes(definitions);
    this.indexes = ImmutableSet.copyOf(definitions.values());
  }

  /**
   * @param definitions
   */
  private void computeIndexes(final Map<String, IIndexDefinition> definitions) {
    Field[] fields = getMapperClass().getFields();
    for (Field field : fields) {
      computeIndexByField(definitions, field);
    }
  }

  /**
   * @param definitions
   * @param field
   */
  private void computeIndexByField(final Map<String, IIndexDefinition> definitions, final Field field) {
    int modifiers = field.getModifiers();
    Class<?> type = field.getType();
    if (Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers) && IIndexedField.class.isAssignableFrom(type)
        && !IdField.class.isAssignableFrom(type)) {
      try {
        IIndexedField indexedField = (IIndexedField) field.get(null);
        IndexDefinition indexDefinition = new IndexDefinition(indexedField, this);
        if (definitions.containsKey(indexDefinition.getIdentifier())) {
          assert indexDefinition.getIndexOptions()
              .isEmpty() : "if indexed fields define index options, incompatibility must be checked here";
          LOGGER
              .info("Didn't add index definition because there already is one for its identifier: " + indexDefinition);
        } else
          definitions.put(indexDefinition.getIdentifier(), indexDefinition);
      } catch (IllegalArgumentException | IllegalAccessException e) {
        throw new InitException(e);
      }
    }
  }

  protected void computeKeyGenerator() {
    if (getMapperFactory().getDataStore() != null) {
      KeyGenerator gen = getAnnotation(KeyGenerator.class);
      if (gen != null) {
        String name = gen.value();
        keyGenerator = getMapperFactory().getDataStore().getKeyGenerator(name);
      } else {
        keyGenerator = getMapperFactory().getDataStore().getDefaultKeyGenerator();
      }
    }
  }

  protected final void computeClassAnnotations() {
    for (Class<? extends Annotation> annClass : CLASS_ANNOTATIONS) {
      Annotation ann = mapperClass.getAnnotation(annClass);
      if (ann != null)
        existingClassAnnotations.put(annClass, ann);
    }
  }

  /**
   * Computes the methods, which are annotated with the lifecycle annotations like {@link BeforeLoad}
   */
  protected final void computeLifeCycleAnnotations() {
    List<Method> methods = ClassUtil.getDeclaredAndInheritedMethods(mapperClass);
    for (Method method : methods) {
      for (Class<? extends Annotation> ann : LIFECYCLE_ANNOTATIONS) {
        if (method.isAnnotationPresent(ann)) {
          addLifecycleAnnotationMethod(ann, method);
        }
      }
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see de.braintags.vertx.jomnigate.mapping.IMapper#getFieldNames()
   */
  @Override
  public Set<String> getFieldNames() {
    return this.mappedProperties.keySet();
  }

  /*
   * (non-Javadoc)
   *
   * @see de.braintags.vertx.jomnigate.mapping.IMapper#getField(java.lang.String)
   */
  @Override
  public IProperty getField(final String name) {
    IProperty field = this.mappedProperties.get(name);
    if (field == null)
      throw new de.braintags.vertx.jomnigate.exception.NoSuchFieldException(this, name);
    return field;
  }

  /*
   * (non-Javadoc)
   *
   * @see de.braintags.vertx.jomnigate.mapping.IMapper#getAnnotatedFields(java.lang.Class)
   */
  @Override
  public IProperty[] getAnnotatedFields(final Class<? extends Annotation> annotationClass) {
    if (!this.propertyCache.containsKey(annotationClass)) {
      IProperty[] result = new IProperty[0];
      for (IProperty field : this.mappedProperties.values()) {
        if (field.getAnnotation(annotationClass) != null) {
          IProperty[] newArray = new IProperty[result.length + 1];
          System.arraycopy(result, 0, newArray, 0, result.length);
          result = newArray;
          result[result.length - 1] = field;
        }
      }
      this.propertyCache.put(annotationClass, result);
    }
    return this.propertyCache.get(annotationClass);
  }

  /*
   * (non-Javadoc)
   *
   * @see de.braintags.vertx.jomnigate.mapping.IMapper#executeLifecycle(java.lang.Class, java.lang.Object)
   */
  @Override
  public void executeLifecycle(final Class<? extends Annotation> annotationClass, final T entity,
      final Handler<AsyncResult<Void>> handler) {
    if (LOGGER.isDebugEnabled())
      LOGGER.debug("start executing Lifecycle " + annotationClass.getSimpleName());
    List<IMethodProxy> methods = getLifecycleMethods(annotationClass);
    if (methods == null || methods.isEmpty()) {
      if (LOGGER.isDebugEnabled())
        LOGGER.debug("nothing to execute");
      handler.handle(Future.succeededFuture());
    } else {
      executeLifecycleMethods(entity, handler, methods);
    }
  }

  /**
   * @param entity
   * @param handler
   * @param methods
   */
  private void executeLifecycleMethods(final Object entity, final Handler<AsyncResult<Void>> handler,
      final List<IMethodProxy> methods) {
    CompositeFuture cf = CompositeFuture.all(createFutureList(entity, methods));
    cf.setHandler(res -> {
      if (res.failed()) {
        handler.handle(Future.failedFuture(res.cause()));
      } else {
        handler.handle(Future.succeededFuture());
      }
    });
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private List<Future> createFutureList(final Object entity, final List<IMethodProxy> methods) {
    List<Future> fl = new ArrayList<>();
    for (IMethodProxy mp : methods) {
      Future f = Future.future();
      if (LOGGER.isDebugEnabled())
        LOGGER
            .debug("execute lifecycle method: " + getMapperClass().getSimpleName() + " - " + mp.getMethod().getName());
      executeMethod(mp, entity, f.completer());
      fl.add(f);
    }
    return fl;
  }

  /**
   * Execute the trigger method. IMPORTANT: if a TriggerContext is created, the handler is informed by the
   * TriggerContext, if not, then the handler is informed by this method
   * 
   * @param mp
   * @param entity
   * @param handler
   */
  private void executeMethod(final IMethodProxy mp, final Object entity, final Handler<AsyncResult<Void>> handler) {
    Method method = mp.getMethod();
    method.setAccessible(true);
    Object[] args = mp.getParameterTypes() == null ? null
        : new Object[] {
            getMapperFactory().getDataStore().getTriggerContextFactory().createTriggerContext(this, handler) };
    try {
      if (LOGGER.isDebugEnabled())
        LOGGER.debug("invoking trigger method " + getMapperClass().getSimpleName() + " - " + method.getName());
      method.invoke(entity, args);
      if (args == null) {
        // ONLY INFORM HANDLER, if no TriggerContext is given
        handler.handle(Future.succeededFuture());
      }
      if (LOGGER.isDebugEnabled())
        LOGGER.debug("trigger method invokement finished");
    } catch (Exception e) {
      handler.handle(Future.failedFuture(e));
    }
  }

  protected final void addLifecycleAnnotationMethod(final Class<? extends Annotation> ann, final Method method) {
    List<IMethodProxy> lcMethods = lifecycleMethods.get(ann);
    if (lcMethods == null) {
      lcMethods = new ArrayList<>();
      lifecycleMethods.put(ann, lcMethods);
    }
    MethodProxy mp = new MethodProxy(method, this);
    if (!lcMethods.contains(mp)) {
      lcMethods.add(mp);
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see de.braintags.vertx.jomnigate.mapping.IMapper#getMapperClass()
   */
  @Override
  public final Class<T> getMapperClass() {
    return mapperClass;
  }

  /*
   * (non-Javadoc)
   *
   * @see de.braintags.vertx.jomnigate.mapping.IMapper#getLifecycleMethods(java.lang.Class)
   */
  @Override
  public final List<IMethodProxy> getLifecycleMethods(final Class<? extends Annotation> annotation) {
    return lifecycleMethods.get(annotation);
  }

  /*
   * (non-Javadoc)
   *
   * @see de.braintags.vertx.jomnigate.mapping.IMapper#getAnnotation(java.lang.Class)
   */
  @Override
  public <U extends Annotation> U getAnnotation(final Class<U> annotationClass) {
    return (U) existingClassAnnotations.get(annotationClass);
  }

  @Override
  public ITableInfo getTableInfo() {
    return tableInfo;
  }

  @Override
  public final IIdInfo getIdInfo() {
    return idInfo;
  }

  protected void setIdInfo(final IIdInfo idInfo) {
    this.idInfo = idInfo;
  }

  /*
   * (non-Javadoc)
   *
   * @see de.braintags.vertx.jomnigate.mapping.IMapper#getKeyGenerator()
   */
  @Override
  public IKeyGenerator getKeyGenerator() {
    return keyGenerator;
  }

  /**
   * Returns true if at least one field of the mapper is annotated with {@link Referenced}
   * 
   * @return
   */
  @Override
  public boolean hasReferencedFields() {
    return hasReferencedFields;
  }

  /*
   * (non-Javadoc)
   *
   * @see de.braintags.vertx.jomnigate.mapping.IMapper#isSyncNeeded()
   */
  @Override
  public final boolean isSyncNeeded() {
    return syncNeeded;
  }

  /*
   * (non-Javadoc)
   *
   * @see de.braintags.vertx.jomnigate.mapping.IMapper#setSyncNeeded(boolean)
   */
  @Override
  public final void setSyncNeeded(final boolean syncNeeded) {
    this.syncNeeded = syncNeeded;
  }

  /*
   * (non-Javadoc)
   *
   * @see de.braintags.vertx.jomnigate.mapping.IMapper#getEntity()
   */
  @Override
  public Entity getEntity() {
    return this.entity;
  }

  @Override
  public VersionInfo getVersionInfo() {
    return versionInfo;
  }

  @Override
  public ImmutableSet<IIndexDefinition> getIndexDefinitions() {
    return indexes;
  }

  /**
   * Get the {@link MapperFactory} which created the current instance
   *
   * @return
   */
  @Override
  public IMapperFactory getMapperFactory() {
    return this.mapperFactory;
  }

  protected Map<String, IProperty> getMappedProperties() {
    return mappedProperties;
  }

  @Override
  public IObserverHandler getObserverHandler() {
    return this.observerHandler;
  }
}
